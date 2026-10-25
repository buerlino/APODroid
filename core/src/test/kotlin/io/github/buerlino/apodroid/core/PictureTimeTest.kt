package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The picture time (J1): the change times through the DST days, [nextRun], and the three rules
 * over whole days in real time zones, with the posts coming out when the sites post them (APOD
 * at 00:05 US Eastern, Earth Observatory at 00:00 US Eastern on weekdays, NASA at 16:30 UTC on
 * weekdays) and a run at each [nextRun], as J2's alarms will.
 */
class PictureTimeTest {
    private val zurich = ZoneId.of("Europe/Zurich")
    private val la = ZoneId.of("America/Los_Angeles")
    private val sydney = ZoneId.of("Australia/Sydney")
    private val helsinki = ZoneId.of("Europe/Helsinki")
    private val newYork = ZoneId.of("America/New_York")
    private val utc = ZoneOffset.UTC

    private fun at(zone: ZoneId, local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun local(zone: ZoneId, t: Instant): String = t.atZone(zone).format(DateTimeFormatter.ofPattern("dd HH:mm"))

    // The change times

    @Test
    fun theHoursOfAColumn() {
        assertEquals(listOf(6), PictureTime(6, 6).hours(turns = false))
        assertEquals(listOf(0, 6, 12, 18), PictureTime(6, 6).hours(turns = true))
        assertEquals(listOf(1, 4, 7, 10, 13, 16, 19, 22), PictureTime(22, 3).hours(turns = true))
        assertEquals(listOf(9, 21), PictureTime(21, 12).hours(turns = true))
        assertEquals(listOf(6), PictureTime(6, 24).hours(turns = true))
    }

    @Test
    fun lastAndNextInSummerAndWinter() {
        val time = PictureTime()
        val summer = at(zurich, "2026-07-15T05:00")
        assertEquals(at(zurich, "2026-07-14T06:00"), time.last(summer, zurich, false))
        assertEquals(at(zurich, "2026-07-15T06:00"), time.next(summer, zurich, false))
        // At the change time itself it is the current one.
        val six = at(zurich, "2026-12-02T06:00")
        assertEquals(six, time.last(six, zurich, false))
        assertEquals(at(zurich, "2026-12-03T06:00"), time.next(six, zurich, false))
        assertEquals(Instant.parse("2026-12-02T05:00:00Z"), six)
    }

    /** The change times from the local day before [day] to the one after, as local "dd HH:mm". */
    private fun changes(time: PictureTime, zone: ZoneId, day: LocalDate, turns: Boolean = true): List<String> {
        var t = day.minusDays(1).atStartOfDay(zone).toInstant() - Duration.ofSeconds(1)
        val end = day.plusDays(2).atStartOfDay(zone).toInstant()
        val out = mutableListOf<String>()
        while (true) {
            t = time.next(t, zone, turns)
            if (t >= end) return out
            assertTrue(time.isChangeTime(t, zone, turns))
            out += local(zone, t)
        }
    }

    @Test
    fun theDstDaysOf2026KeepTheClocksHoursWithEveryInterval() {
        // EU 25 Oct (Zurich, back), US 1 Nov (Los Angeles, back), Sydney 4 Oct (forward); none
        // of the hours from 06:00 falls in the changed hour.
        for ((zone, day) in listOf(zurich to "2026-10-25", la to "2026-11-01", sydney to "2026-10-04")) {
            val date = LocalDate.parse(day)
            for (every in listOf(24, 12, 6, 3)) {
                val time = PictureTime(6, every)
                val expected = (-1L..1L).flatMap { d ->
                    time.hours(true).map { "%02d %02d:00".format(date.plusDays(d).dayOfMonth, it) }
                }
                assertEquals(expected, changes(time, zone, date), "$zone every $every h")
            }
        }
    }

    @Test
    fun aDstDayIs23Or25HoursBetweenTwoChanges() {
        val time = PictureTime()
        // Zurich, 25 Oct 2026: 25 h from 06:00 to 06:00.
        val back = time.next(at(zurich, "2026-10-24T06:00"), zurich, false)
        assertEquals(Duration.ofHours(25), Duration.between(at(zurich, "2026-10-24T06:00"), back))
        // Sydney, 4 Oct 2026: 23 h.
        val forward = time.next(at(sydney, "2026-10-03T06:00"), sydney, false)
        assertEquals(Duration.ofHours(23), Duration.between(at(sydney, "2026-10-03T06:00"), forward))
        // Every 3 h from 00:00 in Zurich on 25 Oct: the change after 00:00 comes at the clock's
        // 03:00, four real hours later (J2's DST night).
        val three = PictureTime(0, 3)
        val midnight = at(zurich, "2026-10-25T00:00")
        assertEquals(midnight + Duration.ofHours(4), three.next(midnight, zurich, true))
        assertEquals("25 03:00", local(zurich, three.next(at(zurich, "2026-10-25T01:50"), zurich, true)))
    }

    @Test
    fun anHourThatHappensTwiceIsOneChange() {
        // 25 Oct 2026, Zurich: 02:00 CEST (00:00 UTC), then 02:00 CET (01:00 UTC).
        val time = PictureTime(2, 24)
        val first = Instant.parse("2026-10-25T00:00:00Z")
        val second = Instant.parse("2026-10-25T01:00:00Z")
        assertEquals(first, time.next(at(zurich, "2026-10-24T12:00"), zurich, false))
        assertEquals(at(zurich, "2026-10-26T02:00"), time.next(first, zurich, false))
        assertFalse(time.isChangeTime(second, zurich, false))
        assertEquals(first, time.last(second + Duration.ofMinutes(30), zurich, false))
        assertEquals(listOf("24 02:00", "25 02:00", "26 02:00"), changes(time, zurich, LocalDate.parse("2026-10-25"), false))
    }

    @Test
    fun anHourThatDoesntExistComesAtTheGapsEnd() {
        // 28 Mar 2027, Zurich: 02:00 CET jumps to 03:00 CEST.
        val time = PictureTime(2, 24)
        val change = time.next(at(zurich, "2027-03-27T12:00"), zurich, false)
        assertEquals("28 03:00", local(zurich, change))
        assertEquals(Instant.parse("2027-03-28T01:00:00Z"), change)
        assertEquals(listOf("27 02:00", "28 03:00", "29 02:00"), changes(time, zurich, LocalDate.parse("2027-03-28"), false))
    }

    @Test
    fun aStampThatIsntAChangeTime() {
        val time = PictureTime(6, 6)
        assertTrue(time.isChangeTime(at(zurich, "2026-10-19T12:00"), zurich, true))
        assertFalse(time.isChangeTime(at(zurich, "2026-10-19T12:00"), zurich, false))
        // 0.6.0's Set clock: any moment.
        assertFalse(time.isChangeTime(Instant.parse("2026-10-19T10:23:17.123Z"), zurich, true))
        // Another zone's 06:00 (15:00 in Zurich).
        assertFalse(time.isChangeTime(at(la, "2026-10-19T06:00"), zurich, true))
    }

    // The next run

    private fun next(
        now: String,
        time: PictureTime = PictureTime(),
        turns: Boolean = false,
        failures: Int = 0,
        offset: Duration = Duration.ZERO,
        waiting: Boolean = false,
        zone: ZoneId = zurich,
    ) = local(zone, nextRun(at(zone, now), zone, failures, time, turns, offset, waiting))

    @Test
    fun theChecksAfterAChangeTime() {
        // 06:00, 06:15, 07:15, 08:15: the :05 posts of every whole-hour time zone; then the next day.
        assertEquals("15 06:00", next("2026-07-15T05:00"))
        assertEquals("15 06:15", next("2026-07-15T06:00"))
        assertEquals("15 07:15", next("2026-07-15T06:15"))
        assertEquals("15 08:15", next("2026-07-15T07:15"))
        // The window's end (B15): a half-hour zone's post, published within it.
        assertEquals("15 09:00", next("2026-07-15T08:15"))
        assertEquals("16 06:00", next("2026-07-15T09:00"))
        // A run in between (a page open) keeps the grid.
        assertEquals("15 07:15", next("2026-07-15T06:40"))
        // With turns the next change comes first: every 3 h, the window ends with it.
        assertEquals("15 09:00", next("2026-07-15T08:15", PictureTime(6, 3), turns = true))
        assertEquals("15 09:15", next("2026-07-15T09:00", PictureTime(6, 3), turns = true))
        assertEquals("15 09:00", next("2026-07-15T08:15", PictureTime(6, 6), turns = true))
        assertEquals("15 12:00", next("2026-07-15T09:00", PictureTime(6, 6), turns = true))
    }

    @Test
    fun aWaitingScreenIsCheckedHourly() {
        val time = PictureTime(6, 6)
        assertEquals("15 09:00", next("2026-07-15T08:15", time, turns = true, waiting = true))
        assertEquals("15 09:15", next("2026-07-15T09:00", time, turns = true, waiting = true))
        assertEquals("15 11:15", next("2026-07-15T10:15", time, turns = true, waiting = true))
        assertEquals("15 12:00", next("2026-07-15T11:15", time, turns = true, waiting = true))
        // Without a waiting screen: the window's end, then the next change time.
        assertEquals("15 09:00", next("2026-07-15T08:15", time, turns = true))
        assertEquals("15 12:00", next("2026-07-15T09:00", time, turns = true))
        // Every 24 h: hourly all day.
        assertEquals("15 21:15", next("2026-07-15T20:15", PictureTime(6, 24), turns = true, waiting = true))
    }

    @Test
    fun aFailedRunIsRetriedIn15MinutesThenHourly() {
        assertEquals("15 10:15", next("2026-07-15T10:00", failures = 1))
        assertEquals("15 11:00", next("2026-07-15T10:00", failures = 2))
        assertEquals("15 11:15", next("2026-07-15T10:15", failures = 5))
        // A failure at the change time: the window's own check is the retry.
        assertEquals("15 06:15", next("2026-07-15T06:00", failures = 1))
        // The next change or check comes first when it's earlier.
        assertEquals("15 06:00", next("2026-07-15T05:30", failures = 2))
        assertEquals("15 06:30", next("2026-07-15T06:15", failures = 1))
    }

    // The alarms armed outside a run's end (J2)

    private fun alarm(
        now: String,
        failedAt: String? = null,
        failures: Int = 1,
        time: PictureTime = PictureTime(),
        turns: Boolean = false,
        offset: Duration = Duration.ZERO,
        waiting: Boolean = false,
    ) = local(zurich, nextAlarm(at(zurich, now), zurich, failures, failedAt?.let { at(zurich, it) }, time, turns, offset, waiting))

    /** At a run's end the alarms come at [nextRun]'s time, with or without failures. */
    @Test
    fun theAlarmAtARunsEndIsTheNextRun() {
        for (now in listOf("2026-07-15T05:00", "2026-07-15T06:00", "2026-07-15T06:40", "2026-07-15T10:00", "2026-07-15T23:59")) {
            for (failures in 0..3) {
                for (turns in listOf(false, true)) {
                    val t = at(zurich, now)
                    val time = PictureTime(6, 3)
                    val seven = Duration.ofMinutes(7)
                    assertEquals(
                        nextRun(t, zurich, failures, time, turns, seven, waiting = turns),
                        nextAlarm(t, zurich, failures, t, time, turns, seven, waiting = turns),
                        "$now $failures $turns",
                    )
                }
            }
        }
    }

    /** A Set or a page open after a failed run keeps its retry: armed from the run, not from now. */
    @Test
    fun aLaterArmingKeepsTheRetry() {
        assertEquals("15 10:15", alarm("2026-07-15T10:05", failedAt = "2026-07-15T10:00"))
        assertEquals("15 11:00", alarm("2026-07-15T10:30", failedAt = "2026-07-15T10:00", failures = 2))
        // Without a failure: by the clock.
        assertEquals("16 06:00", alarm("2026-07-15T10:05", failures = 0))
        assertEquals("16 06:00", alarm("2026-07-15T10:05", failedAt = "2026-07-15T10:00", failures = 0))
        assertEquals("16 06:00", alarm("2026-07-15T10:05", failedAt = null))
        // The clock's check first when it's earlier.
        assertEquals("15 06:00", alarm("2026-07-15T05:50", failedAt = "2026-07-15T05:00", failures = 2))
    }

    // A passed check kept (B16)

    private val sevenMinutes = Duration.ofMinutes(7)

    /** The alarms armed at [now] after the job's last run started at [lastRun], with an offset of 7 min. */
    private fun owed(
        now: String,
        lastRun: String?,
        time: PictureTime = PictureTime(),
        turns: Boolean = false,
        waiting: Boolean = false,
        failedAt: String? = null,
        failures: Int = 0,
    ) = local(zurich, nextAlarm(at(zurich, now), zurich, failures, failedAt?.let { at(zurich, it) }, time, turns, sevenMinutes, waiting, lastRun?.let { at(zurich, it) }))

    /**
     * A page open (or a Set, a clock change) between the 06:07 check and its alarm's delivery
     * (up to 06:17): the pair it replaces would have run, so the run comes at once. Before B16 it
     * was armed for 06:22, and the 06:07 check never ran.
     */
    @Test
    fun aPageOpenBeforeTheAlarmCameKeepsTheCheck() {
        assertEquals("15 06:10", owed("2026-07-15T06:10", lastRun = "2026-07-14T09:07"))
        assertEquals("15 06:16", owed("2026-07-15T06:16", lastRun = "2026-07-14T09:07"))
        // The same without a last run: dropped, as before (a fresh install has none).
        assertEquals("15 06:22", owed("2026-07-15T06:10", lastRun = null))
        // The 06:22 check passed too, its alarm not yet delivered.
        assertEquals("15 06:25", owed("2026-07-15T06:25", lastRun = "2026-07-15T06:08"))
    }

    /** A run that did its update before the check (a fallback run at 06:05) ends after it: one more run, at once. */
    @Test
    fun aRunThatStartedBeforeTheCheckAndEndedAfterIt() {
        assertEquals("15 06:08", owed("2026-07-15T06:08", lastRun = "2026-07-15T06:05"))
        // Ended before the check: the check stays the alarm.
        assertEquals("15 06:07", owed("2026-07-15T06:06", lastRun = "2026-07-15T06:05"))
        // Started at or after the check (the alarm's run, late in its window): nothing owed.
        assertEquals("15 06:22", owed("2026-07-15T06:13", lastRun = "2026-07-15T06:12"))
        assertEquals("15 06:22", owed("2026-07-15T06:08", lastRun = "2026-07-15T06:07"))
    }

    /**
     * A page open (or a Set, the switch on, a TIME_SET) while a run goes keeps the pair the run's
     * start armed, 15 min after it, so a process killed mid-run is retried (B16f1). Before, a
     * fallback run at 12:00 and a page open at 12:01 armed tomorrow's 06:07 check.
     */
    @Test
    fun aReArmDuringARunKeepsItsRetry() {
        fun during(now: String, lastRun: String) = local(
            zurich,
            nextAlarm(at(zurich, now), zurich, 0, null, PictureTime(), false, sevenMinutes, false, at(zurich, lastRun), running = true),
        )
        assertEquals("15 12:15", during("2026-07-15T12:01", "2026-07-15T12:00"))
        // Not running: the clock's next check, as before.
        assertEquals("16 06:07", owed("2026-07-15T12:01", lastRun = "2026-07-15T12:00"))
        // An earlier check stays the alarm.
        assertEquals("15 06:07", during("2026-07-15T06:01", "2026-07-15T06:00"))
        // A run going longer than 15 min: at once (the floor).
        assertEquals("15 12:20", during("2026-07-15T12:20", "2026-07-15T12:00"))
    }

    /**
     * A run that started and never ended (killed by `install -r`, `kill -9`, the low-memory killer
     * or a Recents swipe) counts as going in a new process too, so an arming there keeps its retry
     * (B19; V11a armed the clock's next check, 05:24 for a 04:27 run). The cap is 15 min after the
     * start with no failure before, an hour with one or more: the next run's start counts the
     * killed one ([failedAt] its start), so a crash on every run is retried 15 min later, then
     * hourly. Before B19 the counted failure's retry made it 15 min every time.
     */
    @Test
    fun aKilledRunKeepsItsRetryAndCounts() {
        fun during(now: String, lastRun: String, failures: Int = 0, failedAt: String? = null) = local(
            zurich,
            nextAlarm(
                at(zurich, now), zurich, failures, failedAt?.let { at(zurich, it) }, PictureTime(), false, sevenMinutes, false,
                at(zurich, lastRun), running = true,
            ),
        )
        // No failure before: 15 min after the start (the page open after a kill at 04:28).
        assertEquals("15 04:42", during("2026-07-15T04:29", "2026-07-15T04:27"))
        // The next run's start counted the killed one (failedAt = its start): an hour after it.
        assertEquals("15 05:42", during("2026-07-15T04:42", "2026-07-15T04:42", failures = 1, failedAt = "2026-07-15T04:42"))
        assertEquals("15 05:42", during("2026-07-15T04:50", "2026-07-15T04:42", failures = 1, failedAt = "2026-07-15T04:42"))
        assertEquals("15 13:00", during("2026-07-15T12:01", "2026-07-15T12:00", failures = 3, failedAt = "2026-07-15T12:00"))
        // The clock's earlier check stays the alarm; one long passed: at once.
        assertEquals("15 06:07", during("2026-07-15T05:30", "2026-07-15T05:20", failures = 1, failedAt = "2026-07-15T05:20"))
        assertEquals("15 14:00", during("2026-07-15T14:00", "2026-07-15T12:00", failures = 2, failedAt = "2026-07-15T12:00"))
        // The retry while no run goes stays as before: 15 min after the first failure, then hourly.
        assertEquals("15 04:57", alarmAt("2026-07-15T04:43", failures = 1, failedAt = "2026-07-15T04:42"))
        assertEquals("15 05:42", alarmAt("2026-07-15T04:43", failures = 2, failedAt = "2026-07-15T04:42"))
        assertEquals(Duration.ofMinutes(15), runRetry(0))
        assertEquals(Duration.ofHours(1), runRetry(1))
        assertEquals(Duration.ofHours(1), runRetry(5))
    }

    private fun alarmAt(now: String, failures: Int, failedAt: String) =
        local(zurich, nextAlarm(at(zurich, now), zurich, failures, at(zurich, failedAt), PictureTime(), false, sevenMinutes, false))

    /** Network time moves the clock past a check (TIME_SET): at once. The clock set back: nothing owed. */
    @Test
    fun theClockMovedOn() {
        assertEquals("15 08:30", owed("2026-07-15T08:30", lastRun = "2026-07-15T06:22"))
        // A missed morning (the phone off), switched on after the window: the change time is owed.
        assertEquals("15 11:00", owed("2026-07-15T11:00", lastRun = "2026-07-14T09:07"))
        assertEquals("15 06:22", owed("2026-07-15T06:10", lastRun = "2026-07-15T07:00"))
        // Nothing passed since the last run (after the window, all checks done): the next morning.
        assertEquals("16 06:07", owed("2026-07-15T11:00", lastRun = "2026-07-15T09:07"))
    }

    /**
     * The checks of the change time before stay in view until the offset of the current one has
     * passed: with turns every 3 h, at 06:03 with an offset of 7 min, a run that started at 05:20
     * missed 03:00's 05:22 check.
     */
    @Test
    fun theChangeTimeBeforeWithinTheOffset() {
        val time = PictureTime(0, 3)
        assertEquals("15 06:03", owed("2026-07-15T06:03", lastRun = "2026-07-15T05:20", time = time, turns = true, waiting = true))
        assertEquals("15 06:07", owed("2026-07-15T06:03", lastRun = "2026-07-15T05:23", time = time, turns = true, waiting = true))
    }

    /** An owed check comes before a retry not yet due. */
    @Test
    fun anOwedCheckBeforeTheRetry() {
        assertEquals("15 06:10", owed("2026-07-15T06:10", lastRun = "2026-07-15T06:00", failedAt = "2026-07-15T06:02", failures = 1))
        // Before the check: the check, then the retry.
        assertEquals("15 06:07", owed("2026-07-15T06:05", lastRun = "2026-07-15T06:00", failedAt = "2026-07-15T06:02", failures = 1))
    }

    /** A retry whose time passed without a run (the process died, a boot): at once. */
    @Test
    fun aRetryDueComesAtOnce() {
        assertEquals("15 10:40", alarm("2026-07-15T10:40", failedAt = "2026-07-15T10:00"))
        assertEquals("15 13:00", alarm("2026-07-15T13:00", failedAt = "2026-07-15T10:00", failures = 4))
        // The clock set back: a failure "after" now retries no later than the clock's next check.
        assertEquals("16 06:00", alarm("2026-07-15T10:00", failedAt = "2026-07-17T10:00"))
    }

    @Test
    fun theOffsetMovesTheClocksChecksNotTheRetries() {
        val seven = Duration.ofMinutes(7)
        assertEquals("15 06:07", next("2026-07-15T05:00", offset = seven))
        assertEquals("15 06:22", next("2026-07-15T06:07", offset = seven))
        assertEquals("15 07:22", next("2026-07-15T06:22", offset = seven))
        // A page's run at 06:03 doesn't skip the alarm at 06:07.
        assertEquals("15 06:07", next("2026-07-15T06:03", offset = seven))
        assertEquals("15 10:15", next("2026-07-15T10:00", failures = 1, offset = seven))
        // Kept in range.
        assertEquals("15 06:10", next("2026-07-15T05:00", offset = Duration.ofMinutes(25)))
        assertEquals("15 06:00", next("2026-07-15T05:00", offset = Duration.ofMinutes(-5)))
    }

    @Test
    fun theOffsetIsInRange() {
        val random = Random(42)
        val offsets = List(2000) { randomOffset(random) }
        assertTrue(offsets.all { it >= Duration.ZERO && it <= maxOffset })
        assertTrue(offsets.toSet().size > 100)
        assertEquals(Duration.ofMinutes(10), maxOffset)
    }

    // The rules over whole days

    private data class Out(val at: Instant, val post: Post)

    /** A post published at [at] (B15), as the sites say. */
    private fun post(source: Source, date: LocalDate, at: Instant, video: Boolean = false) =
        Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video, published = at)

    private fun out(source: Source, date: LocalDate, at: Instant, video: Boolean = false) = Out(at, post(source, date, at, video))

    private fun apod(day: String, video: Boolean = false): Out {
        val date = LocalDate.parse(day)
        return out(APOD, date, date.atTime(0, 5).atZone(newYork).toInstant(), video)
    }

    private fun earth(day: String): Out {
        val date = LocalDate.parse(day)
        return out(EARTH, date, date.atTime(0, 0, 30).atZone(newYork).toInstant())
    }

    private fun nasa(day: String): Out {
        val date = LocalDate.parse(day)
        return out(NASA, date, date.atTime(16, 30).atZone(utc).toInstant())
    }

    /** Every source's posts from [from] to [to]: APOD daily, NASA and Earth on weekdays. */
    private fun week(from: String, to: String, sources: List<Source> = Source.entries): List<Out> {
        val days = generateSequence(LocalDate.parse(from)) { it.plusDays(1) }.takeWhile { it <= LocalDate.parse(to) }
        return days.flatMap { day ->
            val weekday = day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY
            sources.mapNotNull { s ->
                when {
                    s == APOD -> apod(day.toString())
                    !weekday -> null
                    s == NASA -> nasa(day.toString())
                    else -> earth(day.toString())
                }
            }
        }.toList()
    }

    private fun pic(out: Out) = out.post.picture()

    private fun notes(vararg outs: Out) = outs.associate { it.post.source to it.post.picture() }

    /**
     * A phone: [records] stored, [posts] coming out, a run at each [nextRun] (the alarms, offset
     * 0) and at each [pages] time (a page open), [sets] logged as "dd HH:mm SOURCE MM-dd".
     */
    private inner class Phone(
        val zone: ZoneId,
        val columns: Map<Screen, List<Source>>,
        val time: PictureTime,
        val posts: List<Out>,
        var records: Map<Screen, Record>,
        val standIn: StandIn = StandIn.SKIP,
        /** While offline every check fails and the stored posts are those out at its start (B15). */
        val offline: ClosedRange<Instant>? = null,
    ) {
        val sets = mutableListOf<String>()
        val runs = mutableListOf<String>()
        private val turns = columns.values.any { it.size >= 2 }

        fun isOffline(now: Instant) = offline?.contains(now) == true

        fun stored(now: Instant): Stored {
            val known = if (isOffline(now)) offline!!.start else now
            val newest = posts.filter { it.at <= known }.groupBy { it.post.source }.mapValues { (_, outs) -> outs.maxBy { it.at }.post }
            return Stored(newest, newest.filterValues { !it.isVideo }.keys, true, standIn)
        }

        fun check(now: Instant, failSets: Boolean = false, pull: Boolean = false) {
            val plan = dueWallpapers(columns, records, stored(now), time, zone, now, pull)
            if (failSets) {
                records = records + plan.screens.filterValues { !it.set }.mapValues { it.value.record }
                return
            }
            records = records + plan.screens.mapValues { it.value.record }
            for ((picture, screens) in plan.sets) {
                val what = if (picture is Picture.Of) "${picture.source} ${picture.date.toString().substring(5)}" else "own"
                sets += local(zone, now) + " " + what + if (screens.size == 1) " ${screens.single()}" else ""
            }
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

    private val both = apodColumns(setOf(HOME, LOCK))

    /** APOD mode, yesterday's APOD on, set at yesterday's [change]. */
    private fun apodPhone(zone: ZoneId, onDay: String, change: String, posts: List<Out>, pictureAt: Int = 6, offline: ClosedRange<Instant>? = null): Phone {
        val on = apod(onDay)
        val record = Record(APOD, pic(on), at(zone, change), notes(on))
        return Phone(zone, both, PictureTime(pictureAt), posts, mapOf(HOME to record, LOCK to record), offline = offline)
    }

    // The window by publish time (B15)

    @Test
    fun zurichSetsTodaysApodFirstSeenAt0901() {
        // Every check failed until 09:00 (a 503, Doze); a page open at 09:01 sees the 06:05 post.
        val posts = listOf(apod("2026-07-14"), apod("2026-07-15"), apod("2026-07-16"))
        val phone = apodPhone(zurich, "2026-07-14", "2026-07-14T06:00", posts, offline = at(zurich, "2026-07-15T05:59")..at(zurich, "2026-07-15T09:00"))
        phone.run(at(zurich, "2026-07-15T05:00"), at(zurich, "2026-07-16T05:00"), pages = listOf(at(zurich, "2026-07-15T09:01")))
        assertEquals(listOf("15 09:01 APOD 07-15"), phone.sets)
    }

    @Test
    fun zurichOfflineFrom0600To0900SetsTodaysApodAt1000() {
        // The failing checks took the change time (one source); the 10:00 retry still sets it.
        val posts = listOf(apod("2026-07-14"), apod("2026-07-15"), apod("2026-07-16"))
        val phone = apodPhone(zurich, "2026-07-14", "2026-07-14T06:00", posts, offline = at(zurich, "2026-07-15T05:59")..at(zurich, "2026-07-15T09:59"))
        phone.run(at(zurich, "2026-07-15T05:00"), at(zurich, "2026-07-16T05:00"))
        assertEquals(listOf("15 10:00 APOD 07-15"), phone.sets)
        assertEquals(listOf("15 05:00", "15 06:00", "15 06:15", "15 07:15", "15 08:15", "15 09:00", "15 10:00"), phone.runs)
    }

    @Test
    fun zurichAPageOpenedOfflineAt0700ThenARunAt1000() {
        val posts = listOf(apod("2026-07-14"), apod("2026-07-15"))
        val phone = apodPhone(zurich, "2026-07-14", "2026-07-14T06:00", posts, offline = at(zurich, "2026-07-15T05:59")..at(zurich, "2026-07-15T07:00"))
        phone.check(at(zurich, "2026-07-15T07:00"))
        assertEquals(at(zurich, "2026-07-15T06:00"), phone.records.getValue(HOME).stamp)
        assertEquals(emptyList(), phone.sets)
        phone.check(at(zurich, "2026-07-15T10:00"))
        assertEquals(listOf("15 10:00 APOD 07-15"), phone.sets)
    }

    @Test
    fun losAngelesAfterAnEveningPull() {
        // A pull at 21:30 puts the 21:05 post on at once; the next morning nothing goes back and
        // nothing changes at 06:00 (it's the target, on already); the next 21:05 post waits for its 06:00.
        val posts = listOf(apod("2026-10-19"), apod("2026-10-20"), apod("2026-10-21"))
        val phone = apodPhone(la, "2026-10-19", "2026-10-19T06:00", posts)
        phone.check(at(la, "2026-10-19T21:30"), pull = true)
        phone.run(at(la, "2026-10-19T22:00"), at(la, "2026-10-21T07:00"), pages = listOf(at(la, "2026-10-20T21:30")))
        assertEquals(listOf("19 21:30 APOD 10-20", "21 06:00 APOD 10-21"), phone.sets)
        assertTrue(phone.runs.containsAll(listOf("20 06:00", "20 06:15", "20 09:00")))
    }

    @Test
    fun tehranGetsTheApodOf0835AtThe0900Check() {
        // Iran on UTC+3:30 all year: APOD at 00:05 EST is 08:35, within the window, seen at its end.
        val tehran = ZoneId.of("Asia/Tehran")
        val phone = apodPhone(tehran, "2026-12-01", "2026-12-01T06:00", listOf(apod("2026-12-01"), apod("2026-12-02")))
        phone.run(at(tehran, "2026-12-02T05:00"), at(tehran, "2026-12-03T05:00"))
        assertEquals(listOf("02 09:00 APOD 12-02"), phone.sets)
        assertEquals(at(tehran, "2026-12-02T08:35"), apod("2026-12-02").at)
    }

    @Test
    fun aPictureTimeOf0200OnTheEuDstNightOf2026() {
        // 25 Oct 2026, Zurich: 02:00 happens twice, the change at the first (00:00 UTC). Today's
        // APOD (05:05 CET) is after the window (04:00 CET): yesterday's goes on, today's the next night.
        val posts = listOf(apod("2026-10-23"), apod("2026-10-24"), apod("2026-10-25"))
        val phone = apodPhone(zurich, "2026-10-23", "2026-10-24T02:00", posts, pictureAt = 2)
        phone.run(at(zurich, "2026-10-25T01:30"), at(zurich, "2026-10-26T03:00"))
        assertEquals(listOf("25 02:00 APOD 10-24", "26 02:00 APOD 10-25"), phone.sets)
        // The first check of the night: 02:00 CEST, 02:15 CEST, 02:15 CET, 03:15, the window's end 04:00 CET.
        assertEquals(listOf("25 01:30", "25 02:00", "25 02:15", "25 02:15", "25 03:15", "25 04:00"), phone.runs.take(6))
        assertEquals(Instant.parse("2026-10-25T03:00:00Z"), nextRun(at(zurich, "2026-10-25T03:15"), zurich, 0, PictureTime(2), false, Duration.ZERO, false))
    }

    @Test
    fun aPictureTimeOf0200OnTheEuDstNightOf2027() {
        // 28 Mar 2027, Zurich: no 02:00, the change at 03:00 CEST (01:00 UTC), the window to 06:00
        // CEST; APOD at 06:05 CEST is after it, so 27 Mar's goes on, 28 Mar's the next night.
        val posts = listOf(apod("2027-03-26"), apod("2027-03-27"), apod("2027-03-28"))
        val phone = apodPhone(zurich, "2027-03-26", "2027-03-27T02:00", posts, pictureAt = 2)
        phone.run(at(zurich, "2027-03-28T01:00"), at(zurich, "2027-03-29T03:00"))
        assertEquals(listOf("28 03:00 APOD 03-27", "29 02:00 APOD 03-28"), phone.sets)
        assertEquals(listOf("28 01:00", "28 03:00", "28 03:15", "28 04:15", "28 05:15", "28 06:00", "29 02:00"), phone.runs.take(7))
    }

    @Test
    fun losAngelesOnTheUsDstDay() {
        // 1 Nov 2026: Los Angeles and New York fall back at 02:00. The APOD of 1 Nov is out at
        // 21:05 PDT the evening before, that of 2 Nov at 21:05 PST: each on at its 06:00.
        val posts = listOf(apod("2026-10-30"), apod("2026-10-31"), apod("2026-11-01"), apod("2026-11-02"))
        val phone = apodPhone(la, "2026-10-30", "2026-10-30T06:00", posts)
        phone.run(at(la, "2026-10-31T05:00"), at(la, "2026-11-02T07:00"))
        assertEquals(listOf("31 06:00 APOD 10-31", "01 06:00 APOD 11-01", "02 06:00 APOD 11-02"), phone.sets)
        assertTrue(phone.runs.containsAll(listOf("01 06:00", "01 06:15", "01 07:15", "01 08:15", "01 09:00")))
    }

    @Test
    fun sydneyOnItsDstDay() {
        // 4 Oct 2026: Sydney springs forward at 02:00. APOD is out at 14:05 AEST on 3 Oct, at
        // 15:05 AEDT on 4 Oct: each on the next morning at 06:00.
        val posts = listOf(apod("2026-10-02"), apod("2026-10-03"), apod("2026-10-04"))
        val phone = apodPhone(sydney, "2026-10-02", "2026-10-03T06:00", posts)
        phone.run(at(sydney, "2026-10-03T07:00"), at(sydney, "2026-10-05T07:00"))
        assertEquals(listOf("04 06:00 APOD 10-03", "05 06:00 APOD 10-04"), phone.sets)
        assertTrue(phone.runs.containsAll(listOf("04 06:00", "04 06:15", "04 07:15", "04 08:15", "04 09:00")))
    }

    @Test
    fun aTimeZoneChangeBetweenRuns() {
        // On 19 Oct in Zurich, today's APOD on since 06:15; the phone then runs in New York.
        val posts = listOf(apod("2026-10-18"), apod("2026-10-19"), apod("2026-10-20"))
        val on = apod("2026-10-19")
        val record = Record(APOD, pic(on), at(zurich, "2026-10-19T06:00"), notes(on))
        val phone = Phone(newYork, both, PictureTime(), posts, mapOf(HOME to record, LOCK to record))
        // Zurich's 06:00 isn't a change time there: New York's 06:00 is taken, nothing is set.
        phone.check(at(newYork, "2026-10-19T18:00"))
        assertEquals(emptyList(), phone.sets)
        assertEquals(at(newYork, "2026-10-19T06:00"), phone.records.getValue(HOME).stamp)
        // The next morning by New York's clock: the APOD of 00:05 at the 06:00 change.
        phone.run(at(newYork, "2026-10-19T19:00"), at(newYork, "2026-10-21T05:00"))
        assertEquals(listOf("20 06:00 APOD 10-20"), phone.sets)
        // With turns: the stamp taken, nothing set until the next change time.
        val turns = Record(APOD, pic(on), at(zurich, "2026-10-19T06:00"), notes(on, nasa("2026-10-16"), earth("2026-10-16")))
        val weekend = Phone(newYork, mapOf(HOME to Source.entries, LOCK to Source.entries), PictureTime(6, 6), week("2026-10-16", "2026-10-19"), mapOf(HOME to turns, LOCK to turns))
        weekend.check(at(newYork, "2026-10-19T03:00"))
        assertEquals(emptyList(), weekend.sets)
        assertEquals(at(newYork, "2026-10-19T00:00"), weekend.records.getValue(HOME).stamp)
    }

    @Test
    fun zurichInSummerGetsTodaysApodAtThe0615Check() {
        val phone = apodPhone(zurich, "2026-07-14", "2026-07-14T06:00", listOf(apod("2026-07-14"), apod("2026-07-15"), apod("2026-07-16")))
        phone.run(at(zurich, "2026-07-15T05:00"), at(zurich, "2026-07-16T05:00"))
        assertEquals(listOf("15 06:15 APOD 07-15"), phone.sets)
        assertEquals(listOf("15 05:00", "15 06:00", "15 06:15", "15 07:15", "15 08:15", "15 09:00"), phone.runs)
    }

    @Test
    fun zurichInTheWeekBetweenTheDstChangesGetsApodAt0600() {
        // 25 Oct to 1 Nov 2026: Zurich on CET, New York still on EDT, so APOD comes at 05:05.
        val phone = apodPhone(zurich, "2026-10-26", "2026-10-26T06:00", listOf(apod("2026-10-26"), apod("2026-10-27")))
        phone.run(at(zurich, "2026-10-27T05:00"), at(zurich, "2026-10-28T05:00"))
        assertEquals(listOf("27 06:00 APOD 10-27"), phone.sets)
    }

    @Test
    fun zurichInWinterGetsApodAt0615() {
        val phone = apodPhone(zurich, "2026-12-01", "2026-12-01T06:00", listOf(apod("2026-12-01"), apod("2026-12-02")))
        phone.run(at(zurich, "2026-12-02T05:00"), at(zurich, "2026-12-03T05:00"))
        assertEquals(listOf("02 06:15 APOD 12-02"), phone.sets)
    }

    @Test
    fun zurichAfterAMissedMorningIsNotADayLate() {
        // The phone was off all of 14 Jul: yesterday's APOD at 06:00, today's at 06:15 all the same.
        val posts = listOf(apod("2026-07-13"), apod("2026-07-14"), apod("2026-07-15"))
        val phone = apodPhone(zurich, "2026-07-13", "2026-07-13T06:00", posts)
        phone.run(at(zurich, "2026-07-15T06:00"), at(zurich, "2026-07-16T05:00"))
        assertEquals(listOf("15 06:00 APOD 07-14", "15 06:15 APOD 07-15"), phone.sets)
    }

    @Test
    fun zurichAfterAVideoDayWithSkipIt() {
        val posts = listOf(apod("2026-07-13"), apod("2026-07-14", video = true), apod("2026-07-15"))
        val phone = apodPhone(zurich, "2026-07-13", "2026-07-13T06:00", posts)
        phone.run(at(zurich, "2026-07-14T05:00"), at(zurich, "2026-07-16T05:00"))
        assertEquals(listOf("15 06:15 APOD 07-15"), phone.sets)
        // The video day kept the picture and moved the stamp: the next morning is a change as usual.
        assertEquals(at(zurich, "2026-07-15T06:00"), phone.records.getValue(HOME).stamp)
    }

    @Test
    fun losAngelesGetsThe2105PostAt0600AndNothingAt2105() {
        val posts = listOf(apod("2026-10-19"), apod("2026-10-20"), apod("2026-10-21"))
        val phone = apodPhone(la, "2026-10-19", "2026-10-19T06:00", posts)
        val pages = listOf(at(la, "2026-10-19T21:10"), at(la, "2026-10-19T23:30"), at(la, "2026-10-20T21:30"))
        phone.run(at(la, "2026-10-19T20:00"), at(la, "2026-10-21T05:00"), pages)
        assertEquals(listOf("20 06:00 APOD 10-20"), phone.sets)
    }

    @Test
    fun losAngelesAfterAnEveningSet() {
        val posts = listOf(apod("2026-10-19"), apod("2026-10-20"), apod("2026-10-21"), apod("2026-10-22"))
        val phone = apodPhone(la, "2026-10-19", "2026-10-19T06:00", posts)
        val pages = listOf(at(la, "2026-10-20T21:30"), at(la, "2026-10-21T08:00"))
        phone.run(at(la, "2026-10-19T20:00"), at(la, "2026-10-21T21:00"), pages, setAt = at(la, "2026-10-19T21:30"))
        // Set puts the 21:05 post on at once; 06:00 the next day changes as usual (it's on
        // already), and the next 21:05 post waits for its 06:00, a page open or not.
        assertEquals(listOf("19 21:30 APOD 10-20", "21 06:00 APOD 10-21"), phone.sets)
        // The Set took the current change time: the morning's.
        assertTrue(phone.runs.containsAll(listOf("20 06:00", "20 06:15", "21 06:00")))
    }

    @Test
    fun helsinkiGetsApodAt0715() {
        val phone = apodPhone(helsinki, "2026-07-14", "2026-07-14T06:00", listOf(apod("2026-07-14"), apod("2026-07-15")))
        phone.run(at(helsinki, "2026-07-15T05:00"), at(helsinki, "2026-07-16T05:00"))
        assertEquals(listOf("15 07:15 APOD 07-15"), phone.sets)
    }

    @Test
    fun sydneyGetsTheAfternoonsPostTheNextMorning() {
        // APOD comes at 15:05 in Sydney (AEDT since 4 Oct).
        val posts = listOf(apod("2026-10-19"), apod("2026-10-20"), apod("2026-10-21"))
        val phone = apodPhone(sydney, "2026-10-19", "2026-10-20T06:00", posts)
        phone.run(at(sydney, "2026-10-20T05:00"), at(sydney, "2026-10-22T07:00"))
        assertEquals(listOf("21 06:00 APOD 10-20", "22 06:00 APOD 10-21"), phone.sets)
        assertEquals(at(sydney, "2026-10-21T15:05"), apod("2026-10-21").at)
    }

    @Test
    fun utcGetsApodAt0600() {
        val phone = apodPhone(utc, "2026-07-14", "2026-07-14T06:00", listOf(apod("2026-07-14"), apod("2026-07-15")))
        phone.run(at(utc, "2026-07-15T05:00"), at(utc, "2026-07-16T05:00"))
        assertEquals(listOf("15 06:00 APOD 07-15"), phone.sets)
        // With the picture time 04:00 the 04:05 post goes on at the 04:15 check.
        val early = apodPhone(utc, "2026-07-14", "2026-07-14T04:00", listOf(apod("2026-07-14"), apod("2026-07-15")), pictureAt = 4)
        early.run(at(utc, "2026-07-15T03:00"), at(utc, "2026-07-16T03:00"))
        assertEquals(listOf("15 04:15 APOD 07-15"), early.sets)
    }

    // Several sources

    private fun turnsPhone(zone: ZoneId, every: Int, posts: List<Out>, record: Record, sources: List<Source> = Source.entries) =
        Phone(zone, mapOf(HOME to sources, LOCK to sources), PictureTime(6, every), posts, mapOf(HOME to record, LOCK to record))

    @Test
    fun aMondayInZurichEvery6Hours() {
        // Mon 19 Oct 2026 (CEST): APOD at 06:05, Earth at 06:00:30, NASA at 18:30.
        val posts = week("2026-10-16", "2026-10-20")
        val sunday = Record(APOD, pic(apod("2026-10-18")), at(zurich, "2026-10-18T18:00"),
            notes(apod("2026-10-18"), nasa("2026-10-16"), earth("2026-10-16")))
        val phone = turnsPhone(zurich, 6, posts, sunday)
        phone.run(at(zurich, "2026-10-19T00:00"), at(zurich, "2026-10-20T05:00"))
        assertEquals(
            listOf(
                // 06:15: APOD and Earth are new; Earth is next after APOD.
                "19 06:15 EARTH 10-19",
                // 12:00: APOD is next after Earth.
                "19 12:00 APOD 10-19",
                // 18:00: nothing new yet, the screens wait; NASA at 18:30, seen at 19:15.
                "19 19:15 NASA 10-19",
            ),
            phone.sets,
        )
        // Waiting from 00:00 to 06:00: hourly checks.
        assertTrue(phone.runs.containsAll(listOf("19 00:15", "19 01:15", "19 05:15", "19 18:15", "19 19:15")))
        // Tue 00:00: nothing new, waiting again.
        assertTrue(waiting(phone.columns, phone.records, phone.time, zurich, at(zurich, "2026-10-20T00:30")))
    }

    @Test
    fun aWeekendKeepsApodAndSetsNothingAtNasasAndEarthsTimes() {
        val posts = week("2026-10-16", "2026-10-19")
        val friday = Record(NASA, pic(nasa("2026-10-16")), at(zurich, "2026-10-16T18:00"),
            notes(apod("2026-10-16"), nasa("2026-10-16"), earth("2026-10-16")))
        val phone = turnsPhone(zurich, 6, posts, friday)
        phone.run(at(zurich, "2026-10-17T00:00"), at(zurich, "2026-10-19T00:00"))
        assertEquals(listOf("17 06:15 APOD 10-17", "18 06:15 APOD 10-18"), phone.sets)
    }

    @Test
    fun every12HoursWithApodAndNasaInWinterZurich() {
        // Tue 1 Dec 2026 (CET): APOD at 06:05, NASA at 17:30; changes at 06:00 and 18:00.
        val sources = listOf(APOD, NASA)
        val posts = week("2026-11-30", "2026-12-02", sources)
        val monday = Record(NASA, pic(nasa("2026-11-30")), at(zurich, "2026-11-30T18:00"), notes(apod("2026-11-30"), nasa("2026-11-30")))
        val phone = turnsPhone(zurich, 12, posts, monday, sources)
        phone.run(at(zurich, "2026-12-01T05:00"), at(zurich, "2026-12-02T05:00"))
        // Today's APOD at about 06:15, not a day late; NASA at 18:00.
        assertEquals(listOf("01 06:15 APOD 12-01", "01 18:00 NASA 12-01"), phone.sets)
    }

    @Test
    fun every24HoursInLosAngelesOnApodsDay() {
        val sources = listOf(APOD, NASA)
        val posts = week("2026-10-19", "2026-10-22", sources)
        val monday = Record(NASA, pic(nasa("2026-10-19")), at(la, "2026-10-19T06:00"), notes(apod("2026-10-19"), nasa("2026-10-19")))
        val phone = turnsPhone(la, 24, posts, monday, sources)
        val pages = listOf(at(la, "2026-10-20T21:10"), at(la, "2026-10-20T22:00"))
        phone.run(at(la, "2026-10-20T05:00"), at(la, "2026-10-22T05:00"), pages)
        // APOD's day: on at 06:00 (the 21:05 post of the evening before), no change at 21:05;
        // NASA's next morning.
        assertEquals(listOf("20 06:00 APOD 10-20", "21 06:00 NASA 10-20"), phone.sets)
    }

    @Test
    fun aDstNightEvery3HoursChangesOnTheClocksHours() {
        // 25 Oct 2026, Zurich, every 3 h from 00:00, all three; nothing new: the change after
        // 00:00 is at the clock's 03:00 (four real hours), waiting checks hourly in between.
        val posts = week("2026-10-23", "2026-10-24")
        val record = Record(APOD, pic(apod("2026-10-24")), at(zurich, "2026-10-24T21:00"),
            notes(apod("2026-10-24"), nasa("2026-10-23"), earth("2026-10-23")))
        val phone = Phone(zurich, mapOf(HOME to Source.entries, LOCK to Source.entries), PictureTime(0, 3), posts, mapOf(HOME to record, LOCK to record))
        phone.run(at(zurich, "2026-10-25T01:50"), at(zurich, "2026-10-25T04:00"))
        assertEquals(emptyList(), phone.sets)
        // 02:00 CET (01:00 UTC) is the window's end, three real hours after 00:00 CEST.
        assertEquals(listOf("25 01:50", "25 02:15", "25 02:00", "25 02:15", "25 03:00", "25 03:15"), phone.runs)
    }
}
