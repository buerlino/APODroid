package io.github.buerlino.apodroid

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Display
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import io.github.buerlino.apodroid.core.DEFAULT_PICTURE_AT
import io.github.buerlino.apodroid.core.OWN_PICTURE
import io.github.buerlino.apodroid.core.PickerTrip
import io.github.buerlino.apodroid.core.Picture
import io.github.buerlino.apodroid.core.PictureTime
import io.github.buerlino.apodroid.core.Plan
import io.github.buerlino.apodroid.core.Post
import io.github.buerlino.apodroid.core.Problem
import io.github.buerlino.apodroid.core.Record
import io.github.buerlino.apodroid.core.Restart
import io.github.buerlino.apodroid.core.Rotation
import io.github.buerlino.apodroid.core.Screen
import io.github.buerlino.apodroid.core.HttpStatus
import io.github.buerlino.apodroid.core.earthPictureMissing
import io.github.buerlino.apodroid.core.earthThumbnail
import io.github.buerlino.apodroid.core.thumbFallbackNote
import io.github.buerlino.apodroid.core.SiteChanged
import io.github.buerlino.apodroid.core.StandIn
import io.github.buerlino.apodroid.core.StorageFailed
import io.github.buerlino.apodroid.core.Source
import io.github.buerlino.apodroid.core.Stored
import io.github.buerlino.apodroid.core.TooLarge
import io.github.buerlino.apodroid.core.afterGap
import io.github.buerlino.apodroid.core.Size as Sizing
import io.github.buerlino.apodroid.core.boxResize
import io.github.buerlino.apodroid.core.Part
import io.github.buerlino.apodroid.core.forPost
import io.github.buerlino.apodroid.core.framePosition
import io.github.buerlino.apodroid.core.sidewaysHelps
import io.github.buerlino.apodroid.core.sidewaysOf
import io.github.buerlino.apodroid.core.sidewaysSubjectPosition
import io.github.buerlino.apodroid.core.sizeOf
import io.github.buerlino.apodroid.core.subjectCopySize
import io.github.buerlino.apodroid.core.subjectPosition
import io.github.buerlino.apodroid.core.livePosition
import io.github.buerlino.apodroid.core.equalColumns
import io.github.buerlino.apodroid.core.handOverWaits
import io.github.buerlino.apodroid.core.lockFollowsHome
import io.github.buerlino.apodroid.core.lostLive
import io.github.buerlino.apodroid.core.opensPicker
import io.github.buerlino.apodroid.core.sharedLockFollows
import io.github.buerlino.apodroid.core.stillOrLive
import io.github.buerlino.apodroid.core.unnotedLive
import io.github.buerlino.apodroid.core.uprightOf
import io.github.buerlino.apodroid.core.Lost
import io.github.buerlino.apodroid.core.alarmsLost
import io.github.buerlino.apodroid.core.missedChange
import io.github.buerlino.apodroid.core.apodColumns
import io.github.buerlino.apodroid.core.columnsInOneOrder
import io.github.buerlino.apodroid.core.screensOf
import io.github.buerlino.apodroid.core.darkening
import io.github.buerlino.apodroid.core.download
import io.github.buerlino.apodroid.core.dueWallpapers
import io.github.buerlino.apodroid.core.fetchLatest
import io.github.buerlino.apodroid.core.migrate
import io.github.buerlino.apodroid.core.nextAlarm as coreNextAlarm
import io.github.buerlino.apodroid.core.screenOnRunOwed as coreScreenOnRunOwed
import io.github.buerlino.apodroid.core.liveSettling as coreLiveSettling
import io.github.buerlino.apodroid.core.liveHolds
import io.github.buerlino.apodroid.core.homeLive
import io.github.buerlino.apodroid.core.hasUp
import io.github.buerlino.apodroid.core.liveReconnect
import io.github.buerlino.apodroid.core.randomOffset
import io.github.buerlino.apodroid.core.retryAfter
import io.github.buerlino.apodroid.core.orderAlone
import io.github.buerlino.apodroid.core.orderOf
import io.github.buerlino.apodroid.core.originalUrl
import io.github.buerlino.apodroid.core.pictureAtOrDefault
import io.github.buerlino.apodroid.core.restartAndForget
import io.github.buerlino.apodroid.core.sourcesInUse
import io.github.buerlino.apodroid.core.toSave
import io.github.buerlino.apodroid.core.usEastern
import io.github.buerlino.apodroid.core.videoDayKept
import io.github.buerlino.apodroid.core.renditionUrl
import io.github.buerlino.apodroid.core.originalSize
import io.github.buerlino.apodroid.core.takesOriginal
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Locale
import java.net.SocketTimeoutException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** APOD mode's "Where": the screens APOD goes on. */
enum class Where(val screens: Set<Screen>) {
    HOME(setOf(Screen.HOME)),
    LOCK(setOf(Screen.LOCK)),
    BOTH(Screen.entries.toSet()),
    ;

    companion object {
        /** The one naming exactly [screens], null for none. */
        fun of(screens: Set<Screen>): Where? = entries.firstOrNull { it.screens == screens }
    }
}

/**
 * Everything "Set as wallpaper" saves at once ([Store.saveSetup]); the page's draft until then.
 * [list] is the list's columns, kept in APOD mode too; [parts] ⛶'s part per source; [pictureAt]
 * "New picture at", an hour; [soon] "As soon as it's out" (SO2: a newer picture of a screen's
 * source goes on whenever a check sees it, with hourly checks all day); [darken] "Darken bright
 * pictures" (BR2: each set darkens a picture brighter than `:core`'s target, [Store.set]); [order]
 * the list's order (OR2: the turns' order, all three sources, the top first; saved only while
 * the list shows); [size] Size (L6, SA3a: Centre, a still set at the middle as 0.6.0; Auto, a
 * still set on the subject, [Store.subject]; or Live, home's picture drawn by APODroid's live
 * wallpaper while Android's picker put it on home; with Auto an APOD landscape picture goes on
 * sideways by its text, [Store.turnsSideways]); [autoFallback] "Centre when unsure" (UI1, was SA3b's Advanced switch: with
 * Size Auto a subject only slightly off the middle stays centred, [Store.autoFallback]). Not the daily switch: it acts at once ([Store.switchDaily]).
 */
/** A post's subject (SA3a, [Store.subject]): Fill's raw frame positions upright and sideways (`:core`'s subjectPosition), null where none was found. */
data class Subject(val position: Float?, val sideways: Float?)

data class Setup(
    val allSources: Boolean,
    val where: Where,
    val list: Map<Screen, List<Source>>,
    val interval: Interval,
    val videoDays: VideoDays,
    val parts: Map<Source, Part>,
    val pictureAt: Int = DEFAULT_PICTURE_AT,
    val soon: Boolean = false,
    val darken: Boolean = true,
    val order: List<Source> = Source.entries,
    val size: Sizing = Sizing.AUTO,
    val autoFallback: Boolean = true,
) {
    /** The columns this setup puts on the screens: APOD mode's from [where], else [list]. */
    val columns: Map<Screen, List<Source>>
        get() = if (allSources) list else apodColumns(where.screens)

    /** Its change times ([PictureTime]): at [pictureAt], with turns every [interval] from it. */
    val pictureTime: PictureTime
        get() = PictureTime(pictureAt, interval.hours)

    /** The same setup with each list column in one order, for comparing ([orderAlone]). */
    fun normalized(): Setup = copy(list = columnsInOneOrder(list))

    /** Whether this setup differs from [was] in the list's order alone ([Store.saveSetup]'s "the order alone"; `:core`'s [orderAlone]). */
    fun onlyOrderDiffers(was: Setup): Boolean = orderAlone(was, this, Setup::order, { setup, order -> setup.copy(order = order) }, Setup::normalized)
}

/**
 * What [Store.update] did: [done] for the log (null: nothing was set), whether a set failed
 * ([setFailed]), the first failure, a fetch's or a set's ([failure]; the job retries), and
 * whether it went on to set ([wentToSet]: the daily switch was on at its check, or `set`; false
 * too when the switch went off between two sets and the second was left, O4c).
 */
data class Updated(val done: String?, val setFailed: Boolean, val failure: Exception?, val wentToSet: Boolean)

/** L8f1: a run held its sets while Android may be rebinding the engine ([Store.liveSettling]): a failed set, retried by the alarms. */
class LiveSettling : Exception("Live: the engine's process just started, nothing set")

/**
 * What fills APOD's turn on an APOD video day ("If APOD is a video", both modes): "Skip it"
 * ([KEEP], the pref's name from 0.5.0's "Keep"), another source's newest, or the user's own.
 */
enum class VideoDays(val standIn: StandIn) {
    KEEP(StandIn.SKIP),
    NASA(StandIn.NASA),
    EARTH(StandIn.EARTH),
    MINE(StandIn.OWN),
    ;

    /** The source whose newest picture stands in, null for "Skip it" and the user's own. */
    val source: Source?
        get() = standIn.source
}

/** How long each source's turn lasts with two or more sources ticked. */
enum class Interval(val hours: Int) { DAY(24), H12(12), H6(6), H3(3) }

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
        foldRunGoing()
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

    /**
     * 0.6.0's run bookkeeping → 0.7.0's (O4b), once: 0.6.0 noted a run going (`runGoing`) and a
     * failed run's end (`failedAt`); 0.7.0 counts a run as failed from its start. A `runGoing`
     * left by 0.6.0 (the update's `install -r` killed a run, V11a) is counted once in
     * `failedRuns`, as 0.6.0's next start would have, so its retry stays; both keys go. Drop it two
     * releases after F-Droid ships past 0.7.0. The caller holds [migrationLock].
     */
    private fun foldRunGoing() {
        if (!prefs.contains("runGoing") && !prefs.contains("failedAt")) return
        val killed = prefs.getBoolean("runGoing", false)
        prefs.edit(commit = true) {
            if (killed) putInt("failedRuns", prefs.getInt("failedRuns", 0) + 1)
            remove("runGoing")
            remove("failedAt")
        }
        Log.i("APODroid", "Migrated 0.6.0's run notes${if (killed) ": a killed run counted as failed" else ""}")
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
        get() = if (!hasList) apodColumns(where.screens) else Screen.entries.associateWith { screen ->
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
        get() = setup.columns

    /** A column takes turns: its change times are every [interval] from [pictureAt] ([PictureTime]). */
    val hasTurns: Boolean
        get() = columns.values.any { it.size >= 2 }

    /** The setup last set ("Set as wallpaper"), which the job follows. */
    val setup: Setup
        get() = Setup(allSources, where, list, interval, videoDays, parts, pictureAt, soon, darken, order, size, autoFallback)

    /**
     * The list's order (OR2, the pref `order`, a comma list of all three; read tolerantly by
     * `:core`'s [orderOf]; default APOD → NASA → Earth): the turns' order, part of the [setup].
     */
    val order: List<Source>
        get() = orderOf(prefs.getString("order", null))

    /**
     * The anchor (OR2, the pref `orderFrom`, a local date): the "New picture at" of that date is
     * the list's top's turn ([Rotation]). Written by a Set of a new order with the list shown, or
     * the first Set with it shown while none is stored ([saveSetup]); null before: RO1's rotation.
     */
    val orderFrom: LocalDate?
        get() = prefs.getString("orderFrom", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** The turns' order and anchor of the stored setup, for the rules. */
    private val rotation: Rotation
        get() = Rotation(order, orderFrom)

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
     * [daily] says. Returns whether the commit was written to disk (B20: on a full disk it
     * isn't, and the setup lives only in the process's memory; Set's toast says so).
     *
     * **The list's order** (OR2, `order-0.7.0.md`): the order and the anchor are written only
     * while the list shows ([Setup.allSources]; in APOD mode both stay as stored). The anchor
     * (`orderFrom`, the date of the next "New picture at" by the saved setup's hour) moves only
     * for a new order, or when none is stored. Before it, [restartAndForget] keeps what's on: a
     * Set of the order alone ([Setup] equal to the stored one but for the order, no [pick])
     * keeps each record and sets nothing; anything else keeps the owner and sets it again; with
     * the daily switch off the top goes on.
     */
    fun saveSetup(setup: Setup, pick: Boolean): Boolean = synchronized(wallpaperLock) {
        if (pick && draftFallbackFile.exists() && !draftFallbackFile.renameTo(fallbackFile)) Log.w("APODroid", "Couldn't rename ${draftFallbackFile.name}")
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val was = this.setup
        val before = was.columns
        // The order only while the list shows (Night default 3): APOD mode keeps the stored one.
        val order = if (setup.allSources) setup.order else was.order
        val anchor = orderFrom
        val newOrder = setup.allSources && (order != was.order || anchor == null)
        val from = if (newOrder) setup.pictureTime.anchorDate(now, zone) else anchor
        val orderOnly = !pick && setup.copy(order = order).onlyOrderDiffers(was)
        // The new setup's stand-in, not the stored one (RO2's fix round): else a Set that changed
        // "If APOD is a video" picked each screen's owner by the old choice ("Skip it" passed APOD's
        // turn to NASA, so Earth went on only at the second Set).
        val restart = restartAndForget(
            before, setup.columns, records(), stored(setup.videoDays.standIn), setup.pictureTime, zone, now,
            Rotation(order, from), orderOnly, daily,
        )
        // edit(commit = true) { } drops commit()'s result (B20), so not the KTX edit lint suggests.
        @SuppressLint("UseKtx") val saved = with(prefs.edit()) {
            putBoolean("allSources", setup.allSources)
            putString("where", setup.where.name)
            // Before the first time on, the list stays unsaved, so it still starts from "Where".
            if (setup.allSources || hasList) putColumns(setup.list, list)
            putString("interval", setup.interval.name)
            putInt("pictureAt", setup.pictureAt)
            putBoolean("soon", setup.soon)
            putBoolean("darken", setup.darken)
            putBoolean("autoFallback", setup.autoFallback)
            putString("size", setup.size.name)
            if (setup.allSources) putString("order", order.joinToString(",") { it.name })
            if (newOrder) putString("orderFrom", from.toString())
            putString("videoDays", setup.videoDays.name)
            for ((source, part) in setup.parts) putPart(source, part)
            putRestart(restart)
            commit()
        }
        if (!saved) Log.w("APODroid", "Couldn't save the setup")
        setsSinceSetup = mutableMapOf()
        saved
    }

    /** ⛶'s part of [source]'s post (its date and picture URL, the frame's place, the whole picture, sideways; a value not noted, null, removed). */
    private fun SharedPreferences.Editor.putPart(source: Source, part: Part) {
        putString(source.key("cropDate"), part.date.toString())
        putString(source.key("cropUrl"), part.url)
        // Null (SA3b, ⛶'s frame not dragged): the stored Size's frame, so the key goes.
        part.position?.let { putFloat(source.key("cropPosition"), it) } ?: remove(source.key("cropPosition"))
        putBoolean(source.key("whole"), part.whole)
        // Null (SW3): Auto's turn decides, so the key goes.
        part.sideways?.let { putBoolean(source.key("sideways"), it) } ?: remove(source.key("sideways"))
    }

    /**
     * ⛶'s ✓ (U0, 2026-11-02): [part] as [source]'s stored part, at once, on the page's thread, so
     * every page (and a page a rotation makes) reads it before [setPart]'s set; that set and any
     * run's read the part when they set, so the last ✓ wins.
     */
    fun savePart(source: Source, part: Part) = prefs.edit(commit = true) { putPart(source, part) }

    /**
     * Blocking. After [savePart]: sets the screens with a column whose record shows [post]
     * again with its stored part. Nothing else: the records, the turns and the change times stay
     * as they are, only `wallpaperSetAt` moves (the status line). A post on no screen, or one a
     * newer post replaced meanwhile, isn't set; the job uses the part when it puts the post on.
     * Under [wallpaperLock], so a run's set comes before or after it. Returns the screens set
     * (empty: none showed it); throws when the set failed (the part stays saved).
     */
    fun setPart(post: Post): Set<Screen> = synchronized(wallpaperLock) {
        val stored = stored()
        val current = stored.post(post.source)?.takeIf { it.date == post.date && it.imageUrl == post.imageUrl } ?: return emptySet()
        // Only screens with a column: a screen left alone keeps its record, but may show anything now.
        val columns = columns
        val showing = Screen.entries.filter { columns[it].orEmpty().isNotEmpty() }.mapNotNull { screen ->
            (record(screen)?.picture as? Picture.Of)?.takeIf { it.source == post.source && current.isNoted(it.date, it.url) }?.let { screen to it }
        }.toMap()
        val picture = showing.values.firstOrNull() ?: return emptySet()
        // Live (L5): a screen the engine shows gets the copy again (a changed whole picture), not a still set.
        val live = liveState()
        val split = stillOrLive(size, live.home, live.shared, columns, record(Screen.HOME)?.picture, listOf(picture to showing.keys))
        for ((still, screens) in split.still) {
            val darker = set(still, screens, stored)
            Log.i("APODroid", "Part: wallpaper set: $still on ${screens.joinToString("+")}$darker")
            prefs.edit(commit = true) { screens.forEach { putSetAt(it); remove(it.key("wallpaperLive")) } }
        }
        split.engine?.let { (shown, screens) ->
            val copy = copyForLive(shown, stored)
            Log.i("APODroid", "Part: wallpaper shown live: $shown on ${screens.joinToString("+")}${liveNote(copy)}")
            prefs.edit(commit = true) {
                putLive(shown, copy)
                screens.forEach { putSetAt(it); putBoolean(it.key("wallpaperLive"), true) }
            }
        }
        showing.keys
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
     * on starts afresh), a run going among them (a run from before it no longer counts, B19).
     * Turning it on, the caller then runs [startAfresh] and [update] with `set`, which counts as
     * the last run's start (`lastRunAt`, B16): the checks before it aren't owed. The detector's
     * note of alarms found gone goes both ways (O14: off cancels them, on starts afresh).
     */
    fun switchDaily(on: Boolean) {
        prefs.edit(commit = true) {
            putBoolean("daily", on)
            remove("failedRuns")
            if (on) putLong("lastRunAt", System.currentTimeMillis()) else remove("lastRunAt")
            removeLost()
        }
    }

    /**
     * Blocking. The stored setup afresh, as Set does without saving anything (the daily switch
     * turned on): [restartAndForget] with the stored columns, so the records of the screens with a
     * column are forgotten; the next [setDue] sets each screen anew. The anchor (`orderFrom`)
     * stays (OR2): before it each screen keeps its owner and sets it again, from it on RO1's.
     */
    fun startAfresh(): Unit = synchronized(wallpaperLock) {
        val restart = restartAndForget(columns, columns, records(), stored(), setup.pictureTime, ZoneId.systemDefault(), Instant.now(), rotation, daily = daily)
        prefs.edit(commit = true) { putRestart(restart) }
    }

    private fun SharedPreferences.Editor.putRestart(restart: Restart) {
        putPlan(restart.plan)
        restart.drop.forEach {
            remove(it.key("wallpaperSource"))
            remove(it.key("wallpaperLive"))
        }
    }

    /**
     * The list's columns, as comma lists (`home`, `lock`), and each source's screens for
     * [lastScreens]: where it is now, or for one unticked where it was in [before].
     */
    private fun SharedPreferences.Editor.putColumns(columns: Map<Screen, List<Source>>, before: Map<Screen, List<Source>>) {
        for (screen in Screen.entries) putString(screen.name.lowercase(), columns[screen].orEmpty().joinToString(",") { it.name })
        for (source in Source.entries) {
            val screens = columns.screensOf(source).ifEmpty { before.screensOf(source) }
            Where.of(screens)?.let { putString(source.key("lastScreens"), it.name) }
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

    /**
     * "New picture at", an hour 0–23 (part of the [setup]; an update from 0.5.0 gets 06:00).
     * 0.6.0's `turnStartedAt` is no longer read (left in the prefs).
     */
    val pictureAt: Int
        get() = pictureAtOrDefault(prefs.getInt("pictureAt", DEFAULT_PICTURE_AT))

    /**
     * "As soon as it's out" (SO2, part of the [setup], default off: 0.6.0's 3-hour window): on, a
     * newer picture of a screen's source goes on whenever a check sees it ([dueWallpapers]'
     * `soon`), and the alarms check hourly all day ([nextAlarm]'s `allDay`).
     */
    val soon: Boolean
        get() = prefs.getBoolean("soon", false)

    /**
     * "Darken bright pictures" (BR2, part of the [setup], default on, also for an update from
     * 0.6.0: the user, 2026-11-14): each [set] darkens a picture whose top third is brighter than
     * `:core`'s target.
     */
    val darken: Boolean
        get() = prefs.getBoolean("darken", true)

    /**
     * Whether [source]'s stored picture goes on sideways with its post's [part] (0.9.0, SH2a,
     * `:core`'s [sidewaysOf]): the part's noted value (⛶'s pill), else Auto's turn: the stored
     * [size] Auto, APOD, landscape by its [uprightSize], and [post]'s text ([hasUp] of its title,
     * alt and explanation) without an up; else upright. The pref `wideSideways` (SW3's "Wide
     * pictures sideways") is no longer read (left in the prefs, no migration). The one reading
     * for [set] and [copyForLive]; the page's ⛶ reads the same rule.
     */
    fun turnsSideways(source: Source, part: Part?, post: Post? = post(source)): Boolean =
        sidewaysOf(part?.sideways, size, source, { uprightSize(imageFile(source))?.let { it.width to it.height } }) {
            post?.let { hasUp(it.title, it.alt, it.explanation) } ?: true
        }

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
     * The job's run starts ([DailyJob]): it counts as failed until it ends well (`failedRuns` + 1,
     * O4b; [ranWell] forgets them), so a run that never reaches its end (killed: `install -r`,
     * `kill -9`, the low-memory killer, a Recents swipe; or stopped by Android) stays counted and
     * an arming in a new process keeps its retry (B19); and notes when (`lastRunAt`), so the retry
     * counts from it and a check that passes after it and before the next run is armed at once
     * ([nextAlarm], B16), not dropped by a re-arming. One commit.
     */
    fun runStarts() = prefs.edit(commit = true) {
        putInt("failedRuns", prefs.getInt("failedRuns", 0) + 1)
        putLong("lastRunAt", System.currentTimeMillis())
    }

    /**
     * How long after the last failed run's start the alarms retry ([retryAfter]): 15 min after the
     * first, an hour after more; during a run (counted by [runStarts]) its own retry, should it
     * never end. 15 min with none (a run that couldn't be scheduled, O4a).
     */
    fun runRetry(): Duration = retryAfter(prefs.getInt("failedRuns", 0))

    /** The job's run ended well ([DailyJob]): the failed runs forgotten, this one's count with them. A failed or stopped run stays counted. */
    fun ranWell() = prefs.edit(commit = true) { remove("failedRuns") }

    /**
     * When the alarms should start the next run ([nextAlarm]): the change times of the stored
     * setup, with their checks (15 min after a change time, then hourly in its window, all day with [soon]; no waiting
     * since RO1) and this device's [offset], or, after failed runs (a run going among
     * them, O4b), [runRetry] after the last one's start (`lastRunAt`) when that comes first; at
     * once when a check passed since the last run's start (B16). So an arming during a run keeps
     * the pair its start armed (B16f1), also in a new process after it was killed (B19). Not under
     * the wallpaper lock: the page's `onResume` and the alarms' receiver call it on the main
     * thread, and a set holds that lock for seconds.
     */
    fun nextAlarm(now: Instant = Instant.now()): Instant {
        val zone = ZoneId.systemDefault()
        val setup = setup
        return coreNextAlarm(now, zone, prefs.getInt("failedRuns", 0), setup.pictureTime, hasTurns, offset, instant("lastRunAt"), allDay = setup.soon)
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

    /**
     * "Keep running" (O11): a foreground service keeps the process, so a maker's cleaner that
     * stops cached apps leaves it and its alarms alone ([KeepRunning], while [daily] too).
     */
    var keepRunning: Boolean
        get() = prefs.getBoolean("keepRunning", false)
        set(value) = prefs.edit { putBoolean("keepRunning", value) }

    /**
     * Auto's fallback (SA3a; Setup's switch "Centre when unsure", part of the [setup] since UI1,
     * saved by [saveSetup]; was SA3b's "Auto: centre when unsure" in Advanced, at once): on, Auto keeps the middle
     * for a picture whose subject would move the frame less than `:core`'s SUBJECT_MIN_MOVE;
     * off, Auto places every frame on its subject. The pref `autoFallback`, default on; read by
     * each [set].
     */
    val autoFallback: Boolean
        get() = prefs.getBoolean("autoFallback", true)

    /** The user hid the hint about Android pausing the app, accepting the warning. */
    var pauseHintHidden: Boolean
        get() = prefs.getBoolean("pauseHintHidden", false)
        set(value) = prefs.edit { putBoolean("pauseHintHidden", value) }

    /**
     * The detector (O14): the change time of the last miss ([missedChange]: the phone stopped the
     * app, and its change came late), null before the first. The box shows it until [missDismissed].
     */
    val missedAt: Instant?
        get() = instant("missedAt")

    /** "Keep running" was on when [missedAt] was noted: the box names the maker's settings, else the switch. */
    val missedKeep: Boolean
        get() = prefs.getBoolean("missedKeep", false)

    /** The miss whose box the user dismissed (its change time); the next miss shows again. */
    var missDismissed: Instant?
        get() = instant("missDismissed")
        set(value) = prefs.edit { if (value == null) remove("missDismissed") else putLong("missDismissed", value.toEpochMilli()) }

    /**
     * The alarms were armed ([DailyJob]'s arming): which boot and which install they belong to, so
     * the next process that finds them gone ([noteAlarmsGone]) can tell a boot or an update from a
     * stopped app. The boot by `Settings.Global.BOOT_COUNT`, which a clock change doesn't move
     * (the boot's moment, the clock less the time since boot, as a fallback where it's missing);
     * the install by `lastUpdateTime`, compared as a value.
     */
    fun noteArmed() = prefs.edit {
        putInt("armedBoot", bootCount() ?: -1)
        putLong("armedBootAt", bootMoment())
        putLong("armedUpdate", updateTime())
    }

    /**
     * The first arming in a process found the alarms gone while the daily switch is on
     * ([DailyJob]): unless a boot or an update since they were armed or a pending run ([runPending])
     * explains it ([alarmsLost]), notes when (`alarmsLostAt`) and the last run's start before it
     * (`lostAfterRun`), for the next ordinary run's sets ([setDue]). No note of the arming yet (the
     * first start of 0.7.0, after the update) explains it too.
     */
    fun noteAlarmsGone(runPending: Boolean) {
        val booted = prefs.getInt("armedBoot", -1).let { armed ->
            val now = bootCount()
            if (armed >= 0 && now != null) now != armed
            else !prefs.contains("armedBootAt") || abs(bootMoment() - prefs.getLong("armedBootAt", 0)) > TimeUnit.MINUTES.toMillis(1)
        }
        val updated = !prefs.contains("armedUpdate") || prefs.getLong("armedUpdate", 0) != updateTime()
        val lost = alarmsLost(gone = true, booted, updated, runPending, instant("lastRunAt"), Instant.now())
        Log.i("APODroid", "Detector: the alarms found gone${if (lost == null) " (booted $booted, updated $updated, run pending $runPending)" else ", after the run of ${lost.lastRun}"}")
        if (lost != null) prefs.edit(commit = true) {
            putLong("alarmsLostAt", lost.at.toEpochMilli())
            putLong("lostAfterRun", lost.lastRun.toEpochMilli())
        }
    }

    /** The alarms found gone and not yet judged by a run's sets ([judgeMiss]). */
    private fun lost(): Lost? {
        val at = instant("alarmsLostAt") ?: return null
        return Lost(at, instant("lostAfterRun") ?: return null)
    }

    private fun SharedPreferences.Editor.removeLost() {
        remove("alarmsLostAt")
        remove("lostAfterRun")
    }

    private fun bootCount(): Int? = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()

    private fun bootMoment() = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    private fun updateTime() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime }.getOrDefault(0L)

    /** The video-day notice was dismissed for this date (stored as text), and it's back on the next video day. */
    var videoNoticeDate: String?
        get() = prefs.getString("videoNoticeDate", null)
        set(value) = prefs.edit { putString("videoNoticeDate", value) }

    /** The user hid the video-day notice for good. */
    var videoNoticeHidden: Boolean
        get() = prefs.getBoolean("videoNoticeHidden", false)
        set(value) = prefs.edit { putBoolean("videoNoticeHidden", value) }

    /**
     * The problem the user dismissed for [source], as kind and day (`http:503:2026-10-16`, NASA's
     * with `nasa:` first); a different kind, code or day shows it again. One per source (FP6,
     * 2026-11-15, BP2: one shared slot brought the first source's box back after the second's
     * Dismiss); APOD's is the pref of before, so no migration.
     */
    fun problemNoticeKey(source: Source): String? = prefs.getString(source.key("problemNoticeKey"), null)

    fun dismissProblem(source: Source, key: String) = prefs.edit { putString(source.key("problemNoticeKey"), key) }

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
            // SH2a, APOD only (Auto's turn); a post stored before 0.9.0 has none until the next fetch (the job's next run, D1).
            alt = prefs.getString(source.key("alt"), null),
            // APOD only; a post stored by 0.5.0 has none until the next one.
            credit = prefs.getString(source.key("credit"), null),
            // B15; a post stored before it has none and counts from its date's 00:00 US Eastern (Post.publishedAt).
            published = instant(source.key("published")),
            // VS1; a post stored before 0.8.0 has none (the black tile) until the next fetch.
            videoUrl = prefs.getString(source.key("videoUrl"), null),
        )
    }

    /**
     * Writes [post]. [afterGap]: the mark of a post stored after a gap in the source's checks
     * (`<source>.afterGap`, B21); null keeps the mark (the same post again, a changed title or
     * explanation). A post stored before B21 has none.
     */
    private fun store(post: Post, afterGap: Boolean?) = prefs.edit {
        val source = post.source
        afterGap?.let { putBoolean(source.key("afterGap"), it) }
        putString(source.key("date"), post.date.toString())
        putString(source.key("title"), post.title)
        putString(source.key("imageUrl"), post.imageUrl)
        putString(source.key("pageUrl"), post.pageUrl)
        putBoolean(source.key("isVideo"), post.isVideo)
        putString(source.key("explanation"), post.explanation)
        putString(source.key("credit"), post.credit)
        putString(source.key("alt"), post.alt)
        putString(source.key("videoUrl"), post.videoUrl)
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
        // A forgotten picture (Set, lost Live) isn't on the engine any more (L5).
        if (picture == null) remove(screen.key("wallpaperLive"))
        // The last post shown per source (J1's rule 1: a change skips what this screen has shown).
        for (source in Source.entries) {
            val shown = record.shown[source]
            putString(screen.key(source.key("shownDate")), shown?.date?.toString())
            putString(screen.key(source.key("shownUrl")), shown?.url)
        }
    }

    /** Writes [plan]'s records without a set; the sets are [setDue]'s. */
    private fun SharedPreferences.Editor.putPlan(plan: Plan) {
        plan.screens.forEach { (screen, due) -> putRecord(screen, due.record) }
    }

    /**
     * The screen APOD mode's page reads: home's, unless only the lock screen has a column
     * ("Where" = Lock: after the migration home's record is empty there).
     */
    val pageScreen: Screen
        get() = columns.let { if (it[Screen.HOME].orEmpty().isEmpty() && it[Screen.LOCK].orEmpty().isNotEmpty()) Screen.LOCK else Screen.HOME }

    /** What the page says about [screen]: from its record, the chip, the page it opens on and its status line. */
    fun screenState(screen: Screen): ScreenState {
        val record = record(screen)
        val setAt = instant(screen.key("wallpaperSetAt"))
        return ScreenState(record?.picture, setAt, videoDayKept(columns[screen].orEmpty(), record, stored(), setAt != null))
    }

    /** When a check of [source] last worked: a refresh, or finding today's post already stored. */
    fun checkedAt(source: Source): Instant? = instant(source.key("checkedAt"))

    /** What the last check of [source] ran into, or null when it worked. */
    fun problem(source: Source): Problem? =
        Problem.of(prefs.getString(source.key("problem"), null), prefs.getInt(source.key("httpCode"), 0))

    /**
     * Calls [action] when a set, a stored post, a save or a miss (O14) changes, also the job's or one started
     * before a rotation (they run in this process), until [stopWatching]. Android holds the listener weakly: keep the one returned.
     */
    fun watch(action: () -> Unit) = OnSharedPreferenceChangeListener { _, key ->
        if (key in recordKeys || key == "missedAt" || key in videoStillKeys || Source.entries.any { key in listOf(it.key("date"), it.key("imageUrl"), it.key("title"), it.key("explanation"), it.key("credit"), it.key("alt"), it.key("savedUri"), it.key("problem"), it.key("httpCode")) }) {
            action()
        }
    }.also(prefs::registerOnSharedPreferenceChangeListener)

    /** VS1: the video's still; the open page shows it once made. */
    private val videoStillKeys = setOf("videoStill.date", "videoStill.url")

    /** Both screens' records and when they were set: the open page follows a set on either screen. */
    private val recordKeys = Screen.entries.flatMap { screen ->
        listOf("wallpaperSource", "wallpaperPicture", "wallpaperDate", "wallpaperUrl", "wallpaperSetAt").map(screen::key)
    }.toSet()

    fun stopWatching(listener: OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private fun instant(key: String) = prefs.getLong(key, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /** True when [source]'s stored post is today's (the dates are US Eastern). */
    fun isCurrent(source: Source): Boolean =
        post(source)?.let { it.date == LocalDate.now(usEastern) && isComplete(it) } == true

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
        // SA3a: the stored post's subject, when still owed (after an update or a death before its note).
        noteSubject(source)
        // B21: read before checkSite notes this check.
        val gap = afterGap(checkedAt(source), Instant.now())
        val latest = fetchLatest(source, userAgent)
        val stored = post(source)
        if (stored != null && latest.date < stored.date && isComplete(stored)) return
        val url = latest.imageUrl
        val file = imageFile(source)
        val downloads = url != null && (latest.date != stored?.date || url != stored.imageUrl || !file.exists() ||
            (source == Source.EARTH && prefs.getString("earth.thumbFallback", null) == url))
        if (url != null && downloads) {
            // Else every page open, job run and retry would download it again (13 MB, say).
            if (url == prefs.getString(source.key("badUrl"), null)) throw SiteChanged("Not an image, not downloaded again: $url")
            val part = File(context.filesDir, "${source.name.lowercase()}.part")
            var thumbnail = false
            try {
                thumbnail = downloadPicture(latest, url, part)
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
            prefs.edit {
                remove(source.key("badUrl"))
                // SH2b: the download probed the original, so the check of a post stored by 0.8.0 isn't owed.
                putString(source.key("sizeChecked"), url)
                // EF1: noted only once the thumbnail is stored, cleared by a real download.
                if (source == Source.EARTH) {
                    val note = thumbFallbackNote(thumbnail, url)
                    if (note != null) putString("earth.thumbFallback", note) else remove("earth.thumbFallback")
                }
            }
        } else if (url != null) {
            // SH2b: the same post with its picture; one stored by 0.8.0 (the rendition) is checked once.
            checkSize(source)
        }
        // A new post (another date or picture) is marked as its check says; the same post keeps its mark.
        store(latest, gap.takeIf { stored == null || latest.date != stored.date || latest.imageUrl != stored.imageUrl })
        // D1: the job asks every run, also with today's post stored.
        if (stored != null && latest.date == stored.date) {
            Log.i("APODroid", "Refresh: ${source.name} ${latest.date} ${if (url == stored.imageUrl) "same post" else "new picture"}")
        }
        // After storing: a page drawing meanwhile shows the video entry, never the old picture under it.
        if (url == null) file.delete()
        // VS1: a new post or an image day lets the video's still go.
        if (latest.source == Source.APOD) dropVideoStill(keep = latest)
        noteSubject(source)
    }

    /**
     * What a page open or Set that skips [source]'s check owes its stored post (T3f1's hooks):
     * the check of its size ([checkSize], SH2b), then its subject ([noteSubject]).
     */
    private fun noteStored(source: Source) {
        checkSize(source)
        noteSubject(source)
    }

    /**
     * Blocking (the network: a 64 KB request, then maybe the original). SH2b: [source]'s stored
     * post, stored by 0.8.0 as the screen-tall rendition, gets the probe its download would have
     * had now, once per post (`<source>.sizeChecked`, the picture URL, committed first, so a
     * failure, a kill or a stop doesn't repeat it): when the original wouldn't cover the screen,
     * it is downloaded (via `.part`, decodable) and replaces the stored file, the same post and
     * key, its subject noted again ([noteSubject]: it was noted from the rendition). Not while
     * Earth's thumbnail is noted (each refresh tries the real picture anyway, EF1). A failure is
     * logged and never fails the refresh; a stop is thrown. Under [refreshLock].
     */
    private fun checkSize(source: Source): Unit = synchronized(refreshLock) {
        val post = post(source) ?: return
        val url = post.imageUrl ?: return
        val file = imageFile(source)
        if (post.isVideo || !file.exists() || prefs.getString(source.key("sizeChecked"), null) == url) return
        if (source == Source.EARTH && prefs.getString("earth.thumbFallback", null) == url) return
        prefs.edit(commit = true) { putString(source.key("sizeChecked"), url) }
        val part = File(context.filesDir, "${source.name.lowercase()}.part")
        try {
            if (!downloadOriginal(post, part)) return
            if (!part.renameTo(file)) throw StorageFailed(IOException("Couldn't rename ${part.name}"))
            // The subject was noted from the rendition: noted again from the original.
            prefs.edit {
                remove(source.key("subjectDate"))
                remove(source.key("subjectUrl"))
            }
            Log.i("APODroid", "Refresh: ${source.name} ${post.date} stored picture replaced by the original")
        } catch (e: IOException) {
            part.delete()
            if (e.isStop()) throw e
            Log.w("APODroid", "Refresh: ${source.name} ${post.date} couldn't store the original: $e")
        }
    }

    /**
     * SH2b: probes [post]'s original ([originalSize], its first 64 KB) and, when it wouldn't cover
     * the screen at 1:1 ([takesOriginal]), downloads it to [part] (a complete, decodable image);
     * true then. False (nothing in [part]) when the original covers the screen (the screen-tall
     * rendition is the one), the probe failed or found no size (logged: no limit can bite on a
     * rendition), the original's download failed (logged), or the host doesn't scale (the
     * rendition is the original itself: no probe). Throws a stop and [StorageFailed].
     */
    private fun downloadOriginal(post: Post, part: File): Boolean {
        val url = post.imageUrl ?: return false
        val original = originalUrl(post) ?: return false
        val screen = screen
        if (original == renditionUrl(url, screen.height)) return false
        val what = "Refresh: ${post.source.name} ${post.date}"
        val size = try {
            originalSize(original, userAgent)
        } catch (e: IOException) {
            if (e.isStop()) throw e
            Log.i("APODroid", "$what probe failed: $e")
            return false
        }
        if (size == null) {
            Log.i("APODroid", "$what probe failed: no size in the original's first bytes")
            return false
        }
        val (w, h) = size
        if (!takesOriginal(w, h, screen.width, screen.height)) {
            Log.i("APODroid", "$what rendition (original ${w}×$h)")
            return false
        }
        try {
            download(original, part, userAgent)
            if (!isImage(part)) throw SiteChanged("Not an image: $original")
        } catch (e: IOException) {
            part.delete()
            if (e is StorageFailed || e.isStop()) throw e
            Log.w("APODroid", "$what original ${w}×$h failed, the rendition instead: $e")
            return false
        }
        Log.i("APODroid", "$what original ${w}×$h (smaller than the screen)")
        return true
    }

    /**
     * [post]'s subject (SA3a), when one was noted for its date and picture URL: the raw positions
     * of Fill's frame on its subject, upright and (a landscape picture only) sideways; a
     * [Subject] with both null when it couldn't be found. Null: none noted yet.
     */
    fun subject(post: Post): Subject? {
        val source = post.source
        if (prefs.getString(source.key("subjectDate"), null) != post.date.toString() ||
            prefs.getString(source.key("subjectUrl"), null) != post.imageUrl
        ) return null
        fun float(key: String) = source.key(key).takeIf(prefs::contains)?.let { prefs.getFloat(it, 0.5f) }
        return Subject(float("subjectPosition"), float("subjectSideways"))
    }

    /**
     * Blocking (a decode and `:core`'s map: a fraction of a second). Under [refreshLock]. When
     * [source]'s stored post has its picture and no subject is noted for it ([subject]): a copy
     * decoded upright (`ImageDecoder` turns an EXIF photo), sampled by SA2's rule (the largest
     * power of 2 keeping the shorter side at least 512 px), box-resized to [subjectCopySize],
     * then `:core`'s [subjectPosition] upright and, while [sidewaysHelps], sideways
     * ([sidewaysSubjectPosition]: the same decoded pixels turned clockwise, their own copy); noted with
     * the post's date and picture URL in one edit. A failure (out of memory too) is noted
     * without positions, so it isn't tried again for that post, and never fails the refresh.
     */
    fun noteSubject(source: Source): Unit = synchronized(refreshLock) {
        val post = post(source) ?: return
        val file = imageFile(source)
        if (post.isVideo || post.imageUrl == null || !file.exists() || subject(post) != null) return
        val started = SystemClock.elapsedRealtime()
        val screen = screen
        val found = runCatching {
            var size = Size(0, 0)
            val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                size = info.size
                var sample = 1
                while (minOf(info.size.width, info.size.height) / (sample * 2) >= 512) sample *= 2
                decoder.setTargetSampleSize(sample)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val (dw, dh) = decoded.width to decoded.height
            val pixels = IntArray(dw * dh)
            decoded.getPixels(pixels, 0, dw, 0, 0, dw, dh)
            decoded.recycle()
            val (w, h) = size.width to size.height
            val (cw, ch) = subjectCopySize(w, h, screen.width, screen.height)
            val copy = boxResize(pixels, dw, dh, cw, ch)
            Subject(
                subjectPosition(copy, cw, ch, w, h, screen.width, screen.height),
                if (sidewaysHelps(w, h)) sidewaysSubjectPosition(pixels, dw, dh, w, h, screen.width, screen.height) else null,
            )
        }
        val ms = SystemClock.elapsedRealtime() - started
        val subject = found.getOrElse { e ->
            Log.w("APODroid", "Subject: ${source.name} ${post.date} couldn't: $e")
            Subject(null, null)
        }
        prefs.edit {
            putString(source.key("subjectDate"), post.date.toString())
            putString(source.key("subjectUrl"), post.imageUrl)
            subject.position?.let { putFloat(source.key("subjectPosition"), it) } ?: remove(source.key("subjectPosition"))
            subject.sideways?.let { putFloat(source.key("subjectSideways"), it) } ?: remove(source.key("subjectSideways"))
        }
        if (found.isSuccess) {
            val sideways = subject.sideways?.let { String.format(Locale.ROOT, " (sideways %.2f)", it) }.orEmpty()
            Log.i("APODroid", String.format(Locale.ROOT, "Subject: %s %s p=%.2f%s in %d ms", source.name, post.date, subject.position, sideways, ms))
        }
    }

    /** VS1: the still from APOD's video (the page only, never the wallpaper), with its post noted. */
    private val videoStillFile get() = File(context.filesDir, "video-still.jpg")

    /** The still of [post]'s video, when one was made for it (its date and video URL noted). */
    fun videoStill(post: Post): File? = videoStillFile.takeIf {
        post.videoUrl != null &&
            prefs.getString("videoStill.date", null) == post.date.toString() &&
            prefs.getString("videoStill.url", null) == post.videoUrl &&
            it.exists()
    }

    /** [post]'s video still is still to try: an mp4, no still made, no try yet (one per post, as `badUrl`). */
    fun videoStillOwed(post: Post): Boolean =
        post.isVideo && post.videoUrl != null && prefs.getString("videoStillTried", null) != post.videoUrl && videoStill(post) == null

    /** Deletes the still and its notes unless they are [keep]'s. Under [refreshLock] (the caller's). */
    private fun dropVideoStill(keep: Post) {
        val date = prefs.getString("videoStill.date", null) ?: return
        if (keep.isVideo && date == keep.date.toString() && prefs.getString("videoStill.url", null) == keep.videoUrl) return
        videoStillFile.delete()
        prefs.edit {
            remove("videoStill.date")
            remove("videoStill.url")
        }
    }

    /**
     * Blocking (the network: seconds). VS1: a frame about 3 s into [post]'s mp4 (the nearest
     * sync frame), at most the screen's width (1280 px) on its longer side, saved as
     * `files/video-still.jpg` via `.part`, its post noted. One try per post: `videoStillTried`
     * is committed first, so a failure, a kill or a stop leaves the black tile until the next
     * post. True when a still was made.
     */
    fun makeVideoStill(post: Post): Boolean = synchronized(videoStillLock) {
        val url = post.videoUrl ?: return false
        if (!videoStillOwed(post)) return false
        prefs.edit(commit = true) { putString("videoStillTried", url) }
        val started = SystemClock.elapsedRealtime()
        val part = File(context.filesDir, "video-still.part")
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(url, mapOf("User-Agent" to userAgent))
            val size = minOf(screen.width, 1280)
            val frame = retriever.getScaledFrameAtTime(3_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size, size)
                ?: throw IOException("no frame")
            try {
                part.outputStream().use { if (!frame.compress(Bitmap.CompressFormat.JPEG, 85, it)) throw IOException("couldn't write the JPEG") }
                synchronized(refreshLock) {
                    // A newer post stored meanwhile: this still isn't its.
                    val stored = post(Source.APOD)
                    if (stored?.date != post.date || stored.videoUrl != url) throw IOException("a newer post came")
                    if (!part.renameTo(videoStillFile)) throw IOException("couldn't rename ${part.name}")
                    prefs.edit(commit = true) {
                        putString("videoStill.date", post.date.toString())
                        putString("videoStill.url", url)
                    }
                }
                Log.i("APODroid", "Video still: ${SystemClock.elapsedRealtime() - started} ms, ${frame.width}×${frame.height}")
                true
            } finally {
                frame.recycle()
            }
        } catch (e: Exception) {
            part.delete()
            Log.w("APODroid", "Video still: couldn't make one of $url: $e")
            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w("APODroid", "Video still: couldn't release the retriever: $e")
            }
        }
    }

    /**
     * The original when it's smaller than the screen ([downloadOriginal], SH2b: the rendition
     * would be it enlarged by the server), else [url] scaled by the host to the screen's height,
     * which fills the screen without the original's size (NASA's go up to 97 MB). If that fails,
     * APOD and Earth Observatory fall back to [url] as is (1280 px for APOD); NASA's is the
     * original, so it fails. True when Earth's thumbnail came (EF1).
     */
    private fun downloadPicture(post: Post, url: String, part: File): Boolean {
        if (downloadOriginal(post, part)) return false
        try {
            downloadRendition(post, url, part)
            return false
        } catch (e: IOException) {
            // EF1: Earth's picture missing (404, 410 or not an image), never the network or a timeout:
            // else a slow connection would store the thumbnail under the picture's key for good.
            if (post.source != Source.EARTH || !earthPictureMissing(e)) throw e
            Log.w("APODroid", "Earth: picture missing, the thumbnail instead: $e")
            downloadRendition(post, earthThumbnail(url), part)
            return true
        }
    }

    private fun downloadRendition(post: Post, url: String, part: File) {
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
        if (!isImage(part)) throw SiteChanged("Not an image: $url")
    }

    /**
     * Blocking. What the page, Set and the daily job run: [refresh] each source in use and each
     * of [show] (the sources the page offers, so one ticked in its draft only has a picture);
     * for the [page] (an open, a pull, Set) not when its stored post is today's or it was checked
     * within the hour ([checkedRecently]; its subject still noted, [noteSubject]), always with [force] (pull to refresh) and for the job
     * (D1: a second post on the same date), then, while the
     * daily change is on or with [set] (Set, also with it off), sets what each screen is due
     * ([setDue]). The sources in use: the ones ticked on any screen, and on an APOD video day the
     * stand-in, while APOD is ticked somewhere. A failing source doesn't stop the others, and its
     * stored picture still counts. Only a stop is thrown; every other failure is in the result
     * (the job retries). A stop is the thread's interrupt or [stopped] (the job's, which outlives
     * the interrupt: see [isStopped]).
     */
    fun update(
        force: Boolean,
        page: Boolean = false,
        show: Collection<Source> = emptyList(),
        set: Boolean = false,
        stopped: () -> Boolean = { false },
    ): Updated {
        noteShared()
        var failure: Exception? = null
        for (source in Source.entries) {
            // Asked for each source: APOD's refresh (first) tells whether the stand-in is in use.
            if (source !in show && source !in inUse()) continue
            // A stopped job (DailyJob.onStopJob) ends the run: no more downloads, no set.
            if (isStopped(stopped)) throw InterruptedException()
            try {
                // T3f1: a page open or Set within the hour of the last check still notes the subject
                // (an update from 0.7.0), else Set's still went on at the middle with Size Auto.
                if (force || !page || !checkedRecently(source)) checkSite(source, force, page) else noteStored(source)
            } catch (e: Exception) {
                if (e.isStop()) throw e
                failure = failure ?: e
            }
        }
        if (isStopped(stopped)) throw InterruptedException()
        if (!daily && !set) return Updated(null, false, failure, wentToSet = false)
        val (done, setFailure, gaveUp) = setDue(pull = force, setAsked = set, stopped)
        if (failure != null && done != null) Log.i("APODroid", "Despite the failure: $done")
        return Updated(done, setFailure != null, failure ?: setFailure, wentToSet = !gaveUp)
    }

    /**
     * A stop: the thread interrupted, or [stopped] (the job's `onStopJob` came). The interrupt
     * alone isn't enough: an interrupted thread's `commit()` returns early, skips the prefs
     * listeners and clears the flag, so after a commit the run would go on as if not stopped
     * (CR2's finding 2: the other screen's set, then "Save every picture"'s downloads).
     */
    private fun isStopped(stopped: () -> Boolean) = Thread.currentThread().isInterrupted || stopped()

    /**
     * A commit on a thread a stop may have reached: the flag cleared first, so the commit calls
     * the listeners (the open page hears a set's record), then the stop thrown.
     */
    private fun commitBeforeStop(stopped: () -> Boolean, action: SharedPreferences.Editor.() -> Unit) {
        val interrupted = Thread.interrupted()
        prefs.edit(commit = true, action)
        if (interrupted || stopped()) throw InterruptedException()
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
     * no retry. Returns what it saved for the log, or null. [stopped] as for [update].
     */
    fun saveInUse(stopped: () -> Boolean): String? {
        if (!saveEvery) return null
        val saved = Source.entries.mapNotNull { source ->
            prefs.getString(source.key("savedDate"), null)?.let { source to Picture.Of(source, LocalDate.parse(it), prefs.getString(source.key("savedUrl"), null)) }
        }.toMap()
        val tried = Source.entries.mapNotNull { source -> prefs.getString(source.key("saveTried"), null)?.let { source to it } }.toMap()
        val done = mutableListOf<String>()
        for ((post, original) in toSave(inUse(), stored(), saved, tried)) {
            if (isStopped(stopped)) throw InterruptedException()
            if (original) commitBeforeStop(stopped) { putString(post.source.key("saveTried"), post.imageUrl) }
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
     * A [pull] (pull to refresh) sets a newer picture of each screen's source at once. A stop
     * ([isStopped]) ends it before the next set, the record of a set done still written.
     * Without [setAsked] (not Set's), the daily switch turned off since ends it before the next set
     * too (O4c: off is off, also between a switch-on run's two sets), not thrown: what was done
     * is returned, and it gave up (the third value; [Updated.wentToSet] false, so B6's last on
     * runs again).
     *
     * **Live** (L5): before the rules a screen noted live (`wallpaperLive`) that isn't live now is
     * forgotten (`:core`'s [lostLive]; home's loss forgets both, Decision 12), so it's due again
     * and gets Fill in this run. Then, while home is live ([liveOn]), home's set is the engine's
     * hand-over ([copyForLive]: Live's copy, no `WallpaperManager` call, the record noted live
     * and `wallpaperSetAt` as after a set), and the lock screen's too while shared, with equal
     * columns (B28) and due home's picture; else the lock screen's still set with `FLAG_LOCK`, before home's hand-over
     * (`:core`'s [stillOrLive], Decision 10). A still set removes the live note.
     */
    private fun setDue(pull: Boolean, setAsked: Boolean, stopped: () -> Boolean): Triple<String?, Exception?, Boolean> = synchronized(wallpaperLock) {
        val now = Instant.now()
        val stored = stored()
        val setup = setup
        val columns = setup.columns
        // Lost Live (L5, Decision 12): a screen noted live that isn't live now is forgotten before
        // the rules, so it's due again and this run sets Fill; home's loss forgets both screens.
        val live = liveState()
        val onLock = lockEngine()
        val lost = lostLive(
            homeNoted = prefs.getBoolean(Screen.HOME.key("wallpaperLive"), false),
            lockNoted = prefs.getBoolean(Screen.LOCK.key("wallpaperLive"), false),
            homeLive = live.home, shared = live.shared,
            sharedNote = prefs.getBoolean("live.shared", false),
            lockColumn = columns[Screen.LOCK].orEmpty().isNotEmpty(),
            lockEngine = onLock,
        )
        // L8f1: in a process Android just started to rebind the engine (a process death with Live
        // on) a screen that only looks not live isn't lost: nothing set, the run failed, retried.
        if (liveHolds(liveSettling, lost)) {
            Log.i("APODroid", "Live: ${lost.forget.joinToString("+")} not live yet, the engine's process ${processAge.seconds} s old: nothing set")
            return Triple(null, LiveSettling(), false)
        }
        if (lost.forget.isNotEmpty()) {
            val why = when {
                prefs.getBoolean(Screen.HOME.key("wallpaperLive"), false) && !live.home -> "home isn't live"
                onLock -> "the engine is on the lock screen alone"
                else -> "the lock screen isn't live"
            }
            Log.i("APODroid", "Live: $why, Fill on ${lost.forget.joinToString("+")}${if (lost.lockTakesHome) ", the lock screen takes home's" else ""}")
        }
        // L6: a screen the engine is on whose record isn't noted live (just picked in Set's picker,
        // or Android's by hand) is forgotten too, so this run hands it over (`:core`'s unnotedLive).
        val unnotedLive = unnotedLive(
            setup.size, live.home, live.shared,
            homeNoted = prefs.getBoolean(Screen.HOME.key("wallpaperLive"), false),
            lockNoted = prefs.getBoolean(Screen.LOCK.key("wallpaperLive"), false),
            columns = columns,
        )
        if (unnotedLive.isNotEmpty()) Log.i("APODroid", "Live: the engine is on ${unnotedLive.joinToString("+")}, not noted live")
        var records = lost.forgetting(records()).mapValues { (screen, record) -> if (screen in unnotedLive) record.forget() else record }
        // A shared engine's lock screen without a column takes home's still set along (`:core`'s sharedLockFollows).
        fun plan(records: Map<Screen, Record>) = sharedLockFollows(
            lost.onPlan(dueWallpapers(columns, records, stored, setup.pictureTime, ZoneId.systemDefault(), now, pull, soon = setup.soon, rotation = rotation), records),
            setup.size, live.shared, lockColumn = columns[Screen.LOCK].orEmpty().isNotEmpty(),
        )
        fun split(plan: Plan) = stillOrLive(setup.size, live.home, live.shared, columns, plan.screens[Screen.HOME]?.record?.picture ?: records[Screen.HOME]?.picture, plan.sets)
        var plan = plan(records)
        // L6: home's hand-over would show its new picture on a lock screen the engine covers that
        // isn't due it, or (B28) the engine covers a lock screen whose column differs from home's
        // (one engine, home's pan): the lock screen forgotten and planned again (`:core`'s
        // lockFollowsHome), so it gets its own still Fill.
        // B27: with nothing of its own even then, it keeps what the engine draws now (Live's copy,
        // else its fallback) by a still set before the hand-over, its record kept. An engine that
        // draws black has nothing to keep: the lock screen follows home.
        val lock = records[Screen.LOCK]
        var keep: File? = null
        if (lock != null && lockFollowsHome(split(plan), live.shared, columns)) {
            val forgotten = records + (Screen.LOCK to lock.forget())
            val again = plan(forgotten)
            val drawn = liveFile.takeIf { it.exists() } ?: liveFallback()
            val why = if (equalColumns(columns)) "home's new picture would show on the lock screen" else "the engine covers the lock screen, whose column differs from home's"
            if (drawn != null && lockFollowsHome(split(again), live.shared, columns)) {
                Log.i("APODroid", "Live: $why, which has nothing of its own: it keeps ${if (drawn == liveFile) livePicture else drawn.name}")
                keep = drawn
            } else {
                Log.i("APODroid", "Live: $why, its own again")
                records = forgotten
                plan = again
            }
        }
        val unset = plan.screens.filterValues { !it.set }
        // A forgotten screen the rules leave out (no column) keeps its record, but not the live note.
        val unnoted = lost.forget - plan.screens.keys
        if (unset.isNotEmpty() || unnoted.isNotEmpty()) {
            commitBeforeStop(stopped) {
                putPlan(plan.copy(screens = unset))
                unnoted.forEach { remove(it.key("wallpaperLive")) }
            }
        }
        // Size Live while the picker put APODroid's engine on home: home's set goes to the engine,
        // the lock screen's too while shared, with equal columns and due home's picture, else still (`:core`'s
        // stillOrLive), the lock screen's still set first.
        val split = split(plan)
        val steps = split.still.map { it to false } + listOfNotNull(split.engine?.let { it to true })
        val done = mutableListOf<String>()
        val shown = mutableListOf<String>()
        fun said() = listOfNotNull(
            done.takeIf { it.isNotEmpty() }?.joinToString(prefix = "wallpaper set: "),
            shown.takeIf { it.isNotEmpty() }?.joinToString(prefix = "wallpaper shown live: "),
        ).joinToString("; ").ifEmpty { null }
        var failure: Exception? = null
        val failedStill = mutableListOf<Set<Screen>>()
        // B27: the lock screen keeps what the engine draws, a still set of it: Live's copy as the
        // engine draws it now (its whole flag, its darkening, the frame where the clock has it), the
        // fallback as it is. No record, no wallpaperSetAt: it shows what it showed. A failed one
        // holds home's hand-over (handOverWaits).
        keep?.let { file ->
            if (isStopped(stopped)) throw InterruptedException()
            val copy = file == liveFile
            try {
                setFile(
                    file, setOf(Screen.LOCK), liveAt(now), whole = copy && liveWhole, sideways = copy && liveSideways,
                    factor = if (copy) liveDarken else 1f,
                )
            } catch (e: Exception) {
                Log.w("APODroid", "Live: the lock screen's still set of what the engine draws failed: $e")
                failure = e
                failedStill += setOf(Screen.LOCK)
                return@let
            }
            Log.i("APODroid", "Live: the lock screen keeps ${if (copy) livePicture else file.name}, still")
            commitBeforeStop(stopped) { remove(Screen.LOCK.key("wallpaperLive")) }
        }
        for ((index, step) in steps.withIndex()) {
            val (entry, toEngine) = step
            val (picture, screens) = entry
            if (isStopped(stopped)) throw InterruptedException()
            if (index > 0 && !setAsked && !daily) {
                Log.i("APODroid", "Switched off after: ${said() ?: "nothing"}")
                return@synchronized Triple(said(), failure, true)
            }
            // The lock screen's still set failed while the engine covers it: home's hand-over
            // would show home's new picture there too (Decision 10). A failed set, so the
            // alarms' retry plans both again (`:core`'s handOverWaits).
            if (toEngine && handOverWaits(live.shared, failedStill)) {
                Log.i("APODroid", "Live: the lock screen's set failed, home's hand-over waits")
                failure = failure ?: IllegalStateException("the lock screen's set failed, home's hand-over waits")
                screens.forEach { setsSinceSetup[it] = false }
                continue
            }
            // The engine's hand-over writes Live's copy, no WallpaperManager call; a failed copy is a failed set.
            var copy: LiveCopy? = null
            val darker = try {
                if (toEngine) {
                    copyForLive(picture, stored).also { copy = it }.let(::liveNote)
                } else {
                    set(picture, screens, stored)
                }
            } catch (e: Exception) {
                failure = failure ?: e
                screens.forEach { setsSinceSetup[it] = false }
                if (!toEngine) failedStill += screens
                continue
            }
            screens.forEach { setsSinceSetup[it] = true }
            (if (toEngine) shown else done) += "$picture on ${screens.joinToString("+")}$darker"
            // A stop during the set (seconds with the whole picture) ends the run here.
            try {
                commitBeforeStop(stopped) {
                    copy?.let { putLive(picture, it) }
                    screens.forEach {
                        putRecord(it, plan.screens.getValue(it).record)
                        putSetAt(it)
                        if (toEngine) putBoolean(it.key("wallpaperLive"), true) else remove(it.key("wallpaperLive"))
                    }
                }
            } catch (e: InterruptedException) {
                Log.i("APODroid", "Stopped after: ${said()}")
                throw e
            }
        }
        if (lost.dropShared && failure == null) prefs.edit(commit = true) { remove("live.shared") }
        if (!pull && !setAsked) judgeMiss(plan)
        Triple(said(), failure, false)
    }

    /**
     * The detector (O14): after the alarms were found gone ([noteAlarmsGone]), the next ordinary
     * run's sets (the page's load or the run its arming started, whichever gets here first; not a
     * pull's nor Set's) tell whether a change was missed ([missedChange]): noted with "Keep
     * running"'s state (`missedAt`, `missedKeep`) for the box. Either way the note is used up: a
     * run with nothing to set lost nothing.
     */
    private fun judgeMiss(plan: Plan) {
        val lost = lost() ?: return
        val stamps = plan.sets.flatMap { (_, screens) -> screens.mapNotNull { plan.screens.getValue(it).record.stamp } }
        val missed = missedChange(stamps, offset, lost)
        val keep = keepRunning
        prefs.edit(commit = true) {
            removeLost()
            if (missed != null) {
                putLong("missedAt", missed.toEpochMilli())
                putBoolean("missedKeep", keep)
            }
        }
        Log.i("APODroid", "Detector: ${if (missed != null) "missed the change of $missed (Keep running ${if (keep) "on" else "off"})" else "nothing missed"}")
    }

    private fun SharedPreferences.Editor.putSetAt(screen: Screen) = putLong(screen.key("wallpaperSetAt"), System.currentTimeMillis())

    /** What is stored for `:core`: the posts, which pictures are there, the user's own, the video-day choice ([standIn]: Set's own before it's saved), the posts after a gap (B21). */
    private fun stored(standIn: StandIn = videoDays.standIn) = Stored(
        Source.entries.mapNotNull { s -> post(s)?.let { s to it } }.toMap(),
        Source.entries.filter { imageFile(it).exists() }.toSet(),
        fallbackFile.exists(),
        standIn,
        Source.entries.filter { prefs.getBoolean(it.key("afterGap"), false) }.toSet(),
    )

    /**
     * What [source]'s picture is now ([Stored.picture]): on APOD's video day the stand-in (NASA's
     * or Earth's, or the user's own; null with "Skip it" or nothing stored yet).
     */
    fun sourcePicture(source: Source): Picture? = stored().picture(source)

    /**
     * [refresh] unless, for the [page], [source]'s stored post is today's (always with [force];
     * the job's runs always ask, D1: a second post on the same date arrives at the next check),
     * noting how it went for the page: a success sets [checkedAt] and clears [problem], a failure
     * names it. Anything but the three typed failures counts as the network, except an
     * interrupted download (a stopped job), which notes nothing. A failure while today's post is
     * stored (the job's check of a complete day) is only logged: no problem, no failed run,
     * [checkedAt] untouched (D1's night default).
     */
    private fun checkSite(source: Source, force: Boolean, page: Boolean) {
        val current = isCurrent(source)
        try {
            // SA3a: a page open that skips today's stored post still notes its subject (an update from 0.7.0), for Set.
            if (force || !page || !current) refresh(source) else noteStored(source)
        } catch (e: Exception) {
            if (e.isStop()) throw e // not the site's fault
            if (current && !force) {
                Log.i("APODroid", "Refresh: ${source.name} ${post(source)?.date} stored, check failed: $e")
                return
            }
            val problem = Problem.of(e)
            prefs.edit {
                putString(source.key("problem"), problem.key)
                if (problem is Problem.Http) putInt(source.key("httpCode"), problem.code)
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

    // Live (0.7.0, L5; live-0.7.0.md, The engine and "Store, Size and Set"): APODroid's live
    // wallpaper (LiveWallpaper.kt) draws home's picture from its own copy; the rules decide it.

    /**
     * Size in Setup (L6, SA3a; the pref `size`, [Sizing]'s CENTRE, AUTO or LIVE, read by `:core`'s
     * [sizeOf]: default Auto, and 0.7.0's `FILL` reads as Auto until the first Set rewrites it):
     * Centre and Auto set every picture still (`:core`'s [stillOrLive]; a still set replaces
     * APODroid's live wallpaper like any other), Centre at the middle, Auto on the subject
     * ([subject], `:core`'s [framePosition]); Live hands home's picture to the engine while
     * Android's picker put it on home ([liveOn]), else still at the middle.
     */
    val size: Sizing
        get() = sizeOf(prefs.getString("size", null))

    /**
     * Before [saveSetup]: whether Set of [draft] opens Android's live wallpaper picker, and for
     * what (`:core`'s [opensPicker]): for home while its Size is Live and home has a column not
     * live, or (14+) for the lock screen too while its columns are equal, home is live and the
     * engine doesn't cover the lock screen (SPLIT). Asked before saving, so a back-out of the
     * picker saves nothing.
     */
    fun opensPicker(draft: Setup): PickerTrip {
        val live = liveState()
        return opensPicker(draft.size, draft.columns, live.home, live.shared, lockShares = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    }

    /**
     * The status line "Live isn't on" (L6): Size Live and home with a column, and APODroid's
     * live wallpaper not on home (never picked, backed out of the picker, another wallpaper
     * picked, a force stop, Android's revert). The page shows it while the daily switch is on.
     */
    val liveMissing: Boolean
        get() = size == Sizing.LIVE && columns[Screen.HOME].orEmpty().isNotEmpty() && !liveOn(Screen.HOME)

    /** Both screens' wallpaper ids (`getWallpaperId`, no permission; any set or clear changes one), noted before Set opens the picker ([awaitLive]). */
    fun wallpaperIds(): List<Int> {
        val manager = WallpaperManager.getInstance(context)
        return listOf(WallpaperManager.FLAG_SYSTEM, WallpaperManager.FLAG_LOCK).map { runCatching { manager.getWallpaperId(it) }.getOrDefault(-2) }
    }

    /**
     * Blocking. Back from the picker: when a wallpaper id changed since [before] ([wallpaperIds]),
     * waits up to 3 s for [liveOn] home (below 14 the engine notes its id asynchronously, at its
     * start), so Set's update doesn't set a still picture over the live wallpaper just picked.
     * Returns whether an id changed: false is a back-out, for which Set saves and sets nothing.
     * A wallpaper changed and home not live by then (the lock screen picked alone): Fill.
     */
    fun awaitLive(before: List<Int>): Boolean {
        val changed = wallpaperIds() != before
        val until = SystemClock.elapsedRealtime() + 3000
        while (changed && !liveOn(Screen.HOME) && SystemClock.elapsedRealtime() < until) Thread.sleep(100)
        Log.i("APODroid", "Live: back from the picker, ${if (changed) "a wallpaper changed" else "nothing changed"}, home ${if (liveOn(Screen.HOME)) "live" else "not live"}${if (liveShared) ", shared" else ""}")
        return changed
    }

    /** Live's copy of home's picture, the only picture the engine draws (not the source's file, which changes with every stored post). */
    val liveFile = File(context.filesDir, "live.jpg")

    /** Whether Live's copy is drawn whole (its post's [Part.whole] at the hand-over), noted beside it (`live.whole`). */
    val liveWhole: Boolean
        get() = prefs.getBoolean("live.whole", false)

    /**
     * Whether Live's copy is drawn turned 90° clockwise (its post's [Part.sideways] at the
     * hand-over, SW2), noted beside it (`live.sideways`); false for a copy from before 0.8.0.
     */
    val liveSideways: Boolean
        get() = prefs.getBoolean("live.sideways", false)

    /**
     * The darkening of Live's copy (BR3), `:core`'s [darkening] of its one measure at the
     * hand-over ([copyForLive]), noted beside it (`live.darken`); 1 while "Darken bright pictures"
     * is off, for a dark picture, a failed measure and a copy from before BR3.
     */
    val liveDarken: Float
        get() = prefs.getFloat("live.darken", 1f).coerceIn(0f, 1f)

    /** What Live's copy is, for the engine's log (`live.picture`). */
    val livePicture: String?
        get() = prefs.getString("live.picture", null)

    /** What [copyForLive] noted of Live's copy: its whole and sideways flags, its darkening and the measure (null: not measured). */
    private data class LiveCopy(val whole: Boolean, val sideways: Boolean, val factor: Float, val grey: Double?)

    /** The hand-over's log note: ", sideways" (SW2) and [darker]'s. */
    private fun liveNote(copy: LiveCopy) = sidewaysNote(copy.sideways) + darker(copy.factor, copy.grey)

    /** Writes the notes beside Live's copy of [picture] ([copyForLive]): its whole and sideways flags, its darkening and its name. */
    private fun SharedPreferences.Editor.putLive(picture: Picture, copy: LiveCopy) {
        putBoolean("live.whole", copy.whole)
        putBoolean("live.sideways", copy.sideways)
        putFloat("live.darken", copy.factor)
        putString("live.picture", picture.toString())
    }

    /**
     * Blocking, under [wallpaperLock]. The engine's hand-over: [picture]'s file (the source's or
     * the user's own) copied to [liveFile] via a `.part` and renamed, so the engine never reads
     * half a copy. Returns the whole and sideways flags of its post's stored part and, with
     * [darken], the darkening (BR3), measured once per picture so it doesn't change as the frame
     * moves: the whole picture's top third ([pictureGrey]), or a whole one's on its background
     * ([wholeGrey]), turned when sideways (SW2), for [putLive]; a failed measure leaves it at 1
     * (logged). Throws [StorageFailed] (a full disk): a failed set.
     */
    private fun copyForLive(picture: Picture, stored: Stored): LiveCopy {
        val file = if (picture is Picture.Of) imageFile(picture.source) else fallbackFile
        val part = File(context.filesDir, "live.part")
        try {
            file.inputStream().use { input -> part.outputStream().use { input.copyTo(it) } }
            if (!part.renameTo(liveFile)) throw IOException("Couldn't rename ${part.name}")
        } catch (e: IOException) {
            part.delete()
            throw StorageFailed(e)
        }
        val post = (picture as? Picture.Of)?.let { stored.post(it.source) }
        val noted = post?.let(parts::forPost)
        val whole = noted?.whole == true
        val sideways = picture is Picture.Of && turnsSideways(picture.source, noted, post)
        val grey = if (!darken) null else runCatching { if (whole) wholeGrey(liveFile, screen, sideways) else pictureGrey(liveFile, sideways) }
            .onFailure { Log.w("APODroid", "Live: couldn't measure the brightness: $it") }.getOrNull()
        return LiveCopy(whole, sideways, grey?.let(::darkening) ?: 1f, grey)
    }

    /**
     * What the engine draws without a copy (the picker's preview before the first hand-over, a
     * fresh install): the stored picture of home's column's first source in the list's order,
     * else APOD's; null: black.
     */
    fun liveFallback(): File? {
        val column = columns[Screen.HOME].orEmpty()
        return listOfNotNull(order.firstOrNull { it in column }, Source.APOD).map(::imageFile).firstOrNull { it.exists() }
    }

    /** Where the engine's frame is at [now] (`:core`'s [livePosition]): home's column's change times, with turns from two sources. */
    fun liveAt(now: Instant): Float =
        livePosition(now, ZoneId.systemDefault(), setup.pictureTime, turns = columns[Screen.HOME].orEmpty().size >= 2)

    private val liveComponent = ComponentName(context, LiveWallpaper::class.java)

    /**
     * Whether APODroid's live wallpaper is on [screen] (L1's table): home on Android 14+ when
     * `getWallpaperInfo(FLAG_SYSTEM)` names it or home's wallpaper id is the one noted while it
     * did (L8f1 fix round 1: a rebind after a process death), below 14 when home's wallpaper id
     * is the one the engine noted at its start (`live.homeId`: `getWallpaperInfo()` needs
     * `QUERY_ALL_PACKAGES` there); `:core`'s [homeLive]. The lock screen only while home is and
     * the one engine is shared ([liveShared]).
     * The engine's own flags aren't asked (a stray on the lock screen says 3).
     */
    fun liveOn(screen: Screen): Boolean = if (screen == Screen.HOME) liveState().home else liveState().shared

    /** A screen's record noted live (`wallpaperLive`, either screen): a stray engine is then owed a run at screen-on (D, L8). */
    val liveNoted: Boolean
        get() = Screen.entries.any { prefs.getBoolean(it.key("wallpaperLive"), false) }

    /**
     * D, the screen-on check (L8): whether an engine that became visible starts the alarms' run
     * (`:core`'s [screenOnRunOwed]: the daily switch on, no run started in the last 15 min, and a
     * check owed by [nextAlarm] or a [stray] engine while a record is noted live). On the main
     * thread: prefs only, no lock.
     */
    fun screenOnRunOwed(stray: Boolean): Boolean {
        // The clock read once: nextAlarm hands back its own now for an owed check, a read later would be ahead.
        val now = Instant.now()
        return coreScreenOnRunOwed(now, daily, nextAlarm(now), instant("lastRunAt"), stray, liveNoted)
    }

    /** How long ago Android started this process. */
    private val processAge: Duration
        get() = Duration.ofMillis(SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime())

    /**
     * L8f1: a non-preview engine created in this process ([LiveProcess.engine]) less than
     * `:core`'s [liveReconnect] after its start: Android may be rebinding the engine after a
     * process death, and its "home isn't live" then isn't trusted (`:core`'s [coreLiveSettling]).
     */
    val liveSettling: Boolean
        get() = coreLiveSettling(LiveProcess.engine, processAge)

    /** L8f1: what's left of [liveSettling]'s grace, zero when none. */
    val liveSettlingLeft: Duration
        get() = if (liveSettling) liveReconnect - processAge else Duration.ZERO

    /** The one engine on the lock screen too: home live and no lock wallpaper of its own (`getWallpaperId(FLAG_LOCK)` -1; 14+ only, L1). */
    val liveShared: Boolean
        get() = liveState().shared

    private class LiveState(val home: Boolean, val shared: Boolean)

    private fun liveState(): LiveState {
        val manager = WallpaperManager.getInstance(context)
        val named = liveNamed(manager)
        val id = runCatching { manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
        if (named) noteId(id)
        val noted = if (prefs.contains("live.homeId")) prefs.getInt("live.homeId", -1) else null
        val home = homeLive(named, id, noted)
        val shared = home && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            runCatching { manager.getWallpaperId(WallpaperManager.FLAG_LOCK) == -1 }.getOrDefault(false)
        return LiveState(home, shared)
    }

    /**
     * At each run's start ([update]): while home is live, notes whether the one engine covers the
     * lock screen too (`live.shared`), so a re-apply or a split through the system's picker is
     * seen; while home isn't, the last note stays for Decision 12's lock screen without a column.
     */
    private fun noteShared() {
        val live = liveState()
        if (live.home) prefs.edit(commit = true) { putBoolean("live.shared", live.shared) }
    }

    /**
     * 14+: whether the lock screen's own wallpaper is APODroid's engine (`getWallpaperInfo(FLAG_LOCK)`
     * names it and the lock screen has a wallpaper of its own, `getWallpaperId(FLAG_LOCK)` not -1):
     * "Lock screen" picked in Android's picker, or home reverted (Decision 12). `:core`'s [lostLive]
     * replaces it while home isn't live, and while home is live when the lock screen has a column;
     * Set's picker reads it for a "Lock screen" answer. Below 14: false.
     */
    fun lockEngine(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val manager = WallpaperManager.getInstance(context)
        return runCatching {
            manager.getWallpaperId(WallpaperManager.FLAG_LOCK) != -1 &&
                manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.component == liveComponent
        }.getOrDefault(false)
    }

    /** 14+: whether `getWallpaperInfo(FLAG_SYSTEM)` names APODroid's live wallpaper (below 14 the app can't ask: false). */
    private fun liveNamed(manager: WallpaperManager): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            runCatching { manager.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM)?.component == liveComponent }.getOrDefault(false)

    private fun noteId(id: Int) {
        if (id > 0 && (!prefs.contains("live.homeId") || prefs.getInt("live.homeId", -1) != id)) prefs.edit(commit = true) { putInt("live.homeId", id) }
    }

    /**
     * Notes home's wallpaper id as live's (`live.homeId`, [liveOn], `:core`'s [homeLive]); any set,
     * clear, revert or pick changes the id. Below 14 at the engine's start and re-apply, as it
     * is; on 14+ (L8f1 fix round 1) only while `getWallpaperInfo` names APODroid's (also at each
     * [liveOn] that finds it so), so a rebind after a process death, which keeps the id, still
     * reads live while `getWallpaperInfo` doesn't name it.
     */
    fun noteHomeId() {
        val manager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !liveNamed(manager)) return
        val id = runCatching { manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrNull() ?: return
        prefs.edit(commit = true) { putInt("live.homeId", id) }
    }

    /** ⛶'s stored part per source, for the page's draft ([Setup.parts]) and the job's set ([forPost]). */
    val parts: Map<Source, Part>
        get() = Source.entries.mapNotNull { source ->
            prefs.getString(source.key("cropDate"), null)?.let {
                source to Part(
                    LocalDate.parse(it),
                    prefs.getString(source.key("cropUrl"), null),
                    // Missing (SA3b, not dragged): Size decides (framePosition: Auto's subject, else the middle).
                    source.key("cropPosition").takeIf(prefs::contains)?.let { prefs.getFloat(it, 0.5f) },
                    prefs.getBoolean(source.key("whole"), false),
                    // Missing (SW3, a part from before 0.8.0 or untouched by the pill): Auto's turn (SH2a).
                    source.key("sideways").takeIf(prefs::contains)?.let { prefs.getBoolean(it, false) },
                )
            }
        }.toMap()

    /**
     * Blocking. Sets [picture] on [screens] (both: one set with both flags, one wallpaper); the
     * callers read [stored] and the records first: setting takes seconds, and a refresh storing
     * a new post meanwhile must not mark the new one as set. The crop is [wallpaperFrame], the
     * screen's portrait shape: without one, Android 15+ shows the left part of a wide picture.
     * The user's own picture (video days) is always Fill, centred. A source's post with ⛶'s
     * whole picture ([Part.whole]): all of it, on black or blurred ([wholePicture], `Drawing.kt`).
     * With [darken] (BR2) what the screen will show is measured first (its top third,
     * [topThirdGrey], [frameGrey]); a bright one (`:core`'s [darkening] below 1) goes on through a bitmap of
     * the screen's size drawn with the darkening (Fill's frame [decodeFrame]d alone, the EXIF
     * photo too; the whole picture darkened as drawn), all of it the hint. A factor of 1 leaves
     * every path as 0.6.0's. [Part.sideways] (SW2): the picture turned 90° clockwise, measured and
     * drawn turned, always through a bitmap (`setStream` can't turn). The frame's position
     * (SA3a, `:core`'s framePosition): the part's, else with Size Auto the [subject]'s for the
     * picture's orientation, else the middle (Centre; the screens Live leaves still). Writes no
     * record. The orientation [turnsSideways] (⛶'s pill, else Auto's turn, SH2a). Returns the log's note:
     * ", auto 0.78", ", sideways" (", sideways, auto turn" when Auto's turn, not the pill, turned it)
     * and ", darker ×0.78 (grey 141)", or "".
     */
    private fun set(picture: Picture, screens: Set<Screen>, stored: Stored): String {
        val file = if (picture is Picture.Of) imageFile(picture.source) else fallbackFile
        val post = (picture as? Picture.Of)?.let { stored.post(it.source) }
        val part = post?.let(parts::forPost)
        val sideways = picture is Picture.Of && turnsSideways(picture.source, part, post)
        // SH2a: the log's ", auto turn" when Auto's turn (not ⛶'s pill) turned it.
        val autoTurn = sideways && part?.sideways == null
        // SA3a: ⛶'s dragged part (SA3b: a part's position is null while not dragged), else with Auto the subject for this orientation, else the middle ("my picture" too).
        val subject = post?.let(::subject)
        val raw = if (sideways) subject?.sideways else subject?.position
        val position = framePosition(size, part?.position, raw, autoFallback)
        // The log's note while Auto placed the frame by the subject (the middle when it fell back).
        val auto = if (size == Sizing.AUTO && part?.position == null && raw != null) String.format(Locale.ROOT, ", auto %.2f", position) else ""
        return auto + setFile(file, screens, position, part?.whole == true, sideways, factor = null, autoTurn = autoTurn)
    }

    /**
     * Blocking. [set]'s drawing of [file] on [screens]: Fill at [position], or [whole]; turned 90°
     * clockwise when [sideways] (the frame `:core`'s sidewaysFrame, the band drawn through a
     * rotating matrix into a bitmap of the screen's size, `setBitmap`); darkened by [factor], or
     * with null by the measure while [darken] is on. An upright picture keeps every path as it
     * was. Writes no record.
     */
    private fun setFile(
        file: File, screens: Set<Screen>, position: Float, whole: Boolean, sideways: Boolean, factor: Float?, autoTurn: Boolean = false,
    ): String {
        val flags = screens.fold(0) { flags, it -> flags or if (it == Screen.HOME) WallpaperManager.FLAG_SYSTEM else WallpaperManager.FLAG_LOCK }
        val manager = WallpaperManager.getInstance(context)
        val screen = screen
        val darken = factor == null && darken
        val fixed = factor
        if (whole) {
            // The screen's size already: the hint is all of it. Measured with its background (Open 5).
            val whole = wholePicture(file, screen, sideways)
            dumpWhole(whole)
            val grey = if (darken) topThirdGrey(whole) else null
            val factor = fixed ?: grey?.let(::darkening) ?: 1f
            val bitmap = if (factor < 1f) darkened(whole, factor) else whole
            dumpSet(bitmap)
            manager.setBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), true, flags)
            return sidewaysNote(sideways, autoTurn) + darker(factor, grey)
        }
        // A failed measure leaves the picture as it is: the set matters more than the darkening.
        val grey = if (!darken) null else runCatching { frameGrey(file, screen, position, sideways) }
            .onFailure { Log.w("APODroid", "Couldn't measure the brightness: $it") }.getOrNull()
        val factor = fixed ?: grey?.let(::darkening) ?: 1f
        if (factor < 1f || sideways) {
            // Only the frame is decoded, at the screen's size, upright (an EXIF photo too); a
            // sideways band turned as it's drawn, no turned copy.
            val frame = decodeFrame(file, screen, position, screen.height, sideways)
            val bitmap = createBitmap(screen.width, screen.height)
            if (sideways) drawSideways(Canvas(bitmap), frame, screen, factor) else drawPanned(Canvas(bitmap), frame, screen, position, factor)
            frame.recycle()
            dumpSet(bitmap)
            manager.setBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), true, flags)
        } else if (needsTurning(file)) {
            // Android would measure it unturned but draw it turned, stretching it (seen on API 36).
            val bitmap = decodeUpright(file, screen)
            dumpSet(bitmap)
            manager.setBitmap(bitmap, wallpaperFrame(bitmap.width, bitmap.height, screen, position), true, flags)
        } else {
            dumpSet(null)
            val crop = imageBounds(file)?.let { wallpaperFrame(it.outWidth, it.outHeight, screen, position) }
            file.inputStream().use { manager.setStream(it, crop, true, flags) }
        }
        return sidewaysNote(sideways, autoTurn) + darker(factor, grey)
    }

    /** The log's note of a sideways set or hand-over (SW2): ", sideways", ", sideways, auto turn" when Auto's turn (not ⛶'s pill) turned it (SH2a), or "". */
    private fun sidewaysNote(sideways: Boolean, autoTurn: Boolean = false) = if (!sideways) "" else if (autoTurn) ", sideways, auto turn" else ", sideways"

    /** The log's note of a darkened set ([set]): ", darker ×0.78 (grey 141)", or "" at 1. */
    private fun darker(factor: Float, grey: Double?) =
        if (factor < 1f && grey != null) String.format(Locale.ROOT, ", darker ×%.2f (grey %.0f)", factor, grey) else ""

    private val debuggable get() = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    /**
     * In a debuggable build only: [bitmap], the whole picture [set] sets, to `files/debug-whole.png`,
     * so a check can compare the pixels before and after a change to the drawing (L4, L5); before
     * the darkening, which [dumpSet] shows.
     */
    private fun dumpWhole(bitmap: Bitmap) = dump(bitmap, "debug-whole.png")

    /**
     * In a debuggable build only: the bitmap [set] sets to `files/debug-set.png` (BR2), so a check
     * can measure the pixels (the darkening); a set by `setStream` (null: no bitmap) deletes it.
     */
    private fun dumpSet(bitmap: Bitmap?) {
        if (bitmap != null) dump(bitmap, "debug-set.png") else if (debuggable) File(context.filesDir, "debug-set.png").delete()
    }

    private fun dump(bitmap: Bitmap, name: String) {
        if (!debuggable) return
        runCatching {
            File(context.filesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { Log.w("APODroid", "Debug dump failed", it) }
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

/** One video still made at a time ([Store.makeVideoStill]); not [refreshLock]: it streams for seconds. */
private val videoStillLock = Any()

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

/** The MIME type of a decodable image, e.g. `image/jpeg`, or null. */
private fun imageType(file: File): String? = imageBounds(file)?.outMimeType

/**
 * The size of a decodable image as it's drawn, upright (SW3): [imageBounds] with width and height
 * swapped when its EXIF orientation transposes it (`:core`'s [uprightOf]: 5 to 8, a camera photo
 * turned a quarter), as `ImageDecoder` decodes it; null when it isn't one. The one size for the sideways rule ([Store.turnsSideways]).
 */
@SuppressLint("ExifInterface") // Its security bugs were in Android 7.0 and older; minSdk is 29.
fun uprightSize(file: File): Size? {
    val bounds = imageBounds(file) ?: return null
    val orientation = runCatching {
        android.media.ExifInterface(file).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
    }.getOrDefault(1)
    return uprightOf(bounds.outWidth, bounds.outHeight, orientation).let { (w, h) -> Size(w, h) }
}

/** Size and MIME type of a decodable image (`outWidth`, `outHeight`, `outMimeType`), or null. */
fun imageBounds(file: File): BitmapFactory.Options? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    return options.takeIf { it.outWidth > 0 && it.outHeight > 0 && it.outMimeType != null }
}

/** The entry of [T] named [name], null for none (a pref or saved state from another version). */
internal inline fun <reified T : Enum<T>> enumValue(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
