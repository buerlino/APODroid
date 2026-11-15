package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Live (L3): the position by the clock, the decode size, still or engine, lost Live. */
class LiveTest {
    private val zurich = ZoneId.of("Europe/Zurich")
    private val utc = ZoneOffset.UTC

    private fun at(local: String, zone: ZoneId = zurich): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun p(local: String, time: PictureTime = PictureTime(), turns: Boolean = false, zone: ZoneId = zurich) =
        livePosition(at(local, zone), zone, time, turns)

    private fun assertP(expected: Double, actual: Float) = assertEquals(expected, actual.toDouble(), 1e-4)

    // The position

    @Test
    fun oneSourceCrossesFrom0600To2400ThenStandsStill() {
        assertP(0.0, p("2026-07-01T06:00"))
        assertP(1.0 / 3, p("2026-07-01T12:00"))
        assertP(2.0 / 3, p("2026-07-01T18:00"))
        assertP(1.0, p("2026-07-02T00:00"))
        assertP(1.0, p("2026-07-02T03:00"))
        assertP(1.0, p("2026-07-02T05:59"))
        assertP(0.0, p("2026-07-02T06:00"))
    }

    @Test
    fun eachTurnCrossesWithinItself() {
        val twelve = PictureTime(6, 12)
        assertP(0.0, p("2026-07-01T06:00", twelve, true))
        assertP(0.5, p("2026-07-01T12:00", twelve, true))
        assertP(0.0, p("2026-07-01T18:00", twelve, true))
        assertP(0.5, p("2026-07-02T00:00", twelve, true))
        assertP(719.0 / 720, p("2026-07-02T05:59", twelve, true))
        val six = PictureTime(6, 6)
        assertP(0.5, p("2026-07-01T09:00", six, true))
        assertP(0.0, p("2026-07-01T12:00", six, true))
        assertP(0.5, p("2026-07-01T15:00", six, true))
        assertP(0.5, p("2026-07-02T03:00", six, true))
        val three = PictureTime(6, 3)
        assertP(0.5, p("2026-07-01T07:30", three, true))
        assertP(0.0, p("2026-07-01T09:00", three, true))
        assertP(0.5, p("2026-07-02T01:30", three, true))
    }

    @Test
    fun everyTwentyFourHoursWithTwoSourcesCrossesIn18HoursThenStandsStill() {
        val day = PictureTime(6, 24)
        assertP(0.5, p("2026-07-01T15:00", day, true))
        assertP(1.0, p("2026-07-02T00:00", day, true))
        assertP(1.0, p("2026-07-02T05:00", day, true))
        assertP(0.0, p("2026-07-02T06:00", day, true))
    }

    @Test
    fun newPictureAtMidnightAnd2300() {
        val midnight = PictureTime(0)
        assertP(0.0, p("2026-07-01T00:00", midnight))
        assertP(0.5, p("2026-07-01T09:00", midnight))
        assertP(1.0, p("2026-07-01T18:00", midnight))
        assertP(1.0, p("2026-07-01T23:59", midnight))
        val late = PictureTime(23)
        assertP(0.0, p("2026-07-01T23:00", late))
        assertP(0.5, p("2026-07-02T08:00", late))
        assertP(1.0, p("2026-07-02T17:00", late))
        assertP(1.0, p("2026-07-02T22:59", late))
        assertP(1.0 / 18, p("2026-07-02T00:00", late))
    }

    @Test
    fun theDstNightsInZurichMoveTheRightEdgeByAnHourOnTheClock() {
        // Spring, 28 Mar 2027: 00:00 CET, then 02:00 → 03:00. 18 real hours end at 19:00 CEST.
        val midnight = PictureTime(0)
        assertP(17.0 / 18, p("2027-03-28T18:00", midnight))
        assertP(1.0, p("2027-03-28T19:00", midnight))
        assertP(1.0 / 18, p("2027-03-28T01:00", midnight))
        // 03:00 CEST is 2 real hours after 00:00 CET.
        assertP(2.0 / 18, p("2027-03-28T03:00", midnight))
        // Autumn, 25 Oct 2026: 00:00 CEST, then 03:00 → 02:00. 18 real hours end at 17:00 CET.
        assertP(17.0 / 18, p("2026-10-25T16:00", midnight))
        assertP(1.0, p("2026-10-25T17:00", midnight))
    }

    @Test
    fun theDstNightsInZurichWithThreeHourTurns() {
        val three = PictureTime(0, 3)
        // Spring: 00:00 CET to 03:00 CEST is a turn of 2 real hours; 01:00 CET is half of it.
        assertP(0.5, p("2027-03-28T01:00", three, true))
        assertP(0.0, p("2027-03-28T03:00", three, true))
        assertP(0.5, p("2027-03-28T04:30", three, true))
        // Autumn: 00:00 CEST to 03:00 CET is a turn of 4 real hours; the first 02:00 (CEST) is half of it.
        val firstTwo = Instant.parse("2026-10-25T00:00:00Z")
        assertP(0.5, livePosition(firstTwo, zurich, three, true))
        assertP(0.75, livePosition(firstTwo + Duration.ofHours(1), zurich, three, true))
        assertP(0.0, p("2026-10-25T03:00", three, true))
    }

    @Test
    fun theChangeTimesFollowTheZone() {
        val now = Instant.parse("2026-07-01T12:00:00Z")
        assertP(8.0 / 18, livePosition(now, zurich, PictureTime(), false))
        assertP(2.0 / 18, livePosition(now, ZoneId.of("America/New_York"), PictureTime(), false))
        assertP(16.0 / 18, livePosition(now, ZoneId.of("Australia/Sydney"), PictureTime(), false))
    }

    // The decode size

    private fun bytes(size: Pair<Int, Int>) = size.first.toLong() * size.second * 4

    private fun assertShape(width: Int, height: Int, size: Pair<Int, Int>) =
        assertTrue(abs(size.first.toDouble() / size.second - width.toDouble() / height) < 0.002, "$size for $width×$height")

    @Test
    fun aThreeToTwoIsDecodedAtTheScreensHeight() {
        assertEquals(3726 to 2484, liveDecodeSize(6000, 4000, 2484))
        assertEquals(3726 to 2484, liveDecodeSize(3726, 2484, 2484))
    }

    @Test
    fun aSmallPictureIsNotEnlarged() {
        assertEquals(1440 to 600, liveDecodeSize(1440, 600, 2484))
        assertEquals(1280 to 853, liveDecodeSize(1280, 853, 2400))
    }

    @Test
    fun theWidestRealOnesAreDecodedWithinTheBudget() {
        // The widest of private/pictures/, 6090×2085 (L1): not enlarged, 50.8 MB, so a little smaller.
        val widest = liveDecodeSize(6090, 2085, 2484)
        assertTrue(bytes(widest) <= LIVE_DECODE_BUDGET, "$widest")
        assertTrue(widest.first in 6000..6090, "$widest")
        assertShape(6090, 2085, widest)
        // The widest of 0.6.0's mock-up, about 2.14:1: 5316×2484 at the screen's height, 53 MB.
        val mockUp = liveDecodeSize(10632, 4968, 2484)
        assertTrue(bytes(mockUp) <= LIVE_DECODE_BUDGET, "$mockUp")
        assertTrue(mockUp.first in 5100 until 5316, "$mockUp")
        assertShape(10632, 4968, mockUp)
    }

    @Test
    fun aPanoramaIsDecodedWithinTheBudget() {
        val size = liveDecodeSize(12000, 3000, 2484)
        assertTrue(bytes(size) <= LIVE_DECODE_BUDGET, "$size")
        assertTrue(bytes(size) > LIVE_DECODE_BUDGET * 0.99, "$size")
        assertTrue(size.second < 2484)
        assertShape(12000, 3000, size)
    }

    @Test
    fun aTinyBudgetStillGivesAPicture() {
        assertEquals(1 to 1, liveDecodeSize(4000, 3000, 2484, maxBytes = 4))
    }

    // Still or engine

    private val x: Picture = Picture.Of(APOD, LocalDate.parse("2026-11-14"), "https://x/a.jpg")
    private val y: Picture = Picture.Of(NASA, LocalDate.parse("2026-11-13"), "https://x/n.jpg")
    private val both = listOf(x to setOf(HOME, LOCK))
    private val apart = listOf(x to setOf(HOME), y to setOf(LOCK))

    @Test
    fun sizeFillSetsEverythingStill() {
        assertEquals(LiveSets(both), stillOrLive(Size.FILL, homeLive = true, shared = true, columns = equal, homePicture = x, sets = both))
        assertEquals(LiveSets(apart), stillOrLive(Size.FILL, homeLive = true, shared = false, columns = equal, homePicture = x, sets = apart))
    }

    @Test
    fun liveOnHomeOnlySetsTheLockScreenStill() {
        assertEquals(
            LiveSets(listOf(x to setOf(LOCK)), x to setOf(HOME)),
            stillOrLive(Size.LIVE, homeLive = true, shared = false, columns = equal, homePicture = x, sets = both),
        )
    }

    @Test
    fun oneEngineOnBothTakesTheSamePictureWithNoStillSet() {
        assertEquals(LiveSets(emptyList(), x to setOf(HOME, LOCK)), stillOrLive(Size.LIVE, true, true, equal, x, both))
        // The lock screen alone due home's picture (home's is on already): noted live, no set.
        assertEquals(LiveSets(emptyList(), x to setOf(LOCK)), stillOrLive(Size.LIVE, true, true, equal, x, listOf(x to setOf(LOCK))))
    }

    @Test
    fun differentPicturesSetTheLockScreenStillBeforeHomesHandOver() {
        val sets = stillOrLive(Size.LIVE, true, true, equal, x, apart)
        assertEquals(LiveSets(listOf(y to setOf(LOCK)), x to setOf(HOME)), sets)
        // The lock screen alone due another picture than home's: still.
        assertEquals(LiveSets(listOf(y to setOf(LOCK))), stillOrLive(Size.LIVE, true, true, equal, x, listOf(y to setOf(LOCK))))
    }

    @Test
    fun liveWithHomeNotLiveSetsEverythingStill() {
        assertEquals(LiveSets(both), stillOrLive(Size.LIVE, homeLive = false, shared = false, columns = equal, homePicture = x, sets = both))
        // Also a lock screen the engine still shows (a stray after home's revert).
        assertEquals(LiveSets(both), stillOrLive(Size.LIVE, homeLive = false, shared = true, columns = equal, homePicture = x, sets = both))
        assertEquals(LiveSets(listOf(x to setOf(LOCK))), stillOrLive(Size.LIVE, false, true, equal, x, listOf(x to setOf(LOCK))))
    }

    @Test
    fun noHomeColumnSetsEverythingStill() {
        val lock = listOf(y to setOf(LOCK))
        assertEquals(LiveSets(lock), stillOrLive(Size.LIVE, homeLive = true, shared = true, columns = mapOf(HOME to emptyList(), LOCK to listOf(NASA)), homePicture = null, sets = lock))
    }

    @Test
    fun aScreenWithoutAColumnGetsNothing() {
        // APOD mode's "Where" Home: the plan has home's set only, the lock screen gets none.
        val home = listOf(x to setOf(HOME))
        assertEquals(LiveSets(emptyList(), x to setOf(HOME)), stillOrLive(Size.LIVE, true, true, equal, x, home))
        assertEquals(LiveSets(emptyList(), x to setOf(HOME)), stillOrLive(Size.LIVE, true, false, equal, x, home))
        assertEquals(LiveSets(emptyList()), stillOrLive(Size.LIVE, true, true, equal, x, emptyList()))
    }

    // Lost Live

    private val time = PictureTime(6, 6)
    private val t0 = Instant.parse("2026-11-16T06:00:00Z")
    private val d1 = LocalDate.parse("2026-11-16")

    private fun post(source: Source) =
        Post(source, d1, "$source", "https://x/$source.jpg", "https://p/$source", false, published = t0 - Duration.ofHours(2))

    private val stored = Stored(Source.entries.associateWith { post(it) }, Source.entries.toSet(), own = false, standIn = StandIn.SKIP)

    private fun due(columns: Map<Screen, List<Source>>, records: Map<Screen, Record>, now: Instant = t0 + Duration.ofHours(1)) =
        dueWallpapers(columns, records, stored, time, utc, now)

    /** The records after the first run's sets at 06:30, then a run at 07:00 with nothing due. */
    private fun settled(columns: Map<Screen, List<Source>>): Map<Screen, Record> {
        val records = due(columns, emptyMap(), t0 + Duration.ofMinutes(30)).screens.mapValues { it.value.record }
        assertTrue(due(columns, records).sets.isEmpty())
        return records
    }

    private fun bare(record: Record) = record.copy(shown = emptyMap())

    @Test
    fun aLockScreenNotSharedAnyMoreIsForgottenAlone() {
        for (columns in listOf(mapOf(HOME to listOf(APOD), LOCK to listOf(NASA)), mapOf(HOME to listOf(APOD), LOCK to listOf(APOD, NASA, EARTH)))) {
            val records = settled(columns)
            val lost = lostLive(homeNoted = true, lockNoted = true, homeLive = true, shared = false, sharedNote = true, lockColumn = true)
            assertEquals(LostLive(forget = setOf(LOCK)), lost)
            val forgotten = lost.forgetting(records)
            assertEquals(records[HOME], forgotten[HOME])
            val plan = due(columns, forgotten)
            assertEquals(listOf(records.getValue(LOCK).picture!! to setOf(LOCK)), plan.sets)
            val lock = plan.screens.getValue(LOCK).record
            assertEquals(records.getValue(LOCK).owner, lock.owner)
            assertEquals(records.getValue(LOCK).stamp, lock.stamp)
            // Still: the engine doesn't cover the lock screen.
            assertEquals(LiveSets(plan.sets), stillOrLive(Size.LIVE, true, false, columns, records.getValue(HOME).picture, plan.sets))
        }
    }

    @Test
    fun homeLostLiveForgetsBothAndSetsBothStill() {
        val equal = mapOf(HOME to listOf(APOD, NASA), LOCK to listOf(APOD, NASA))
        val different = mapOf(HOME to listOf(APOD), LOCK to listOf(NASA, EARTH))
        for (columns in listOf(equal, different, mapOf(HOME to listOf(APOD), LOCK to listOf(APOD)))) {
            val records = settled(columns)
            val lost = lostLive(homeNoted = true, lockNoted = columns == equal, homeLive = false, shared = false, sharedNote = true, lockColumn = true)
            assertEquals(LostLive(forget = setOf(HOME, LOCK), dropShared = true), lost)
            val plan = lost.onPlan(due(columns, lost.forgetting(records)), records)
            assertEquals(setOf(HOME, LOCK), plan.sets.flatMap { it.second }.toSet())
            for (screen in Screen.entries) {
                assertEquals(bare(records.getValue(screen)), bare(plan.screens.getValue(screen).record))
            }
            if (columns != different) assertEquals(1, plan.sets.size)
            val split = stillOrLive(Size.LIVE, homeLive = false, shared = true, columns = columns, homePicture = records.getValue(HOME).picture, sets = plan.sets)
            assertEquals(LiveSets(plan.sets), split)
            assertNull(split.engine)
        }
    }

    @Test
    fun aLockScreenWithoutAColumnTheEngineCoveredTakesHomesPicture() {
        val columns = mapOf(HOME to listOf(APOD), LOCK to emptyList())
        val records = settled(columns)
        assertEquals(setOf(HOME), records.keys)
        val lost = lostLive(homeNoted = true, lockNoted = true, homeLive = false, shared = false, sharedNote = true, lockColumn = false)
        assertEquals(LostLive(forget = setOf(HOME), lockTakesHome = true, dropShared = true), lost)
        val plan = lost.onPlan(due(columns, lost.forgetting(records)), records)
        val home = records.getValue(HOME).picture!!
        assertEquals(listOf(home to setOf(HOME, LOCK)), plan.sets)
        assertEquals(LiveSets(plan.sets), stillOrLive(Size.LIVE, false, true, columns, home, plan.sets))
    }

    @Test
    fun aLockScreenWithoutAColumnNotCoveredIsLeftAlone() {
        val columns = mapOf(HOME to listOf(APOD), LOCK to emptyList())
        val records = settled(columns)
        val lost = lostLive(homeNoted = true, lockNoted = false, homeLive = false, shared = false, sharedNote = false, lockColumn = false)
        assertEquals(LostLive(forget = setOf(HOME), dropShared = true), lost)
        val plan = lost.onPlan(due(columns, lost.forgetting(records)), records)
        assertEquals(listOf(records.getValue(HOME).picture!! to setOf(HOME)), plan.sets)
        assertNull(plan.screens[LOCK])
    }

    @Test
    fun homeLostWithNothingDueLeavesTheLockScreenAlone() {
        // Home forgotten with nothing to show (no record either): no picture for the lock screen to take.
        val lost = LostLive(forget = setOf(HOME), lockTakesHome = true, dropShared = true)
        val plan = Plan(emptyMap())
        assertEquals(plan, lost.onPlan(plan, emptyMap()))
    }

    @Test
    fun nothingLostWhileLiveHolds() {
        assertEquals(LostLive(), lostLive(homeNoted = true, lockNoted = true, homeLive = true, shared = true, sharedNote = true, lockColumn = true))
        assertEquals(LostLive(), lostLive(homeNoted = true, lockNoted = false, homeLive = true, shared = false, sharedNote = false, lockColumn = true))
        assertEquals(LostLive(), lostLive(homeNoted = false, lockNoted = false, homeLive = false, shared = false, sharedNote = true, lockColumn = true))
        val records = mapOf(HOME to Record(APOD, x, t0))
        assertEquals(records, LostLive().forgetting(records))
        assertNotNull(records[HOME]?.picture)
    }

    // When Set opens the picker (L6)

    private val equal = mapOf(HOME to listOf(APOD), LOCK to listOf(APOD))
    private val homeOnly = mapOf(HOME to listOf(APOD), LOCK to emptyList())
    private val lockOnly = mapOf(HOME to emptyList(), LOCK to listOf(APOD))
    private val split = mapOf(HOME to listOf(APOD), LOCK to listOf(EARTH))

    private fun picker(before: Map<Screen, List<Source>>, columns: Map<Screen, List<Source>>, homeLive: Boolean, shared: Boolean = false, size: Size = Size.LIVE, lockShares: Boolean = true) =
        opensPicker(size, before, columns, homeLive, shared, lockShares)

    @Test
    fun fillNeverOpensThePicker() {
        assertFalse(picker(equal, equal, homeLive = false, size = Size.FILL))
        assertFalse(picker(split, equal, homeLive = true, size = Size.FILL))
    }

    @Test
    fun liveOpensThePickerWhileHomeIsntLive() {
        assertTrue(picker(equal, equal, homeLive = false))
        assertTrue(picker(homeOnly, homeOnly, homeLive = false))
        // Decision 10: home APOD, lock Earth, Live on none: once, for home.
        assertTrue(picker(split, split, homeLive = false))
        // Below 14 too: Live is home's there.
        assertTrue(picker(equal, equal, homeLive = false, lockShares = false))
    }

    @Test
    fun theLockScreenAloneNeverOpensThePicker() {
        assertFalse(picker(lockOnly, lockOnly, homeLive = false))
        assertFalse(picker(equal, lockOnly, homeLive = false))
    }

    @Test
    fun homeLiveOpensThePickerOnlyWhenTheSetMakesTheColumnsEqual() {
        // Decision 10's second Set: home live, the lock screen Fill of its own column: no picker.
        assertFalse(picker(split, split, homeLive = true))
        // The columns made equal after the split, the lock screen not covered: once.
        assertTrue(picker(split, equal, homeLive = true))
        // Already covered: no picker.
        assertFalse(picker(split, equal, homeLive = true, shared = true))
        // The next Set with the same columns: they were equal before it.
        assertFalse(picker(equal, equal, homeLive = true))
        // Equal from the start (APOD mode's "Where" Both), Live on home only: the lock screen Fill.
        assertFalse(picker(equal, equal, homeLive = true, shared = false))
        // A column made to differ, or home alone: never.
        assertFalse(picker(equal, split, homeLive = true))
        assertFalse(picker(split, homeOnly, homeLive = true))
        // Below 14 the engine never covers the lock screen.
        assertFalse(picker(split, equal, homeLive = true, lockShares = false))
    }

    @Test
    fun equalColumnsAreTheSameSourcesOnBothScreens() {
        assertTrue(equalColumns(equal))
        assertTrue(equalColumns(mapOf(HOME to listOf(APOD, NASA), LOCK to listOf(NASA, APOD))))
        assertFalse(equalColumns(split))
        assertFalse(equalColumns(homeOnly))
        assertFalse(equalColumns(mapOf(HOME to emptyList(), LOCK to emptyList())))
    }

    // The engine on a screen whose record isn't noted live (L6, SIM)

    @Test
    fun theEngineOnUnnotedScreensForgetsThemWithSizeLiveOnly() {
        val columns = mapOf(HOME to listOf(APOD), LOCK to listOf(EARTH))
        // Just picked: home live, nothing noted; shared, so the lock screen too.
        assertEquals(setOf(HOME, LOCK), unnotedLive(Size.LIVE, homeLive = true, shared = true, homeNoted = false, lockNoted = false, columns = columns))
        assertEquals(setOf(HOME), unnotedLive(Size.LIVE, homeLive = true, shared = false, homeNoted = false, lockNoted = false, columns = columns))
        // Noted: nothing.
        assertEquals(emptySet(), unnotedLive(Size.LIVE, homeLive = true, shared = true, homeNoted = true, lockNoted = true, columns = columns))
        // Size Fill, or home not live: nothing (a still set replaces the engine when one is due).
        assertEquals(emptySet(), unnotedLive(Size.FILL, homeLive = true, shared = true, homeNoted = false, lockNoted = false, columns = columns))
        assertEquals(emptySet(), unnotedLive(Size.LIVE, homeLive = false, shared = false, homeNoted = false, lockNoted = false, columns = columns))
        // A screen without a column is left alone.
        assertEquals(setOf(HOME), unnotedLive(Size.LIVE, homeLive = true, shared = true, homeNoted = false, lockNoted = false, columns = mapOf(HOME to listOf(APOD), LOCK to emptyList())))
    }

    @Test
    fun homesHandOverOverASharedLockScreenNotDueItMakesTheLockScreenFollow() {
        // Home's new picture to the engine while it covers a lock screen of another column with nothing due there.
        val handOver = LiveSets(emptyList(), x to setOf(HOME))
        assertTrue(lockFollowsHome(handOver, shared = true, columns = split))
        assertFalse(lockFollowsHome(handOver, shared = false, columns = split))
        assertFalse(lockFollowsHome(handOver, shared = true, columns = homeOnly))
        // The lock screen has its own set this run, or goes with home: no.
        assertFalse(lockFollowsHome(LiveSets(listOf(y to setOf(LOCK)), x to setOf(HOME)), shared = true, columns = split))
        assertFalse(lockFollowsHome(LiveSets(emptyList(), x to setOf(HOME, LOCK)), shared = true, columns = equal))
        // Nothing for the engine and the lock screen's own still set: no.
        assertFalse(lockFollowsHome(LiveSets(listOf(y to setOf(LOCK))), shared = true, columns = split))
        // Nothing at all this run with equal columns: the shared engine stays.
        assertFalse(lockFollowsHome(LiveSets(emptyList()), shared = true, columns = equal))
    }

    // B28, the tablet's real night of 2026-11-15: home APOD + NASA every 6 h, the lock screen APOD
    // alone, the picker's "Home screen" made one shared engine; both showed APOD, so the lock screen
    // stayed on the engine and panned by home's 6 h turn (p 0.28 at 07:41) instead of its own 18 h.
    private val night = mapOf(HOME to listOf(APOD, NASA), LOCK to listOf(APOD))

    @Test
    fun aSharedLockScreenWithAnotherColumnGetsItsOwnStillForHomesPicture() {
        // Both due home's picture: home to the engine, the lock screen still (FLAG_LOCK).
        assertEquals(LiveSets(listOf(x to setOf(LOCK)), x to setOf(HOME)), stillOrLive(Size.LIVE, true, true, night, x, both))
        // The lock screen alone due home's picture: still, not noted live.
        assertEquals(LiveSets(listOf(x to setOf(LOCK))), stillOrLive(Size.LIVE, true, true, night, x, listOf(x to setOf(LOCK))))
    }

    @Test
    fun aSharedLockScreenWithTheSameColumnStaysOnTheEngine() {
        val same = mapOf(HOME to listOf(APOD, NASA), LOCK to listOf(NASA, APOD))
        assertEquals(LiveSets(emptyList(), x to setOf(HOME, LOCK)), stillOrLive(Size.LIVE, true, true, same, x, both))
        assertFalse(lockFollowsHome(LiveSets(emptyList()), shared = true, columns = same))
    }

    @Test
    fun aLockScreenNotSharedIsUnchangedByItsColumn() {
        for (columns in listOf(night, equal)) {
            assertEquals(LiveSets(listOf(x to setOf(LOCK)), x to setOf(HOME)), stillOrLive(Size.LIVE, true, false, columns, x, both))
            assertFalse(lockFollowsHome(LiveSets(emptyList(), x to setOf(HOME)), shared = false, columns = columns))
            assertFalse(lockFollowsHome(LiveSets(emptyList()), shared = false, columns = columns))
        }
    }

    @Test
    fun aSharedLockScreenWithAnotherColumnAndNothingDueLeavesTheEngine() {
        // The night's run after the picker's: nothing due on either screen, the engine on both.
        val records = settled(night)
        val plan = due(night, records)
        assertTrue(plan.sets.isEmpty())
        val split = stillOrLive(Size.LIVE, true, true, night, records.getValue(HOME).picture, plan.sets)
        assertTrue(lockFollowsHome(split, shared = true, columns = night))
        // Forgotten and planned again: its own picture, still, before nothing for the engine.
        val again = due(night, records + (LOCK to records.getValue(LOCK).forget()))
        val lockPicture = records.getValue(LOCK).picture!!
        assertEquals(listOf(lockPicture to setOf(LOCK)), again.sets)
        val splitAgain = stillOrLive(Size.LIVE, true, true, night, records.getValue(HOME).picture, again.sets)
        assertEquals(LiveSets(listOf(lockPicture to setOf(LOCK))), splitAgain)
        assertFalse(lockFollowsHome(splitAgain, shared = true, columns = night))
    }

    // L6f1: a failed lock still set while the engine covers the lock screen holds home's hand-over.
    @Test
    fun aFailedLockSetHoldsTheHandOverWhileShared() {
        assertTrue(handOverWaits(shared = true, failedStill = listOf(setOf(LOCK))))
        assertTrue(handOverWaits(shared = true, failedStill = listOf(setOf(HOME, LOCK))))
        // Not shared: the engine is home's alone, the hand-over runs.
        assertFalse(handOverWaits(shared = false, failedStill = listOf(setOf(LOCK))))
        // No lock step failed (none, or home's alone): the hand-over runs.
        assertFalse(handOverWaits(shared = true, failedStill = emptyList()))
        assertFalse(handOverWaits(shared = true, failedStill = listOf(setOf(HOME))))
    }

    // L6 fix round 1: a maker's picker that isn't exported throws SecurityException, which crashed Set.
    @Test
    fun aPickerThatRefusesFallsToTheNextAndNoneLeavesNull() {
        val failed = mutableListOf<String>()
        val tried = mutableListOf<String>()
        val started = firstStarted(listOf("direct", "chooser"), { o, _ -> failed += o }) {
            tried += it
            if (it == "direct") throw SecurityException("Permission Denial: starting Intent")
        }
        assertEquals("chooser", started)
        assertEquals(listOf("direct", "chooser"), tried)
        assertEquals(listOf("direct"), failed)

        val none = firstStarted(listOf("direct", "chooser"), { o, _ -> failed += o }) {
            if (it == "direct") throw SecurityException("denied") else throw IllegalStateException("no picker")
        }
        assertNull(none)
        assertEquals(listOf("direct", "direct", "chooser"), failed)

        assertEquals("direct", firstStarted(listOf("direct", "chooser"), { _, _ -> }) {})
    }

    // D, the screen-on check (L8).

    private val wake = Instant.parse("2026-11-14T07:00:00Z")
    private fun owed(daily: Boolean = true, nextAlarm: Instant = wake.plusSeconds(3600), lastRun: Instant? = wake.minus(Duration.ofHours(2)), stray: Boolean = false, notedLive: Boolean = false) =
        screenOnRunOwed(wake, daily, nextAlarm, lastRun, stray, notedLive)

    @Test
    fun screenOnStartsTheRunWhenACheckIsOwed() {
        assertTrue(owed(nextAlarm = wake.minusSeconds(60)))
        assertTrue(owed(nextAlarm = wake), "due this very moment")
        assertTrue(owed(nextAlarm = wake.minusSeconds(60), lastRun = null))
    }

    @Test
    fun screenOnStartsNothingWhenNothingIsOwed() {
        assertFalse(owed())
        assertFalse(owed(stray = true), "a stray with nothing noted live: the run removed the notes")
        assertFalse(owed(notedLive = true), "noted live, but not a stray")
    }

    @Test
    fun screenOnStartsNothingWithTheDailySwitchOff() {
        assertFalse(owed(daily = false, nextAlarm = wake.minusSeconds(60)))
        assertFalse(owed(daily = false, stray = true, notedLive = true))
    }

    @Test
    fun screenOnStartsNothingWithin15MinOfARun() {
        assertFalse(owed(nextAlarm = wake.minusSeconds(60), lastRun = wake.minus(Duration.ofMinutes(14))))
        assertFalse(owed(stray = true, notedLive = true, lastRun = wake.minusSeconds(1)))
        assertTrue(owed(nextAlarm = wake.minusSeconds(60), lastRun = wake.minus(Duration.ofMinutes(15))))
        assertTrue(owed(nextAlarm = wake.minusSeconds(60), lastRun = wake.plus(Duration.ofHours(3))), "the clock set back")
    }

    @Test
    fun aStrayWithARecordNotedLiveIsOwedARun() {
        assertTrue(owed(stray = true, notedLive = true))
        assertTrue(owed(stray = true, notedLive = true, lastRun = null))
    }
    @Test
    fun anOwedCheckFromTheRealNextAlarmIsOwed() {
        // nextAlarm hands back its own now for an owed check (fix round 1): the clock read once.
        val passed = nextAlarm(wake, zurich, 0, PictureTime(), false, Duration.ZERO, Instant.parse("2026-11-13T06:00:00Z"))
        assertEquals(wake, passed, "the 06:00 check passed since the last run")
        assertTrue(owed(nextAlarm = passed, lastRun = Instant.parse("2026-11-13T06:00:00Z")))

        val retryRun = wake.minus(Duration.ofMinutes(20))
        val retry = nextAlarm(wake, zurich, 1, PictureTime(), false, Duration.ZERO, retryRun)
        assertEquals(wake, retry, "a failed run's retry is due")
        assertTrue(owed(nextAlarm = retry, lastRun = retryRun))

        // A clock read twice: nextAlarm's own now a millisecond later wasn't owed.
        assertFalse(owed(nextAlarm = nextAlarm(wake.plusMillis(1), zurich, 0, PictureTime(), false, Duration.ZERO, Instant.parse("2026-11-13T06:00:00Z"))))
    }

    // L8f1: a process death with Live on (regression 12): the rebind's "home isn't live" for a moment isn't a loss.

    @Test
    fun aYoungProcessWithAnEngineIsSettling() {
        assertTrue(liveSettling(engine = true, processAge = Duration.ofSeconds(2)))
        assertTrue(liveSettling(engine = true, processAge = Duration.ZERO))
        assertTrue(liveSettling(engine = true, processAge = Duration.ofMillis(14_999)))
        assertFalse(liveSettling(engine = true, processAge = liveReconnect), "the grace ends at 15 s")
        assertFalse(liveSettling(engine = true, processAge = Duration.ofMinutes(5)))
        assertFalse(liveSettling(engine = false, processAge = Duration.ofSeconds(2)), "no engine (the page after a force stop): no grace")
        assertFalse(liveSettling(engine = true, processAge = Duration.ofSeconds(-1)))
    }

    @Test
    fun noStrayWhileSettling() {
        assertFalse(liveStray(homeLive = false, settling = true), "a rebind isn't a revert")
        assertTrue(liveStray(homeLive = false, settling = false), "D's stray after the grace")
        assertFalse(liveStray(homeLive = true, settling = false))
        assertFalse(liveStray(homeLive = true, settling = true))
    }

    @Test
    fun aKilledEngineOwesNoRunWhileSettling() {
        // The regression's sequence: the new engine at 2 s, lastRunAt 2 h old, nothing else owed.
        val stray = liveStray(homeLive = false, settling = liveSettling(true, Duration.ofSeconds(2)))
        assertFalse(owed(stray = stray, notedLive = true))
    }

    @Test
    fun aRunInTheGraceHoldsInsteadOfFill() {
        // Live on both screens (one engine, shared), home looks not live for a moment.
        val lost = lostLive(homeNoted = true, lockNoted = true, homeLive = false, shared = false, sharedNote = true, lockColumn = true)
        assertEquals(setOf(HOME, LOCK), lost.forget, "the old code's Fill on HOME+LOCK")
        assertTrue(liveHolds(settling = liveSettling(true, Duration.ofSeconds(2)), lost = lost))
        // Live on home alone, the lock screen still Fill (the split): the same.
        val split = lostLive(homeNoted = true, lockNoted = false, homeLive = false, shared = false, sharedNote = false, lockColumn = true)
        assertTrue(liveHolds(settling = true, lost = split))
        // Nothing lost: nothing held, the run goes on.
        assertFalse(liveHolds(settling = true, lost = lostLive(true, true, homeLive = true, shared = true, sharedNote = true, lockColumn = true)))
    }

    @Test
    fun decision12sRevertAfterTheGraceIsFillOnBoth() {
        // Android reverted home after two deaths in 10 s; the engine on the lock screen alone, 20 s on.
        val settling = liveSettling(engine = true, processAge = Duration.ofSeconds(20))
        assertTrue(liveStray(homeLive = false, settling = settling))
        assertTrue(owed(stray = true, notedLive = true))
        val lost = lostLive(homeNoted = true, lockNoted = true, homeLive = false, shared = false, sharedNote = true, lockColumn = true)
        assertFalse(liveHolds(settling, lost))
        assertEquals(setOf(HOME, LOCK), lost.forget)
        val sets = listOf(y to setOf(HOME, LOCK))
        assertEquals(LiveSets(sets), stillOrLive(Size.LIVE, homeLive = false, shared = false, columns = equal, homePicture = y, sets = sets), "both still")
    }

    // L8f1 fix round 1: the tablet's split state after one kill: getWallpaperInfo doesn't name APODroid's for minutes, home's id unchanged.

    @Test
    fun homesUnchangedIdKeepsItLive() {
        assertTrue(homeLive(named = true, id = 1499, noted = 1499))
        assertTrue(homeLive(named = true, id = 1499, noted = null), "Android says so: no id needed")
        assertTrue(homeLive(named = false, id = 1499, noted = 1499), "a rebind keeps the id")
        assertFalse(homeLive(named = false, id = 1501, noted = 1499), "a revert, a still set or another pick: a new id")
        assertFalse(homeLive(named = false, id = 1499, noted = null), "never noted live")
        assertFalse(homeLive(named = false, id = -1, noted = -1), "a failed read never matches")
        assertFalse(homeLive(named = false, id = 0, noted = 0))
    }

    @Test
    fun aKillInTheSplitStateAfterTheGraceIsNoLoss() {
        // Rosie's 01:38 run: Live on home, the lock screen still (split), one kill, the run 30 s on.
        val home = homeLive(named = false, id = 1499, noted = 1499)
        val settling = liveSettling(engine = true, processAge = Duration.ofSeconds(30))
        assertFalse(liveStray(home, settling), "no stray at the grace's end")
        assertFalse(owed(stray = liveStray(home, settling), notedLive = true))
        val lost = lostLive(homeNoted = true, lockNoted = false, homeLive = home, shared = false, sharedNote = false, lockColumn = true)
        assertEquals(LostLive(), lost, "the old reading's Fill on HOME+LOCK")
        // The old code read home by getWallpaperInfo alone: Fill on both.
        assertEquals(setOf(HOME, LOCK), lostLive(true, false, homeLive = false, shared = false, sharedNote = false, lockColumn = true).forget)
    }

    @Test
    fun decision12sRevertGivesANewIdAndFill() {
        val home = homeLive(named = false, id = 1501, noted = 1499)
        assertTrue(liveStray(home, settling = false))
        assertEquals(setOf(HOME, LOCK), lostLive(true, true, homeLive = home, shared = false, sharedNote = true, lockColumn = true).forget)
    }
}
