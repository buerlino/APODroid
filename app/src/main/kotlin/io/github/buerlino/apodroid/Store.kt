package io.github.buerlino.apodroid

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.Display
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.buerlino.apodroid.core.Apod
import io.github.buerlino.apodroid.core.HttpStatus
import io.github.buerlino.apodroid.core.SiteChanged
import io.github.buerlino.apodroid.core.download
import io.github.buerlino.apodroid.core.fetchLatest
import io.github.buerlino.apodroid.core.wallpaperFrame as coreWallpaperFrame
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Where(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

enum class VideoDays { KEEP, MINE }

/** Why the last check failed, for the page to name. */
sealed interface Problem {
    data object Network : Problem
    data class Http(val code: Int) : Problem
    data object SiteChanged : Problem
}

/** Settings and the current APOD in SharedPreferences, the pictures in the app's private files. */
class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences("apodroid", Context.MODE_PRIVATE)
    val imageFile = File(context.filesDir, "apod.jpg")
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

    /** The APOD whose picture is in [imageFile]; on video days there's none. */
    val apod: Apod?
        get() {
            val date = prefs.getString("date", null) ?: return null
            return Apod(
                date = LocalDate.parse(date),
                title = prefs.getString("title", null).orEmpty(),
                imageUrl = prefs.getString("imageUrl", null),
                pageUrl = prefs.getString("pageUrl", null).orEmpty(),
                isVideo = prefs.getBoolean("isVideo", false),
                explanation = prefs.getString("explanation", null).orEmpty(),
            )
        }

    private fun store(apod: Apod) = prefs.edit {
        putString("date", apod.date.toString())
        putString("title", apod.title)
        putString("imageUrl", apod.imageUrl)
        putString("pageUrl", apod.pageUrl)
        putBoolean("isVideo", apod.isVideo)
        putString("explanation", apod.explanation)
    }

    /** The date of the APOD last set as wallpaper, so the daily job sets each one once. */
    var wallpaperDate: LocalDate?
        get() = prefs.getString("wallpaperDate", null)?.let(LocalDate::parse)
        private set(value) = prefs.edit { putString("wallpaperDate", value?.toString()) }

    /** When [setWallpaper] last worked (the job, the page or a button). */
    var wallpaperSetAt: Instant?
        get() = instant("wallpaperSetAt")
        private set(value) = prefs.edit { putLong("wallpaperSetAt", value?.toEpochMilli() ?: 0) }

    /** When a check last worked: a refresh, or finding today's APOD already stored. */
    val checkedAt: Instant?
        get() = instant("checkedAt")

    /** What the last check ran into, or null when it worked. */
    val problem: Problem?
        get() = when (prefs.getString("problem", null)) {
            "network" -> Problem.Network
            "http" -> Problem.Http(prefs.getInt("httpCode", 0))
            "site" -> Problem.SiteChanged
            else -> null
        }

    private fun instant(key: String) = prefs.getLong(key, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /** Makes the next [update] set the stored APOD again (the daily switch turned on). */
    fun forgetWallpaper() { wallpaperDate = null }

    /** True when the stored APOD is today's (APOD dates are US Eastern). */
    val isCurrent: Boolean
        get() = apod?.let { it.date == LocalDate.now(ZoneId.of("America/New_York")) && isComplete(it) } == true

    /** Its picture is there, or it needs none (a video day). */
    private fun isComplete(apod: Apod) = apod.isVideo || imageFile.exists()

    /**
     * Blocking. Asks for the newest APOD and, when its date is new, downloads its picture. The
     * stored APOD only changes once the picture is complete and decodable. A video day has no
     * picture: the previous one is deleted. An older post than the stored one (a stale cache) is
     * ignored. The page and the daily job may call it at the same time, hence the lock.
     */
    fun refresh(): Unit = synchronized(refreshLock) {
        val latest = fetchLatest(userAgent)
        val stored = apod
        if (stored != null && latest.date < stored.date && isComplete(stored)) return
        val url = latest.imageUrl
        if (url != null && (latest.date != stored?.date || !imageFile.exists())) {
            val part = File(context.filesDir, "apod.part")
            download(url, part, userAgent)
            if (!isImage(part) || !part.renameTo(imageFile)) {
                part.delete()
                throw SiteChanged("Not an image: $url")
            }
        }
        store(latest)
        // After storing: a page drawing meanwhile shows the video entry, never the old picture under it.
        if (url == null) imageFile.delete()
    }

    /**
     * Blocking. What the page and the daily job both run: [refresh] unless the stored APOD is
     * today's (always with [force], pull to refresh), then, while the daily change is on, sets the
     * stored APOD as wallpaper once per date. Returns what it did for the log, or null when there
     * was nothing to set.
     */
    fun update(force: Boolean): String? {
        checkSite(force)
        if (!daily) return null
        // Checked and set under one lock: the page and the job may both get here.
        return synchronized(wallpaperLock) {
            val date = apod?.date ?: return null
            if (date == wallpaperDate) return null
            val file = wallpaperFile()
            if (file != null) setWallpaper(file)
            "$date, ${if (file != null) "wallpaper set" else "video, wallpaper kept"}"
        }
    }

    /**
     * [refresh] unless the stored APOD is today's (always with [force]), noting how it went for
     * the page: a success sets [checkedAt] and clears [problem], a failure names it. Anything but
     * the two typed failures counts as the network.
     */
    private fun checkSite(force: Boolean) {
        try {
            if (force || !isCurrent) refresh()
        } catch (e: Exception) {
            prefs.edit {
                when (e) {
                    is SiteChanged -> putString("problem", "site")
                    is HttpStatus -> putString("problem", "http").putInt("httpCode", e.code)
                    else -> putString("problem", "network")
                }
            }
            throw e
        }
        prefs.edit {
            putLong("checkedAt", System.currentTimeMillis())
            remove("problem")
            remove("httpCode")
        }
    }

    /** The picture to set as wallpaper, or null to keep the current one (video day). */
    fun wallpaperFile(): File? = when {
        apod?.isVideo != true -> imageFile.takeIf { it.exists() }
        videoDays == VideoDays.MINE -> fallbackFile.takeIf { it.exists() }
        else -> null
    }

    /** The screen's size in portrait, the shape of the wallpaper. */
    val screen: Size
        get() {
            val mode = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).mode
            return Size(minOf(mode.physicalWidth, mode.physicalHeight), maxOf(mode.physicalWidth, mode.physicalHeight))
        }

    /**
     * Where the wallpaper sits on [date]'s picture, from 0 (left or top edge) to 1 (right or
     * bottom edge): the middle unless the user moved it for this date.
     */
    fun cropPosition(date: LocalDate?): Float =
        if (prefs.getString("cropDate", null) == date?.toString()) prefs.getFloat("cropPosition", 0.5f) else 0.5f

    fun setCropPosition(date: LocalDate, position: Float) = prefs.edit {
        putString("cropDate", date.toString())
        putFloat("cropPosition", position)
    }

    /**
     * Blocking. The date is read first: setting takes seconds, and a refresh storing a new APOD
     * meanwhile must not mark the new one as set. Without a crop, Android 15+ shows the left
     * part of a wide picture. The user's own picture (video days) is always centred.
     */
    fun setWallpaper(file: File): Unit = synchronized(wallpaperLock) {
        val date = apod?.date
        val position = if (file == imageFile) cropPosition(date) else 0.5f
        val manager = WallpaperManager.getInstance(context)
        if (needsTurning(file)) {
            // Android would measure it unturned but draw it turned, stretching it (seen on API 36).
            val bitmap = decodeUpright(file, screen)
            manager.setBitmap(bitmap, wallpaperFrame(bitmap.width, bitmap.height, screen, position), true, where.flags)
        } else {
            val crop = imageBounds(file)?.let { wallpaperFrame(it.outWidth, it.outHeight, screen, position) }
            file.inputStream().use { manager.setStream(it, crop, true, where.flags) }
        }
        wallpaperDate = date
        wallpaperSetAt = Instant.now()
    }

    /** True when the stored APOD's picture is in the gallery (saved and not deleted since). */
    val isSaved: Boolean
        get() {
            val date = apod?.date ?: return false
            if (prefs.getString("savedDate", null) != date.toString()) return false
            val uri = prefs.getString("savedUri", null)?.toUri() ?: return false
            return runCatching {
                context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                    ?.use { it.count > 0 } == true
            }.getOrDefault(false)
        }

    /**
     * Blocking. Copies the stored APOD's picture to Pictures/APODroid (no permission on Android
     * 10+), unless it's there already (a quick double tap). Returns false and saves nothing when
     * the stored APOD isn't [date]'s any more: a refresh stored a new one while this waited for
     * the lock.
     */
    fun save(date: LocalDate): Boolean = synchronized(refreshLock) {
        val apod = apod ?: throw IOException("Nothing to save")
        if (apod.date != date) return false
        if (isSaved) return true
        val type = imageType(imageFile) ?: throw IOException("Not an image")
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${apod.fileName}.${type.substringAfter('/').replace("jpeg", "jpg")}")
            put(MediaStore.Images.Media.MIME_TYPE, type)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/APODroid")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IOException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)!!.use { out -> imageFile.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        prefs.edit {
            putString("savedDate", apod.date.toString())
            putString("savedUri", uri.toString())
        }
        true
    }
}

private val refreshLock = Any()

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
