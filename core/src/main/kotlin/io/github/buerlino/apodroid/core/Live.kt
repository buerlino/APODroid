package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Live, the day's pan (0.7.0, L3; live-0.7.0.md, The position and "Store, Size and Set";
 * CLAUDE.md, 0.7.0: Live). What decides the picture stays the rules' (Screens.kt); this file has
 * where the engine draws it ([livePosition]), at what size it decodes it ([liveDecodeSize]),
 * which of a run's sets go to the engine and which are still ([stillOrLive]), and what a lost
 * Live forgets ([lostLive], Decision 12), and when Set opens Android's picker ([opensPicker], L6).
 */

/** Size in Setup: Fill (a still set, as 0.6.0) or Live (home's picture drawn by APODroid's live wallpaper). */
enum class Size { FILL, LIVE }

/** A picture crosses the screen in its turn, but in this long at most (Decision 1: about 18 h, 06:00 to 24:00); then it stands still (Decision 2). */
val liveSpan: Duration = Duration.ofHours(18)

/** The largest picture the engine decodes, in bytes (ARGB, 4 a pixel; 1 MB = 1,000,000 bytes): L1, 50 MB a bitmap. */
const val LIVE_DECODE_BUDGET: Long = 50_000_000

/**
 * Where the frame is at [now] ([wallpaperFrame]'s position, 0 the left edge, 1 the right), by
 * the clock only (not by when the picture went on): T the column's current change time
 * ([PictureTime.last], [turns] with two or more sources), the span the shorter of the turn
 * (T to [PictureTime.next], real time, so a DST night's turn is an hour longer or shorter) and
 * [liveSpan], p = (now − T) / span, held at 1 after the span until the next change time.
 */
fun livePosition(now: Instant, zone: ZoneId, time: PictureTime, turns: Boolean): Float {
    val t = time.last(now, zone, turns)
    val turn = Duration.between(t, time.next(t, zone, turns))
    val span = minOf(turn, liveSpan)
    val p = Duration.between(t, now).toMillis().toDouble() / span.toMillis()
    return p.coerceIn(0.0, 1.0).toFloat()
}

/**
 * The size the engine decodes a [width]×[height] picture at: its shape at [screenHeight] (the
 * screen's portrait height, the longer side; landscape draws the same decode scaled down, so a
 * rotation decodes nothing), never larger than the picture, and smaller, the shape kept, while
 * width × height × 4 bytes would be over [maxBytes] ([LIVE_DECODE_BUDGET]). At least 1×1.
 */
fun liveDecodeSize(width: Int, height: Int, screenHeight: Int, maxBytes: Long = LIVE_DECODE_BUDGET): Pair<Int, Int> {
    require(width > 0 && height > 0 && screenHeight > 0) { "size $width×$height at $screenHeight" }
    var scale = minOf(1.0, screenHeight.toDouble() / height)
    val pixels = maxBytes / 4.0
    if (width * scale * height * scale > pixels) scale = sqrt(pixels / (width.toDouble() * height))
    var w = maxOf(1, (width * scale).roundToInt())
    var h = maxOf(1, (height * scale).roundToInt())
    while (w.toLong() * h * 4 > maxBytes && (w > 1 || h > 1)) {
        scale *= 0.999
        w = maxOf(1, floor(width * scale).toInt())
        h = maxOf(1, floor(height * scale).toInt())
    }
    return w to h
}

/**
 * A run's sets split for Live ([stillOrLive]), in this order: the [still] sets first (as
 * [Plan.sets]: a picture and its screens, one set with both flags when both), then the
 * [engine]'s hand-over (home's picture and the screens noted live without a set: home, and the
 * lock screen while the one engine shows home's picture there too). The lock screen's still set
 * comes before home's hand-over, so it never flashes home's new picture. Null [engine]: nothing
 * for the engine this run.
 */
data class LiveSets(val still: List<Pair<Picture, Set<Screen>>>, val engine: Pair<Picture, Set<Screen>>? = null)

/**
 * Which of a run's [sets] ([Plan.sets]) the engine takes and which are set still (live-0.7.0.md,
 * "Store, Size and Set"; Decisions 4, 5, 10):
 * - Size [Size.FILL], home not [homeLive], or no home column ([homeColumn] false: Live is
 *   home's): every set still, as 0.6.0 (a still set replaces APODroid's engine like any
 *   wallpaper, a stray on the lock screen too).
 * - Size Live and home live: home's set goes to the engine; the lock screen's goes to the engine
 *   too only while [shared] (the one engine on both) and it's due home's picture ([homePicture],
 *   home's after the plan, whether or not home is set this run), else it's set still
 *   (`FLAG_LOCK`; a shared engine then moves to home).
 * A screen without a column has no set in [sets] (but Decision 12's, [lostLive]) and gets none.
 */
fun stillOrLive(
    size: Size,
    homeLive: Boolean,
    shared: Boolean,
    homeColumn: Boolean,
    homePicture: Picture?,
    sets: List<Pair<Picture, Set<Screen>>>,
): LiveSets {
    if (size != Size.LIVE || !homeLive || !homeColumn) return LiveSets(sets)
    val still = mutableListOf<Pair<Picture, Set<Screen>>>()
    val engine = mutableSetOf<Screen>()
    var enginePicture: Picture? = null
    for ((picture, screens) in sets) {
        if (Screen.HOME in screens) {
            engine += Screen.HOME
            enginePicture = picture
        }
        if (Screen.LOCK in screens) {
            if (shared && (Screen.HOME in screens || picture == homePicture)) {
                engine += Screen.LOCK
                if (enginePicture == null) enginePicture = picture
            } else {
                still += picture to setOf(Screen.LOCK)
            }
        }
    }
    return LiveSets(still, enginePicture?.let { it to engine })
}

/**
 * What a lost Live does before the rules run ([lostLive]; live-0.7.0.md, Lost Live): the
 * records to [forget] ([Record.forget]: the picture dropped, owner and stamp kept, so the rules
 * make the screen due again and it gets Fill at once), whether a lock screen without a column
 * takes home's picture ([lockTakesHome]: one set with both flags, [onPlan]), and whether
 * `live.shared` goes ([dropShared]). Forgetting removes the records' live notes too (the caller's).
 */
data class LostLive(val forget: Set<Screen> = emptySet(), val lockTakesHome: Boolean = false, val dropShared: Boolean = false) {
    /** [records] with the screens in [forget] forgotten. */
    fun forgetting(records: Map<Screen, Record>): Map<Screen, Record> =
        records.mapValues { (screen, record) -> if (screen in forget) record.forget() else record }

    /**
     * [plan] ([dueWallpapers] on [forgetting]'s records) with Decision 12's lock screen without a
     * column added: due home's picture, home's record ([records] when the plan leaves home out),
     * so [Plan.sets] makes one set with both flags while home is set too. Unchanged without
     * [lockTakesHome] or when home has no picture.
     */
    fun onPlan(plan: Plan, records: Map<Screen, Record>): Plan {
        if (!lockTakesHome) return plan
        val home = plan.screens[Screen.HOME]?.record ?: records[Screen.HOME] ?: return plan
        if (home.picture == null) return plan
        return Plan(plan.screens + (Screen.LOCK to Due(home, set = true)))
    }
}

/**
 * Lost Live (live-0.7.0.md, "Store, Size and Set"; Decisions 4 and 12), from the records' live
 * notes ([homeNoted], [lockNoted]: `wallpaperLive`, `lock.wallpaperLive`), whether home is live
 * now ([homeLive], `Store.liveOn(HOME)`), whether the one engine covers the lock screen now
 * ([shared], `Store.liveShared`), the last noted `live.shared` ([sharedNote]) and whether the lock
 * screen has a column ([lockColumn]):
 * - **Home's record noted live and home not live** (another wallpaper, a force stop, Android's
 *   revert after two deaths in 10 s, which leaves the engine on the lock screen): home's record
 *   forgotten, and the lock screen's when it has a column; a lock screen without a column that
 *   the engine covered ([sharedNote]) takes home's picture; `live.shared` goes. Every set of the
 *   run is then still (home isn't live, [stillOrLive]), so a stray engine is replaced.
 * - **Else the lock screen's record noted live and not [shared]** (the user set a lock
 *   wallpaper): it alone is forgotten.
 * - Else nothing.
 */
fun lostLive(homeNoted: Boolean, lockNoted: Boolean, homeLive: Boolean, shared: Boolean, sharedNote: Boolean, lockColumn: Boolean): LostLive = when {
    homeNoted && !homeLive -> LostLive(
        forget = if (lockColumn) setOf(Screen.HOME, Screen.LOCK) else setOf(Screen.HOME),
        lockTakesHome = !lockColumn && sharedNote,
        dropShared = true,
    )
    lockNoted && !(homeLive && shared) -> LostLive(forget = setOf(Screen.LOCK))
    else -> LostLive()
}

/**
 * Whether home's hand-over in [split] ([stillOrLive]) would also change a lock screen the one
 * engine covers ([shared]) that has a column ([lockColumn]) and has no set of its own in this run
 * (L6, found by SIM): the engine would draw home's new picture there while the lock screen's
 * record names its own. The caller then forgets the lock screen's record and plans again, so it
 * gets its due picture this run: the engine's too when it's home's new one, else its still Fill
 * first (Decision 10). When it holds for that second plan too (nothing of its own to show, B27),
 * the lock screen keeps what the engine draws now by a still set before the hand-over, its record
 * kept. With equal columns the lock screen is due home's picture with it, so never.
 */
fun lockFollowsHome(split: LiveSets, shared: Boolean, lockColumn: Boolean): Boolean {
    val (_, screens) = split.engine ?: return false
    return shared && lockColumn && Screen.HOME in screens && Screen.LOCK !in screens && split.still.none { Screen.LOCK in it.second }
}

/**
 * Whether the engine's hand-over in this run waits (L6f1): while the one engine covers the lock
 * screen ([shared]) and a still set of this run that included the lock screen failed
 * ([failedStill], the screens of each failed still set). Else the engine would draw home's new
 * picture on the lock screen too, against Decision 10 (the lock screen never shows home's new
 * picture through the engine while its own set is due). The caller counts the skipped hand-over
 * as a failed set, so the alarms' retry plans both again.
 */
fun handOverWaits(shared: Boolean, failedStill: List<Set<Screen>>): Boolean =
    shared && failedStill.any { Screen.LOCK in it }

/**
 * The screens APODroid's engine is on whose records aren't noted live (L6), forgotten before the
 * rules as [lostLive]'s are ([Record.forget]: owner and stamp kept), so the same run hands home's
 * picture to the engine and gives the lock screen its own (the engine's too when it's home's,
 * else its still Fill, Decision 10). Only with Size [Size.LIVE] and home [homeLive]: home when it
 * has a column and isn't [homeNoted]; the lock screen while [shared] when it has a column and
 * isn't [lockNoted]. Else the engine went on (Set's picker, or Android's picker by hand) and drew
 * its old copy while the records named other pictures: a Set of the order alone, or one with
 * nothing due, set nothing. With Size Fill a live screen waits for its next set (any still set
 * replaces the engine).
 */
fun unnotedLive(size: Size, homeLive: Boolean, shared: Boolean, homeNoted: Boolean, lockNoted: Boolean, columns: Map<Screen, List<Source>>): Set<Screen> {
    if (size != Size.LIVE || !homeLive) return emptySet()
    return buildSet {
        if (!homeNoted && columns[Screen.HOME].orEmpty().isNotEmpty()) add(Screen.HOME)
        if (shared && !lockNoted && columns[Screen.LOCK].orEmpty().isNotEmpty()) add(Screen.LOCK)
    }
}

/** The same sources on both screens, home's not empty: the rules' equal columns ([dueWallpapers]), whose lock screen takes home's record and picture. */
fun equalColumns(columns: Map<Screen, List<Source>>): Boolean {
    val home = columns[Screen.HOME].orEmpty()
    return home.isNotEmpty() && home.toSet() == columns[Screen.LOCK].orEmpty().toSet()
}

/**
 * Whether "Set as wallpaper" opens Android's live wallpaper picker (L6; live-0.7.0.md, "Store,
 * Size and Set", Set with Live), after saving the setup: [columns] the saved ones, [before] the
 * stored ones before this Set, [homeLive] and [shared] as the system says now (`Store.liveOn`,
 * `liveShared`), [lockShares] whether the one engine can cover the lock screen at all (Android
 * 14+; below it a live wallpaper is home's only, Decision 5).
 * - Size [Size.LIVE], home has a column and isn't live: the picker, for home (also after a
 *   back-out or a lost Live: every such Set offers it again).
 * - Or, on [lockShares] only, this Set makes the columns equal ([equalColumns]), the stored ones
 *   having differed, while home is live and the engine doesn't cover the lock screen: once per
 *   split, since the next Set finds them equal before it (a home-only answer or a back-out
 *   leaves the lock screen Fill, with no picker again).
 * Never with Size Fill, never for the lock screen alone (no home column: Live is home's), never
 * for a lock column that differs (Decision 10: the lock screen Fill), and not for equal columns
 * that were equal before (APOD mode's "Where" Both with home-only Live: the lock screen Fill).
 */
fun opensPicker(size: Size, before: Map<Screen, List<Source>>, columns: Map<Screen, List<Source>>, homeLive: Boolean, shared: Boolean, lockShares: Boolean): Boolean = when {
    size != Size.LIVE || columns[Screen.HOME].orEmpty().isEmpty() -> false
    !homeLive -> true
    else -> lockShares && !shared && equalColumns(columns) && !equalColumns(before)
}

/**
 * Set's picker (L6, fix round 1): [start]s each of [options] in order and returns the first
 * that started, null when none did. Any [RuntimeException] of a start is passed to [failed] and
 * the next is tried: a maker's picker may be missing (`ActivityNotFoundException`), not
 * exported or guarded by a permission (`SecurityException`), and an uncaught one crashed Set
 * after its setup was saved, on every Set again.
 */
fun <T> firstStarted(options: List<T>, failed: (T, RuntimeException) -> Unit, start: (T) -> Unit): T? {
    for (option in options) {
        try {
            start(option)
            return option
        } catch (e: RuntimeException) {
            failed(option, e)
        }
    }
    return null
}

/** D (L8): a screen-on starts no run within this long after the last run's start (`lastRunAt`). */
val screenOnQuiet: Duration = Duration.ofMinutes(15)

/**
 * D, the screen-on check (L8; live-0.7.0.md, D): whether a non-preview engine that becomes
 * visible starts the alarms' run (job 2, `DailyJob.runNow`). Only while [daily] is on and no run
 * started in the last [screenOnQuiet] ([lastRun], `lastRunAt`; one ahead of [now], the clock set
 * back, doesn't hold it back); then when a check is owed ([nextAlarm] ≤ [now]: a check time passed
 * since the last run, or a failed run's retry due), or the engine is a [stray] (14+, home not live)
 * while a record is still [notedLive], so Decision 12's Fill on both screens comes at this
 * screen-on. The run removes the notes, so a stray left on a lock screen doesn't start a run at
 * every screen-on.
 */
fun screenOnRunOwed(now: Instant, daily: Boolean, nextAlarm: Instant, lastRun: Instant?, stray: Boolean, notedLive: Boolean): Boolean {
    if (!daily) return false
    if (lastRun != null && !lastRun.isAfter(now) && Duration.between(lastRun, now) < screenOnQuiet) return false
    return !nextAlarm.isAfter(now) || (stray && notedLive)
}
