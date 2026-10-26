package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * SIM (0.6.0's release gate): the rules over random months. Each seed draws a phone (a time zone,
 * the setup, the device's offset) and 60 days from 20 Oct 2026 (both 2026 DST ends) or from
 * 20 Mar 2027 (the EU's spring change): the posts as the sites post them (APOD late now and then,
 * video days, a post without its publish time, a second picture on the same date), the alarms'
 * runs and the fallback job's, page opens, pulls, Sets (now and then with a new setup), the daily
 * switch off and on, offline spans from minutes to two days, failed checks and sets, killed runs
 * and the phone switched off. [SimPhone] does what `Store` and `DailyJob` do around `:core`'s
 * rules; the invariants come from CLAUDE.md's rules (Background update; Turns and the picture
 * time), not from the code, and a failure names the seed, the invariant and the day's runs and
 * sets. A seed that finds a bug stays in [knownBugs] with its own `@Ignore`d test until a fix.
 */
class RulesSimulationTest {
    @Test
    fun theRulesHoldOverRandomMonths() {
        val checked = IntArray(9)
        val failures = (1..SEEDS).filter { it !in knownBugs }.mapNotNull { seed ->
            val phone = SimPhone(seed)
            try {
                phone.simulate()
                null
            } catch (e: SimFailure) {
                e
            } finally {
                phone.checked.forEachIndexed { i, n -> checked[i] += n }
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} of $SEEDS seeds failed: ${failures.map { "${it.seed} (${it.invariant})" }}\n\n" + failures.take(3).joinToString("\n\n") { it.message!! })
        }
        // Each invariant was checked, none is idle (about 25,000 sets and 270,000 armings over 240 seeds).
        for (invariant in 1..8) assertTrue(checked[invariant] > 100, "invariant $invariant checked ${checked[invariant]} times")
    }

    private companion object {
        const val SEEDS = 240

        /** Seeds that found a bug, each with an `@Ignore`d test of its own until the fix. */
        val knownBugs = setOf<Int>()
    }
}

private class SimFailure(val seed: Int, val invariant: Int, message: String) : AssertionError(message)

private val zones = listOf("Europe/Zurich", "America/Los_Angeles", "Australia/Sydney", "Europe/Helsinki", "America/New_York", "Asia/Tehran", "Asia/Kolkata").map(ZoneId::of)

private val window = pictureWindow
private val minute = Duration.ofMinutes(1)
private val hour = Duration.ofHours(1)

/** What a run is: the job's (the alarms' or the fallback's), a page open, or one that may set what shows (CLAUDE.md: Set, the daily switch on, a pull). */
private enum class Kind(val free: Boolean = false) { ALARM, FALLBACK, PAGE, PULL(true), SET(true), SWITCH_ON(true) }

/** The setup "Set as wallpaper" saves. */
private data class Setup(val allSources: Boolean, val columns: Map<Screen, List<Source>>, val time: PictureTime, val standIn: StandIn, val own: Boolean) {
    override fun toString() = "${if (allSources) "More NASA pictures" else "APOD mode"}, home ${columns[HOME]}, lock ${columns[LOCK]}, " +
        "at ${time.pictureAt}:00 every ${time.everyHours} h, video days $standIn${if (standIn == StandIn.OWN && !own) " (no picture)" else ""}"
}

private fun drawSetup(r: Random): Setup {
    val allSources = r.nextInt(3) != 0
    val columns = if (!allSources) {
        apodColumns(listOf(setOf(HOME), setOf(LOCK), setOf(HOME, LOCK)).random(r))
    } else {
        val screens = listOf(emptySet(), setOf(HOME), setOf(LOCK), setOf(HOME, LOCK))
        generateSequence { Source.entries.fold(emptyMap<Screen, List<Source>>()) { c, s -> c.placing(s, screens.random(r)) } }
            .first { c -> c.values.any { it.isNotEmpty() } }
    }
    val time = PictureTime(if (r.nextBoolean()) 6 else r.nextInt(24), listOf(24, 12, 6, 3).random(r))
    return Setup(allSources, columns, time, StandIn.entries.random(r), r.nextInt(8) != 0)
}

/**
 * The posts from [from] to [to], as the sites post them (Phone.kt's times): APOD daily, late by up
 * to 12 h on about 1 day in 10, a video about 1 day in 9, a second picture on the same date about
 * 1 day in 30; NASA (some weekdays without) and Earth on weekdays; about 1 post in 20 without its
 * publish time (stored before B15: [Post.publishedAt] is its date's 00:00 US Eastern).
 */
private fun drawPosts(r: Random, from: LocalDate, to: LocalDate): List<Out> {
    val outs = mutableListOf<Out>()
    fun add(source: Source, date: LocalDate, at: Instant, video: Boolean = false, second: Boolean = false) {
        val url = if (video) null else "https://x/$source-$date${if (second) "-b" else ""}.jpg"
        outs += Out(at, Post(source, date, "$source $date", url, "https://p/$source-$date", video, published = at.takeIf { r.nextInt(20) != 0 }))
    }
    var day = from
    while (day <= to) {
        var at = apodAt(day)
        if (r.nextInt(10) == 0) at += Duration.ofMinutes(r.nextLong(1, 12 * 60 + 1))
        val video = r.nextInt(9) == 0
        add(APOD, day, at, video)
        if (!video && r.nextInt(30) == 0) add(APOD, day, at + Duration.ofMinutes(r.nextLong(30, 10 * 60 + 1)), second = true)
        if (isWeekday(day)) {
            if (r.nextInt(20) != 0) add(NASA, day, nasaAt(day))
            add(EARTH, day, earthAt(day))
        }
        day = day.plusDays(1)
    }
    return outs.sortedBy { it.at }
}

private enum class Event { PAGE, PULL, SET, DAILY_OFF, DAILY_ON, POWER_OFF, BOOT }

/**
 * One seed's phone: the store (`Store`'s posts, picture files, `afterGap` marks, `checkedAt` and
 * problems), the job's bookkeeping (`DailyJob`, `Store.runStarts`, `ranJob`, `nextAlarm`), the
 * wallpaper actually on each screen ([wall]), and the invariants checked around every set.
 */
private class SimPhone(val seed: Int) : Phone(
    zones[Random(seed * 7919L + 1).nextInt(zones.size)], emptyMap(), PictureTime(), emptyList(), emptyMap(),
) {
    private val r = Random(seed)
    private val start: Instant
    private val end: Instant
    private val offset: Duration
    private val allPosts: Map<Source, List<Out>>
    private val offlineSpans = mutableListOf<ClosedRange<Instant>>()
    private val events = sortedMapOf<Instant, Event>()
    private var allSources = false
    private var own = false

    // The store
    private val storedPosts = mutableMapOf<Source, Post>()
    private val files = mutableSetOf<Source>()
    private val gaps = mutableSetOf<Source>()
    private val checkedAt = mutableMapOf<Source, Instant>()
    private val problem = mutableSetOf<Source>()

    // The job
    private var daily = false
    private var failedRuns = 0
    private var failedAt: Instant? = null
    private var lastRunAt: Instant? = null
    private var runGoing = false
    private var alarm: Instant? = null
    private var alarmFires: Instant? = null
    private var fallback: Instant? = null
    private var poweredOff = false

    // What the screens show, and what the invariants keep
    private val wall = mutableMapOf<Screen, Picture>()
    private val history = Screen.entries.associateWith { mutableSetOf<Picture.Of>() }
    private val everSet = mutableSetOf<Screen>()
    private val kept = mutableMapOf<Screen, Pair<Post, Picture>>()
    private val log = mutableListOf<Pair<Instant, String>>()

    /** How often each invariant was checked (index 1–8). */
    val checked = IntArray(9)

    // The run going through setDue
    private var kind = Kind.PAGE
    private var recordsBefore: Map<Screen, Record> = emptyMap()
    private var storedBefore: Stored? = null
    private var storedNow: Stored? = null
    private val failedScreens = mutableSetOf<Screen>()
    private val setNow = mutableMapOf<Screen, Picture>()

    init {
        val first = if (r.nextBoolean()) LocalDate.of(2026, 10, 20) else LocalDate.of(2027, 3, 20)
        start = first.atTime(LocalTime.of(r.nextInt(24), r.nextInt(60))).atZone(zone).toInstant()
        end = start + Duration.ofDays(60)
        offset = randomOffset(r)
        val setup = drawSetup(r)
        apply(setup)
        allPosts = drawPosts(r, first.minusDays(10), first.plusDays(62)).groupBy { it.post.source }
        repeat(r.nextInt(7)) {
            val from = randomTime()
            val length = when (r.nextInt(3)) {
                0 -> Duration.ofMinutes(r.nextLong(5, 121))
                1 -> Duration.ofMinutes(r.nextLong(2 * 60, 20 * 60 + 1))
                else -> Duration.ofMinutes(r.nextLong(24 * 60, 48 * 60 + 1))
            }
            offlineSpans += from..from + length
        }
        for (d in 0 until 60) {
            val day = start + Duration.ofDays(d.toLong())
            repeat(r.nextInt(5)) { schedule(day + Duration.ofMinutes(r.nextLong(1, 24 * 60)), Event.PAGE) }
            if (r.nextInt(7) == 0) schedule(day + Duration.ofMinutes(r.nextLong(1, 24 * 60)), Event.PULL)
            if (r.nextInt(16) == 0) schedule(day + Duration.ofMinutes(r.nextLong(1, 24 * 60)), Event.SET)
        }
        repeat(r.nextInt(3)) {
            val off = randomTime()
            schedule(off, Event.DAILY_OFF)
            schedule(off + Duration.ofMinutes(r.nextLong(1, 30 * 60)), Event.DAILY_ON)
        }
        repeat(r.nextInt(3)) {
            val off = randomTime()
            schedule(off, Event.POWER_OFF)
            schedule(off + Duration.ofMinutes(r.nextLong(30, 30 * 60)), Event.BOOT)
        }
        // The install: the page opens, then the daily switch goes on.
        schedule(start, Event.PAGE)
        schedule(start + Duration.ofMinutes(r.nextLong(1, 6)), Event.DAILY_ON)
    }

    private fun randomTime(): Instant = start + Duration.ofMinutes(r.nextLong(10, 60 * 24 * 60))

    private fun schedule(at: Instant, event: Event) {
        var t = at
        while (t in events) t += Duration.ofSeconds(1)
        events[t] = event
    }

    private fun apply(setup: Setup) {
        allSources = setup.allSources
        columns = setup.columns
        time = setup.time
        standIn = setup.standIn
        own = own || setup.own
    }

    private val setup get() = Setup(allSources, columns, time, standIn, own)

    // The store, as `Store` keeps it

    override fun stored(now: Instant) = Stored(storedPosts.toMap(), files.toSet(), own, standIn, gaps.toSet())

    /** The site's newest post at [now] (by date, a second picture of the same date after the first). */
    private fun newest(source: Source, now: Instant): Post? =
        allPosts[source].orEmpty().filter { it.at <= now }.maxWithOrNull(compareBy<Out>({ it.post.date }, { it.at }))?.post

    private fun isCurrent(source: Source, now: Instant) =
        storedPosts[source]?.let { it.date == now.atZone(eastern).toLocalDate() && (it.isVideo || source in files) } == true

    /** A stale cache (CLAUDE.md, Fetch: guarded against, never seen): the newest post of an earlier date than the newest. */
    private fun stale(source: Source, now: Instant): Post? {
        val newest = newest(source, now) ?: return null
        return allPosts[source].orEmpty().filter { it.at <= now && it.post.date < newest.date }.maxWithOrNull(compareBy<Out>({ it.post.date }, { it.at }))?.post
    }

    /** `Store.refresh`: the site's answer (about 1 in 50 a stale one) stored unless it's older; a new post marked as stored after a gap (B21). */
    private fun refresh(source: Source, now: Instant) {
        val gap = afterGap(checkedAt[source], now)
        val latest = (if (r.nextInt(50) == 0) stale(source, now) else null) ?: newest(source, now) ?: return
        val old = storedPosts[source]
        if (old != null && latest.date < old.date) return
        val new = old == null || latest.date != old.date || latest.imageUrl != old.imageUrl
        storedPosts[source] = latest
        if (latest.imageUrl != null) files += source else files -= source
        if (new) {
            if (gap) gaps += source else gaps -= source
            note(now, "stored ${latest.picture().label()}${if (latest.isVideo) " video" else ""}${if (gap) " after a gap" else ""}")
        }
    }

    /** `Store.checkSite`: fetches unless today's post is stored (always with [force]); a success notes `checkedAt`. */
    private fun checkSite(source: Source, force: Boolean, now: Instant): Boolean {
        if (force || !isCurrent(source, now)) {
            if (offlineSpans.any { now in it } || r.nextInt(33) == 0) {
                problem += source
                note(now, "check of $source failed")
                return false
            }
            refresh(source, now)
        }
        checkedAt[source] = now
        problem -= source
        return true
    }

    private fun checkedRecently(source: Source, now: Instant): Boolean {
        val at = checkedAt[source] ?: return false
        return source !in problem && at <= now && Duration.between(at, now) < hour &&
            storedPosts[source]?.let { it.isVideo || source in files } == true
    }

    private fun inUse() = sourcesInUse(columns, standIn.source, storedPosts[APOD]?.isVideo == true)

    /** The sources the page offers: all three with "More NASA pictures", else APOD and on its video day the video-day source. */
    private fun offered(): Set<Source> =
        if (allSources) Source.entries.toSet() else setOfNotNull(APOD, standIn.source.takeIf { storedPosts[APOD]?.isVideo == true })

    /**
     * `Store.update`: checks each source in use and offered (a page skips one checked within the
     * hour), then, while the daily switch is on or for Set, sets what's due. True when a check or
     * a set failed; null when the run was killed after its checks ([killAfterChecks]).
     */
    private fun update(now: Instant, kind: Kind, force: Boolean, page: Boolean, set: Boolean = false, killAfterChecks: Boolean = false): Boolean? {
        storedBefore = stored(now)
        val show = if (page) offered() else emptySet()
        var failure = false
        for (source in Source.entries) {
            if (source !in show && source !in inUse()) continue
            if (force || !page || !checkedRecently(source, now)) failure = !checkSite(source, force, now) || failure
        }
        if (killAfterChecks) return null
        if (!daily && !set) return failure
        return setDue(now, kind, pull = force) || failure
    }

    // The sets and the invariants around them

    private fun equalColumns() = columns[HOME].orEmpty().isNotEmpty() && columns[HOME].orEmpty().toSet() == columns[LOCK].orEmpty().toSet()

    /** The record a screen's due picture comes from: with equal columns home's. */
    private fun lead(screen: Screen) = if (equalColumns()) recordsBefore[HOME] else recordsBefore[screen]

    /** A forgotten picture (Set) or a new screen: the owner's picture goes on at once (CLAUDE.md, Turns). */
    private fun forgotten(screen: Screen) = recordsBefore[screen]?.picture == null || lead(screen)?.picture == null

    /** `Store.setDue`: [Phone.check] with [setFails] and [setDone]; true when a set failed. */
    private fun setDue(now: Instant, kind: Kind, pull: Boolean): Boolean {
        this.kind = kind
        recordsBefore = records
        failedScreens.clear()
        setNow.clear()
        val stored = stored(now)
        storedNow = stored
        check(now, pull = pull, stored = stored)
        oneSource(now, stored)
        waitingScreens(now, stored)
        keptChecks(now, stored)
        return failedScreens.isNotEmpty()
    }

    override fun setFails(picture: Picture, screens: Set<Screen>, now: Instant): Boolean {
        if (r.nextInt(33) != 0) return false
        failedScreens += screens
        note(now, "set of ${picture.label()} on ${screens.joinToString("+")} failed")
        return true
    }

    override fun setDone(picture: Picture, screens: Set<Screen>, now: Instant) {
        note(now, "set ${picture.label()} on ${screens.joinToString("+")}")
        for (screen in screens) {
            checkSet(now, screen, picture)
            wall[screen] = picture
            setNow[screen] = picture
            everSet += screen
            if (picture is Picture.Of) history.getValue(screen) += picture
        }
    }

    /** The invariants of one set of [picture] on [screen]. */
    private fun checkSet(now: Instant, screen: Screen, picture: Picture) {
        val on = wall[screen]
        val stored = storedNow!!
        val counts = !kind.free && !forgotten(screen)
        // 1. Never back.
        checked[1]++
        if (picture is Picture.Of && on is Picture.Of && on.source == picture.source && picture.date < on.date) {
            fail(now, 1, "never back: ${picture.label()} set on $screen over ${on.label()}")
        }
        // 2. A job or page run doesn't set what the screen shows.
        if (counts) {
            checked[2]++
            if (on == picture) fail(now, 2, "${picture.label()} set again on $screen, which shows it")
        }
        // 3. No post twice on one screen (a stand-in counts for its own source), but by Set, a pull or a forgotten picture.
        if (counts && picture is Picture.Of) {
            checked[3]++
            if (picture in history.getValue(screen)) fail(now, 3, "${picture.label()} set on $screen a second time")
        }
        // 6. An unticked source's picture never goes on; "Skip it" never puts a stand-in on.
        checked[6]++
        val column = columns[screen].orEmpty()
        val video = stored.post(APOD)?.isVideo == true && APOD in column
        val ticked = when (picture) {
            Picture.Own -> standIn == StandIn.OWN && own && video
            is Picture.Of -> picture.source in column || video && picture == stored.picture(APOD)
        }
        if (!ticked) fail(now, 6, "${picture.label()} set on $screen, not ticked there (${column}) nor the stand-in")
        val owner = records[screen]?.owner
        if (standIn == StandIn.SKIP && owner == APOD && !(picture is Picture.Of && picture.source == APOD)) {
            fail(now, 6, "\"Skip it\" put ${picture.label()} on $screen in APOD's turn")
        }
        // 8. The stand-in doesn't go on where the page said the video day kept the picture.
        kept[screen]?.let { (video, standIn) ->
            checked[8]++
            if (!kind.free && stored.post(APOD) == video && picture == standIn) {
                fail(now, 8, "${picture.label()} set on $screen after \"video day, wallpaper kept\"")
            }
        }
    }

    /** The last change time at or before [t] of a column ([turns]): "New picture at" and every "Change every" from it, on the clock's hours. */
    private fun changeTimes(t: Instant, turns: Boolean): List<Instant> {
        val hours = if (!turns) listOf(time.pictureAt) else (0 until 24 / time.everyHours).map { (time.pictureAt + it * time.everyHours) % 24 }
        val day = t.atZone(zone).toLocalDate()
        return (-2L..2L).flatMap { d -> hours.map { ZonedDateTime.of(day.plusDays(d), LocalTime.of(it, 0), zone).toInstant() } }.distinct().sorted()
    }

    private fun lastChange(t: Instant, turns: Boolean) = changeTimes(t, turns).last { it <= t }

    /**
     * 4. With one source (rule 2): after each run, the screen shows the newest stored post when it
     * was published before the last change time + 3 h or is marked as stored after a gap; one
     * published later isn't put on by a job or page run. (Its video days: invariants 6 and 8.)
     */
    private fun oneSource(now: Instant, stored: Stored) {
        if (kind.free) return
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            if (column.size != 1 || screen in failedScreens) continue
            val source = column.single()
            val post = stored.post(source) ?: continue
            if (post.isVideo) continue
            val target = stored.picture(source) ?: continue
            checked[4]++
            val change = lastChange(now, turns = false)
            val due = post.publishedAt < change + window || source in gaps
            if (due && wall[screen] != target) {
                fail(now, 4, "$screen shows ${wall[screen]?.label()}, not ${target.label()} (published ${local(zone, post.publishedAt)}, change time ${local(zone, change)})")
            }
            if (!due && setNow[screen] == target && !forgotten(screen)) {
                fail(now, 4, "${target.label()} set on $screen, published ${local(zone, post.publishedAt)}, after the window of ${local(zone, change)}")
            }
        }
    }

    /** 5. Rule 3: a waiting screen takes the first new post of a ticked source at the first run that stores it (the next in order when two are). */
    private fun waitingScreens(now: Instant, stored: Stored) {
        if (kind.free) return
        val before = storedBefore ?: return
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            if (column.size < 2 || screen in failedScreens || forgotten(screen)) continue
            val record = lead(screen) ?: continue
            val stamp = record.stamp ?: continue
            if (stamp !in changeTimes(stamp, true) || stamp >= lastChange(now, true)) continue
            if (column.any { before.isNew(it, record.shown) }) continue
            val new = column.filter { stored.isNew(it, record.shown) }
            if (new.isEmpty()) continue
            checked[5]++
            val next = nextSource(column, record.owner) { it in new }!!
            if (wall[screen] != stored.picture(next)) {
                fail(now, 5, "$screen waited and shows ${wall[screen]?.label()}, not ${stored.picture(next)?.label()} ($next, new: $new)")
            }
        }
    }

    /** 8. "Video day, wallpaper kept" ([videoDayKept]) only while nothing takes APOD's place: the stand-in isn't on, and no job or page run puts it on later. */
    private fun keptChecks(now: Instant, stored: Stored) {
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            val video = stored.post(APOD)
            kept[screen]?.let { (post, _) -> if (kind.free || video != post) kept -= screen }
            if (column.isEmpty() || !videoDayKept(column, records[screen], stored, screen in everSet)) continue
            checked[8]++
            val standIn = stored.picture(APOD) ?: continue
            if (wall[screen] == standIn) fail(now, 8, "\"video day, wallpaper kept\" on $screen, which shows the stand-in ${standIn.label()}")
            kept.getOrPut(screen) { video!! to standIn }
        }
    }

    // The job and the alarms, as DailyJob runs them

    /** A job run (either id): its start's notes and alarm, the update, then its end's notes and alarms (B24's order). */
    private fun jobRun(now: Instant, kind: Kind) {
        if (!daily) return
        if (runGoing) {
            failedRuns++
            failedAt = now
        }
        lastRunAt = now
        runGoing = true
        armAt(now + runRetry(failedRuns))
        val kill = if (r.nextInt(30) == 0) r.nextInt(3) else -1
        if (kill == 0) return note(now, "killed at its start")
        val failed = update(now, kind, force = false, page = false, killAfterChecks = kill == 1) ?: return note(now, "killed after its checks")
        if (kill == 2) return note(now, "killed before its end")
        runGoing = false
        if (failed) {
            failedRuns++
            failedAt = now
        } else {
            failedRuns = 0
            failedAt = null
        }
        arm(now, runEnd = true)
    }

    private fun armAt(at: Instant) {
        alarm = at
        alarmFires = at + Duration.ofSeconds(r.nextLong(0, 601))
    }

    /** `DailyJob.ensure`'s alarms: [nextAlarm], checked against CLAUDE.md's checks (invariant 7). */
    private fun arm(now: Instant, runEnd: Boolean = false) {
        val waiting = waiting(columns, records, time, zone, now)
        val at = nextAlarm(now, zone, failedRuns, failedAt, time, turns, offset, waiting, lastRunAt, runGoing)
        checked[7]++
        if (at < now) fail(now, 7, "the alarm at ${local(zone, at)} is in the past")
        val nextChange = changeTimes(now, turns).first { it + offset > now } + offset
        if (at > nextChange) fail(now, 7, "the alarm at ${local(zone, at)} is after the next change time + the offset, ${local(zone, nextChange)}")
        val clockCheck = clockCheck(now, waiting)
        val expected = when {
            runGoing && lastRunAt != null -> minOf(clockCheck, maxOf(lastRunAt!! + retry(failedRuns + 1), now))
            failedAt != null && failedRuns > 0 -> minOf(clockCheck, maxOf(failedAt!! + retry(failedRuns), now))
            else -> clockCheck
        }
        if (at != expected) fail(now, 7, "the alarm at ${local(zone, at)}, the checks say ${local(zone, expected)} (waiting $waiting, ${failedRuns} failed runs)")
        if (runEnd && failedRuns > 0 && at > now + retry(failedRuns)) fail(now, 7, "after $failedRuns failed runs the retry at ${local(zone, at)}")
        armAt(at)
    }

    /** A failed run's retry: 15 min after the first, then hourly. */
    private fun retry(failures: Int): Duration = if (failures <= 1) firstCheck else hour

    /**
     * The clock's next check (CLAUDE.md, Turns, the next run): of the change time of now less the
     * offset, the change time, 15 min after it, then hourly within the window (until the next
     * change time while a screen waits), the window's end, and the next change time, each the
     * offset later; at once when one passed since the last run's start.
     */
    private fun clockCheck(now: Instant, waiting: Boolean): Instant {
        val times = changeTimes(now - offset, turns)
        val last = times.last { it <= now - offset }
        val next = times.first { it > now - offset }
        val checks = mutableListOf(last, next, last + window)
        var check = last + firstCheck
        while (check < next && (waiting || check < last + window)) {
            checks += check
            check += hour
        }
        val shifted = checks.map { it + offset }
        val lastRun = lastRunAt
        if (lastRun != null && shifted.any { it > lastRun && it <= now }) return now
        return shifted.filter { it > now }.min()
    }

    // The page

    private fun pageOpen(now: Instant, pull: Boolean) {
        update(now, if (pull) Kind.PULL else Kind.PAGE, force = pull, page = true)
        if (daily) arm(now)
    }

    /** "Set as wallpaper": the draft saved (now and then a new setup), the records forgotten, then Set's update. */
    private fun tapSet(now: Instant) {
        val new = if (r.nextBoolean()) drawSetup(r).let { if (r.nextBoolean()) it.copy(columns = columns, allSources = allSources) else it } else setup
        note(now, "Set: $new")
        val restart = restartAndForget(columns, new.columns, records, stored(now), new.time, zone, now)
        if (new.columns != columns) history.values.forEach { it.clear() }
        apply(new)
        records = records + restart.plan.screens.mapValues { it.value.record } - restart.drop
        update(now, Kind.SET, force = false, page = true, set = true)
        if (daily) arm(now) else alarm = null.also { alarmFires = null }
    }

    private fun switchDaily(now: Instant, on: Boolean) {
        if (daily == on) return
        note(now, "the daily switch ${if (on) "on" else "off"}")
        daily = on
        failedRuns = 0
        failedAt = null
        runGoing = false
        lastRunAt = if (on) now else null
        if (!on) {
            alarm = null
            alarmFires = null
            fallback = null
            return
        }
        val restart = restartAndForget(columns, columns, records, stored(now), time, zone, now)
        records = records + restart.plan.screens.mapValues { it.value.record } - restart.drop
        update(now, Kind.SWITCH_ON, force = false, page = true)
        arm(now)
        // A freshly scheduled fallback runs at once.
        fallback = now + Duration.ofMinutes(r.nextLong(1, 4))
    }

    fun simulate() {
        while (true) {
            val scripted = if (events.isEmpty()) null else events.firstKey()
            val next = listOfNotNull(scripted, alarmFires?.takeIf { !poweredOff }, fallback?.takeIf { !poweredOff }).min()
            if (next >= end) return
            when (next) {
                scripted -> {
                    val event = events.remove(scripted)!!
                    if (poweredOff && event == Event.DAILY_ON) schedule(next + hour, event)
                    if (poweredOff && event != Event.BOOT) continue
                    when (event) {
                        Event.PAGE -> note(next, "page open").also { pageOpen(next, pull = false) }
                        Event.PULL -> note(next, "pull").also { pageOpen(next, pull = true) }
                        Event.SET -> tapSet(next)
                        Event.DAILY_OFF -> switchDaily(next, false)
                        Event.DAILY_ON -> switchDaily(next, true)
                        Event.POWER_OFF -> {
                            note(next, "the phone off")
                            poweredOff = true
                            alarm = null
                            alarmFires = null
                        }
                        Event.BOOT -> {
                            note(next, "boot")
                            poweredOff = false
                            fallback = fallback?.let { maxOf(it, next + Duration.ofMinutes(r.nextLong(1, 11))) }
                            if (daily) arm(next)
                        }
                    }
                }
                alarmFires -> {
                    note(next, "alarm run (armed for ${local(zone, alarm!!)})")
                    alarmFires = null
                    jobRun(next, Kind.ALARM)
                }
                else -> {
                    note(next, "fallback run")
                    fallback = next + Duration.ofMinutes(r.nextLong(3 * 60, 9 * 60))
                    jobRun(next, Kind.FALLBACK)
                }
            }
        }
    }

    private fun note(now: Instant, what: String) {
        log += now to what
    }

    private fun fail(now: Instant, invariant: Int, what: String): Nothing {
        val day = now.atZone(zone).toLocalDate()
        val lines = log.filter { it.first.atZone(zone).toLocalDate() >= day.minusDays(1) }.joinToString("\n") { "  ${local(zone, it.first)} ${it.second}" }
        throw SimFailure(
            seed, invariant,
            "seed $seed, invariant $invariant at ${local(zone, now)} ($kind): $what\n" +
                "  $zone, offset ${offset.seconds} s, from ${local(zone, start)} ${start.atZone(zone).toLocalDate()}; $setup\n" +
                "  records: $records\n" +
                "  the runs and sets of the day before and the day:\n$lines",
        )
    }
}
