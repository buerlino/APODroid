package io.github.buerlino.apodroid

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.buerlino.apodroid.core.Apod
import io.github.buerlino.apodroid.core.download
import io.github.buerlino.apodroid.core.fetchLatest
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

enum class Where(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

enum class VideoDays { KEEP, MINE }

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
     * so the page schedules it again when this is on. Until 0.1.2 the switch was only the
     * scheduled job, so the first read takes it from there (migration).
     */
    var daily: Boolean
        get() {
            if (!prefs.contains("daily")) daily = DailyJob.isScheduled(context)
            return prefs.getBoolean("daily", false)
        }
        set(value) = prefs.edit { putBoolean("daily", value) }

    /** The user hid the hint about Android pausing the app, accepting the warning. */
    var pauseHintHidden: Boolean
        get() = prefs.getBoolean("pauseHintHidden", false)
        set(value) = prefs.edit { putBoolean("pauseHintHidden", value) }

    /** The APOD whose picture is in [imageFile]. */
    val apod: Apod?
        get() {
            val date = prefs.getString("date", null) ?: return null
            return Apod(
                date = LocalDate.parse(date),
                title = prefs.getString("title", null).orEmpty(),
                imageUrl = prefs.getString("imageUrl", null).orEmpty(),
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

    /** True when the stored APOD is today's (APOD dates are US Eastern). */
    val isCurrent: Boolean
        get() = apod?.date == LocalDate.now(ZoneId.of("America/New_York")) && imageFile.exists()

    /**
     * Blocking. Asks for the newest APOD and, when its date is new, downloads its picture. The
     * stored APOD only changes once the picture is complete and decodable. An older post than
     * the stored one (a stale cache) is ignored. The page and the daily job may call it at the
     * same time, hence the lock.
     */
    fun refresh(): Unit = synchronized(refreshLock) {
        val latest = fetchLatest(userAgent)
        val stored = apod
        if (stored != null && latest.date < stored.date && imageFile.exists()) return
        if (latest.date != stored?.date || !imageFile.exists()) {
            val part = File(context.filesDir, "apod.part")
            download(latest.imageUrl, part, userAgent)
            if (!isImage(part) || !part.renameTo(imageFile)) {
                part.delete()
                throw IOException("Not an image: ${latest.imageUrl}")
            }
        }
        store(latest)
    }

    /** The picture to set as wallpaper, or null to keep the current one (video day). */
    fun wallpaperFile(): File? = when {
        apod?.isVideo != true -> imageFile.takeIf { it.exists() }
        videoDays == VideoDays.MINE -> fallbackFile.takeIf { it.exists() }
        else -> null
    }

    /** Blocking. */
    fun setWallpaper(file: File) {
        file.inputStream().use { WallpaperManager.getInstance(context).setStream(it, null, true, where.flags) }
        wallpaperDate = apod?.date
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

fun isImage(file: File): Boolean = imageBounds(file) != null

/** The MIME type of a decodable image, e.g. `image/jpeg`, or null. */
private fun imageType(file: File): String? = imageBounds(file)?.outMimeType

/** Size and MIME type of a decodable image (`outWidth`, `outHeight`, `outMimeType`), or null. */
fun imageBounds(file: File): BitmapFactory.Options? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    return options.takeIf { it.outWidth > 0 && it.outHeight > 0 && it.outMimeType != null }
}

private inline fun <reified T : Enum<T>> enumValue(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
