package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreensTest {
    private val t0 = Instant.parse("2026-10-19T06:00:00Z")
    private val h1 = Duration.ofHours(1)
    private val h3 = Duration.ofHours(3)
    private val d1 = LocalDate.parse("2026-10-19")
    private val d2 = LocalDate.parse("2026-10-20")
    private val d3 = LocalDate.parse("2026-10-21")
    private val all = Source.entries

    private fun post(source: Source, date: LocalDate = d1, video: Boolean = false) =
        Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video)

    private val posts = all.associateWith { post(it) }

    private fun stored(
        posts: Map<Source, Post> = this.posts,
        standIn: StandIn = StandIn.SKIP,
        own: Boolean = true,
        files: Set<Source> = posts.keys,
    ) = Stored(posts, files, own, standIn)

    private fun pic(source: Source, date: LocalDate = d1) = post(source, date).picture()

    private fun cols(home: List<Source>, lock: List<Source> = home) = mapOf(HOME to home, LOCK to lock)

    private fun due(
        columns: Map<Screen, List<Source>>,
        records: Map<Screen, Record>,
        stored: Stored = stored(),
        clock: Instant? = t0,
        now: Instant = t0 + h1,
    ) = dueWallpapers(columns, clock, h3, records, stored, now)

    /** The records after [plan], all its sets done. */
    private fun apply(records: Map<Screen, Record>, plan: Plan) = records + plan.screens.mapValues { it.value.record }

    // Grouping

    @Test
    fun equalColumnsAreOneSet() {
        val plan = due(cols(listOf(APOD)), emptyMap())
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), plan.sets)
        val turns = due(cols(all), emptyMap())
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), turns.sets)
    }

    @Test
    fun differentColumnsAreTwoSets() {
        val plan = due(cols(listOf(APOD), listOf(NASA)), emptyMap())
        assertEquals(listOf(pic(APOD) to setOf(HOME), pic(NASA) to setOf(LOCK)), plan.sets)
    }

    @Test
    fun anEmptyColumnIsLeftAlone() {
        val records = mapOf(LOCK to Record(NASA, pic(NASA), t0))
        val plan = due(cols(listOf(APOD), emptyList()), records)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        assertNull(plan.screens[LOCK])
    }

    @Test
    fun theColumnsOfApodModeComeFromWhere() {
        assertEquals(cols(listOf(APOD)), apodColumns(setOf(HOME, LOCK)))
        assertEquals(cols(emptyList(), listOf(APOD)), apodColumns(setOf(LOCK)))
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), due(apodColumns(setOf(HOME, LOCK)), emptyMap()).sets)
        assertEquals(listOf(pic(APOD) to setOf(LOCK)), due(apodColumns(setOf(LOCK)), emptyMap()).sets)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(apodColumns(setOf(HOME)), emptyMap()).sets)
    }

    @Test
    fun equalColumnsWithAHoldOnOneScreenAreTwoSets() {
        val two = listOf(APOD, NASA)
        val records = mapOf(
            HOME to Record(APOD, pic(EARTH), t0, Hold(EARTH, pic(EARTH))),
            LOCK to Record(APOD, pic(APOD), t0),
        )
        // APOD and Earth post: Lock gets APOD's, the held Home Earth's.
        val newer = stored(posts + mapOf(APOD to post(APOD, d2), EARTH to post(EARTH, d2)))
        val plan = due(cols(two), records, newer)
        assertEquals(listOf(pic(EARTH, d2) to setOf(HOME), pic(APOD, d2) to setOf(LOCK)), plan.sets)
        assertEquals(Hold(EARTH, pic(EARTH, d2)), plan.screens.getValue(HOME).record.hold)
    }

    @Test
    fun aPictureHeldOnHomeIsntOverwrittenOnLock() {
        val records = mapOf(
            HOME to Record(NASA, pic(EARTH), t0, Hold(EARTH, pic(EARTH))),
            LOCK to Record(NASA, pic(NASA), t0),
        )
        val plan = due(cols(listOf(APOD, NASA)), records, stored(posts + (NASA to post(NASA, d2))))
        assertEquals(listOf(pic(NASA, d2) to setOf(LOCK)), plan.sets)
        assertNull(plan.screens[HOME])
    }

    @Test
    fun nothingNewIsNothingToDo() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        assertEquals(emptyMap(), due(cols(listOf(APOD, NASA)), records).screens)
    }

    @Test
    fun aRecordWithoutAUrlMatchesByDate() {
        // 0.5.0's prefs from 0.4.0: the date alone.
        val records = mapOf(HOME to Record(APOD, Picture.Of(APOD, d1, null), t0))
        val plan = due(apodColumns(setOf(HOME)), records)
        assertEquals(emptyList(), plan.sets)
        assertEquals(pic(APOD), plan.screens.getValue(HOME).record.picture)
    }

    // The clock and the stamp

    @Test
    fun twoScreensWhoseTurnEndsChangeTogetherAndTheClockRestartsOnce() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val now = t0 + h3
        val plan = due(cols(all), records, now = now)
        assertTrue(plan.restarted)
        assertEquals(now, plan.clock)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), plan.sets)
        for (screen in Screen.entries) assertEquals(Record(NASA, pic(NASA), now), plan.screens.getValue(screen).record)
    }

    @Test
    fun theClockRestartsOnlyWithTurns() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val plan = due(apodColumns(setOf(HOME)), records, now = t0 + Duration.ofDays(2))
        assertFalse(plan.restarted)
        assertEquals(t0, plan.clock)
        // A clock in the future (set back) counts as over.
        val back = due(cols(all), mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0)), now = t0 - h1)
        assertTrue(back.restarted)
    }

    @Test
    fun aFailedSetIsRetriedOnThatScreenOnly() {
        // The turn ended at t1; Home's set went through, Lock's failed.
        val t1 = t0 + h3
        val records = mapOf(HOME to Record(NASA, pic(NASA), t1), LOCK to Record(APOD, pic(APOD), t0))
        val plan = due(cols(all), records, clock = t1, now = t1 + Duration.ofMinutes(1))
        assertFalse(plan.restarted)
        assertEquals(listOf(pic(NASA) to setOf(LOCK)), plan.sets)
        assertEquals(Record(NASA, pic(NASA), t1), plan.screens.getValue(LOCK).record)
        assertNull(plan.screens[HOME])
    }

    @Test
    fun aTurnEndingWithNoOtherPictureMovesTheStampOnly() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val now = t0 + h3
        val plan = due(cols(listOf(APOD, NASA), emptyList()), records, stored(files = setOf(APOD)), now = now)
        assertEquals(emptyList(), plan.sets)
        assertEquals(Record(APOD, pic(APOD), now), plan.screens.getValue(HOME).record)
        // None with a picture at all: the same.
        val none = due(cols(listOf(APOD, NASA), emptyList()), records, stored(files = emptySet()), now = now)
        assertEquals(emptyList(), none.sets)
        assertEquals(Record(APOD, pic(APOD), now), none.screens.getValue(HOME).record)
    }

    @Test
    fun anEarlyChangeKeepsTheStampAndEndsWithTheTurn() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        // An APOD video with "Skip it".
        val video = stored(posts + (APOD to post(APOD, d2, video = true)))
        val early = due(cols(all, emptyList()), records, video)
        assertFalse(early.restarted)
        assertEquals(Record(NASA, pic(NASA), t0), early.screens.getValue(HOME).record)
        val over = due(cols(all, emptyList()), apply(records, early), video, now = t0 + h3)
        assertEquals(Record(EARTH, pic(EARTH), t0 + h3), over.screens.getValue(HOME).record)
        // An untick.
        val untick = due(cols(listOf(NASA, EARTH), emptyList()), records)
        assertEquals(Record(NASA, pic(NASA), t0), untick.screens.getValue(HOME).record)
        // A source with no picture yet.
        val missing = due(cols(all, emptyList()), records, stored(files = setOf(NASA, EARTH)))
        assertEquals(Record(NASA, pic(NASA), t0), missing.screens.getValue(HOME).record)
    }

    @Test
    fun aSecondTickRestartsTheClockOnlyWhenNoColumnTookTurns() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val now = t0 + h1
        val first = columnsChanged(cols(listOf(APOD)), cols(listOf(APOD, NASA), listOf(APOD)), t0, records, stored(), now)
        assertTrue(first.restarted)
        assertEquals(now, first.clock)
        for (screen in Screen.entries) assertEquals(now, first.screens.getValue(screen).record.stamp)
        // The tick doesn't end the current picture at once.
        assertEquals(emptyList(), due(cols(listOf(APOD, NASA), listOf(APOD)), apply(records, first), clock = now, now = now + h1).sets)

        val second = columnsChanged(cols(listOf(APOD, NASA), listOf(APOD)), cols(listOf(APOD, NASA)), t0, records, stored(), now)
        assertFalse(second.restarted)
        assertEquals(t0, second.clock)
        assertEquals(emptyMap(), second.screens)
    }

    // The hold

    @Test
    fun aPictureHeldOnHomeGoesOnFromItsTurnAtTheTurnsEnd() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val at = t0 + h1
        val hand = setByHand(pic(EARTH), EARTH, setOf(HOME), cols(all), t0, records, stored(), at)
        assertTrue(hand.restarted)
        assertEquals(at, hand.clock)
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), hand.sets)
        assertEquals(Record(APOD, pic(EARTH), at, Hold(EARTH, pic(EARTH))), hand.screens.getValue(HOME).record)
        // Lock takes the new stamp, nothing to set.
        assertEquals(Due(Record(APOD, pic(APOD), at), false), hand.screens.getValue(LOCK))
        val held = apply(records, hand)
        assertEquals(emptyMap(), due(cols(all), held, clock = at, now = at + h1).screens)
        // The turn ends: both go on from APOD, one wallpaper again.
        val end = due(cols(all), held, clock = at, now = at + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), end.sets)
        assertNull(end.screens.getValue(HOME).record.hold)
    }

    @Test
    fun anUntickedSourceIsHeldOnAOneSourceScreenUntilThatSourcePosts() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val home = cols(listOf(APOD), emptyList())
        val hand = setByHand(pic(NASA), NASA, setOf(HOME), home, t0, records, stored(), t0 + h1)
        assertFalse(hand.restarted)
        val held = apply(records, hand)
        assertEquals(Hold(NASA, pic(NASA), pic(APOD)), held.getValue(HOME).hold)
        assertEquals(emptyMap(), due(home, held, now = t0 + Duration.ofDays(1)).screens)
        // A newer post of the held source: set at once, still held.
        val nasa2 = stored(posts + (NASA to post(NASA, d2)))
        val newer = due(home, held, nasa2)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), newer.sets)
        assertEquals(Hold(NASA, pic(NASA, d2), pic(APOD)), newer.screens.getValue(HOME).record.hold)
        // APOD posts: the hold ends.
        val apod2 = due(home, apply(held, newer), stored(posts + mapOf(NASA to post(NASA, d2), APOD to post(APOD, d2))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), apod2.sets)
        assertNull(apod2.screens.getValue(HOME).record.hold)
    }

    @Test
    fun aHeldOneSourceScreenTakesTheCurrentStamp() {
        // Home is [APOD] and holds Earth by hand; Lock turns through all three.
        val home = Record(APOD, pic(EARTH), t0, Hold(EARTH, pic(EARTH), pic(APOD)))
        val records = mapOf(HOME to home, LOCK to Record(EARTH, pic(EARTH), t0))
        val before = cols(listOf(APOD), all)
        val t3 = t0 + h3
        val end = due(before, records, now = t3)
        assertTrue(end.restarted)
        assertEquals(listOf(pic(APOD) to setOf(LOCK)), end.sets)
        assertEquals(home.copy(stamp = t3), end.screens.getValue(HOME).record)
        // A tick in Home's column clears the hold: Home stays on APOD, in step with Lock.
        val after = cols(listOf(APOD, NASA), all)
        val turned = apply(records, end)
        val tick = columnsChanged(before, after, t3, turned, stored(), t3 + h1)
        assertFalse(tick.restarted)
        val plan = due(after, apply(turned, tick), clock = t3, now = t3 + h1)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t3), plan.screens.getValue(HOME).record)
    }

    @Test
    fun aNewerPostOfTheHeldSourceKeepsTheHoldWithTurns() {
        val records = mapOf(HOME to Record(APOD, pic(EARTH), t0, Hold(EARTH, pic(EARTH))))
        val plan = due(cols(listOf(APOD, NASA), emptyList()), records, stored(posts + (EARTH to post(EARTH, d2))))
        assertEquals(Due(Record(APOD, pic(EARTH, d2), t0, Hold(EARTH, pic(EARTH, d2))), true), plan.screens.getValue(HOME))
    }

    @Test
    fun aTickOrAnUntickInItsColumnClearsTheHold() {
        val hold = Hold(EARTH, pic(EARTH))
        val records = mapOf(HOME to Record(APOD, pic(EARTH), t0, hold), LOCK to Record(APOD, pic(EARTH), t0, hold))
        val before = cols(listOf(APOD, NASA))
        for (after in listOf(cols(all, listOf(APOD, NASA)), cols(listOf(APOD), listOf(APOD, NASA)))) {
            val change = columnsChanged(before, after, t0, records, stored(), t0 + h1)
            assertFalse(change.restarted)
            assertEquals(Record(APOD, pic(EARTH), t0), change.screens.getValue(HOME).record)
            assertNull(change.screens[LOCK])
            // Home gets what its settings say at once; Lock stays held.
            val plan = due(after, apply(records, change))
            assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        }
    }

    @Test
    fun anEmptiedColumnTickedAgainStartsOnTheOtherScreensSource() {
        // Home was emptied with a record on Earth, or Earth was set by hand on the empty Home.
        val lock = Record(APOD, pic(APOD), t0)
        val before = cols(emptyList(), all)
        val hand = setByHand(pic(EARTH), EARTH, setOf(HOME), before, t0, mapOf(LOCK to lock), stored(), t0 + h1)
        for (records in listOf(mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to lock), apply(mapOf(LOCK to lock), hand))) {
            assertEquals(EARTH, records.getValue(HOME).owner)
            val t3 = t0 + h3
            val turn = due(before, records, now = t3)
            assertTrue(turn.restarted)
            assertEquals(listOf(pic(NASA) to setOf(LOCK)), turn.sets)
            assertNull(turn.screens[HOME])
            val turned = apply(records, turn)
            // Home is ticked like Lock: no restart, it starts on Lock's source and stamp.
            val tick = columnsChanged(before, cols(all), t3, turned, stored(), t3 + h1)
            assertFalse(tick.restarted)
            assertEquals(Record(NASA, pic(EARTH), t3), tick.screens.getValue(HOME).record)
            val ticked = apply(turned, tick)
            val run = due(cols(all), ticked, clock = t3, now = t3 + h1)
            assertEquals(listOf(pic(NASA) to setOf(HOME)), run.sets)
            // One wallpaper from then on.
            val next = due(cols(all), apply(ticked, run), clock = t3, now = t3 + h3)
            assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), next.sets)
        }
    }

    @Test
    fun anEmptiedColumnTickedAgainMovesOnWithTheOtherScreenDueARetry() {
        // Lock's set failed at the turn's end: it's still on APOD with the old stamp.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to Record(APOD, pic(APOD), t0))
        val t3 = t0 + h3
        val tick = columnsChanged(cols(emptyList(), all), cols(all), t3, records, stored(), t3 + h1)
        assertEquals(Record(APOD, pic(EARTH), t0), tick.screens.getValue(HOME).record)
        val run = due(cols(all), apply(records, tick), clock = t3, now = t3 + h1)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), run.sets)
    }

    @Test
    fun anEmptiedColumnTickedAgainWithoutTheOtherScreensSourceStartsOnItsFirstWithAPicture() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val stored = stored(files = setOf(APOD, EARTH))
        val tick = columnsChanged(cols(emptyList(), listOf(APOD)), cols(listOf(NASA, EARTH), listOf(APOD)), t0, records, stored, t0 + h1)
        assertTrue(tick.restarted)
        assertEquals(Record(EARTH, pic(APOD), t0 + h1), tick.screens.getValue(HOME).record)
    }

    @Test
    fun aHandSetOnAScreenWithoutTurnsLeavesTheClockAndTheOtherScreenAlone() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val plan = setByHand(pic(EARTH), EARTH, setOf(HOME), cols(listOf(APOD), listOf(APOD, NASA)), t0, records, stored(), t0 + h1)
        assertFalse(plan.restarted)
        assertEquals(t0, plan.clock)
        assertEquals(setOf(HOME), plan.screens.keys)
        // And on a screen with no ticks: no hold, it's left alone anyway.
        val empty = setByHand(pic(EARTH), EARTH, setOf(HOME), cols(emptyList(), listOf(APOD)), t0, records, stored(), t0 + h1)
        assertNull(empty.screens.getValue(HOME).record.hold)
        assertFalse(empty.restarted)
    }

    @Test
    fun aHandSetWhileTheOtherScreenIsDueARetryLeavesItsStamp() {
        val t1 = t0 + h3
        val records = mapOf(HOME to Record(NASA, pic(NASA), t1), LOCK to Record(APOD, pic(APOD), t0))
        val at = t1 + h1
        val hand = setByHand(pic(EARTH), EARTH, setOf(HOME), cols(all), t1, records, stored(), at)
        assertEquals(setOf(HOME), hand.screens.keys)
        val next = due(cols(all), apply(records, hand), clock = at, now = at + Duration.ofMinutes(1))
        assertEquals(listOf(pic(NASA) to setOf(LOCK)), next.sets)
        assertEquals(Record(NASA, pic(NASA), at), next.screens.getValue(LOCK).record)
    }

    @Test
    fun aHandSetOnAScreenDueARetryMovesItOnFirst() {
        val t1 = t0 + h3
        val records = mapOf(HOME to Record(NASA, pic(NASA), t1), LOCK to Record(APOD, pic(APOD), t0))
        val at = t1 + h1
        val hand = setByHand(pic(EARTH), EARTH, setOf(LOCK), cols(all), t1, records, stored(), at)
        assertEquals(Record(NASA, pic(EARTH), at, Hold(EARTH, pic(EARTH))), hand.screens.getValue(LOCK).record)
        assertEquals(Due(Record(NASA, pic(NASA), at), false), hand.screens.getValue(HOME))
        // At the turn's end both go on to Earth.
        val end = due(cols(all), apply(records, hand), clock = at, now = at + h3)
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), end.sets)
        assertEquals(EARTH, end.screens.getValue(LOCK).record.owner)
    }

    @Test
    fun aHandSetFromApodsVideoTabCountsAsApods() {
        val video = posts + (APOD to post(APOD, d2, video = true))
        val nasaStandsIn = stored(video, StandIn.NASA)
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0))
        val home = cols(listOf(APOD, EARTH), emptyList())
        val hand = setByHand(pic(NASA), APOD, setOf(HOME), home, t0, records, nasaStandsIn, t0 + h1)
        val held = apply(records, hand)
        assertEquals(Hold(APOD, post(APOD, d2, video = true).picture()), held.getValue(HOME).hold)
        // NASA's next post leaves it.
        val nasa2 = due(home, held, stored(video + (NASA to post(NASA, d2)), StandIn.NASA), clock = t0 + h1, now = t0 + h3)
        assertEquals(emptyMap(), nasa2.screens)
        // APOD's next picture replaces it.
        val apod3 = due(home, held, stored(video + (APOD to post(APOD, d3)), StandIn.NASA), clock = t0 + h1, now = t0 + h3)
        assertEquals(listOf(pic(APOD, d3) to setOf(HOME)), apod3.sets)
        assertNotNull(apod3.screens.getValue(HOME).record.hold)
    }

    @Test
    fun aHandSetOfAOneSourceScreensOwnSourceIsNoHold() {
        val records = mapOf(HOME to Record(APOD, pic(EARTH), t0, Hold(EARTH, pic(EARTH), pic(APOD))))
        val plan = setByHand(pic(APOD), APOD, setOf(HOME), cols(listOf(APOD), emptyList()), t0, records, stored(), t0 + h1)
        assertEquals(Record(APOD, pic(APOD), t0), plan.screens.getValue(HOME).record)
    }

    @Test
    fun aHeldApodWhoseNextPostIsAVideo() {
        val records = mapOf(HOME to Record(NASA, pic(APOD), t0, Hold(APOD, pic(APOD))))
        val home = cols(listOf(NASA, EARTH), emptyList())
        val video = posts + (APOD to post(APOD, d2, video = true))
        val standIn = due(home, records, stored(video, StandIn.EARTH))
        assertEquals(Due(Record(NASA, pic(EARTH), t0, Hold(APOD, post(APOD, d2, video = true).picture())), true), standIn.screens.getValue(HOME))
        // "Skip it" keeps the held picture.
        assertEquals(emptyMap(), due(home, records, stored(video, StandIn.SKIP)).screens)
    }

    // Forgetting and starting

    @Test
    fun aForgottenRecordWithTurnsGetsTheSameOwnersPicture() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, Hold(EARTH, pic(EARTH))).forget())
        assertEquals(Record(NASA, null, t0), records.getValue(HOME))
        val plan = due(cols(all, emptyList()), records)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        // The daily switch going on: the clock restarts, the screens on the current turn take it.
        val t1 = t0 + h3
        val stale = mapOf(HOME to Record(NASA, pic(NASA), t0), LOCK to Record(APOD, pic(APOD), t0 - h3))
        val restart = restartAndForget(t0, stale, t0 + h1)
        assertEquals(Record(NASA, null, t0 + h1), restart.screens.getValue(HOME).record)
        assertEquals(Record(APOD, null, t0 - h3), restart.screens.getValue(LOCK).record)
        val next = due(cols(all), apply(stale, restart), clock = restart.clock, now = t1)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), next.sets)
    }

    @Test
    fun aScreenWithNoRecordStartsOnTheOtherScreensSource() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0))
        val ticked = due(cols(all, listOf(APOD, NASA)), records)
        assertEquals(Due(Record(NASA, pic(NASA), t0), true), ticked.screens.getValue(LOCK))
        // Not ticked there: the first ticked source with a picture.
        val first = due(cols(all, listOf(APOD, EARTH)), records)
        assertEquals(Record(APOD, pic(APOD), t0), first.screens.getValue(LOCK).record)
        val noApod = due(cols(all, listOf(APOD, EARTH)), records, stored(files = setOf(NASA, EARTH)))
        assertEquals(Record(EARTH, pic(EARTH), t0), noApod.screens.getValue(LOCK).record)
    }

    // The stand-in

    @Test
    fun theStandInWithApodAlone() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val home = apodColumns(setOf(HOME))
        val video = posts + (APOD to post(APOD, d2, video = true))
        assertEquals(emptyMap(), due(home, records, stored(video, StandIn.SKIP)).screens)
        for ((choice, picture) in listOf(StandIn.NASA to pic(NASA), StandIn.EARTH to pic(EARTH), StandIn.OWN to Picture.Own)) {
            val plan = due(home, records, stored(video, choice))
            assertEquals(Due(Record(APOD, picture, t0), true), plan.screens.getValue(HOME), "$choice")
        }
        // Nothing stored for it yet: kept.
        assertEquals(emptyMap(), due(home, records, stored(video, StandIn.OWN, own = false)).screens)
        assertEquals(emptyMap(), due(home, records, stored(video - NASA, StandIn.NASA)).screens)
    }

    @Test
    fun theStandInWithTurns() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val home = cols(listOf(APOD, EARTH), emptyList())
        val video = posts + (APOD to post(APOD, d2, video = true))
        for ((choice, picture) in listOf(StandIn.NASA to pic(NASA), StandIn.EARTH to pic(EARTH), StandIn.OWN to Picture.Own)) {
            val plan = due(home, records, stored(video, choice))
            // APOD's turn, the stand-in's picture.
            assertEquals(Due(Record(APOD, picture, t0), true), plan.screens.getValue(HOME), "$choice")
        }
        // "Skip it": the next source, an early change.
        assertEquals(Due(Record(EARTH, pic(EARTH), t0), true), due(home, records, stored(video, StandIn.SKIP)).screens.getValue(HOME))
    }

    @Test
    fun aStandInAlsoTickedMayShowTwiceInARow() {
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.NASA)
        val home = cols(listOf(APOD, NASA), emptyList())
        val apodTurn = due(home, mapOf(HOME to Record(EARTH, pic(EARTH), t0)), video, now = t0 + h3)
        assertEquals(Due(Record(APOD, pic(NASA), t0 + h3), true), apodTurn.screens.getValue(HOME))
        val nasaTurn = due(home, apply(emptyMap(), apodTurn), video, clock = t0 + h3, now = t0 + Duration.ofHours(6))
        assertEquals(Due(Record(NASA, pic(NASA), t0 + Duration.ofHours(6)), false), nasaTurn.screens.getValue(HOME))
        assertEquals(emptyList(), nasaTurn.sets)
    }
}
