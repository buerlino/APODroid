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
 * runs (the alarm's id 2 pending while offline, its `schedule()` failing now and then) and the
 * fallback job's, page opens, pulls, Sets (now and then with a new setup), the daily switch off
 * and on, offline spans from minutes to two days, failed checks and sets, killed runs and the
 * phone switched off; since OR1 a Set draws the list's order now and then (alone, with other changes,
 * in APOD mode), moving the anchor as `Store.saveSetup` does; since L6 a Set draws Size now and then,
 * Set's picker trip ([opensPicker]) is answered (backed out, "Home screen", "Home screen and lock
 * screen"), and Live is lost now and then (another wallpaper, Android's revert of home, a force
 * stop), with `Store.setDue`'s [lostLive] and [stillOrLive]. [SimPhone] does what `Store` and `DailyJob` do around `:core`'s
 * rules; the invariants come from CLAUDE.md's rules (Background update; Turns and the picture
 * time), not from the code, and a failure names the seed, the invariant and the day's runs and
 * sets. A seed that finds a bug stays in [knownBugs] with its own `@Ignore`d test until a fix.
 */
class RulesSimulationTest {
    @Test
    fun theRulesHoldOverRandomMonths() = simulate(soon = false, invariants = (1..9) + (11..16))

    /**
     * The same seeds with "As soon as it's out" on (SO1), now and then turned off and on again by
     * a Set: the invariants above with rule 2 without the window (4: one source shows its newest
     * stored picture after every run; 7: the checks hourly all day) and 10, the turn's source's
     * newest after every run.
     */
    @Test
    fun theRulesHoldOverRandomMonthsWithSoon() = simulate(soon = true, invariants = (1..16).toList())

    private fun simulate(soon: Boolean, invariants: List<Int>) {
        val checked = IntArray(17)
        val failures = (1..SEEDS).filter { it !in knownBugs }.mapNotNull { seed ->
            val phone = SimPhone(seed, soon)
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
        for (invariant in invariants) assertTrue(checked[invariant] > 100, "invariant $invariant checked ${checked[invariant]} times")
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

private enum class Event { PAGE, PULL, SET, DAILY_OFF, DAILY_ON, POWER_OFF, BOOT, LOSE_LIVE }

/**
 * One seed's phone: the store (`Store`'s posts, picture files, `afterGap` marks, `checkedAt` and
 * problems), the job's bookkeeping (`DailyJob`, `Store.runStarts`, `ranWell`, `nextAlarm`), the
 * wallpaper actually on each screen ([wall]), and the invariants checked around every set.
 */
private class SimPhone(val seed: Int, private val soonDrawn: Boolean = false) : Phone(
    zones[Random(seed * 7919L + 1).nextInt(zones.size)], emptyMap(), PictureTime(), emptyList(), emptyMap(), soon = soonDrawn,
) {
    private val r = Random(seed)

    /** Draws "As soon as it's out" at a Set ([soonDrawn] only), apart from [r], so the seeds without it run as before. */
    private val soonR = Random(seed * 104729L + 3)

    /** Draws the list's order at a Set (OR1), apart from [r] and [soonR], so the seeds' other draws stay as they were. */
    private val orderR = Random(seed * 15485863L + 5)

    /** The last event was a Set that may move the anchor (invariant 13). */
    private var anchorMayMove = false
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
    /** Failed runs in a row, a run going counted from its start until it ends well (O4b). */
    private var failedRuns = 0
    private var lastRunAt: Instant? = null
    private var alarm: Instant? = null
    private var alarmFires: Instant? = null
    private var fallback: Instant? = null

    /** When the pending run (`DailyJob`'s id 2) starts: the end of the offline span it waits in. */
    private var runPending: Instant? = null
    private var poweredOff = false

    // What the screens show, and what the invariants keep
    private val wall = mutableMapOf<Screen, Picture>()
    private val everSet = mutableSetOf<Screen>()
    private val kept = mutableMapOf<Screen, Pair<Post, Picture>>()
    private val log = mutableListOf<Pair<Instant, String>>()

    /** How often each invariant was checked (index 1–16). */
    val checked = IntArray(17)

    // Live (L6): Size, what the system shows, the records' live notes; drawn by [liveR] apart
    // from [r], so a seed that stays on Fill runs as before.
    private val liveR = Random(seed * 32452843L + 7)
    private var size = if (liveR.nextBoolean()) Size.LIVE else Size.FILL

    /** APODroid's engine on home (`Store.liveOn(HOME)`). */
    private var homeLive = false

    /** The one engine on the lock screen too (`Store.liveShared`). */
    private var shared = false

    /** The engine on the lock screen alone (home reverted or replaced, Decision 12). */
    private var stray = false

    /** `live.shared`, noted at each run's start while home is live. */
    private var sharedNote = false

    /** The records noted live (`wallpaperLive`, `lock.wallpaperLive`). */
    private val liveNoted = mutableSetOf<Screen>()

    /** Live's copy (`live.jpg`): what the engine draws. */
    private var copy: Picture? = null

    /** The run's lost Live ([lostLive]) and whether the lock screen without a column took home's picture. */
    private var lostNow = LostLive()

    // The run going through setDue
    private var kind = Kind.PAGE
    private var recordsBefore: Map<Screen, Record> = emptyMap()
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
        // L6: Live lost now and then (it acts only while home's record is noted live).
        for (d in 0 until 60) if (liveR.nextInt(8) == 0) schedule(start + Duration.ofDays(d.toLong()) + Duration.ofMinutes(liveR.nextLong(1, 24 * 60)), Event.LOSE_LIVE)
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
        // Store.noteShared: at each run's start while home is live.
        if (homeLive) sharedNote = shared
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
    private fun forgotten(screen: Screen) =
        recordsBefore[screen]?.picture == null || lead(screen)?.picture == null || screen == LOCK && lostNow.lockTakesHome && columns[LOCK].orEmpty().isEmpty()

    /** `Store.setDue`: [Phone.check] with [setFails] and [setDone]; true when a set failed. */
    private fun setDue(now: Instant, kind: Kind, pull: Boolean): Boolean {
        this.kind = kind
        recordsBefore = records
        failedScreens.clear()
        setNow.clear()
        val stored = stored(now)
        storedNow = stored
        liveCheck(now, pull, stored)
        oneSource(now, stored)
        rotation(now, stored)
        turnsNewest(now, stored)
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
        }
    }

    /**
     * `Store.setDue` with Live (L5, L6): [lostLive] forgets the records noted live whose screen
     * isn't live (home's loss both, Decision 12) before the rules, [unnotedLive] those the engine
     * is on and that aren't noted live; [stillOrLive] splits the plan's
     * sets, the still ones first, then home's hand-over to the engine (Live's copy, the records
     * noted live). As [Phone.check] otherwise: the records without a set written at once, each
     * set's after it; a failed set leaves them (and their live notes).
     * 14. With Size Live a still set never goes to home while home is live, and to the lock screen
     * only when its picture differs from home's or the engine doesn't cover it.
     * 15. Nothing is handed to the engine while home isn't live, nor with Size Fill.
     * 16. A screen noted live that lost Live is due again in the same run (both when home lost it,
     * the lock screen with a column); after the run a screen noted live shows the engine (but
     * one whose set failed).
     */
    private fun liveCheck(now: Instant, pull: Boolean, stored: Stored) {
        val lockColumn = columns[LOCK].orEmpty().isNotEmpty()
        val lost = lostLive(HOME in liveNoted, LOCK in liveNoted, homeLive, shared, sharedNote, lockColumn)
        lostNow = lost
        if (lost.forget.isNotEmpty()) note(now, "Live lost, forgetting ${lost.forget}${if (lost.lockTakesHome) ", the lock screen takes home's picture" else ""}")
        val unnoted = unnotedLive(size, homeLive, shared, HOME in liveNoted, LOCK in liveNoted, columns)
        if (unnoted.isNotEmpty()) note(now, "the engine on $unnoted, not noted live: forgetting")
        var view = lost.forgetting(records).mapValues { (screen, record) -> if (screen in unnoted) record.forget() else record }
        fun plan() = lost.onPlan(dueWallpapers(columns, view, stored, time, zone, now, pull, soon, rotation), view)
        fun split(plan: Plan) =
            stillOrLive(size, homeLive, shared, columns[HOME].orEmpty().isNotEmpty(), plan.screens[HOME]?.record?.picture ?: view[HOME]?.picture, plan.sets)
        var plan = plan()
        val lock = view[LOCK]
        if (lock != null && lockFollowsHome(split(plan), shared, lockColumn)) {
            note(now, "home's hand-over would change the lock screen: forgetting it")
            view = view + (LOCK to lock.forget())
            plan = plan()
        }
        recordsBefore = view
        val unset = plan.screens.filterValues { !it.set }
        records = records + unset.mapValues { it.value.record }
        liveNoted -= lost.forget.filter { it !in plan.screens || it in unset }.toSet()
        if (HOME in lost.forget) {
            checked[16]++
            val home = plan.screens[HOME]
            if (columns[HOME].orEmpty().isNotEmpty() && home != null && !home.set && home.record.picture != null) fail(now, 16, "home lost Live and isn't due again")
            if (lockColumn && LOCK !in lost.forget) fail(now, 16, "home lost Live, the lock screen with a column isn't forgotten")
        }
        for (screen in lost.forget) {
            val due = plan.screens[screen] ?: continue
            checked[16]++
            if (!due.set && due.record.picture != null) fail(now, 16, "$screen lost Live and isn't due again")
        }
        val homePicture = plan.screens[HOME]?.record?.picture ?: view[HOME]?.picture
        val split = split(plan)
        checked[16]++
        if (lockFollowsHome(split, shared, lockColumn)) fail(now, 16, "home's hand-over changes the lock screen, which isn't due it")
        for ((picture, screens) in split.still) {
            if (size == Size.LIVE && homeLive) {
                checked[14]++
                if (HOME in screens) fail(now, 14, "a still set of ${picture.label()} on home, which is live")
                if (shared && LOCK in screens && picture == homePicture) fail(now, 14, "a still set of home's ${picture.label()} on the lock screen the engine covers")
            }
            if (setFails(picture, screens, now)) continue
            records = records + screens.associateWith { plan.screens.getValue(it).record }
            liveNoted -= screens
            sets += local(zone, now) + " " + picture.label() + if (screens.size == 1) " ${screens.single()}" else ""
            setDone(picture, screens, now)
            stillSet(screens)
        }
        split.engine?.let { (picture, screens) ->
            checked[15]++
            if (size != Size.LIVE || !homeLive) fail(now, 15, "${picture.label()} handed to the engine with Size $size, home ${if (homeLive) "live" else "not live"}")
            if (setFails(picture, screens, now)) return@let
            records = records + screens.associateWith { plan.screens.getValue(it).record }
            liveNoted += screens
            sets += local(zone, now) + " " + picture.label() + " live" + if (screens.size == 1) " ${screens.single()}" else ""
            setDone(picture, screens, now)
            copy = picture
            note(now, "shown live: ${picture.label()} on ${screens.joinToString("+")}")
            showCopy()
        }
        if (lost.dropShared && failedScreens.isEmpty()) sharedNote = false
        for (screen in liveNoted) {
            if (screen in failedScreens) continue
            checked[16]++
            val engine = if (screen == HOME) homeLive else homeLive && shared
            if (!engine) fail(now, 16, "$screen noted live, but the engine isn't on it")
        }
    }

    /** What a still set on [screens] does to APODroid's live wallpaper (M0, L1): it replaces the engine there. */
    private fun stillSet(screens: Set<Screen>) {
        if (HOME in screens && LOCK in screens) {
            homeLive = false
            shared = false
            stray = false
        } else if (HOME in screens) {
            if (homeLive && shared) stray = true
            homeLive = false
            shared = false
        } else {
            shared = false
            stray = false
        }
    }

    /** The engine draws its copy on the screens it's on (nothing known without a copy: the picker's fallback). */
    private fun showCopy() {
        val on = listOfNotNull(HOME.takeIf { homeLive }, LOCK.takeIf { homeLive && shared || stray })
        for (screen in on) copy?.let { wall[screen] = it } ?: wall.remove(screen)
    }

    /** Live lost by the system (L1): another wallpaper, Android's revert of home after two deaths in 10 s, a force stop. */
    private fun loseLive(now: Instant) {
        if (!homeLive || HOME !in liveNoted) return
        val engineOnLock = shared || stray
        when (liveR.nextInt(4)) {
            0 -> {
                note(now, "another wallpaper picked for home")
                stray = engineOnLock
            }
            1 -> {
                note(now, "another wallpaper picked for both screens")
                stray = false
                if (engineOnLock) wall.remove(LOCK)
            }
            2 -> {
                note(now, "Android reverted home to the default (the engine kept on the lock screen)")
                stray = engineOnLock
            }
            else -> {
                note(now, "a force stop: both screens the default")
                stray = false
                if (engineOnLock) wall.remove(LOCK)
            }
        }
        homeLive = false
        shared = false
        wall.remove(HOME)
    }

    /**
     * Set's picker trip (L6, [opensPicker]) after the setup is saved, answered: backed out,
     * "Home screen" (shared or not: ZUI made it shared over a shared still, Open over a home-only
     * engine) or "Home screen and lock screen" (shared). The engine then draws its copy.
     */
    private fun picker(now: Instant, before: Map<Screen, List<Source>>) {
        if (!opensPicker(size, before, columns, homeLive, shared, lockShares = true)) return
        when (liveR.nextInt(3)) {
            0 -> return note(now, "the picker: backed out")
            1 -> {
                homeLive = true
                shared = liveR.nextBoolean()
                note(now, "the picker: Home screen (${if (shared) "shared" else "home only"})")
            }
            else -> {
                homeLive = true
                shared = true
                note(now, "the picker: Home screen and lock screen")
            }
        }
        stray = false
        showCopy()
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
        // 3. A job or page run puts on only the newest picture of the source whose turn it is (on
        // APOD's video day the stand-in), also one the screen has shown before (RO1: repeats accepted).
        if (counts) {
            checked[3]++
            val owner = records[screen]?.owner
            if (owner == null || picture != stored.picture(owner)) fail(now, 3, "${picture.label()} set on $screen, not the newest of the turn's $owner")
        }
        // 6. An unticked source's picture never goes on; "Skip it" never puts a stand-in on.
        checked[6]++
        val column = columns[screen].orEmpty()
        val video = stored.post(APOD)?.isVideo == true && APOD in column
        val ticked = when (picture) {
            Picture.Own -> standIn == StandIn.OWN && own && video
            is Picture.Of -> picture.source in column || video && picture == stored.picture(APOD)
        }
        // Decision 12: a lock screen without a column that the engine covered takes home's picture.
        val tookHome = screen == LOCK && column.isEmpty() && lostNow.lockTakesHome
        if (!ticked && !tookHome) fail(now, 6, "${picture.label()} set on $screen, not ticked there (${column}) nor the stand-in")
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
            val due = soon || post.publishedAt < change + window || source in gaps
            if (due && wall[screen] != target) {
                fail(now, 4, "$screen shows ${wall[screen]?.label()}, not ${target.label()} (published ${local(zone, post.publishedAt)}, change time ${local(zone, change)})")
            }
            if (!due && setNow[screen] == target && !forgotten(screen)) {
                fail(now, 4, "${target.label()} set on $screen, published ${local(zone, post.publishedAt)}, after the window of ${local(zone, change)}")
            }
        }
    }

    /**
     * The change times with turns around [t], each with its number (RO1, by the clock): counted
     * from "New picture at" each day (0, 1, …), plus the days since 1970-01-01 × the change times
     * a day, on the clock's hours (an hour that doesn't exist comes at the gap's end).
     */
    private fun numbered(t: Instant): List<Pair<Instant, Long>> {
        val perDay = 24 / time.everyHours
        val day = t.atZone(zone).toLocalDate()
        return (-2L..2L).flatMap { d ->
            val ofDay = day.plusDays(d)
            (0 until perDay).map { k ->
                val hour = time.pictureAt + k * time.everyHours
                ZonedDateTime.of(ofDay.plusDays(hour / 24L), LocalTime.of(hour % 24, 0), zone).toInstant() to ofDay.toEpochDay() * perDay + k
            }
        }.distinctBy { it.first }.sortedBy { it.first }
    }

    /**
     * 5. The rotation (RO1): at a change time a screen with turns takes the change time and shows
     * its new owner's newest picture (also one shown before; never back). Missed change times make
     * one change, by the current one.
     * 11. The owner (OR1; was 5's, by APOD → NASA → Earth from 1970-01-01): the column's ticked
     * sources in the list's order, S[(N(c) − N(A)) mod n] with the anchor stored then (N(A) its
     * date's epoch day × the change times a day, 0 without one), or, when that one has nothing to
     * show, the next in the list's order with a picture.
     */
    private fun rotation(now: Instant, stored: Stored) {
        if (kind.free) return
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            if (column.size < 2 || screen in failedScreens || forgotten(screen)) continue
            val stamp = lead(screen)?.stamp ?: continue
            val change = lastChange(now, true)
            if (stamp !in changeTimes(stamp, true) || stamp >= change) continue
            checked[5]++
            val anchor = rotation.from?.let { it.toEpochDay() * (24 / time.everyHours) } ?: 0L
            val number = numbered(now).last { it.first <= now }.second - anchor
            val order = rotation.order.filter { it in column }
            val first = Math.floorMod(number, order.size)
            val turn = order[first]
            val withPicture = order.indices.map { order[(first + it) % order.size] }.firstOrNull { stored.picture(it) != null }
            val record = records[screen]
            if (record?.stamp != change) fail(now, 5, "$screen kept the stamp ${record?.stamp?.let { local(zone, it) }} at the change ${local(zone, change)}")
            val owner = withPicture ?: turn
            checked[11]++
            if (record.owner != owner) fail(now, 11, "$screen went to ${record.owner} at ${local(zone, change)}, the turn is $turn's (number $number from the anchor ${rotation.from}, order ${rotation.order}), with a picture $withPicture")
            val due = withPicture?.let { stored.picture(it) } ?: continue
            val on = wall[screen]
            val older = due is Picture.Of && on is Picture.Of && due.source == on.source && due.date < on.date
            if (!older && on != due) fail(now, 5, "$screen shows ${on?.label()}, not $owner's ${due.label()} after the change ${local(zone, change)}")
        }
    }

    /**
     * 10. With "As soon as it's out" (SO1) and turns: after every run the screen shows the newest
     * stored picture of the source whose turn it is (on APOD's video day the stand-in; never back).
     */
    private fun turnsNewest(now: Instant, stored: Stored) {
        if (!soon) return
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            if (column.size < 2 || screen in failedScreens) continue
            val record = records[screen] ?: continue
            if (record.owner !in column || record.stamp != lastChange(now, true)) continue
            val due = stored.picture(record.owner) ?: continue
            checked[10]++
            val on = wall[screen]
            val older = due is Picture.Of && on is Picture.Of && due.source == on.source && due.date < on.date
            if (!older && on != due) fail(now, 10, "$screen shows ${on?.label()}, not ${record.owner}'s newest ${due.label()} (as soon as it's out)")
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

    /**
     * A job run (either id): its start counts it as failed (`Store.runStarts`) and arms its retry,
     * the update, then a good end forgets the failures (`ranWell`, B24's order) and its end arms
     * the alarms. A failed, stopped or killed run stays counted (O4b).
     */
    private fun jobRun(now: Instant, kind: Kind) {
        if (!daily) return
        failedRuns++
        lastRunAt = now
        armAt(now + retryAfter(failedRuns))
        val kill = if (r.nextInt(30) == 0) r.nextInt(3) else -1
        if (kill == 0) return note(now, "killed at its start")
        val failed = update(now, kind, force = false, page = false, killAfterChecks = kill == 1) ?: return note(now, "killed after its checks")
        if (kill == 2) return note(now, "killed before its end")
        if (!failed) failedRuns = 0
        arm(now, runEnd = true)
    }

    /**
     * `DailyJob.runNow`, an alarm fired: schedules id 2 unless it's pending; it starts at once when
     * online, else when the offline span ends. A `schedule()` fails now and then (1 in 50, O4a):
     * the pair `Store.runRetry` ahead, so an alarm stays armed (invariant 9).
     */
    private fun runNow(now: Instant) {
        if (!daily) return
        if (runPending != null) return note(now, "alarm, a run is pending")
        if (r.nextInt(50) == 0) {
            note(now, "alarm, couldn't schedule the run")
            return armAt(now + retryAfter(failedRuns))
        }
        var starts = now
        while (true) starts = offlineSpans.firstOrNull { starts in it }?.endInclusive?.plusSeconds(1) ?: break
        if (starts == now) jobRun(now, Kind.ALARM)
        else runPending = starts.also { note(now, "alarm, the run waits for the network until ${local(zone, it)}") }
    }

    private fun armAt(at: Instant) {
        alarm = at
        alarmFires = at + Duration.ofSeconds(r.nextLong(0, 601))
    }

    /** `DailyJob.ensure`'s alarms: [nextAlarm], checked against CLAUDE.md's checks (invariant 7). */
    private fun arm(now: Instant, runEnd: Boolean = false) {
        val at = nextAlarm(now, zone, failedRuns, time, turns, offset, lastRunAt, allDay = soon)
        checked[7]++
        if (at < now) fail(now, 7, "the alarm at ${local(zone, at)} is in the past")
        val nextChange = changeTimes(now, turns).first { it + offset > now } + offset
        if (at > nextChange) fail(now, 7, "the alarm at ${local(zone, at)} is after the next change time + the offset, ${local(zone, nextChange)}")
        val clockCheck = clockCheck(now)
        // A run counts as failed from its start until it ends well (O4b): its retry from its start.
        val expected = when {
            failedRuns > 0 && lastRunAt != null -> minOf(clockCheck, maxOf(lastRunAt!! + retry(failedRuns), now))
            else -> clockCheck
        }
        if (at != expected) fail(now, 7, "the alarm at ${local(zone, at)}, the checks say ${local(zone, expected)} (${failedRuns} failed runs)")
        if (runEnd && failedRuns > 0 && at > now + retry(failedRuns)) fail(now, 7, "after $failedRuns failed runs the retry at ${local(zone, at)}")
        armAt(at)
    }

    /** A failed run's retry: 15 min after the first, then hourly. */
    private fun retry(failures: Int): Duration = if (failures <= 1) firstCheck else hour

    /**
     * The clock's next check (CLAUDE.md, Turns, the next run): of the change time of now less the
     * offset, the change time, 15 min after it, then hourly within the window (no waiting since
     * RO1; all day with "As soon as it's out", SO1), the window's end, and the next change time, each the
     * offset later; at once when one passed since the last run's start.
     */
    private fun clockCheck(now: Instant): Instant {
        val times = changeTimes(now - offset, turns)
        val last = times.last { it <= now - offset }
        val next = times.first { it > now - offset }
        val checks = mutableListOf(last, next, last + window)
        var check = last + firstCheck
        while (check < next && (soon || check < last + window)) {
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

    /**
     * "Set as wallpaper": the draft saved (now and then a new setup, now and then a new order: alone,
     * with other changes, or hidden in APOD mode), the records forgotten or kept until the anchor,
     * then Set's update.
     */
    private fun tapSet(now: Instant) {
        val drawn = if (r.nextBoolean()) drawSetup(r).let { if (r.nextBoolean()) it.copy(columns = columns, allSources = allSources) else it } else setup
        val toggleSoon = soonDrawn && soonR.nextInt(4) == 0
        // OR1: half the Sets draw an order, half of those change the order alone.
        val drawOrder = orderR.nextBoolean()
        val alone = drawOrder && orderR.nextBoolean()
        val draftOrder = if (drawOrder) Source.entries.shuffled(orderR) else rotation.order
        val new = if (alone) setup else drawn
        if (toggleSoon && !alone) soon = !soon
        // Store.saveSetup (OR2, Night default 3): the order and the anchor only while the list shows;
        // the anchor moves with a new order, or when none is stored; "only the order" compares the
        // whole setup but the order (no newly picked "my picture", "As soon as it's out" the same).
        val list = new.allSources
        val newOrder = list && draftOrder != rotation.order
        anchorMayMove = list && (newOrder || rotation.from == null)
        // L6: Size now and then, not with the order alone (a changed Size is another change).
        val toggleSize = !alone && liveR.nextInt(3) == 0
        if (toggleSize) size = if (size == Size.FILL) Size.LIVE else Size.FILL
        val orderOnly = newOrder && new == setup && !(toggleSoon && !alone) && !toggleSize
        val newRotation = if (!list) rotation else Rotation(draftOrder, if (anchorMayMove) new.time.anchorDate(now, zone) else rotation.from)
        note(now, "Set: $new, Size $size${if (soon) ", as soon as it's out" else ""}, order ${newRotation.order} from ${newRotation.from}${if (orderOnly) " (the order alone)" else ""}")
        val before = columns
        val prior = records
        val leads = Screen.entries.associateWith { if (equalColumns()) prior[HOME] else prior[it] }
        apply(new)
        rotation = newRotation
        // The new setup's stand-in, as Store.saveSetup (RO2's fix round): not the one before it.
        val stored = stored(now)
        val restart = restartAndForget(before, new.columns, records, stored, new.time, zone, now, rotation, orderOnly, daily)
        records = records + restart.plan.screens.mapValues { it.value.record } - restart.drop
        forgetNotes()
        picker(now, before)
        update(now, Kind.SET, force = false, page = true, set = true)
        // What the update stored (L6: the picker's hand-over may put the owner's newer picture on).
        if (orderOnly && daily) orderAlone(now, leads, stored(now))
        if (daily) arm(now) else alarm = null.also { alarmFires = null; runPending = null }
    }

    /**
     * 12. At a Set of the order alone with the daily switch on (OR1, Night default 1): every screen
     * whose owner can hold (ticked there and with something to show; with equal columns home's)
     * keeps it, and Set's own update puts on no other source's picture (rule 2, "As soon as it's
     * out" or a retry may still set the owner's newer picture).
     */
    private fun orderAlone(now: Instant, leads: Map<Screen, Record?>, stored: Stored) {
        for (screen in Screen.entries) {
            val column = columns[screen].orEmpty()
            if (column.isEmpty()) continue
            val owner = records[screen]?.owner
            setNow[screen]?.let { picture ->
                checked[12]++
                if (owner == null || picture != stored.picture(owner)) fail(now, 12, "the order alone put ${picture.label()} on $screen, not $owner's")
            }
            val lead = leads[screen] ?: continue
            if (lead.owner !in column || stored.picture(lead.owner) == null) continue
            checked[12]++
            if (owner != lead.owner) fail(now, 12, "the order alone moved $screen from ${lead.owner} to $owner")
        }
    }

    /** A forgotten or dropped record isn't on the engine any more (`Store.putRestart`, `putRecord`). */
    private fun forgetNotes() {
        liveNoted.retainAll { records[it]?.picture != null }
    }

    private fun switchDaily(now: Instant, on: Boolean) {
        if (daily == on) return
        note(now, "the daily switch ${if (on) "on" else "off"}")
        daily = on
        failedRuns = 0
        lastRunAt = if (on) now else null
        if (!on) {
            alarm = null
            alarmFires = null
            fallback = null
            runPending = null
            return
        }
        // `Store.startAfresh`: the stored setup and anchor (OR1: kept what's on before it).
        val restart = restartAndForget(columns, columns, records, stored(now), time, zone, now, rotation)
        records = records + restart.plan.screens.mapValues { it.value.record } - restart.drop
        forgetNotes()
        update(now, Kind.SWITCH_ON, force = false, page = true)
        arm(now)
        // A freshly scheduled fallback runs at once.
        fallback = now + Duration.ofMinutes(r.nextLong(1, 4))
    }

    fun simulate() {
        while (true) {
            val scripted = if (events.isEmpty()) null else events.firstKey()
            val next = listOfNotNull(scripted, alarmFires?.takeIf { !poweredOff }, runPending?.takeIf { !poweredOff }, fallback?.takeIf { !poweredOff }).min()
            if (next >= end) return
            val anchorBefore = rotation.from
            anchorMayMove = false
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
                            runPending = null
                        }
                        Event.LOSE_LIVE -> loseLive(next)
                        Event.BOOT -> {
                            note(next, "boot")
                            poweredOff = false
                            fallback = fallback?.let { maxOf(it, next + Duration.ofMinutes(r.nextLong(1, 11))) }
                            if (daily) arm(next)
                        }
                    }
                }
                alarmFires -> {
                    note(next, "alarm (armed for ${local(zone, alarm!!)})")
                    alarmFires = null
                    runNow(next)
                }
                runPending -> {
                    note(next, "the pending run starts (online)")
                    runPending = null
                    jobRun(next, Kind.ALARM)
                }
                else -> {
                    note(next, "fallback run")
                    fallback = next + Duration.ofMinutes(r.nextLong(3 * 60, 9 * 60))
                    jobRun(next, Kind.FALLBACK)
                }
            }
            // Invariant 13 (OR1): the anchor moves only at a Set with the list shown and a new order or none stored.
            checked[13]++
            if (rotation.from != anchorBefore && !anchorMayMove) fail(next, 13, "the anchor moved from $anchorBefore to ${rotation.from}")
            // Invariant 9 (O4a): while the switch is on and the phone is on, an alarm is armed or id 2 is pending.
            if (daily && !poweredOff) {
                checked[9]++
                if (alarmFires == null && runPending == null) fail(next, 9, "no alarm armed and no run pending")
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
                "  $zone, offset ${offset.seconds} s, from ${local(zone, start)} ${start.atZone(zone).toLocalDate()}; $setup${if (soon) ", as soon as it's out" else ""}, order ${rotation.order} from ${rotation.from}\n" +
                "  records: $records\n" +
                "  Size $size, home ${if (homeLive) "live" else "not live"}${if (shared) ", shared" else ""}${if (stray) ", a stray on the lock screen" else ""}, noted live $liveNoted, the copy ${copy?.label()}\n" +
                "  the runs and sets of the day before and the day:\n$lines",
        )
    }
}
