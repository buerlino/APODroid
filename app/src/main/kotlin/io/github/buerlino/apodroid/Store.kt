package io.github.buerlino.apodroid

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
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

    var where: Where
        get() = enumValue(prefs.getString("where", null)) ?: Where.BOTH
        set(value) = prefs.edit().putString("where", value.name).apply()

    var videoDays: VideoDays
        get() = enumValue(prefs.getString("videoDays", null)) ?: VideoDays.KEEP
        set(value) = prefs.edit().putString("videoDays", value.name).apply()

    /** The APOD whose picture is in [imageFile]. */
    var apod: Apod?
        get() {
            val date = prefs.getString("date", null) ?: return null
            return Apod(
                date = LocalDate.parse(date),
                title = prefs.getString("title", null).orEmpty(),
                imageUrl = prefs.getString("imageUrl", null).orEmpty(),
                pageUrl = prefs.getString("pageUrl", null).orEmpty(),
                isVideo = prefs.getBoolean("isVideo", false),
            )
        }
        private set(value) {
            prefs.edit()
                .putString("date", value?.date?.toString())
                .putString("title", value?.title)
                .putString("imageUrl", value?.imageUrl)
                .putString("pageUrl", value?.pageUrl)
                .putBoolean("isVideo", value?.isVideo ?: false)
                .apply()
        }

    /** True when the stored APOD is today's (APOD dates are US Eastern). */
    val isCurrent: Boolean
        get() = apod?.date == LocalDate.now(ZoneId.of("America/New_York")) && imageFile.exists()

    /**
     * Blocking. Asks for the newest APOD and, when its date is new, downloads its picture. The
     * stored APOD only changes once the picture is complete and decodable. Returns true when
     * it changed.
     */
    fun refresh(): Boolean {
        val latest = fetchLatest()
        val changed = latest.date != apod?.date || !imageFile.exists()
        if (changed) {
            val part = File(context.filesDir, "apod.part")
            download(latest.imageUrl, part)
            if (!isImage(part) || !part.renameTo(imageFile)) {
                part.delete()
                throw IOException("Not an image: ${latest.imageUrl}")
            }
        }
        apod = latest
        return changed
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
    }
}

fun isImage(file: File): Boolean {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    return options.outWidth > 0 && options.outHeight > 0
}

private inline fun <reified T : Enum<T>> enumValue(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
