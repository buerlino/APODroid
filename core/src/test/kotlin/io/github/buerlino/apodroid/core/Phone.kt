package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * A phone running the rules over whole days, for PictureTimeTest's days in real time zones and
 * RulesSimulationTest's random months (SIM): the posts coming out when the sites post them (APOD
 * at 00:05 US Eastern, Earth Observatory at 00:00 US Eastern on weekdays, NASA at 16:30 UTC on
 * weekdays) and a run at each [nextRun], as the alarms start them.
 */

internal val eastern: ZoneId = ZoneId.of("America/New_York")

internal fun at(zone: ZoneId, local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

private val dayAndTime = DateTimeFormatter.ofPattern("dd HH:mm")

internal fun local(zone: ZoneId, t: Instant): String = t.atZone(zone).format(dayAndTime)

/** A post that comes out at [at]: from then a check sees it. */
internal data class Out(val at: Instant, val post: Post)

/** A post published at [at] (B15), as the sites say. */
internal fun post(source: Source, date: LocalDate, at: Instant, video: Boolean = false) =
    Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video, published = at)

internal fun out(source: Source, date: LocalDate, at: Instant, video: Boolean = false) = Out(at, post(source, date, at, video))

/** APOD's post of [date], out at 00:05 US Eastern. */
internal fun apodAt(date: LocalDate): Instant = date.atTime(0, 5).atZone(eastern).toInstant()

/** Earth Observatory's post of [date], out at 00:00:30 US Eastern. */
internal fun earthAt(date: LocalDate): Instant = date.atTime(0, 0, 30).atZone(eastern).toInstant()

/** NASA's Image of the Day of [date], out at 16:30 UTC. */
internal fun nasaAt(date: LocalDate): Instant = date.atTime(16, 30).atZone(ZoneOffset.UTC).toInstant()

internal fun apod(day: String, video: Boolean = false): Out = LocalDate.parse(day).let { out(APOD, it, apodAt(it), video) }

internal fun earth(day: String): Out = LocalDate.parse(day).let { out(EARTH, it, earthAt(it)) }

internal fun nasa(day: String): Out = LocalDate.parse(day).let { out(NASA, it, nasaAt(it)) }

internal fun isWeekday(day: LocalDate) = day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY

/** Every source's posts from [from] to [to]: APOD daily, NASA and Earth on weekdays. */
internal fun week(from: String, to: String, sources: List<Source> = Source.entries): List<Out> {
    val days = generateSequence(LocalDate.parse(from)) { it.plusDays(1) }.takeWhile { it <= LocalDate.parse(to) }
    return days.flatMap { day ->
        sources.mapNotNull { s ->
            when {
                s == APOD -> apod(day.toString())
                !isWeekday(day) -> null
                s == NASA -> nasa(day.toString())
                else -> earth(day.toString())
            }
        }
    }.toList()
}

internal fun pic(out: Out) = out.post.picture()

internal fun notes(vararg outs: Out) = outs.associate { it.post.source to it.post.picture() }

/** How a picture shows in the logs: "APOD 10-20", "own". */
internal fun Picture.label() = if (this is Picture.Of) "$source ${date.toString().substring(5)}${if (url?.endsWith("-b.jpg") == true) "b" else ""}" else "own"

/**
 * A phone: [records] stored, [posts] coming out, a run at each [nextRun] (the alarms, offset 0)
 * and at each `pages` time (a page open), [sets] logged as "dd HH:mm SOURCE MM-dd" (a screen's
 * name after it when only one was set). Its store is [stored]: here every post out by then (by
 * the start of the [offline] span while in it); RulesSimulationTest's phone stores what its
 * checks fetched, as `Store` does.
 */
internal open class Phone(
    val zone: ZoneId,
    var columns: Map<Screen, List<Source>>,
    var time: PictureTime,
    val posts: List<Out>,
    var records: Map<Screen, Record>,
    var standIn: StandIn = StandIn.SKIP,
    /** While offline every check fails and the stored posts are those out at its start (B15). */
    val offline: ClosedRange<Instant>? = null,
) {
    val sets = mutableListOf<String>()
    val runs = mutableListOf<String>()
    val turns: Boolean get() = columns.values.any { it.size >= 2 }

    fun isOffline(now: Instant) = offline?.contains(now) == true

    open fun stored(now: Instant): Stored {
        val known = if (isOffline(now)) offline!!.start else now
        val newest = posts.filter { it.at <= known }.groupBy { it.post.source }.mapValues { (_, outs) -> outs.maxBy { it.at }.post }
        return Stored(newest, newest.filterValues { !it.isVideo }.keys, true, standIn)
    }

    /** Whether the set of [picture] on [screens] fails (its screens' records stay as they were). */
    protected open fun setFails(picture: Picture, screens: Set<Screen>, now: Instant): Boolean = false

    /** A set that worked, its records written. */
    protected open fun setDone(picture: Picture, screens: Set<Screen>, now: Instant) {}

    /**
     * What `Store.setDue` does at [now]: [dueWallpapers], the records without a set written at
     * once, then each set with its screens' records after it; a failed set ([failSets], or
     * [setFails]) leaves them. Returns the plan.
     */
    fun check(now: Instant, failSets: Boolean = false, pull: Boolean = false, stored: Stored = stored(now)): Plan {
        val plan = dueWallpapers(columns, records, stored, time, zone, now, pull)
        records = records + plan.screens.filterValues { !it.set }.mapValues { it.value.record }
        for ((picture, screens) in plan.sets) {
            if (failSets || setFails(picture, screens, now)) continue
            records = records + screens.associateWith { plan.screens.getValue(it).record }
            sets += local(zone, now) + " " + picture.label() + if (screens.size == 1) " ${screens.single()}" else ""
            setDone(picture, screens, now)
        }
        return plan
    }

    /** "Set as wallpaper" at [now], the setup unchanged. */
    fun set(now: Instant) {
        val restart = restartAndForget(columns, columns, records, stored(now), time, zone, now)
        records = records + restart.plan.screens.mapValues { it.value.record }
        check(now)
    }

    fun run(from: Instant, until: Instant, pages: List<Instant> = emptyList(), setAt: Instant? = null) {
        val opens = pages.sorted().toMutableList()
        var set = setAt
        var now = from
        var failures = 0
        while (now < until) {
            while (opens.isNotEmpty() && opens.first() < now) check(opens.removeAt(0))
            if (set != null && set < now) set(set).also { set = null }
            runs += local(zone, now)
            check(now)
            // An offline run fails: the alarms retry it (Background update).
            failures = if (isOffline(now)) failures + 1 else 0
            now = nextRun(now, zone, failures, time, turns, Duration.ZERO, waiting(columns, records, time, zone, now))
        }
    }
}
