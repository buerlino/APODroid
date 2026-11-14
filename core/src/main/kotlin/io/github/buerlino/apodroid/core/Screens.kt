package io.github.buerlino.apodroid.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * What each screen is due (0.6.0; the picture time since J1; turns as a plain rotation since
 * RO1, 2026-11-14). The rules (CLAUDE.md, Background update, Turns; the times in PictureTime.kt),
 * by the clock, never by what's on the screen:
 * 1. At a change time a screen changes: with one source to its newest picture, with several to
 *    the turn's source by the clock ([PictureTime.turn] mod the column, in the list's order from
 *    its anchor, [Rotation]; APOD → NASA → Earth from 1970-01-01 without one) and its newest
 *    picture, also one shown before; a source with nothing to show passes the turn to the next
 *    in the list's order with a picture.
 * 2. A newer picture of the source that went on, published in the [pictureWindow] after the
 *    change time, still replaces it whenever a check sees it (B15: by the publish time, not by
 *    when it's seen). With one source that is the whole rule: from each change time T the
 *    screen shows the newest picture published before T + 3 h, and never goes back; a post
 *    stored after a day without a successful check ([Stored.gaps], B21) goes on whenever a run
 *    sees it, so the day's picture isn't lost to a newer one.
 * 0.6.0's "a picture it hasn't shown" and rule 3 (with nothing new the screen waits) went with
 * RO1 (user, 2026-11-11: the screen was unpredictable; repeats accepted).
 * - Each screen has a column, the sources ticked for it; an empty column is left alone. In
 *   APOD mode the columns come from "Where" ([apodColumns]).
 * - Each screen's [Record] notes whose turn it is ([Record.owner]), what it shows
 *   ([Record.picture]), the change time it belongs to ([Record.stamp]) and the last picture it
 *   showed of each source ([Record.shown]; the rules no longer read it, the page's notice does
 *   until RO2).
 * - [dueWallpapers] works out every screen before anything is set: the caller writes the records
 *   without a set at once, then the sets ([Plan.sets]), each screen's record right after its own
 *   set (a failed set leaves it, so the retry sets that screen only).
 * - Settings change only on Set ("Set as wallpaper"): the whole setup at once, then
 *   [restartAndForget] (the records' pictures forgotten, the change times kept) and
 *   [dueWallpapers], so each screen gets what the setup says afresh.
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
 * whether the user's own picture is there ([own]), the video-day choice ([standIn]), and the
 * sources whose newest post was stored after a gap in their checks ([gaps], [afterGap]; B21).
 */
class Stored(
    private val posts: Map<Source, Post>,
    private val files: Set<Source>,
    private val own: Boolean,
    private val standIn: StandIn,
    private val gaps: Set<Source> = emptySet(),
) {
    fun post(source: Source): Post? = posts[source]

    /**
     * Whether something will take APOD's place on its video day (B23): NASA's or Earth's
     * picture, stored or still to load, or the user's own when it's there; not "Skip it".
     */
    val standsIn: Boolean get() = standIn != StandIn.SKIP && (standIn != StandIn.OWN || own)

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

    /**
     * Whether what [source] shows now ([picture]) was published before [limit] (B15): its post
     * ([Post.publishedAt]), and a stand-in's own post too. With [orAfterGap] (one source, B21) a
     * post stored after a gap ([gaps]) counts as published before it. False with nothing stored.
     */
    fun publishedBefore(source: Source, limit: Instant, orAfterGap: Boolean = false): Boolean {
        fun due(post: Post) = post.publishedAt < limit || orAfterGap && post.source in gaps
        val post = posts[source] ?: return false
        if (!due(post)) return false
        val picture = picture(source) as? Picture.Of ?: return true
        return picture.source == source || posts[picture.source]?.let(::due) == true
    }

    private fun sourcePicture(source: Source): Picture.Of? =
        posts[source]?.takeIf { !it.isVideo && it.imageUrl != null && source in files }?.picture()
}

/**
 * One screen's record. [owner]: whose turn it is (on an APOD video day still APOD while a
 * stand-in shows). [picture]: what the screen shows (null: forgotten by Set or never set, so the
 * owner's is due). [stamp]: the change time it belongs to (a stamp that isn't one counts as the
 * current change time). [shown]: per source the last post this screen showed (date and picture
 * URL); a stand-in counts for its own source too, and on a video day APOD's post for APOD. Since
 * RO1 the rules read neither [shown] nor [owner] to choose a turn (the clock does); [owner] says
 * whose picture a change put on, for rule 2 and the page.
 */
data class Record(
    val owner: Source,
    val picture: Picture? = null,
    val stamp: Instant? = null,
    val shown: Map<Source, Picture.Of> = emptyMap(),
) {
    /**
     * Set ([restartAndForget]): drops the picture, keeps the owner, the stamp and the notes, so
     * the next run puts the owner's current picture on again instead of moving the screen on.
     */
    fun forget(): Record = copy(picture = null)
}

/** A screen after a run: its [record], to write after its set when [set], else at once (when it changed). */
data class Due(val record: Record, val set: Boolean)

/** The outcome of a run or a change: the [screens] whose record changed or which need a set. Screens not in it are left alone. */
data class Plan(val screens: Map<Screen, Due>) {
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
 * Per screen what is due at [now] by the change times of [time] in [zone] (a column with two or
 * more sources takes turns: every [PictureTime.everyHours] from [PictureTime.pictureAt]; one
 * source: [PictureTime.pictureAt] only). Screens with an empty column are left out.
 *
 * - **A change time** since the record's stamp (rule 1): with one source the screen is due its
 *   newest picture (on an APOD video day the stand-in; with "Skip it" or nothing stored it keeps
 *   its picture) and takes the change time; with several the turn's source by the clock
 *   ([Rules.turnOf]: [PictureTime.turn] − the anchor's mod the column in the [rotation]'s order;
 *   RO1's APOD → NASA → Earth with the default) becomes the owner and its newest
 *   picture is due, also one this screen has shown (RO1: a plain rotation, repeats accepted). A
 *   source with nothing to show ([Stored.picture] null: nothing stored, APOD's video with "Skip
 *   it" or "Use my picture" without one) passes the turn to the next with a picture; none has
 *   one: the screen keeps its picture. Missed change times make one change, by the current one's
 *   number.
 * - **One source** (B15): whenever a run comes, the screen takes the current change time T
 *   and is due its newest stored picture when that was published before T + [pictureWindow]
 *   ([Stored.publishedBefore]; a post stored before B15 counts from its date's 00:00 US
 *   Eastern) or stored after a gap in its checks ([Stored.gaps], B21: a day offline would
 *   otherwise lose the day's picture to a newer one); one published later waits for the next
 *   change time. A function of the clock, the posts and their marks: a failed or offline check
 *   moves nothing, the picture goes on whenever a run sees it. A forgotten picture (Set), a new screen and [pull] put the newest on at once, whenever
 *   it was published.
 * - **Within the change** (several sources, the stamp is the current change time): a newer
 *   picture of the owner published before the stamp + [pictureWindow] replaces it whenever a run
 *   sees it (rule 2); a later one waits for the owner's next turn. A forgotten picture (Set) or
 *   a new screen puts the owner's picture on at once; an owner with nothing to show or no
 *   longer ticked gives way to the next new source, else the next with any picture. [pull] (a
 *   pull to refresh) puts the owner's newest on at once. A post that comes out after the window
 *   waits for its source's next turn (0.6.0's waiting screen went with RO1).
 * - **A stamp that isn't a change time** (0.6.0's Set clock, migrated 0.5.0 turns, a time zone
 *   changed since) or one after [now] (the clock set back) counts as the current change time: it
 *   takes it; with several sources nothing is set until the next one (but with [pull]), with
 *   one the publish time decides as above.
 * - **The stand-in** fills APOD's turn on an APOD video day, also a picture this screen has shown
 *   (RO1: B22's "not twice" went; NASA's picture may show in APOD's turn and again in NASA's).
 * - **Never back:** a picture of the same source with an older date than the one on the screen
 *   isn't set (only a stored post older than the shown one could ask for it).
 * - **A screen with no record** starts on the turn's source by the clock (or the next with a
 *   picture), with the current change time, and is set at once.
 * - **Equal columns** (the same sources on both screens) change together: home goes as above
 *   and the lock screen takes
 *   home's record and picture, so [Plan.sets] makes one set with both flags. So the screens
 *   match again after Set from columns that differed, and after a failed set on one screen (B2).
 * - A screen needs a set when its due picture isn't the one it shows ([Picture.Of.url] null
 *   matches by date); nothing to show keeps the picture.
 * - **[soon]** ("As soon as it's out", SO1): rule 2 without the [pictureWindow]. A newer picture
 *   of the screen's owner (one source: its source; turns: the turn's source, on an APOD video day
 *   its stand-in) goes on whenever a run sees it, whenever it was published; another source's new
 *   picture still waits for its turn. Everything else as above (never back, equal columns, the
 *   change times); [Stored.gaps] changes nothing while it's on. Off: 0.6.0's window.
 */
fun dueWallpapers(
    columns: Map<Screen, List<Source>>,
    records: Map<Screen, Record>,
    stored: Stored,
    time: PictureTime,
    zone: ZoneId,
    now: Instant,
    pull: Boolean = false,
    soon: Boolean = false,
    rotation: Rotation = Rotation(),
): Plan {
    val rules = Rules(stored, time, zone, now, soon, rotation)
    val due = mutableMapOf<Screen, Due>()
    val home = columns[Screen.HOME].orEmpty()
    if (home.isNotEmpty() && home.toSet() == columns[Screen.LOCK].orEmpty().toSet()) {
        val lead = rules.dueOn(home, records[Screen.HOME] ?: rules.start(home), pull)
        due[Screen.HOME] = lead
        due[Screen.LOCK] = rules.show(lead.record.copy(picture = records[Screen.LOCK]?.picture), lead.record.picture)
    } else {
        val (known, unknown) = Screen.entries.filter { columns[it].orEmpty().isNotEmpty() }.partition { it in records }
        for (screen in known) due[screen] = rules.dueOn(columns.getValue(screen), records.getValue(screen), pull)
        for (screen in unknown) {
            val column = columns.getValue(screen)
            due[screen] = rules.dueOn(column, rules.start(column), pull)
        }
    }
    return Plan(due.filter { (screen, it) -> it.set || it.record != records[screen] })
}

private class Rules(
    val stored: Stored,
    val time: PictureTime,
    val zone: ZoneId,
    val now: Instant,
    val soon: Boolean = false,
    val rotation: Rotation = Rotation(),
) {
    fun current(column: List<Source>): Instant = time.last(now, zone, column.size >= 2)

    /** [now] is before the anchor A ([Rotation.from]'s "New picture at" by this setup's hour; OR1). False without an anchor. */
    val beforeAnchor: Boolean get() = rotation.from?.let { now < time.at(it, zone) } == true

    /**
     * The source whose turn the current change time is (RO1, by the clock; OR1, the list's order
     * from the anchor): the column's sources in the list's order, S[(N(c) − N(A)) mod n], N(c)
     * [PictureTime.turn] at [now], N(A) the anchor's ([PictureTime.turn] of its date, 0 without
     * one). One source: that one.
     */
    fun turnOf(column: List<Source>): Source {
        val order = rotation.ordered(column)
        val anchor = rotation.from?.let { time.turn(it) } ?: 0L
        return order[Math.floorMod(time.turn(now, zone) - anchor, order.size.toLong()).toInt()]
    }

    /** [source] when it has something to show, else the next after it in the list's order with a picture; null: none has one. */
    private fun withPicture(column: List<Source>, source: Source): Source? =
        source.takeIf { stored.picture(it) != null } ?: nextSource(column, source, { stored.picture(it) != null }, rotation.order)

    /** The turn's source ([turnOf]) when it has something to show, else the next after it with a picture; null: none has one. */
    fun pick(column: List<Source>): Source? = withPicture(column, turnOf(column))

    /** The column's top in the list's order (OR1), or the next with a picture when it has none; with none the top. */
    fun top(column: List<Source>): Source {
        val top = rotation.ordered(column).first()
        return withPicture(column, top) ?: top
    }

    /** A screen with no record: on the turn's source (or the next with a picture), with the current change time. */
    fun start(column: List<Source>): Record = Record(pick(column) ?: turnOf(column), stamp = current(column))

    fun dueOn(column: List<Source>, record: Record, pull: Boolean): Due {
        val turns = column.size >= 2
        val current = current(column)
        val stamp = record.stamp
        if (stamp == null || stamp > current || !time.isChangeTime(stamp, zone, turns)) {
            val r = record.copy(stamp = current)
            return if (pull || !turns) within(column, r, pull) else show(r, null)
        }
        if (stamp == current) return within(column, record, pull)
        // Rule 1: a change time came. One source: the publish time decides (B15).
        if (!turns) return within(column, record.copy(stamp = current), pull)
        // Several: the turn's source by the clock (RO1), also a picture shown before; none with a
        // picture keeps the screen's, the change time taken (no waiting).
        val next = pick(column) ?: return show(record.copy(owner = turnOf(column), stamp = current), null)
        return show(record.copy(owner = next, stamp = current), stored.picture(next))
    }

    /** Within the current change: rule 2, a forgotten picture, an owner that has to give way. */
    private fun within(column: List<Source>, record: Record, pull: Boolean): Due {
        if (column.size < 2) {
            val source = column.single()
            val r = record.copy(owner = source)
            // B21: a post stored after a gap in the checks goes on whenever a run sees it.
            val due = record.owner != source || record.picture == null || pull || inWindow(record, orAfterGap = true)
            return show(r, if (due) stored.picture(source) else null)
        }
        if (record.owner !in column || record.picture == null && stored.picture(record.owner) == null) {
            // CR 1: an owner no longer ticked here gives way, and its picture doesn't go on.
            val owner = pick(column) ?: return show(record.copy(owner = record.owner.takeIf { it in column } ?: turnOf(column)), null)
            return show(record.copy(owner = owner), stored.picture(owner))
        }
        val due = record.picture == null || pull || inWindow(record)
        return show(record, if (due) stored.picture(record.owner) else null)
    }

    /**
     * Rule 2 by the publish time (B15): the owner's picture came out before the record's change
     * time + the window. With [soon] (SO1) the window goes: the owner's newer picture is due
     * whenever a run sees it ([show] still never goes back, and sets nothing when it's on).
     */
    private fun inWindow(record: Record, orAfterGap: Boolean = false) =
        soon || stored.publishedBefore(record.owner, record.stamp!! + pictureWindow, orAfterGap)

    /** [record] showing [picture] (null, or an older picture of the source on it, keeps what it shows), its notes brought up to date. */
    fun show(record: Record, picture: Picture?): Due {
        if (picture == null || isOlder(picture, record.picture)) return Due(noted(record), false)
        return Due(noted(record.copy(picture = picture)), !shows(record.picture, picture))
    }

    /** [picture] is an older post of the source [on] shows: a screen never goes back. */
    private fun isOlder(picture: Picture, on: Picture?) =
        picture is Picture.Of && on is Picture.Of && picture.source == on.source && picture.date < on.date

    /** The notes of a screen showing its owner's current picture: the owner's post, and a stand-in's own source. */
    private fun noted(record: Record): Record {
        val picture = record.picture ?: return record
        val current = stored.picture(record.owner) ?: return record
        if (!shows(picture, current)) return record
        var shown = record.shown
        stored.post(record.owner)?.let { shown = shown + (record.owner to it.picture()) }
        if (current is Picture.Of && current.source != record.owner) shown = shown + (current.source to current)
        return if (shown == record.shown) record else record.copy(shown = shown)
    }
}

/**
 * Whether an APOD video day kept the picture of a screen with [column] and [record] (the page's
 * "video day, wallpaper kept"): APOD is ticked there and it's APOD's turn (APOD alone, or no
 * other source had a picture), and nothing will take APOD's place on it (B23): "Skip it" or "Use
 * my picture" without one (B22's stand-in shown before went with RO1: it goes on again).
 * A stand-in that will go on (stored and not set yet, or still loading) isn't kept: the line
 * stays the screen's last change. With turns "Skip it" passes the turn on, so that screen kept
 * nothing. A record without a picture (forgotten by Set, or written by a first video day) counts
 * only when the screen was [everSet] (`wallpaperSetAt`): then a picture is still on it.
 */
fun videoDayKept(column: List<Source>, record: Record?, stored: Stored, everSet: Boolean = true): Boolean {
    if (Source.APOD !in column || stored.post(Source.APOD)?.isVideo != true) return false
    if (record == null || record.owner != Source.APOD) return false
    return !stored.standsIn && (record.picture != null || everSet)
}

private fun shows(noted: Picture?, due: Picture): Boolean =
    noted == due || noted is Picture.Of && due is Picture.Of && noted.url == null &&
        noted.source == due.source && noted.date == due.date

/**
 * Set ("Set as wallpaper", S4; J1; the daily switch on, with [before] = [after]): the setup is
 * saved with the columns [before] → [after], then
 * [dueWallpapers] gives each screen what the setup says afresh. The records of the screens with
 * a column in [after] are forgotten ([Record.forget]: the picture dropped, so it goes on again
 * with the new setup). Each screen goes to the current change time's turn by the clock (RO1:
 * [PictureTime.turn] mod the new column; the next with a picture when that source has none), so
 * a Set never shifts the rotation. Each takes the current change time of
 * [time] in [zone] at [now] as its stamp, so the next run doesn't move it on: the change times
 * don't move, the next change comes at the next one. The notes stay. A screen left alone (an
 * empty column) keeps its record, so the page still knows what it shows. A screen whose column
 * was empty and now isn't is in [Restart.drop]: its record is stale (the app left it alone
 * meanwhile), so it starts as a screen with no record (on the other screen's source if that's
 * in its column). Write the result at once, then run [dueWallpapers].
 *
 * **The list's order** (OR1): [rotation] is the order and the anchor after this Set (a Set of a
 * new order moves the anchor, `Store` writes it). From the anchor A on, or without one, the above,
 * the turn by the formula ([Rules.turnOf]). Before A ([now] < A, by [time]'s hour) each screen
 * keeps what's on until A ([keepUntilAnchor]): [orderOnly] (a Set of the order alone: the draft
 * equals the stored setup but for the order, no newly picked "my picture") keeps the record,
 * otherwise the owner is kept and set again; [daily] false (the switch off) puts the top on.
 * The defaults are RO1's.
 */
fun restartAndForget(
    before: Map<Screen, List<Source>>,
    after: Map<Screen, List<Source>>,
    records: Map<Screen, Record>,
    stored: Stored,
    time: PictureTime,
    zone: ZoneId,
    now: Instant,
    rotation: Rotation = Rotation(),
    orderOnly: Boolean = false,
    daily: Boolean = true,
): Restart {
    val rules = Rules(stored, time, zone, now, rotation = rotation)
    val used = Screen.entries.filter { after[it].orEmpty().isNotEmpty() }
    if (rules.beforeAnchor) return keepUntilAnchor(before, after, records, stored, rules, used, orderOnly, daily)
    val drop = used.filter { before[it].orEmpty().isEmpty() && it in records }.toSet()
    val forgotten = records.filterKeys { it in used && it !in drop }.mapValues { (screen, r) ->
        val column = after.getValue(screen)
        val owner = rules.pick(column) ?: rules.turnOf(column)
        Due(r.forget().copy(owner = owner, stamp = rules.current(column)), false)
    }
    return Restart(Plan(forgotten), drop)
}

/**
 * [restartAndForget] before the anchor (OR1, Night defaults 1 and 2): each screen with a column
 * keeps what's on until A instead of jumping to the clock's turn (right after an anchor mostly the
 * last in the list), and takes the current change time as its stamp (so a change time passed
 * without a run isn't taken by the next run with the new phase):
 * - [daily] and [orderOnly] (a Set of the order alone): the record kept, picture and all; nothing
 *   is set (rule 2 and "As soon as it's out" may still put the owner's newer picture on);
 * - [daily], anything else (other settings too, the daily switch on): the owner kept and its
 *   picture forgotten, so it goes on again at once (a changed "my picture" or part shows);
 * - keep can't hold (no record, a column that was empty, the owner no longer ticked there or with
 *   nothing to show), or the daily switch off (nothing turns, so Set is what puts a picture on):
 *   the column's top in the list's order ([Rules.top]), set at once.
 * A screen whose column was empty gets a fresh record here, so nothing is in [Restart.drop].
 */
private fun keepUntilAnchor(
    before: Map<Screen, List<Source>>,
    after: Map<Screen, List<Source>>,
    records: Map<Screen, Record>,
    stored: Stored,
    rules: Rules,
    used: List<Screen>,
    orderOnly: Boolean,
    daily: Boolean,
): Restart {
    val plan = used.associateWith { screen ->
        val column = after.getValue(screen)
        val current = rules.current(column)
        val record = records[screen]?.takeIf { before[screen].orEmpty().isNotEmpty() }
        val holds = daily && record != null && record.owner in column && stored.picture(record.owner) != null
        val kept = when {
            holds && orderOnly -> record!!.copy(stamp = current)
            holds -> record!!.forget().copy(stamp = current)
            record != null -> record.forget().copy(owner = rules.top(column), stamp = current)
            else -> Record(rules.top(column), stamp = current)
        }
        Due(kept, false)
    }
    return Restart(Plan(plan), emptySet())
}

/** [restartAndForget]'s outcome: the [plan] to write, and the screens whose record to remove ([drop]). */
data class Restart(val plan: Plan, val drop: Set<Screen>)
