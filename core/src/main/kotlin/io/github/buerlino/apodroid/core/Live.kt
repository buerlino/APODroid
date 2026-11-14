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
 * Live forgets ([lostLive], Decision 12).
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
