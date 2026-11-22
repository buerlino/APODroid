package io.github.buerlino.apodroid

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import io.github.buerlino.apodroid.core.WeekLogNames
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/**
 * The app's one log helper (LOG): every line goes to logcat under the tag "APODroid" and, while
 * the pref `weekLog` is on (turned on only by adb: `am start -n …/.MainActivity --ez weekLog
 * true`, off with `false`), also to `Download/APODroid/log.txt` through `MediaStore.Downloads`
 * (no permission on 10+), each line with the local time and the process id. Written on one
 * background thread in the calls' order; a write error is dropped (logcat only), so the log
 * never fails a run. At about [LIMIT] bytes the older half is dropped. A name a stale row holds
 * (after `pm clear` or a reinstall) is skipped for the next, `log (1).txt` and so on (B31).
 */
object AppLog {
    private const val TAG = "APODroid"
    private const val FOLDER = "Download/APODroid/"
    private const val LIMIT = 1_000_000L
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "AppLog").apply { isDaemon = true } }

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var resolver: ContentResolver? = null
    /** The file's URI, found or made once per process (on [writer] only). */
    private var uri: Uri? = null
    /** The first name not found taken in this process ([WeekLogNames], B31; on [writer] only). */
    private var nextName = 0

    fun i(message: String) { Log.i(TAG, message); toFile('I', message, null) }
    fun w(message: String, error: Throwable? = null) { Log.w(TAG, message, error); toFile('W', message, error) }
    fun e(message: String, error: Throwable? = null) { Log.e(TAG, message, error); toFile('E', message, error) }

    /** At each process start ([App]): remembers the prefs and the resolver and, while on, writes [startLine]. */
    fun start(context: Context) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences("apodroid", Context.MODE_PRIVATE)
        resolver = app.contentResolver
        if (prefs?.getBoolean("weekLog", false) == true) writer.execute { i(startLine(app)) }
    }

    /** The adb switch (MainActivity's extra `weekLog`): writes the pref, then a line saying so. */
    fun turn(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences("apodroid", Context.MODE_PRIVATE)
        if (value) {
            prefs.edit { putBoolean("weekLog", true) }
            i("Week log: on")
            writer.execute { i(startLine(context.applicationContext)) }
        } else {
            i("Week log: off")
            prefs.edit { putBoolean("weekLog", false) }
        }
    }

    private fun toFile(level: Char, message: String, error: Throwable?) {
        val resolver = resolver ?: return
        try {
            if (prefs?.getBoolean("weekLog", false) != true) return
            val line = buildString {
                append(LocalDateTime.now().format(stamp)).append(' ').append(Process.myPid()).append(' ')
                append(level).append(' ').append(message)
                if (error != null) append('\n').append(Log.getStackTraceString(error).trimEnd())
                append('\n')
            }
            writer.execute { write(resolver, line) }
        } catch (e: Exception) {
            Log.w(TAG, "Week log: couldn't write: $e")
        }
    }

    private fun write(resolver: ContentResolver, line: String) {
        try {
            val file = uri ?: find(resolver) ?: make(resolver) ?: return
            uri = file
            // One "rw" descriptor for the size, the trim and the append: a row MediaStore has just
            // made has no file on disk yet, and only an open for writing makes it (a first open
            // for reading failed with ENOENT, fix round 2). Appended by the channel's position,
            // not by "wa", which not every Android's MediaStore honours.
            resolver.openFileDescriptor(file, "rw")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).channel.use { out ->
                    var size = out.size()
                    if (size >= LIMIT) {
                        val text = ByteArray(size.toInt())
                        val buffer = ByteBuffer.wrap(text)
                        FileInputStream(pfd.fileDescriptor).channel.let { input ->
                            input.position(0)
                            while (buffer.hasRemaining() && input.read(buffer) >= 0) Unit
                        }
                        // The newer half, from a line's start.
                        var from = text.size / 2
                        while (from < text.size && text[from - 1] != '\n'.code.toByte()) from++
                        out.truncate(0)
                        out.position(0)
                        out.write(ByteBuffer.wrap(text, from, text.size - from))
                        size = out.size()
                    }
                    out.position(size)
                    val bytes = ByteBuffer.wrap(line.toByteArray())
                    while (bytes.hasRemaining()) out.write(bytes)
                }
            }
        } catch (e: Exception) {
            // Gone (deleted in a file manager): made again at the next line.
            uri = null
            Log.w(TAG, "Week log: couldn't write: $e")
        }
    }

    /**
     * A new log in the folder (B31): `log.txt`, else the next name when a row the app can't see
     * holds it (`pm clear`, "Clear storage" or a reinstall left it; MediaStore renames an insert
     * only when the file is on disk, so a row without one fails it with `UNIQUE constraint failed:
     * files._data`, on every line). Null when it couldn't (written to logcat once).
     */
    private fun make(resolver: ContentResolver): Uri? {
        if (nextName >= WeekLogNames.TRIES) return null
        val from = nextName
        var tried = ""
        val made = WeekLogNames.make(from, { name ->
            tried = name
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER)
                },
            )
        }) { it is SQLiteConstraintException || it.message?.contains("UNIQUE constraint failed") == true }
        if (made == null) {
            if (tried == WeekLogNames.name(WeekLogNames.TRIES - 1)) {
                // Every name taken (or the last one's insert gave null): no more tries in this process.
                nextName = WeekLogNames.TRIES
                Log.w(TAG, "Week log: couldn't write: ${WeekLogNames.TRIES} names taken")
            }
            return null
        }
        nextName = made.index
        if (made.index > from) i("Week log: ${WeekLogNames.name(from)} held by an old row, writing ${WeekLogNames.name(made.index)}")
        return made.value
    }

    /**
     * The app's own log in the folder, if there is one (an app sees only its own files): `log.txt`
     * or the renamed `log (1).txt` a reinstall leaves, the newest first.
     */
    private fun find(resolver: ContentResolver): Uri? =
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND (${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR " +
                "(${MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'log (%' AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%.txt'))",
            arrayOf(FOLDER, WeekLogNames.name(0)),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { if (it.moveToFirst()) ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it.getLong(0)) else null }

    /** One line, so a week's file reads without `dumpsys`. */
    private fun startLine(context: Context): String = try {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val bucket = when (val b = context.getSystemService(UsageStatsManager::class.java).appStandbyBucket) {
            5 -> "EXEMPTED"
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
            50 -> "NEVER"
            else -> b.toString()
        }
        val optimised = !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        val store = Store(context)
        "Process start: APODroid $version, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
            "${Build.MANUFACTURER} ${Build.MODEL}, bucket $bucket, battery optimisation ${if (optimised) "on" else "off"}, " +
            "Keep running ${onOff(store.keepRunning)}, ${setupLine(store)}"
    } catch (e: Exception) {
        "Process start: couldn't read the state: $e"
    }

    private fun setupLine(store: Store): String {
        val s = store.setup
        val columns = s.columns.entries.joinToString(" ") { (screen, sources) ->
            "${screen.name.lowercase()} ${sources.joinToString("+").ifEmpty { "-" }}"
        }
        return "setup: daily ${onOff(store.daily)}, ${if (s.allSources) "list" else "APOD mode"}, $columns, " +
            "every ${s.interval.hours} h from ${"%02d:00".format(s.pictureAt)}, as soon as it's out ${onOff(s.soon)}, " +
            "size ${s.size}, video ${s.videoDays}, darken ${onOff(s.darken)}, sharp ${onOff(s.sharp)}, " +
            "order ${s.order.joinToString(",")}, parts ${s.parts.size}"
    }

    private fun onOff(value: Boolean) = if (value) "on" else "off"
}

/** The process's start (LOG): the week log's line. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.start(this)
    }
}
