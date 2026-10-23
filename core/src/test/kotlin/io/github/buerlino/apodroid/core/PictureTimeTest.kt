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
        assertEquals("16 06:00", next("2026-07-15T08:15"))
        // A run in between (a page open) keeps the grid.
        assertEquals("15 07:15", next("2026-07-15T06:40"))
        // With turns the next change comes first: every 3 h, the window ends with it.
        assertEquals("15 09:00", next("2026-07-15T08:15", PictureTime(6, 3), turns = true))
        assertEquals("15 09:15", next("2026-07-15T09:00", PictureTime(6, 3), turns = true))
        assertEquals("15 12:00", next("2026-07-15T08:15", PictureTime(6, 6), turns = true))
    }

    @Test
    fun aWaitingScreenIsCheckedHourly() {
        val time = PictureTime(6, 6)
        assertEquals("15 09:15", next("2026-07-15T08:15", time, turns = true, waiting = true))
        assertEquals("15 11:15", next("2026-07-15T10:15", time, turns = true, waiting = true))
        assertEquals("15 12:00", next("2026-07-15T11:15", time, turns = true, waiting = true))
        // Without a waiting screen: the next change time.
        assertEquals("15 12:00", next("2026-07-15T08:15", time, turns = true))
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

    private fun post(source: Source, date: LocalDate, video: Boolean = false) =
        Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video)

    private fun apod(day: String, video: Boolean = false): Out {
        val date = LocalDate.parse(day)
        return Out(date.atTime(0, 5).atZone(newYork).toInstant(), post(APOD, date, video))
    }

    private fun earth(day: String): Out {
        val date = LocalDate.parse(day)
        return Out(date.atTime(0, 0, 30).atZone(newYork).toInstant(), post(EARTH, date))
    }

    private fun nasa(day: String): Out {
        val date = LocalDate.parse(day)
        return Out(date.atTime(16, 30).atZone(utc).toInstant(), post(NASA, date))
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
    ) {
        val sets = mutableListOf<String>()
        val runs = mutableListOf<String>()
        private val turns = columns.values.any { it.size >= 2 }

        fun stored(now: Instant): Stored {
            val newest = posts.filter { it.at <= now }.groupBy { it.post.source }.mapValues { (_, outs) -> outs.maxBy { it.at }.post }
            return Stored(newest, newest.filterValues { !it.isVideo }.keys, true, standIn)
        }

        fun check(now: Instant, failSets: Boolean = false) {
            val plan = dueWallpapers(columns, records, stored(now), time, zone, now)
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
            while (now < until) {
                while (opens.isNotEmpty() && opens.first() < now) check(opens.removeAt(0))
                if (set != null && set < now) set(set).also { set = null }
                runs += local(zone, now)
                check(now)
                now = nextRun(now, zone, 0, time, turns, Duration.ZERO, waiting(columns, records, time, zone, now))
            }
        }
    }

    private val both = apodColumns(setOf(HOME, LOCK))

    /** APOD mode, yesterday's APOD on, set at yesterday's [change]. */
    private fun apodPhone(zone: ZoneId, onDay: String, change: String, posts: List<Out>, pictureAt: Int = 6): Phone {
        val on = apod(onDay)
        val record = Record(APOD, pic(on), at(zone, change), notes(on))
        return Phone(zone, both, PictureTime(pictureAt), posts, mapOf(HOME to record, LOCK to record))
    }

    @Test
    fun zurichInSummerGetsTodaysApodAtThe0615Check() {
        val phone = apodPhone(zurich, "2026-07-14", "2026-07-14T06:00", listOf(apod("2026-07-14"), apod("2026-07-15"), apod("2026-07-16")))
        phone.run(at(zurich, "2026-07-15T05:00"), at(zurich, "2026-07-16T05:00"))
        assertEquals(listOf("15 06:15 APOD 07-15"), phone.sets)
        assertEquals(listOf("15 05:00", "15 06:00", "15 06:15", "15 07:15", "15 08:15"), phone.runs)
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
        assertEquals(listOf("25 01:50", "25 02:15", "25 02:15", "25 03:00", "25 03:15"), phone.runs)
    }
}
