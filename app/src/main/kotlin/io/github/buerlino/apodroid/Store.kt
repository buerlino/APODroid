package io.github.buerlino.apodroid

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Display
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.buerlino.apodroid.core.HttpStatus
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.SiteChanged
import io.github.buerlino.apodroid.core.StorageFailed
import io.github.buerlino.apodroid.core.Source
import io.github.buerlino.apodroid.core.download
import io.github.buerlino.apodroid.core.fetchLatest
import io.github.buerlino.apodroid.core.nextTurn
import io.github.buerlino.apodroid.core.originalUrl
import io.github.buerlino.apodroid.core.renditionUrl
import io.github.buerlino.apodroid.core.wallpaperFrame as coreWallpaperFrame
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean

enum class Where(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/** What the wallpaper shows on an APOD video day: the previous picture, another source's newest, or the user's own. */
enum class VideoDays(val source: Source? = null) { KEEP, NASA(Source.NASA), EARTH(Source.EARTH), MINE }

/** How long each source's turn lasts with two or more sources ticked. */
enum class Interval(val hours: Long) { DAY(24), H12(12), H6(6), H3(3) }

/** What [Store.save] did. */
enum class Saved {
    /** The original, or it was saved before. */
    ORIGINAL,
    /** The original didn't load: the downloaded picture instead. */
    RENDITION,
    /** Nothing: a newer post was stored meanwhile. */
    STALE,
    /** Nothing: another save is still running (also one started before a rotation). */
    BUSY,
}

/** Why the last check failed, for the page to name. */
sealed interface Problem {
    data object Network : Problem
    data class Http(val code: Int) : Problem
    data object SiteChanged : Problem
    data object Storage : Problem
}

/**
 * Settings and each source's newest post in SharedPreferences, the pictures in the app's private
 * files. APOD's keys have no prefix (as before 0.5.0), the others `nasa.` and `earth.`.
 */
class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences("apodroid", Context.MODE_PRIVATE)

    /** [source]'s picture: `apod.jpg`, `nasa.jpg`, `earth.jpg` (whatever the format). */
    fun imageFile(source: Source) = File(context.filesDir, "${source.name.lowercase()}.jpg")

    val fallbackFile = File(context.filesDir, "fallback.jpg")

    /** Sent with each request; the version comes from `versionName` in app/build.gradle.kts. */
    private val userAgent = "APODroid/${context.packageManager.getPackageInfo(context.packageName, 0).versionName}" +
        " (+https://github.com/buerlino/APODroid)"

    var where: Where
        get() = enumValue(prefs.getString("where", null)) ?: Where.BOTH
        set(value) = prefs.edit { putString("where", value.name) }

    var videoDays: VideoDays
        get() = enumValue(prefs.getString("videoDays", null)) ?: VideoDays.KEEP
        set(value) = prefs.edit { putString("videoDays", value.name) }

    /**
     * The ticked sources, in order, at least one; APOD alone by default. With two or more they
     * take turns; going from one to more starts the current picture's turn, so ticking doesn't
     * end it at once.
     */
    var sources: List<Source>
        get() {
            val names = prefs.getString("sources", null)?.split(',').orEmpty()
            return Source.entries.filter { it.name in names }.ifEmpty { listOf(Source.APOD) }
        }
        set(value) {
            if (sources.size == 1 && value.size > 1) turnStartedAt = Instant.now()
            prefs.edit { putString("sources", Source.entries.filter { it in value }.joinToString(",")) }
        }

    /** How long a turn lasts. */
    var interval: Interval
        get() = enumValue(prefs.getString("interval", null)) ?: Interval.DAY
        set(value) = prefs.edit { putString("interval", value.name) }

    /** The user hid the dialog saying NASA and Earth Observatory post on weekdays only. */
    var weekdayNoticeHidden: Boolean
        get() = prefs.getBoolean("weekdayNoticeHidden", false)
        set(value) = prefs.edit { putBoolean("weekdayNoticeHidden", value) }

    /** When the source on the wallpaper got its turn: it changed, or the user set it. */
    var turnStartedAt: Instant?
        get() = instant("turnStartedAt")
        private set(value) = prefs.edit { putLong("turnStartedAt", value?.toEpochMilli() ?: 0) }

    /**
     * Whether the user turned the daily change on. Hibernation or a force stop deletes the job,
     * so the page schedules it again when this is on.
     */
    var daily: Boolean
        get() = prefs.getBoolean("daily", false)
        set(value) = prefs.edit { putBoolean("daily", value) }

    /** The user hid the hint about Android pausing the app, accepting the warning. */
    var pauseHintHidden: Boolean
        get() = prefs.getBoolean("pauseHintHidden", false)
        set(value) = prefs.edit { putBoolean("pauseHintHidden", value) }

    /** The video-day notice was dismissed for this date (stored as text), and it's back on the next video day. */
    var videoNoticeDate: String?
        get() = prefs.getString("videoNoticeDate", null)
        set(value) = prefs.edit { putString("videoNoticeDate", value) }

    /** The user hid the video-day notice for good. */
    var videoNoticeHidden: Boolean
        get() = prefs.getBoolean("videoNoticeHidden", false)
        set(value) = prefs.edit { putBoolean("videoNoticeHidden", value) }

    /** The problem the user dismissed, as kind and day (`http:503:2026-10-16`); a different kind, code or day shows it again. */
    var problemNoticeKey: String?
        get() = prefs.getString("problemNoticeKey", null)
        set(value) = prefs.edit { putString("problemNoticeKey", value) }

    /** The stored APOD. */
    val apod: Post?
        get() = post(Source.APOD)

    /** [source]'s newest post, whose picture is in [imageFile]; APOD video days have none. */
    fun post(source: Source): Post? {
        val date = prefs.getString(source.key("date"), null) ?: return null
        return Post(
            source = source,
            date = LocalDate.parse(date),
            title = prefs.getString(source.key("title"), null).orEmpty(),
            imageUrl = prefs.getString(source.key("imageUrl"), null),
            pageUrl = prefs.getString(source.key("pageUrl"), null).orEmpty(),
            isVideo = prefs.getBoolean(source.key("isVideo"), false),
            explanation = prefs.getString(source.key("explanation"), null).orEmpty(),
        )
    }

    private fun store(post: Post) = prefs.edit {
        val source = post.source
        putString(source.key("date"), post.date.toString())
        putString(source.key("title"), post.title)
        putString(source.key("imageUrl"), post.imageUrl)
        putString(source.key("pageUrl"), post.pageUrl)
        putBoolean(source.key("isVideo"), post.isVideo)
        putString(source.key("explanation"), post.explanation)
    }

    /**
     * Whose picture was last set as wallpaper, with [wallpaperDate] and [wallpaperUrl], so each
     * post is set once. The user's own picture counts as APOD's (it stands in for the APOD video).
     */
    var wallpaperSource: Source
        get() = enumValue(prefs.getString("wallpaperSource", null)) ?: Source.APOD
        private set(value) = prefs.edit { putString("wallpaperSource", value.name) }

    /** The date of the post last set as wallpaper. */
    var wallpaperDate: LocalDate?
        get() = prefs.getString("wallpaperDate", null)?.let(LocalDate::parse)
        private set(value) = prefs.edit { putString("wallpaperDate", value?.toString()) }

    /** The picture URL of the post last set as wallpaper; none for the user's own picture ([Post.isNoted]). */
    var wallpaperUrl: String?
        get() = prefs.getString("wallpaperUrl", null)
        private set(value) = prefs.edit { putString("wallpaperUrl", value) }

    /** When [setWallpaper] last worked (the job, the page or a button). */
    var wallpaperSetAt: Instant?
        get() = instant("wallpaperSetAt")
        private set(value) = prefs.edit { putLong("wallpaperSetAt", value?.toEpochMilli() ?: 0) }

    /** When a check of [source] last worked: a refresh, or finding today's post already stored. */
    fun checkedAt(source: Source): Instant? = instant(source.key("checkedAt"))

    /** What the last check of [source] ran into, or null when it worked. */
    fun problem(source: Source): Problem? = when (prefs.getString(source.key("problem"), null)) {
        "network" -> Problem.Network
        "http" -> Problem.Http(prefs.getInt(source.key("httpCode"), 0))
        "site" -> Problem.SiteChanged
        "storage" -> Problem.Storage
        else -> null
    }

    /**
     * Calls [action] when a set, a stored post or a save changes, also the job's or one started
     * before a rotation (they run in this process), until [stopWatching]. Android holds the listener weakly: keep the one returned.
     */
    fun watch(action: () -> Unit) = OnSharedPreferenceChangeListener { _, key ->
        if (key == "wallpaperSetAt" || Source.entries.any { key in listOf(it.key("date"), it.key("imageUrl"), it.key("title"), it.key("explanation"), it.key("savedUri"), it.key("problem"), it.key("httpCode")) }) {
            action()
        }
    }.also(prefs::registerOnSharedPreferenceChangeListener)

    fun stopWatching(listener: OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private fun instant(key: String) = prefs.getLong(key, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /**
     * Makes the next [update] set the stored picture again (the daily switch turned on), which
     * starts its turn.
     */
    fun forgetWallpaper() {
        wallpaperDate = null
        turnStartedAt = Instant.now()
    }

    /** True when [source]'s stored post is today's (the dates are US Eastern). */
    fun isCurrent(source: Source): Boolean =
        post(source)?.let { it.date == LocalDate.now(ZoneId.of("America/New_York")) && isComplete(it) } == true

    /** Its picture is there, or it needs none (an APOD video day). */
    private fun isComplete(post: Post) = post.isVideo || imageFile(post.source).exists()

    /**
     * Blocking. Asks for [source]'s newest post and, when it's a new one, downloads its picture
     * as tall as the screen ([downloadPicture]). The stored post only changes once the picture is
     * complete and decodable. An APOD video day has no picture: the previous one is deleted. An
     * older post than the stored one (a stale cache) is ignored. A picture that isn't one is
     * remembered (`badUrl`) and not downloaded again while it's the newest post's. The page and
     * the daily job may call it at the same time, hence the lock, which also makes sources
     * refresh one at a time.
     */
    fun refresh(source: Source): Unit = synchronized(refreshLock) {
        val latest = fetchLatest(source, userAgent)
        val stored = post(source)
        if (stored != null && latest.date < stored.date && isComplete(stored)) return
        val url = latest.imageUrl
        val file = imageFile(source)
        if (url != null && (latest.date != stored?.date || url != stored.imageUrl || !file.exists())) {
            // Else every page open, job run and retry would download it again (13 MB, say).
            if (url == prefs.getString(source.key("badUrl"), null)) throw SiteChanged("Not an image, not downloaded again: $url")
            val part = File(context.filesDir, "${source.name.lowercase()}.part")
            try {
                downloadPicture(latest, url, part)
                if (!isImage(part)) throw SiteChanged("Not an image: $url")
            } catch (e: SiteChanged) {
                part.delete()
                prefs.edit { putString(source.key("badUrl"), url) }
                throw e
            }
            if (!part.renameTo(file)) {
                part.delete()
                throw StorageFailed(IOException("Couldn't rename ${part.name}"))
            }
            prefs.edit { remove(source.key("badUrl")) }
        }
        store(latest)
        // After storing: a page drawing meanwhile shows the video entry, never the old picture under it.
        if (url == null) file.delete()
    }

    /**
     * [url] scaled by the host to the screen's height, which fills the screen without the
     * original's size (NASA's go up to 97 MB). If that fails, APOD and Earth Observatory fall
     * back to [url] as is (1280 px for APOD); NASA's is the original, so it fails.
     */
    private fun downloadPicture(post: Post, url: String, part: File) {
        val rendition = renditionUrl(url, screen.height)
        try {
            download(rendition, part, userAgent)
            if (isImage(part)) return
            throw SiteChanged("Not an image: $rendition")
        } catch (e: IOException) {
            // A stopped job's interrupted download ends the run, not falls back (the interrupt flag is cleared by then).
            if (e is StorageFailed || e.isStop() || post.source == Source.NASA || rendition == url) throw e
            Log.w("APODroid", "Rendition failed, plain picture instead: $e")
        }
        download(url, part, userAgent)
    }

    /** The source chosen for APOD video days; only while APOD is ticked alone. */
    private val videoDaySource: Source?
        get() = videoDays.source.takeIf { sources == listOf(Source.APOD) }

    /**
     * Blocking. What the page and the daily job both run: [refresh] each source in use unless its
     * stored post is today's (always with [force], pull to refresh) or, with [skipRecent] (a page
     * open), it was checked within the hour ([checkedRecently]), then, while the daily change
     * is on, sets [wallpaperFile] once per source and date. The sources in use are the ticked
     * ones, and with APOD alone on an APOD video day the one chosen for video days. A failing
     * source doesn't stop the others, and its stored picture still counts; the first failure is
     * thrown after setting (the job retries). Returns what it did for the log, or null when there
     * was nothing to set.
     */
    fun update(force: Boolean, skipRecent: Boolean = false): String? {
        var failure: Exception? = null
        for (source in sources + listOfNotNull(videoDaySource)) {
            // The video-day source only on a video day: after APOD's refresh, which tells.
            if (source == videoDaySource && apod?.isVideo != true) continue
            // A stopped job (DailyJob.onStopJob) ends the run: no more downloads, no set.
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            try {
                if (force || !skipRecent || !checkedRecently(source)) checkSite(source, force)
            } catch (e: Exception) {
                if (e.isStop()) throw e
                failure = failure ?: e
            }
        }
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        val done = if (daily) setDue() else null
        if (failure != null) {
            // The caller logs only the failure.
            if (done != null) Log.i("APODroid", "Despite the failure: $done")
            throw failure
        }
        return done
    }

    /**
     * Checked less than an hour ago without a problem since, and its picture is there. Spares a
     * page open the feed and the JSON on weekends and before a post: a new one reaches an open
     * page up to an hour late. The job always checks (it runs hours apart; else a page open just
     * before a new post would hold the wallpaper back until its next run).
     */
    private fun checkedRecently(source: Source): Boolean {
        val at = checkedAt(source) ?: return false
        val now = Instant.now()
        return problem(source) == null && at <= now && Duration.between(at, now) < Duration.ofHours(1) &&
            post(source)?.let(::isComplete) == true
    }

    /** Sets [wallpaperFile] unless it's on the wallpaper already; under one lock, as the page and the job may both get here. */
    private fun setDue(): String? = synchronized(wallpaperLock) {
        val file = wallpaperFile()
        if (file == null) {
            val apod = apod
            return if (sources == listOf(Source.APOD) && apod?.isVideo == true) {
                "APOD ${apod.date}, video, wallpaper kept"
            } else {
                null
            }
        }
        val source = sourceOf(file)
        val post = post(source)
        if (source == wallpaperSource && post?.isNoted(wallpaperDate, wallpaperUrl) == true) return null
        setWallpaper(file, startTurn = false)
        "${source.name} ${post?.date}, wallpaper set"
    }

    /**
     * [refresh] unless [source]'s stored post is today's (always with [force]), noting how it
     * went for the page: a success sets [checkedAt] and clears [problem], a failure names it.
     * Anything but the three typed failures counts as the network, except an interrupted download (a stopped job), which notes nothing.
     */
    private fun checkSite(source: Source, force: Boolean) {
        try {
            if (force || !isCurrent(source)) refresh(source)
        } catch (e: Exception) {
            if (e.isStop()) throw e // not the site's fault
            prefs.edit {
                when (e) {
                    is SiteChanged -> putString(source.key("problem"), "site")
                    is StorageFailed -> putString(source.key("problem"), "storage")
                    is HttpStatus -> putString(source.key("problem"), "http").putInt(source.key("httpCode"), e.code)
                    else -> putString(source.key("problem"), "network")
                }
            }
            throw e
        }
        prefs.edit {
            putLong(source.key("checkedAt"), System.currentTimeMillis())
            remove(source.key("problem"))
            remove(source.key("httpCode"))
        }
    }

    /**
     * The picture the wallpaper should show, or null to keep the current one (video day with
     * "keep", nothing to use yet). With APOD ticked alone: APOD's, on a video day the chosen
     * source's or the user's own. Else the source whose turn it is. A
     * stored picture counts also when this run's check of its source failed.
     */
    fun wallpaperFile(): File? {
        val sources = sources
        if (sources != listOf(Source.APOD)) return turn(sources)?.let(::imageFile)
        if (apod?.isVideo != true) return imageFile(Source.APOD).takeIf { it.exists() }
        val source = videoDays.source
        return when {
            source != null -> imageFile(source).takeIf { it.exists() && post(source) != null }
            videoDays == VideoDays.MINE -> fallbackFile.takeIf { it.exists() }
            else -> null
        }
    }

    /** Whose turn it is ([nextTurn]). */
    private fun turn(sources: List<Source>): Source? =
        nextTurn(sources, wallpaperSource, turnStartedAt, Duration.ofHours(interval.hours), Instant.now(), ::hasPicture)

    /** [source]'s stored post has a picture; an APOD video has none. */
    private fun hasPicture(source: Source) = post(source)?.isVideo == false && imageFile(source).exists()

    /** Whose picture [file] is; the user's own (or none, a kept wallpaper) counts as APOD's. */
    private fun sourceOf(file: File?): Source = Source.entries.firstOrNull { imageFile(it) == file } ?: Source.APOD

    /** The screen's size in portrait, the shape of the wallpaper. */
    val screen: Size
        get() {
            val mode = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).mode
            return Size(minOf(mode.physicalWidth, mode.physicalHeight), maxOf(mode.physicalWidth, mode.physicalHeight))
        }

    /**
     * Where the wallpaper sits on [post]'s picture, from 0 (left or top edge) to 1 (right or
     * bottom edge): the middle unless the user moved it for this post.
     */
    fun cropPosition(post: Post?): Float {
        val source = post?.source ?: return 0.5f
        val date = prefs.getString(source.key("cropDate"), null)?.let(LocalDate::parse)
        return if (post.isNoted(date, prefs.getString(source.key("cropUrl"), null))) prefs.getFloat(source.key("cropPosition"), 0.5f) else 0.5f
    }

    fun setCropPosition(post: Post, position: Float) = prefs.edit {
        putString(post.source.key("cropDate"), post.date.toString())
        putString(post.source.key("cropUrl"), post.imageUrl)
        putFloat(post.source.key("cropPosition"), position)
    }

    /**
     * Blocking. Source and post are read first: setting takes seconds, and a refresh storing a
     * new post meanwhile must not mark the new one as set. The crop is [wallpaperFrame], the
     * screen's portrait shape: without one, Android 15+ shows the left part of a wide picture.
     * The user's own picture (video days) is always centred. Another source, or [startTurn] (a
     * set by the user), starts a turn.
     */
    fun setWallpaper(file: File, startTurn: Boolean = true): Unit = synchronized(wallpaperLock) {
        val source = sourceOf(file)
        val post = post(source)
        val own = file != imageFile(source)
        val position = if (own) 0.5f else cropPosition(post)
        val manager = WallpaperManager.getInstance(context)
        if (needsTurning(file)) {
            // Android would measure it unturned but draw it turned, stretching it (seen on API 36).
            val bitmap = decodeUpright(file, screen)
            manager.setBitmap(bitmap, wallpaperFrame(bitmap.width, bitmap.height, screen, position), true, where.flags)
        } else {
            val crop = imageBounds(file)?.let { wallpaperFrame(it.outWidth, it.outHeight, screen, position) }
            file.inputStream().use { manager.setStream(it, crop, true, where.flags) }
        }
        if (startTurn || source != wallpaperSource) turnStartedAt = Instant.now()
        wallpaperSource = source
        wallpaperDate = post?.date
        wallpaperUrl = if (own) null else post?.imageUrl
        wallpaperSetAt = Instant.now()
    }

    /** True when [source]'s stored picture is in the gallery (saved and not deleted since). */
    fun isSaved(source: Source): Boolean {
        val post = post(source) ?: return false
        val date = prefs.getString(source.key("savedDate"), null)?.let(LocalDate::parse)
        if (!post.isNoted(date, prefs.getString(source.key("savedUrl"), null))) return false
        val uri = prefs.getString(source.key("savedUri"), null)?.toUri() ?: return false
        return runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                ?.use { it.count > 0 } == true
        }.getOrDefault(false)
    }

    /**
     * Blocking. Downloads the original of [post]'s picture and copies it to Pictures/APODroid (no
     * permission on Android 10+), unless it's there already (a quick double tap); if the
     * original fails, the stored picture. Saves nothing when the stored post isn't [post] any
     * more: a refresh stored a new one meanwhile. The download runs outside
     * the lock, so a refresh doesn't wait for it. One save at a time: [Saved.BUSY] while another
     * runs.
     */
    fun save(post: Post): Saved {
        if (!saving.compareAndSet(false, true)) return Saved.BUSY
        var original: File? = null
        try {
            val url = post.takeIf(::isStored)?.let(::originalUrl) ?: return Saved.STALE
            original = File.createTempFile("original", ".part", context.cacheDir)
            val loaded = try {
                download(url, original, userAgent)
                isImage(original)
            } catch (e: IOException) {
                Log.w("APODroid", "Original failed, saving the stored picture: $e")
                false
            }
            return copyToGallery(post, if (loaded) original else imageFile(post.source), loaded)
        } finally {
            original?.delete()
            saving.set(false)
        }
    }

    /** [post]'s picture is still the stored one of its source (a changed title doesn't count). */
    private fun isStored(post: Post) = post(post.source)?.let { it.date == post.date && it.imageUrl == post.imageUrl } == true

    private fun copyToGallery(post: Post, file: File, original: Boolean): Saved = synchronized(refreshLock) {
        if (!isStored(post)) return Saved.STALE
        val source = post.source
        if (isSaved(source)) return Saved.ORIGINAL
        val type = imageType(file) ?: throw IOException("Not an image")
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${post.fileName}.${type.substringAfter('/').replace("jpeg", "jpg")}")
            put(MediaStore.Images.Media.MIME_TYPE, type)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/APODroid")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IOException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        prefs.edit {
            putString(source.key("savedDate"), post.date.toString())
            putString(source.key("savedUrl"), post.imageUrl)
            putString(source.key("savedUri"), uri.toString())
        }
        if (original) Saved.ORIGINAL else Saved.RENDITION
    }
}

/** [name]'s pref key for this source: as is for APOD (the keys before 0.5.0), else `nasa.<name>`, `earth.<name>`. */
private fun Source.key(name: String) = if (this == Source.APOD) name else "${this.name.lowercase()}.$name"

/** A stopped job's interrupted download ([download]); a read timeout is an [InterruptedIOException] too, but the network's. */
private fun Exception.isStop() = this is InterruptedIOException && this !is SocketTimeoutException

private val refreshLock = Any()

/** A [Store.save] is running; process-wide, as each page and job has its own [Store]. */
private val saving = AtomicBoolean()

/** Not [refreshLock]: holding that during a set would make ★ wait for seconds. */
private val wallpaperLock = Any()

fun isImage(file: File): Boolean = imageBounds(file) != null

/** True when the picture's EXIF orientation (2 to 8) says to turn or flip it, as camera photos often do. */
@SuppressLint("ExifInterface") // Its security bugs were in Android 7.0 and older; minSdk is 29.
private fun needsTurning(file: File): Boolean = runCatching {
    android.media.ExifInterface(file).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) > 1
}.getOrDefault(false)

/** [file] turned upright, halved as often as it still fills the [screen]. */
private fun decodeUpright(file: File, screen: Size): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        var sample = 1
        while (info.size.width / (sample * 2) >= screen.width && info.size.height / (sample * 2) >= screen.height) sample *= 2
        decoder.setTargetSampleSize(sample)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

/** The MIME type of a decodable image, e.g. `image/jpeg`, or null. */
private fun imageType(file: File): String? = imageBounds(file)?.outMimeType

/** Size and MIME type of a decodable image (`outWidth`, `outHeight`, `outMimeType`), or null. */
fun imageBounds(file: File): BitmapFactory.Options? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    return options.takeIf { it.outWidth > 0 && it.outHeight > 0 && it.outMimeType != null }
}

/** The part of a [width]×[height] picture the wallpaper shows on [screen] (`:core`'s maths). */
fun wallpaperFrame(width: Int, height: Int, screen: Size, position: Float): Rect =
    coreWallpaperFrame(width, height, screen.width, screen.height, position).run { Rect(left, top, right, bottom) }

private inline fun <reified T : Enum<T>> enumValue(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
