package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/*
 * What each screen is due (0.6.0). The rules, in short (the decisions: plan-0.6.0.md in the skill,
 * until M2 writes them into CLAUDE.md, Background update):
 * - Each screen has a column, the sources ticked for it; an empty column is left alone. In
 *   APOD mode the columns come from "Where" ([apodColumns]).
 * - A column with two or more takes turns, on one shared clock (`turnStartedAt`). Each screen's
 *   [Record] notes whose turn it is ([Record.owner]), what it shows ([Record.picture]) and the
 *   clock's value it belongs to ([Record.stamp]).
 * - [dueWallpapers] works out every screen from one reading of the clock, before anything is
 *   set: the caller writes the clock first if it restarts, then the sets ([Plan.sets]), each
 *   screen's record right after its own set (a failed set leaves it, so the retry sets that
 *   screen only).
 * - A set by hand ([setByHand]) changes a screen's picture, not whose turn it is; the [Hold]
 *   keeps the picture until the screen's next normal change.
 * - A changed setting ("Where", the video-day choice, "Change picture", the mode, Fit) is not a
 *   set by hand: [Record.forget], then [dueWallpapers]. No hold, no clock restart.
 */

/** The two screens a wallpaper can go on. */
enum class Screen { HOME, LOCK }

/** The columns of APOD mode: APOD on the [screens] "Where" names, the other column empty. */
fun apodColumns(screens: Set<Screen>): Map<Screen, List<Source>> =
    Screen.entries.associateWith { if (it in screens) listOf(Source.APOD) else emptyList() }

/** What a screen shows. */
sealed interface Picture {
    /**
     * [source]'s post of [date] with the picture [url]. A null [url] (a record from 0.5.0 or
     * before, or an APOD video's post in a [Hold]) matches that date's post, whatever its picture.
     */
    data class Of(val source: Source, val date: LocalDate, val url: String?) : Picture

    /** The user's own picture (video days). */
    data object Own : Picture
}

/** This post as a [Picture.Of]; an APOD video's has no [Picture.Of.url]. */
fun Post.picture(): Picture.Of = Picture.Of(source, date, imageUrl)

/** What fills APOD's turn on an APOD video day ("If APOD is a video"). */
enum class StandIn(val source: Source? = null) {
    /** "Skip it": a screen with APOD alone keeps its picture, one with turns goes on to the next source. */
    SKIP,
    NASA(Source.NASA),
    EARTH(Source.EARTH),

    /** The user's own picture. */
    OWN,
}

/**
 * What is stored: the newest [posts], the sources whose picture file is there ([files]),
 * whether the user's own picture is there ([own]), and the video-day choice ([standIn]).
 */
class Stored(
    private val posts: Map<Source, Post>,
    private val files: Set<Source>,
    private val own: Boolean,
    private val standIn: StandIn,
) {
    fun post(source: Source): Post? = posts[source]

    /**
     * What [source] shows now: its stored picture; on an APOD video day APOD's is the stand-in's
     * (NASA's or Earth's stored picture, or the user's own; null with "Skip it" or nothing stored
     * yet). Null: nothing to show, so a turn passes it by.
     */
    fun picture(source: Source): Picture? {
        sourcePicture(source)?.let { return it }
        if (source != Source.APOD || posts[source]?.isVideo != true) return null
        return when (standIn) {
            StandIn.SKIP -> null
            StandIn.OWN -> Picture.Own.takeIf { own }
            else -> sourcePicture(standIn.source!!)
        }
    }

    private fun sourcePicture(source: Source): Picture.Of? =
        posts[source]?.takeIf { !it.isVideo && it.imageUrl != null && source in files }?.picture()
}

/**
 * A picture set by hand, held on its screen. [source] is whose picture it counts as: the shown
 * tab's (from APOD's video tab the stand-in counts as APOD's). [post] is that source's post when
 * it was set (exact, an APOD video's has no URL), [ownerPost] the post of a one-source screen's
 * ticked source then (null on a screen with turns, or when it had none).
 *
 * The hold ends at the screen's next normal change: with turns when its turn ends (the screen
 * then goes on from its [Record.owner], so equal columns meet again); with one ticked source
 * when that source's stored post is no longer [ownerPost] (it posted). Until then a newer post
 * of [source] is due at once and keeps the hold (on an APOD video the stand-in fills it; "Skip
 * it" keeps the held picture); the stand-in's own next post doesn't touch it. Any tick or
 * untick in the screen's column clears it ([columnsChanged]), and so do [Record.forget] and the
 * mode switch.
 *
 * Never on a screen with no ticks (it's left alone, so its picture stays anyway) nor for a hand
 * set of a one-source screen's own ticked source (that's its normal picture: "ends when it
 * posts" and "its newer post keeps the hold" would collide). Don't add one there.
 */
data class Hold(val source: Source, val post: Picture.Of?, val ownerPost: Picture.Of? = null)

/**
 * One screen's record. [owner]: whose turn it is (on an APOD video day still APOD while a
 * stand-in shows, so NASA standing in doesn't end APOD's turn). [picture]: what the screen shows
 * (null: forgotten or never set, so the owner's is due). [stamp]: the clock's value of the turn
 * it belongs to. [hold]: set by hand.
 */
data class Record(
    val owner: Source,
    val picture: Picture? = null,
    val stamp: Instant? = null,
    val hold: Hold? = null,
) {
    /**
     * A changed setting, the daily switch going on, the mode switch: drops the picture and the
     * hold, keeps the owner and the stamp, so the next run puts the owner's current picture on
     * again instead of moving the screen on one.
     */
    fun forget(): Record = copy(picture = null, hold = null)
}

/** A screen after a run: its [record], to write after its set when [set], else at once (when it changed). */
data class Due(val record: Record, val set: Boolean)

/**
 * The outcome of a run or a change: the [clock] afterwards ([restarted]: it's `now`, write it
 * first) and the [screens] whose record changed or which need a set. Screens not in it are
 * left alone.
 */
data class Plan(val clock: Instant?, val restarted: Boolean, val screens: Map<Screen, Due>) {
    /**
     * The sets, grouped by the due picture, not by the columns: both screens due the same picture
     * and both needing it → one set with both flags (one wallpaper); otherwise each on its own,
     * so a picture held on one screen survives on the other.
     */
    val sets: List<Pair<Picture, Set<Screen>>>
        get() {
            val due = screens.filterValues { it.set }.mapValues { it.value.record.picture!! }
            val pictures = due.values.toSet()
            return if (due.size == 2 && pictures.size == 1) listOf(pictures.single() to due.keys) else due.map { it.value to setOf(it.key) }
        }
}

/**
 * Per screen what is due, from one reading of the clock ([clock], `turnStartedAt`, turns of
 * [length]) at [now]. Screens with an empty column are left out.
 *
 * - **The clock** restarts (to [now], first) when a column takes turns and the turn is over
 *   ([isTurnOver]: [turnSlack], a [clock] in the future counts as over), never for an early
 *   change. Its value is the current stamp.
 * - **With turns**, a screen whose stamp isn't the current one is due its next source
 *   ([nextSource] from its owner, with a picture) and takes the current stamp; when no other
 *   source has a picture the stamp moves and nothing is set. An **early change** (the owner
 *   unticked or with nothing to show: no picture yet, an APOD video with "Skip it") moves the
 *   owner but not the stamp, so it ends with the turn.
 * - **With one source** the screen shows it (on an APOD video day the stand-in; with "Skip it"
 *   or nothing stored it keeps its picture) and takes the current stamp.
 * - **A screen with no record** starts on the other screen's owner if that's in its column,
 *   else the first ticked source with a picture, with the current stamp.
 * - **A held screen** ([Hold]) keeps its picture until its hold ends; a newer post of the held
 *   source is due at once and keeps the hold. It takes the current stamp, as any one-source
 *   screen does.
 * - A screen needs a set when its due picture isn't the one it shows ([Picture.Of.url] null
 *   matches by date); nothing to show keeps the picture.
 */
fun dueWallpapers(
    columns: Map<Screen, List<Source>>,
    clock: Instant?,
    length: Duration,
    records: Map<Screen, Record>,
    stored: Stored,
    now: Instant,
): Plan {
    val restarted = columns.values.any { it.size >= 2 } && isTurnOver(clock, length, now)
    val current = if (restarted) now else clock
    val due = mutableMapOf<Screen, Due>()
    val (known, unknown) = Screen.entries.filter { columns[it].orEmpty().isNotEmpty() }.partition { it in records }
    for (screen in known) due[screen] = dueOn(columns.getValue(screen), records.getValue(screen), current, stored)
    for (screen in unknown) {
        val column = columns.getValue(screen)
        val other = Screen.entries.first { it != screen }
        val owner = (due[other]?.record ?: records[other])?.owner?.takeIf { it in column }
            ?: column.firstOrNull { stored.picture(it) != null } ?: column.first()
        due[screen] = dueOn(column, Record(owner, stamp = current), current, stored)
    }
    return Plan(if (restarted) now else clock, restarted, due.filter { (screen, it) -> it.set || it.record != records[screen] })
}

private fun dueOn(column: List<Source>, record: Record, current: Instant?, stored: Stored): Due {
    var r = record
    val hold = r.hold
    if (hold != null) {
        val ends = r.picture == null ||
            if (column.size >= 2) r.stamp != current else stored.post(column.single())?.picture() != hold.ownerPost
        if (!ends) {
            // A one-source screen takes the current stamp while held too (with turns it has it
            // already), so a later tick or a restart by hand finds it on the current turn.
            r = r.copy(stamp = current)
            val post = stored.post(hold.source)?.picture()
            if (post == hold.post) return Due(r, false)
            val next = stored.picture(hold.source) ?: return Due(r, false)
            return show(r.copy(hold = hold.copy(post = post)), next)
        }
        r = r.copy(hold = null)
    }
    if (column.size < 2) return show(r.copy(owner = column.single(), stamp = current), stored.picture(column.single()))
    val hasPicture = { source: Source -> stored.picture(source) != null }
    r = when {
        r.stamp != current -> r.copy(owner = nextSource(column, r.owner, hasPicture) ?: r.owner, stamp = current)
        r.owner !in column || !hasPicture(r.owner) -> r.copy(owner = nextSource(column, r.owner, hasPicture) ?: r.owner)
        else -> r
    }
    return show(r, stored.picture(r.owner))
}

/** [record] showing [picture]; null keeps what it shows. */
private fun show(record: Record, picture: Picture?): Due {
    if (picture == null) return Due(record, false)
    return Due(record.copy(picture = picture), !shows(record.picture, picture))
}

private fun shows(noted: Picture?, due: Picture): Boolean =
    noted == due || noted is Picture.Of && due is Picture.Of && noted.url == null &&
        noted.source == due.source && noted.date == due.date

/**
 * A set by hand of [picture] from the [tab] of its source (APOD's video tab: the stand-in,
 * counted as APOD's) on [screens]. Every screen in the result is written after the set, one set
 * with [screens]' flags. The set screens get [picture] and a [Hold] (not on a screen with no
 * ticks, nor for a one-source screen's own source); their owner stays.
 *
 * When a set screen takes turns the clock restarts (to [now]), and the new stamp goes into the
 * set screens with turns and the screens whose stamp was the current one; a screen still due a
 * retry keeps its old stamp and is set on the next run. A set screen with turns that was due a
 * retry moves on its owner first, so equal columns stay in step. A set on screens without turns
 * leaves the clock and the other screen alone.
 */
fun setByHand(
    picture: Picture,
    tab: Source,
    screens: Set<Screen>,
    columns: Map<Screen, List<Source>>,
    clock: Instant?,
    records: Map<Screen, Record>,
    stored: Stored,
    now: Instant,
): Plan {
    val restarted = screens.any { columns[it].orEmpty().size >= 2 }
    val stamp = if (restarted) now else clock
    val hasPicture = { source: Source -> stored.picture(source) != null }
    val out = mutableMapOf<Screen, Due>()
    for (screen in Screen.entries) {
        val column = columns[screen].orEmpty()
        val record = records[screen]
        if (screen !in screens) {
            if (restarted && record != null && record.stamp == clock) out[screen] = Due(record.copy(stamp = now), false)
            continue
        }
        val owner = record?.owner ?: column.firstOrNull { hasPicture(it) } ?: column.firstOrNull() ?: tab
        out[screen] = Due(
            when {
                column.isEmpty() -> Record(owner, picture, record?.stamp ?: clock)
                column.size == 1 && tab == column.single() -> Record(tab, picture, stamp)
                column.size == 1 -> Record(column.single(), picture, stamp, Hold(tab, stored.post(tab)?.picture(), stored.post(column.single())?.picture()))
                else -> {
                    val moved = if (record != null && record.stamp != clock) nextSource(column, owner, hasPicture) ?: owner else owner
                    Record(moved, picture, now, Hold(tab, stored.post(tab)?.picture()))
                }
            },
            true,
        )
    }
    return Plan(if (restarted) now else clock, restarted, out)
}

/**
 * Ticks or unticks, [before] → [after]: clears the hold of each screen whose column changed (the
 * user is reconfiguring it; the next [dueWallpapers] gives it what its settings say). A column
 * taking turns when none did before restarts the clock (to [now]) with every record on it, so
 * the tick doesn't end the current picture at once; otherwise the clock stays.
 *
 * A screen whose column goes from empty to ticked starts as a screen with no record: its record
 * was left alone while the column was empty (or written by a hand set there), so its owner and
 * stamp are stale. It takes the other screen's owner and stamp if that source is in its new
 * column (so equal columns are one wallpaper at once; the other's stamp, not the clock's, so a
 * screen still due a retry and this one move on together), else the first ticked source with a
 * picture in [stored] and the current stamp. It keeps what it shows, so it's set only when that
 * isn't due. Write the result at once, then run [dueWallpapers].
 */
fun columnsChanged(
    before: Map<Screen, List<Source>>,
    after: Map<Screen, List<Source>>,
    clock: Instant?,
    records: Map<Screen, Record>,
    stored: Stored,
    now: Instant,
): Plan {
    val restarted = before.values.none { it.size >= 2 } && after.values.any { it.size >= 2 }
    val current = if (restarted) now else clock
    val stamped = records.mapValues { (screen, record) ->
        val changed = before[screen].orEmpty() != after[screen].orEmpty()
        record.copy(hold = if (changed) null else record.hold, stamp = if (restarted) now else record.stamp)
    }
    val started = { screen: Screen -> before[screen].orEmpty().isEmpty() && after[screen].orEmpty().isNotEmpty() }
    val out = stamped.mapValues { (screen, record) ->
        if (!started(screen)) return@mapValues record
        val column = after.getValue(screen)
        val other = Screen.entries.first { it != screen }
        val shared = stamped[other]?.takeIf { !started(other) && after[other].orEmpty().isNotEmpty() && it.owner in column }
        if (shared != null) record.copy(owner = shared.owner, stamp = shared.stamp)
        else record.copy(owner = column.firstOrNull { stored.picture(it) != null } ?: column.first(), stamp = current)
    }.filter { (screen, it) -> it != records[screen] }.mapValues { Due(it.value, false) }
    return Plan(current, restarted, out)
}

/**
 * The daily switch going on: the clock restarts (to [now]), every record is forgotten
 * ([Record.forget]) and the screens on the current turn take the new stamp, so the current
 * sources' pictures go on again. Write the result at once, then run [dueWallpapers].
 */
fun restartAndForget(clock: Instant?, records: Map<Screen, Record>, now: Instant): Plan =
    Plan(now, true, records.mapValues { (_, r) -> Due(r.forget().copy(stamp = if (r.stamp == clock) now else r.stamp), false) })
