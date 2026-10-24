package io.github.buerlino.apodroid

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Display
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.buerlino.apodroid.core.Frame
import io.github.buerlino.apodroid.core.HttpStatus
import io.github.buerlino.apodroid.core.OWN_PICTURE
import io.github.buerlino.apodroid.core.Picture
import io.github.buerlino.apodroid.core.PictureTime
import io.github.buerlino.apodroid.core.Plan
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.Record
import io.github.buerlino.apodroid.core.Restart
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.SiteChanged
import io.github.buerlino.apodroid.core.StandIn
import io.github.buerlino.apodroid.core.StorageFailed
import io.github.buerlino.apodroid.core.Source
import io.github.buerlino.apodroid.core.Stored
import io.github.buerlino.apodroid.core.TooLarge
import io.github.buerlino.apodroid.core.apodColumns
import io.github.buerlino.apodroid.core.screensOf
import io.github.buerlino.apodroid.core.download
import io.github.buerlino.apodroid.core.dueWallpapers
import io.github.buerlino.apodroid.core.fetchLatest
import io.github.buerlino.apodroid.core.blurBackground
import io.github.buerlino.apodroid.core.blurSize
import io.github.buerlino.apodroid.core.edgeCopySize
import io.github.buerlino.apodroid.core.edgesBlack
import io.github.buerlino.apodroid.core.fitFrame
import io.github.buerlino.apodroid.core.gray
import io.github.buerlino.apodroid.core.migrate
import io.github.buerlino.apodroid.core.nextAlarm as coreNextAlarm
import io.github.buerlino.apodroid.core.randomOffset
import io.github.buerlino.apodroid.core.originalUrl
import io.github.buerlino.apodroid.core.restartAndForget
import io.github.buerlino.apodroid.core.sourcesInUse
import io.github.buerlino.apodroid.core.toSave
import io.github.buerlino.apodroid.core.videoDayKept
import io.github.buerlino.apodroid.core.waiting
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

/** APOD mode's "Where": the screens APOD goes on. */
enum class Where(val screens: Set<Screen>) {
    HOME(setOf(Screen.HOME)),
    LOCK(setOf(Screen.LOCK)),
    BOTH(Screen.entries.toSet()),
}

/**
 * ⛶'s part of a source's post: the post's [date] and picture [url], where the frame sits
 * ([position], 0–1), and whether the wallpaper shows the [whole] picture instead (A3).
 */
data class Part(val date: LocalDate, val url: String?, val position: Float, val whole: Boolean = false)

/**
 * Everything "Set as wallpaper" saves at once ([Store.saveSetup]); the page's draft until then.
 * [list] is the list's columns, kept in APOD mode too; [parts] ⛶'s part per source; [pictureAt]
 * "New picture at", an hour (J2 stores it; the page shows it with J3, until then it passes the
 * stored one). Not the daily switch: it acts at once ([Store.switchDaily]).
 */
data class Setup(
    val allSources: Boolean,
    val where: Where,
    val list: Map<Screen, List<Source>>,
    val interval: Interval,
    val videoDays: VideoDays,
    val parts: Map<Source, Part>,
    val pictureAt: Int = DEFAULT_PICTURE_AT,
) {
    /** The columns this setup puts on the screens: APOD mode's from [where], else [list]. */
    val columns: Map<Screen, List<Source>>
        get() = if (allSources) list else apodColumns(where.screens)
}

/**
 * What [Store.update] did: [done] for the log (null: nothing was set), whether a set failed
 * ([setFailed]), the first failure, a fetch's or a set's ([failure]; the job retries), and
 * whether it went on to set ([wentToSet]: the daily switch was on at its check, or `set`).
 */
data class Updated(val done: String?, val setFailed: Boolean, val failure: Exception?, val wentToSet: Boolean)

/**
 * What fills APOD's turn on an APOD video day ("If APOD is a video", both modes): "Skip it"
 * ([KEEP], the pref's name from 0.5.0's "Keep"), another source's newest, or the user's own.
 */
enum class VideoDays(val standIn: StandIn, val source: Source? = standIn.source) {
    KEEP(StandIn.SKIP),
    NASA(StandIn.NASA),
    EARTH(StandIn.EARTH),
    MINE(StandIn.OWN),
}

/** "New picture at"'s default, 06:00 (an update from 0.5.0 gets it too). */
const val DEFAULT_PICTURE_AT = 6

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
 * What a screen shows for the page: its [picture] (null: never set or forgotten), when a set
 * last worked there ([setAt]), and whether an APOD video day kept its picture ([kept],
 * [videoDayKept]).
 */
data class ScreenState(val picture: Picture?, val setAt: Instant?, val kept: Boolean)

/**
 * Settings and each source's newest post in SharedPreferences, the pictures in the app's private
 * files. APOD's keys have no prefix (as before 0.5.0), the others `nasa.` and `earth.`. Each
 * screen's [record]: home's unprefixed (as before 0.6.0), the lock screen's `lock.`.
 */
class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences("apodroid", Context.MODE_PRIVATE)

    init {
        migrateOnce()
    }

    /**
     * 0.5.0's prefs → 0.6.0's ([migrate] in `:core`), once: at the first start of 0.6.0, the page's
     * or the job's, also on a fresh install. Under a lock shared by every [Store], the check
     * inside it and everything in one commit: the page and the job may both start first, and the
     * "Lock" case run twice would copy home's cleared record over the lock screen's.
     */
    private fun migrateOnce(): Unit = synchronized(migrationLock) {
        if (prefs.contains("allSources")) return
        val keys = listOf("sources", "where", "wallpaperSource", "wallpaperDate", "wallpaperUrl", "wallpaperSetAt", "turnStartedAt")
        val all = prefs.all
        val old = keys.mapNotNull { key -> all[key]?.let { key to it.toString() } }.toMap()
        val date = old["wallpaperDate"]
        val apodVideo = date != null && prefs.getString("date", null) == date && prefs.getBoolean("isVideo", false)
        val migration = migrate(old, apodVideo)
        prefs.edit(commit = true) {
            migration.remove.forEach(::remove)
            migration.write.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Long -> putLong(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        Log.i("APODroid", "Migrated from 0.5.0: $migration")
    }

    /** [source]'s picture: `apod.jpg`, `nasa.jpg`, `earth.jpg` (whatever the format). */
    fun imageFile(source: Source) = File(context.filesDir, "${source.name.lowercase()}.jpg")

    val fallbackFile = File(context.filesDir, "fallback.jpg")

    /** A "my picture" picked on the page, not set yet: Set renames it to [fallbackFile]. */
    val draftFallbackFile = File(context.filesDir, "fallback-draft.jpg")

    /** Sent with each request; the version comes from `versionName` in app/build.gradle.kts. */
    private val userAgent = "APODroid/${context.packageManager.getPackageInfo(context.packageName, 0).versionName}" +
        " (+https://github.com/buerlino/APODroid)"

    /** APOD mode's "Where" (the list has its own columns). */
    val where: Where
        get() = enumValue(prefs.getString("where", null)) ?: Where.BOTH

    val videoDays: VideoDays
        get() = enumValue(prefs.getString("videoDays", null)) ?: VideoDays.KEEP

    /** The switch "More NASA pictures": off is APOD mode ("Where"), on the list ([columns]). */
    val allSources: Boolean
        get() = prefs.getBoolean("allSources", false)

    /**
     * The list's columns (`home`, `lock`) whatever the mode, for the page's draft; before the
     * list was first saved, APOD on the screens "Where" names (the list starts from "Where").
     */
    val list: Map<Screen, List<Source>>
        get() = if (!prefs.contains("home")) apodColumns(where.screens) else Screen.entries.associateWith { screen ->
            val names = prefs.getString(screen.name.lowercase(), null)?.split(',').orEmpty()
            Source.entries.filter { it.name in names }
        }

    /** The list was saved once ([list]); before, it starts from "Where". */
    val hasList: Boolean
        get() = prefs.contains("home")

    /**
     * The sources ticked for each screen, in order: in APOD mode APOD on the screens "Where"
     * names, with the switch on the [list]. An empty column leaves its screen alone; one with two
     * or more takes turns.
     */
    val columns: Map<Screen, List<Source>>
        get() = if (!allSources) apodColumns(where.screens) else list

    /** A column takes turns: its change times are every [interval] from [pictureAt] ([PictureTime]). */
    val hasTurns: Boolean
        get() = columns.values.any { it.size >= 2 }

    /** The setup last set ("Set as wallpaper"), which the job follows. */
    val setup: Setup
        get() = Setup(allSources, where, list, interval, videoDays, parts, pictureAt)

    /**
     * Blocking. "Set as wallpaper": saves [setup] in one commit (the list with [putColumns], so
     * [lastScreens] follows; ⛶'s parts), the draft "my picture" renamed into place first when
     * this Set's own draft holds the pick ([pick]: a second page without one leaves the stored
     * picture and the first page's draft file alone, B11), then
     * [restartAndForget]: the records of the screens with a column are forgotten (the change
     * times kept), so the next [setDue] gives each screen what the setup says afresh (a changed
     * whole picture or "my picture" too, with the same picture). A screen whose column was empty starts
     * without a record. The daily switch isn't part of it ([switchDaily]). The caller then runs
     * [update] with `set` and arms the alarms ([DailyJob.ensure]) or cancels them as the stored
     * [daily] says.
     */
    fun saveSetup(setup: Setup, pick: Boolean): Unit = synchronized(wallpaperLock) {
        if (pick && draftFallbackFile.exists() && !draftFallbackFile.renameTo(fallbackFile)) Log.w("APODroid", "Couldn't rename ${draftFallbackFile.name}")
        val before = columns
        val restart = restartAndForget(before, setup.columns, records(), stored(), PictureTime(setup.pictureAt, setup.interval.hours.toInt()), ZoneId.systemDefault(), Instant.now())
        prefs.edit(commit = true) {
            putBoolean("allSources", setup.allSources)
            putString("where", setup.where.name)
            // Before the first time on, the list stays unsaved, so it still starts from "Where".
            if (setup.allSources || prefs.contains("home")) putColumns(setup.list, list)
            putString("interval", setup.interval.name)
            putInt("pictureAt", setup.pictureAt)
            putString("videoDays", setup.videoDays.name)
            for ((source, part) in setup.parts) {
                putString(source.key("cropDate"), part.date.toString())
                putString(source.key("cropUrl"), part.url)
                putFloat(source.key("cropPosition"), part.position)
                putBoolean(source.key("whole"), part.whole)
            }
            putRestart(restart)
        }
        setsSinceSetup = mutableMapOf()
    }

    /**
     * After Set's [update]: whether a screen was set since its [saveSetup] and whether one's last
     * set failed, by any run (a page's load or the job may reach [setDue] first and set the new
     * setup, so Set's own finds nothing due; CR 5: its toast said "Setup saved. Nothing to set yet.").
     */
    fun setsSinceSetup(): Pair<Boolean, Boolean> = synchronized(wallpaperLock) {
        setsSinceSetup.values.let { (true in it) to (false in it) }
    }

    /**
     * The daily switch, at once (not part of [Setup]): writes [daily] (the caller cancels the
     * jobs and the alarms, or arms them) and forgets the job's failed runs, both ways (off is off;
     * on starts afresh). Turning it on, the caller then runs [startAfresh] and [update] with `set`,
     * which counts as the last run's start (`lastRunAt`, B16): the checks before it aren't owed.
     */
    fun switchDaily(on: Boolean) {
        prefs.edit(commit = true) {
            putBoolean("daily", on)
            remove("failedRuns")
            remove("failedAt")
            if (on) putLong("lastRunAt", System.currentTimeMillis()) else remove("lastRunAt")
        }
    }

    /**
     * Blocking. The stored setup afresh, as Set does without saving anything (the daily switch
     * turned on): [restartAndForget] with the stored columns, so the records of the screens with a
     * column are forgotten; the next [setDue] sets each screen anew.
     */
    fun startAfresh(): Unit = synchronized(wallpaperLock) {
        val restart = restartAndForget(columns, columns, records(), stored(), pictureTime(), ZoneId.systemDefault(), Instant.now())
        prefs.edit(commit = true) { putRestart(restart) }
    }

    private fun SharedPreferences.Editor.putRestart(restart: Restart) {
        putPlan(restart.plan)
        restart.drop.forEach { remove(it.key("wallpaperSource")) }
    }

    /**
     * The list's columns, as comma lists (`home`, `lock`), and each source's screens for
     * [lastScreens]: where it is now, or for one unticked where it was in [before].
     */
    private fun SharedPreferences.Editor.putColumns(columns: Map<Screen, List<Source>>, before: Map<Screen, List<Source>>) {
        for (screen in Screen.entries) putString(screen.name.lowercase(), columns[screen].orEmpty().joinToString(",") { it.name })
        for (source in Source.entries) {
            val screens = columns.screensOf(source).ifEmpty { before.screensOf(source) }
            Where.entries.firstOrNull { it.screens == screens }?.let { putString(source.key("lastScreens"), it.name) }
        }
    }

    /**
     * The screens [source] had last in the list (`<source>.lastScreens`): its dropdown shows
     * them while it's unticked, and a tick puts it there. Both when it never had any.
     */
    fun lastScreens(source: Source): Set<Screen> =
        (enumValue<Where>(prefs.getString(source.key("lastScreens"), null)) ?: Where.BOTH).screens

    /** How long a turn lasts. */
    val interval: Interval
        get() = enumValue(prefs.getString("interval", null)) ?: Interval.DAY

    /** The user hid the dialog saying NASA and Earth Observatory post on weekdays only. */
    var weekdayNoticeHidden: Boolean
        get() = prefs.getBoolean("weekdayNoticeHidden", false)
        set(value) = prefs.edit { putBoolean("weekdayNoticeHidden", value) }

    /** "New picture at", an hour 0–23 (part of the [setup]; an update from 0.5.0 gets 06:00). */
    val pictureAt: Int
        get() = prefs.getInt("pictureAt", DEFAULT_PICTURE_AT).takeIf { it in 0..23 } ?: DEFAULT_PICTURE_AT

    /**
     * The change times ([PictureTime]): at [pictureAt], with turns every [interval] from it. 0.6.0's
     * `turnStartedAt` is no longer read (left in the prefs).
     */
    private fun pictureTime() = PictureTime(pictureAt, interval.hours.toInt())

    /**
     * This device's offset after the clock's checks ([randomOffset], 0–10 min), drawn once and
     * stored (`offset`, seconds), so a time zone's phones don't all ask at once.
     */
    private val offset: Duration
        get() = synchronized(migrationLock) {
            if (!prefs.contains("offset")) prefs.edit(commit = true) { putLong("offset", randomOffset().seconds) }
            Duration.ofSeconds(prefs.getLong("offset", 0))
        }

    /**
     * The job's run starts ([DailyJob]): notes when (`lastRunAt`), so a check that passes after it
     * and before the next run is armed at once ([nextAlarm], B16), not dropped by a re-arming.
     */
    fun runStarts() = prefs.edit(commit = true) { putLong("lastRunAt", System.currentTimeMillis()) }

    /**
     * The job's run ended ([DailyJob]): a good one forgets the failed runs; a failed or stopped one
     * counts them (`failedRuns`) and notes its end (`failedAt`), so the alarms retry 15 min later,
     * then hourly ([nextAlarm]), also when armed again before the retry (a Set, a page open).
     */
    fun ranJob(failed: Boolean) = prefs.edit(commit = true) {
        if (!failed) {
            remove("failedRuns")
            remove("failedAt")
        } else {
            putInt("failedRuns", prefs.getInt("failedRuns", 0) + 1)
            putLong("failedAt", System.currentTimeMillis())
        }
    }

    /**
     * When the alarms should start the next run ([nextAlarm]): the change times of the stored
     * setup, with their checks (15 min after a change time, then hourly in its window, hourly
     * while a screen waits) and this device's [offset], or a failed run's retry when that comes
     * first; at once when a check passed since the last run's start (`lastRunAt`, B16); while a run goes ([running]) no later than 15 min after its start (B16f1). Not under the wallpaper lock: the page's `onResume` and the alarms' receiver call it
     * on the main thread, and a set holds that lock for seconds.
     */
    fun nextAlarm(running: Boolean = false): Instant {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val time = pictureTime()
        return coreNextAlarm(now, zone, prefs.getInt("failedRuns", 0), instant("failedAt"), time, hasTurns, offset, waiting(columns, records(), time, zone, now), instant("lastRunAt"), running)
    }

    /**
     * Whether the user turned the daily change on. Hibernation or a force stop deletes the job,
     * so the page schedules it again when this is on.
     */
    val daily: Boolean
        get() = prefs.getBoolean("daily", false)

    /** Save every picture (F5): after each job run, each source in use's new post goes to the gallery ([saveInUse]). */
    var saveEvery: Boolean
        get() = prefs.getBoolean("saveEvery", false)
        set(value) = prefs.edit { putBoolean("saveEvery", value) }

    /**
     * "Largest original" (F6), in MB (0: no limit): the job's saves ([saveInUse]) don't download
     * an original over it and save the stored picture instead. ★ ignores it.
     */
    var saveLimit: Int
        get() = prefs.getInt("saveLimit", 20)
        set(value) = prefs.edit { putInt("saveLimit", value) }

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
            // APOD only; a post stored by 0.5.0 has none until the next one.
            credit = prefs.getString(source.key("credit"), null),
            // B15; a post stored before it has none and counts from its date's 00:00 US Eastern (Post.publishedAt).
            published = instant(source.key("published")),
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
        putString(source.key("credit"), post.credit)
        post.published?.let { putLong(source.key("published"), it.toEpochMilli()) } ?: remove(source.key("published"))
    }

    /**
     * What [screen] shows, so each post is set once: whose turn it is (`wallpaperSource`, the
     * name from 0.5.0), the picture (`wallpaperPicture`: a source's name with `wallpaperDate` and
     * `wallpaperUrl`, or [OWN_PICTURE]) and the turn's stamp (`wallpaperStamp`); null when the app
     * never set that screen, or Set started it afresh ([saveSetup]). Home's keys are unprefixed,
     * the lock screen's `lock.`.
     */
    fun record(screen: Screen): Record? {
        val owner = enumValue<Source>(prefs.getString(screen.key("wallpaperSource"), null)) ?: return null
        val picture = when (val noted = prefs.getString(screen.key("wallpaperPicture"), null)) {
            OWN_PICTURE -> Picture.Own
            else -> enumValue<Source>(noted)?.let { source ->
                prefs.getString(screen.key("wallpaperDate"), null)?.let {
                    Picture.Of(source, LocalDate.parse(it), prefs.getString(screen.key("wallpaperUrl"), null))
                }
            }
        }
        val shown = Source.entries.mapNotNull { source ->
            prefs.getString(screen.key(source.key("shownDate")), null)?.let {
                source to Picture.Of(source, LocalDate.parse(it), prefs.getString(screen.key(source.key("shownUrl")), null))
            }
        }.toMap()
        return Record(owner, picture, instant(screen.key("wallpaperStamp")), shown)
    }

    private fun records(): Map<Screen, Record> = Screen.entries.mapNotNull { s -> record(s)?.let { s to it } }.toMap()

    private fun SharedPreferences.Editor.putRecord(screen: Screen, record: Record) {
        putString(screen.key("wallpaperSource"), record.owner.name)
        val picture = record.picture
        putString(screen.key("wallpaperPicture"), if (picture is Picture.Of) picture.source.name else picture?.let { OWN_PICTURE })
        putString(screen.key("wallpaperDate"), (picture as? Picture.Of)?.date?.toString())
        putString(screen.key("wallpaperUrl"), (picture as? Picture.Of)?.url)
        putLong(screen.key("wallpaperStamp"), record.stamp?.toEpochMilli() ?: 0)
        // The last post shown per source (J1's rule 1: a change skips what this screen has shown).
        for (source in Source.entries) {
            val shown = record.shown[source]
            putString(screen.key(source.key("shownDate")), shown?.date?.toString())
            putString(screen.key(source.key("shownUrl")), shown?.url)
        }
    }

    /** Writes [plan]'s records without a set; the sets are [setDue]'s. */
    private fun write(plan: Plan) {
        if (plan.screens.isEmpty()) return
        prefs.edit(commit = true) { putPlan(plan) }
    }

    private fun SharedPreferences.Editor.putPlan(plan: Plan) {
        plan.screens.forEach { (screen, due) -> putRecord(screen, due.record) }
    }

    /**
     * The screen APOD mode's page reads: home's, unless only the lock screen has a column
     * ("Where" = Lock: after the migration home's record is empty there).
     */
    val pageScreen: Screen
        get() = columns.let { if (it[Screen.HOME].orEmpty().isEmpty() && it[Screen.LOCK].orEmpty().isNotEmpty()) Screen.LOCK else Screen.HOME }

    /** What the page says about [screen]: from its record, the chip, the opening tab and its status line. */
    fun screenState(screen: Screen): ScreenState {
        val record = record(screen)
        val setAt = instant(screen.key("wallpaperSetAt"))
        return ScreenState(record?.picture, setAt, videoDayKept(columns[screen].orEmpty(), record, stored(), setAt != null))
    }

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
        if (key in recordKeys || Source.entries.any { key in listOf(it.key("date"), it.key("imageUrl"), it.key("title"), it.key("explanation"), it.key("credit"), it.key("savedUri"), it.key("problem"), it.key("httpCode")) }) {
            action()
        }
    }.also(prefs::registerOnSharedPreferenceChangeListener)

    /** Both screens' records and when they were set: the open page follows a set on either screen. */
    private val recordKeys = Screen.entries.flatMap { screen ->
        listOf("wallpaperSource", "wallpaperPicture", "wallpaperDate", "wallpaperUrl", "wallpaperSetAt").map(screen::key)
    }.toSet()

    fun stopWatching(listener: OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private fun instant(key: String) = prefs.getLong(key, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

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

    /**
     * Blocking. What the page, Set and the daily job run: [refresh] each source in use and each
     * of [show] (the sources the page offers, so one ticked in its draft only has a picture)
     * unless its stored post is today's (always with [force], pull to refresh) or, for the [page]
     * (an open, a pull, Set), it was checked within the hour ([checkedRecently]), then, while the
     * daily change is on or with [set] (Set, also with it off), sets what each screen is due
     * ([setDue]). The sources in use: the ones ticked on any screen, and on an APOD video day the
     * stand-in, while APOD is ticked somewhere. A failing source doesn't stop the others, and its
     * stored picture still counts. Only a stop is thrown; every other failure is in the result
     * (the job retries).
     */
    fun update(force: Boolean, page: Boolean = false, show: Collection<Source> = emptyList(), set: Boolean = false): Updated {
        var failure: Exception? = null
        for (source in Source.entries) {
            // Asked for each source: APOD's refresh (first) tells whether the stand-in is in use.
            if (source !in show && source !in inUse()) continue
            // A stopped job (DailyJob.onStopJob) ends the run: no more downloads, no set.
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            try {
                if (force || !page || !checkedRecently(source)) checkSite(source, force)
            } catch (e: Exception) {
                if (e.isStop()) throw e
                failure = failure ?: e
            }
        }
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        if (!daily && !set) return Updated(null, false, failure, wentToSet = false)
        val (done, setFailure) = setDue(pull = force)
        if (failure != null && done != null) Log.i("APODroid", "Despite the failure: $done")
        return Updated(done, setFailure != null, failure ?: setFailure, wentToSet = true)
    }

    /** The sources in use ([sourcesInUse]): what the job fetches and [saveInUse] saves. */
    private fun inUse() = sourcesInUse(columns, videoDays.source, apod?.isVideo == true)

    /**
     * Blocking, the job's, after each run but a stopped one, while [saveEvery] is on: [save]s
     * each source in use's stored post, as ★ does ([toSave]: not a video, nor one noted as saved,
     * even if deleted in the gallery since). One skipped while ★ saves ([Saved.BUSY]) or failed
     * is tried by the next run; a stop ends it (thrown). The original once per post: the try is
     * noted first (`saveTried`, its picture URL), so after a stop the next job save is the stored
     * picture. An original over [saveLimit] isn't downloaded in full: the stored picture instead,
     * no retry. Returns what it saved for the log, or null.
     */
    fun saveInUse(): String? {
        if (!saveEvery) return null
        val saved = Source.entries.mapNotNull { source ->
            prefs.getString(source.key("savedDate"), null)?.let { source to Picture.Of(source, LocalDate.parse(it), prefs.getString(source.key("savedUrl"), null)) }
        }.toMap()
        val tried = Source.entries.mapNotNull { source -> prefs.getString(source.key("saveTried"), null)?.let { source to it } }.toMap()
        val done = mutableListOf<String>()
        for ((post, original) in toSave(inUse(), stored(), saved, tried)) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            if (original) prefs.edit(commit = true) { putString(post.source.key("saveTried"), post.imageUrl) }
            val result = try {
                save(post, original, limit = saveLimit * 1_000_000L).also { prefs.edit { remove(post.source.key("saveTried")) } }
            } catch (e: Exception) {
                if (e.isStop()) throw e
                Log.w("APODroid", "Couldn't save ${post.source} ${post.date}: $e")
                continue
            }
            done += "${post.source} ${post.date} ${result.name.lowercase()}"
        }
        return done.takeIf { it.isNotEmpty() }?.joinToString(prefix = "saved: ")
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

    /**
     * Sets what each screen is due ([dueWallpapers], by the change times): the records without a
     * set written first, then the sets (one with both flags when both screens are
     * due the same picture), each screen's record right after its own set. A failed set leaves
     * that record as it was, so the retry sets that screen only; the other sets still run, and
     * the first failure is returned with them. Under one lock, as the page and the job may both
     * get here. Returns what it set for the log (null when nothing) and the first failed set.
     * A [pull] (pull to refresh) sets a newer picture of each screen's source at once.
     */
    private fun setDue(pull: Boolean): Pair<String?, Exception?> = synchronized(wallpaperLock) {
        val now = Instant.now()
        val stored = stored()
        val plan = dueWallpapers(columns, records(), stored, pictureTime(), ZoneId.systemDefault(), now, pull)
        write(plan.copy(screens = plan.screens.filterValues { !it.set }))
        val done = mutableListOf<String>()
        var failure: Exception? = null
        for ((picture, screens) in plan.sets) {
            try {
                set(picture, screens, stored)
            } catch (e: Exception) {
                failure = failure ?: e
                screens.forEach { setsSinceSetup[it] = false }
                continue
            }
            prefs.edit(commit = true) { screens.forEach { putRecord(it, plan.screens.getValue(it).record); putSetAt(it) } }
            screens.forEach { setsSinceSetup[it] = true }
            done += "$picture on ${screens.joinToString("+")}"
        }
        done.takeIf { it.isNotEmpty() }?.joinToString(prefix = "wallpaper set: ") to failure
    }

    private fun SharedPreferences.Editor.putSetAt(screen: Screen) = putLong(screen.key("wallpaperSetAt"), System.currentTimeMillis())

    /** What is stored for `:core`: the posts, which pictures are there, the user's own, the video-day choice. */
    private fun stored() = Stored(
        Source.entries.mapNotNull { s -> post(s)?.let { s to it } }.toMap(),
        Source.entries.filter { imageFile(it).exists() }.toSet(),
        fallbackFile.exists(),
        videoDays.standIn,
    )

    /**
     * What [tab]'s picture is now ([Stored.picture]): on APOD's video tab the stand-in (NASA's or
     * Earth's, or the user's own; null with "Skip it" or nothing stored yet).
     */
    fun tabPicture(tab: Source): Picture? = stored().picture(tab)

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

    /**
     * Whether the wallpaper shows all of [post]'s picture (A3): ⛶'s whole picture, kept beside
     * the part for that post only (date and picture URL), so a new post drops it.
     */
    private fun isWhole(post: Post?): Boolean {
        val source = post?.source ?: return false
        val date = prefs.getString(source.key("cropDate"), null)?.let(LocalDate::parse)
        return post.isNoted(date, prefs.getString(source.key("cropUrl"), null)) && prefs.getBoolean(source.key("whole"), false)
    }

    /** ⛶'s stored part per source, for the page's draft ([Setup.parts]); the job uses [cropPosition] and [isWhole]. */
    val parts: Map<Source, Part>
        get() = Source.entries.mapNotNull { source ->
            prefs.getString(source.key("cropDate"), null)?.let {
                source to Part(
                    LocalDate.parse(it),
                    prefs.getString(source.key("cropUrl"), null),
                    prefs.getFloat(source.key("cropPosition"), 0.5f),
                    prefs.getBoolean(source.key("whole"), false),
                )
            }
        }.toMap()

    /**
     * Blocking. Sets [picture] on [screens] (both: one set with both flags, one wallpaper); the
     * callers read [stored] and the records first: setting takes seconds, and a refresh storing
     * a new post meanwhile must not mark the new one as set. The crop is [wallpaperFrame], the
     * screen's portrait shape: without one, Android 15+ shows the left part of a wide picture.
     * The user's own picture (video days) is always Fill, centred. A source's post with ⛶'s
     * whole picture ([isWhole]): all of it, on black or blurred ([whole]). Writes no record.
     */
    private fun set(picture: Picture, screens: Set<Screen>, stored: Stored) {
        val file = if (picture is Picture.Of) imageFile(picture.source) else fallbackFile
        val position = if (picture is Picture.Of) cropPosition(stored.post(picture.source)) else 0.5f
        val flags = screens.fold(0) { flags, it -> flags or if (it == Screen.HOME) WallpaperManager.FLAG_SYSTEM else WallpaperManager.FLAG_LOCK }
        val manager = WallpaperManager.getInstance(context)
        if (picture is Picture.Of && isWhole(stored.post(picture.source))) {
            // The screen's size already: the hint is all of it.
            val bitmap = whole(file, screen)
            manager.setBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), true, flags)
        } else if (needsTurning(file)) {
            // Android would measure it unturned but draw it turned, stretching it (seen on API 36).
            val bitmap = decodeUpright(file, screen)
            manager.setBitmap(bitmap, wallpaperFrame(bitmap.width, bitmap.height, screen, position), true, flags)
        } else {
            val crop = imageBounds(file)?.let { wallpaperFrame(it.outWidth, it.outHeight, screen, position) }
            file.inputStream().use { manager.setStream(it, crop, true, flags) }
        }
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
     * more: a refresh stored a new one meanwhile. Without [tryOriginal] (the job's second save of a
     * post, [saveInUse]) the stored picture at once; with a [limit] (bytes, 0: none; the job's) an
     * original over it is dropped once that's known ([TooLarge]) for the stored picture. The download runs outside
     * the lock, so a refresh doesn't wait for it. One save at a time: [Saved.BUSY] while another
     * runs.
     */
    fun save(post: Post, tryOriginal: Boolean = true, limit: Long = 0): Saved {
        if (!saving.compareAndSet(false, true)) return Saved.BUSY
        var original: File? = null
        try {
            dropLeftovers()
            val url = post.takeIf(::isStored)?.let(::originalUrl) ?: return Saved.STALE
            val loaded = tryOriginal && try {
                // One name: one save at a time in the process, and a killed one's file is replaced.
                original = File(context.cacheDir, "original.part")
                download(url, original, userAgent, limit)
                isImage(original)
            } catch (e: TooLarge) {
                Log.i("APODroid", "Original over the limit, saving the stored picture: $e")
                false
            } catch (e: IOException) {
                // A stopped job's save ends, not falls back ([saveInUse]).
                if (e.isStop()) throw e
                Log.w("APODroid", "Original failed, saving the stored picture: $e")
                false
            }
            return copyToGallery(post, if (loaded) original!! else imageFile(post.source), loaded)
        } finally {
            original?.delete()
            saving.set(false)
        }
    }

    /**
     * Deletes what a save killed with its process left in `cache/` (B13: up to a 97 MB original
     * each, kept until Android trimmed the cache), unless a save runs now. The page's first start.
     */
    fun dropSaveLeftovers() {
        if (!saving.compareAndSet(false, true)) return
        try {
            dropLeftovers()
        } finally {
            saving.set(false)
        }
    }

    /** The saves' temp files: `original.part`, and before B13 `original<number>.part`. Under [saving]. */
    private fun dropLeftovers() {
        context.cacheDir.listFiles { file -> file.name.startsWith("original") && file.name.endsWith(".part") }?.forEach { it.delete() }
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
fun Throwable.isStop() = this is InterruptedIOException && this !is SocketTimeoutException

/** A screen's record key: home's as is (the keys before 0.6.0), the lock screen's `lock.<name>`. */
private fun Screen.key(name: String) = if (this == Screen.HOME) name else "lock.$name"

/** [Store.migrateOnce] runs once, also when the page and the job start at the same time. */
private val migrationLock = Any()

private val refreshLock = Any()

/** A [Store.save] is running; process-wide, as each page and job has its own [Store]. */
private val saving = AtomicBoolean()

/** Not [refreshLock]: holding that during a set would make ★ wait for seconds. */
private val wallpaperLock = Any()

/**
 * Since the last [Store.saveSetup] (under [wallpaperLock]): whether each screen's last set
 * worked, by any run, so Set's toast also counts a set the page's load or the job made of its
 * setup first ([Store.setsSinceSetup]; CR 5). Process-wide, as each page and job has its own [Store].
 */
private var setsSinceSetup = mutableMapOf<Screen, Boolean>()

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

/**
 * The whole picture (A3): [file] upright, all of it as large as fits ([fitFrame]), centred in
 * the [screen]'s size on black when its edges are nearly black ([edgesBlack]: a planet on
 * space blends in), else on the picture blurred and darkened ([blurred]). Decoded straight to the frame's size and drawn 1:1: shrinking the rendition (3726×2484 for a
 * 3:2 post) about 3× in one bilinear step can make stars jagged. `info.size` and `setTargetSize`
 * are both the upright size (Android 10's codec respects EXIF, 11+ swaps for the origin; AOSP's
 * ImageDecoder.cpp).
 */
private fun whole(file: File, screen: Size): Bitmap {
    val background = if (hasBlackEdges(file)) null else blurred(file, screen)
    lateinit var frame: Frame
    val picture = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        frame = fitFrame(info.size.width, info.size.height, screen.width, screen.height)
        decoder.setTargetSize(maxOf(1, frame.right - frame.left), maxOf(1, frame.bottom - frame.top))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    return Bitmap.createBitmap(screen.width, screen.height, Bitmap.Config.ARGB_8888).also {
        Canvas(it).run {
            drawColor(Color.BLACK)
            background?.let { blur ->
                drawBitmap(blur, null, Rect(0, 0, screen.width, screen.height), Paint(Paint.FILTER_BITMAP_FLAG))
                blur.recycle()
            }
            drawBitmap(picture, frame.left.toFloat(), frame.top.toFloat(), null)
        }
        picture.recycle()
    }
}

/** Whether [file]'s edges are nearly black, from an upright grey copy [edgeCopySize] large ([edgesBlack]). */
private fun hasBlackEdges(file: File): Boolean {
    val copy = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = edgeCopySize(info.size.width, info.size.height)
        decoder.setTargetSize(w, h)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val w = copy.width
    val h = copy.height
    val pixels = IntArray(w * h).also { copy.getPixels(it, 0, w, 0, 0, w, h) }
    copy.recycle()
    return edgesBlack(IntArray(pixels.size) { gray(pixels[it]) }, w, h)
}

/**
 * The blurred background ([blurBackground]): [file]'s middle in the [screen]'s shape, upright,
 * at [blurSize] (decoded halved while it still covers that, then scaled), blurred and darkened.
 * The caller draws it enlarged to the screen.
 */
private fun blurred(file: File, screen: Size): Bitmap {
    val (bw, bh) = blurSize(screen.width, screen.height)
    val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val frame = wallpaperFrame(info.size.width, info.size.height, screen, 0.5f)
        var sample = 1
        while (frame.width() / (sample * 2) >= bw && frame.height() / (sample * 2) >= bh) sample *= 2
        decoder.setTargetSampleSize(sample)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val copy = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    Canvas(copy).drawBitmap(decoded, wallpaperFrame(decoded.width, decoded.height, screen, 0.5f), Rect(0, 0, bw, bh), Paint(Paint.FILTER_BITMAP_FLAG))
    decoded.recycle()
    val pixels = IntArray(bw * bh).also { copy.getPixels(it, 0, bw, 0, 0, bw, bh) }
    copy.setPixels(blurBackground(pixels, bw, bh), 0, bw, 0, 0, bw, bh)
    return copy
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
