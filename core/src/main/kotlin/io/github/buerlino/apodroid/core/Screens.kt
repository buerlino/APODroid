package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/*
 * What each screen is due (0.6.0). The rules, in short (the full rules: CLAUDE.md, Background
 * update, Turns):
 * - Each screen has a column, the sources ticked for it; an empty column is left alone. In
 *   APOD mode the columns come from "Where" ([apodColumns]).
 * - A column with two or more takes turns, on one shared clock (`turnStartedAt`). Each screen's
 *   [Record] notes whose turn it is ([Record.owner]), what it shows ([Record.picture]) and the
 *   clock's value it belongs to ([Record.stamp]).
 * - [dueWallpapers] works out every screen from one reading of the clock, before anything is
 *   set: the caller writes the clock first if it restarts, then the sets ([Plan.sets]), each
 *   screen's record right after its own set (a failed set leaves it, so the retry sets that
 *   screen only).
 * - Settings change only on Set ("Set as wallpaper"): the whole setup at once, then
 *   [restartAndForget] (the clock restarts, the records forgotten) and [dueWallpapers], so each
 *   screen gets what the setup says afresh: the settings always say what each screen gets.
 */

/** The two screens a wallpaper can go on. */
enum class Screen { HOME, LOCK }

/** The columns of APOD mode: APOD on the [screens] "Where" names, the other column empty. */
fun apodColumns(screens: Set<Screen>): Map<Screen, List<Source>> =
    Screen.entries.associateWith { if (it in screens) listOf(Source.APOD) else emptyList() }

/** The screens [source] is ticked for in these columns. */
fun Map<Screen, List<Source>>.screensOf(source: Source): Set<Screen> =
    Screen.entries.filter { source in this[it].orEmpty() }.toSet()

/**
 * The list's change ("Pictures from"): [source] on exactly [screens] (none: off both screens),
 * the other sources where they were, each column in order. A tick, an untick and a new dropdown
 * choice are all this.
 */
fun Map<Screen, List<Source>>.placing(source: Source, screens: Set<Screen>): Map<Screen, List<Source>> =
    Screen.entries.associateWith { screen ->
        val column = this[screen].orEmpty()
        Source.entries.filter { if (it == source) screen in screens else it in column }
    }

/** What a screen shows. */
sealed interface Picture {
    /**
     * [source]'s post of [date] with the picture [url]. A null [url] (a record from 0.5.0 or
     * before) matches that date's post, whatever its picture.
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
 * One screen's record. [owner]: whose turn it is (on an APOD video day still APOD while a
 * stand-in shows, so NASA standing in doesn't end APOD's turn). [picture]: what the screen shows
 * (null: forgotten or never set, so the owner's is due). [stamp]: the clock's value of the turn
 * it belongs to.
 */
data class Record(
    val owner: Source,
    val picture: Picture? = null,
    val stamp: Instant? = null,
) {
    /**
     * Set ([restartAndForget]): drops the picture, keeps the owner and the stamp, so the next run
     * puts the owner's current picture on again instead of moving the screen on one.
     */
    fun forget(): Record = copy(picture = null)
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
     * so a screen whose column differs keeps its picture.
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
    if (column.size < 2) return show(r.copy(owner = column.single(), stamp = current), stored.picture(column.single()))
    val hasPicture = { source: Source -> stored.picture(source) != null }
    r = when {
        r.stamp != current -> r.copy(owner = nextSource(column, r.owner, hasPicture) ?: r.owner, stamp = current)
        r.owner !in column || !hasPicture(r.owner) -> r.copy(owner = nextSource(column, r.owner, hasPicture) ?: r.owner)
        else -> r
    }
    return show(r, stored.picture(r.owner))
}

/**
 * Whether an APOD video day kept the picture of a screen with [column] and [record] (the page's
 * "video day, wallpaper kept"): APOD is ticked there and it's APOD's turn (APOD alone, or no
 * other source had a picture), and nothing stands in on it ("Skip it", the stand-in not stored
 * or not set yet). With turns "Skip it" passes the turn on, so that screen kept nothing.
 */
fun videoDayKept(column: List<Source>, record: Record?, stored: Stored): Boolean {
    if (Source.APOD !in column || stored.post(Source.APOD)?.isVideo != true) return false
    val picture = record?.picture ?: return false
    if (record.owner != Source.APOD) return false
    val standIn = stored.picture(Source.APOD) ?: return true
    return !shows(picture, standIn)
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
 * Set ("Set as wallpaper", S4): the setup is saved with the columns [before] → [after], then
 * [dueWallpapers] gives each screen what the setup says afresh. The clock restarts (to [now]);
 * the records of the screens with a column in [after] are forgotten ([Record.forget]: owner
 * kept) and those on the current turn take the new stamp (one still due a retry keeps its old
 * stamp, so it moves on). A screen left alone (an empty column) keeps its record, so the page
 * still knows what it shows. A screen whose column was empty and now isn't is in [Restart.drop]:
 * its record is stale (the app left it alone meanwhile), so it starts as a screen with no record
 * (on the other screen's source if that's in its column). Write the result at once, then run
 * [dueWallpapers].
 */
fun restartAndForget(
    before: Map<Screen, List<Source>>,
    after: Map<Screen, List<Source>>,
    clock: Instant?,
    records: Map<Screen, Record>,
    now: Instant,
): Restart {
    val used = Screen.entries.filter { after[it].orEmpty().isNotEmpty() }
    val drop = used.filter { before[it].orEmpty().isEmpty() && it in records }.toSet()
    val forgotten = records.filterKeys { it in used && it !in drop }
        .mapValues { (_, r) -> Due(r.forget().copy(stamp = if (r.stamp == clock) now else r.stamp), false) }
    return Restart(Plan(now, true, forgotten), drop)
}

/** [restartAndForget]'s outcome: the [plan] to write, and the screens whose record to remove ([drop]). */
data class Restart(val plan: Plan, val drop: Set<Screen>)
