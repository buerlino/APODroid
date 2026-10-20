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

    @Test
    fun anEmptiedColumnTickedAgainStartsOnTheOtherScreensSource() {
        // Home was emptied with a record on Earth.
        val lock = Record(APOD, pic(APOD), t0)
        val before = cols(emptyList(), all)
        run {
            val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to lock)
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

    // A set by hand: it changes the settings (no holds)

    private fun byHand(
        tab: Source,
        screens: Set<Screen>,
        columns: Map<Screen, List<Source>>,
        records: Map<Screen, Record>,
        stored: Stored = stored(),
        allSources: Boolean = true,
        clock: Instant? = t0,
        now: Instant = t0 + h1,
    ) = setByHand(tab, screens, allSources, columns, clock, records, stored, now)

    private val both = setOf(HOME, LOCK)

    @Test
    fun theSourceGoesOnExactlyTheChosenScreensAndTheOthersStay() {
        val columns = cols(listOf(NASA), listOf(APOD, EARTH))
        assertEquals(cols(emptyList(), all), byHand(NASA, setOf(LOCK), columns, emptyMap())!!.columns)
        assertEquals(cols(listOf(NASA), all), byHand(NASA, both, columns, emptyMap())!!.columns)
        assertEquals(cols(listOf(APOD, NASA), listOf(EARTH)), byHand(APOD, setOf(HOME), columns, emptyMap())!!.columns)
    }

    @Test
    fun apodModeSetsWhereToExactlyTheChosenScreens() {
        val lock = apodColumns(setOf(LOCK))
        for (screens in listOf(setOf(HOME), setOf(LOCK), both)) {
            assertEquals(apodColumns(screens), byHand(APOD, screens, lock, emptyMap(), allSources = false)!!.columns)
        }
        // The stand-in's tab counts as APOD.
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.NASA)
        assertEquals(apodColumns(both), byHand(NASA, both, lock, emptyMap(), video, allSources = false)!!.columns)
    }

    @Test
    fun theClockRestartsWhenAChosenScreenTakesTurns() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val at = t0 + h1
        val turns = byHand(EARTH, setOf(HOME), cols(listOf(APOD), listOf(APOD, NASA)), records)!!
        assertEquals(cols(listOf(APOD, EARTH), listOf(APOD, NASA)), turns.columns)
        assertEquals(Plan(at, true, mapOf(HOME to Due(Record(EARTH, null, at), false), LOCK to Due(Record(APOD, pic(APOD), at), false))), turns.plan)
        // A chosen screen without turns: the clock stays, though the other screen takes turns.
        val one = byHand(EARTH, setOf(HOME), cols(emptyList(), listOf(APOD, NASA, EARTH)), records)!!
        assertEquals(cols(listOf(EARTH), listOf(APOD, NASA)), one.columns)
        assertEquals(Plan(t0, false, mapOf(HOME to Due(Record(EARTH, null, t0), false))), one.plan)
    }

    @Test
    fun theChosenScreensGetTheSourceAndBothAreOneSet() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        val hand = byHand(APOD, both, cols(listOf(NASA, EARTH)), records)!!
        val at = t0 + h1
        assertEquals(cols(all), hand.columns)
        val after = apply(records, hand.plan)
        assertEquals(Record(APOD, null, at), after.getValue(HOME))
        assertEquals(Record(APOD, null, at), after.getValue(LOCK))
        val plan = due(hand.columns, after, clock = at, now = at)
        assertFalse(plan.restarted)
        assertEquals(listOf(pic(APOD) to both), plan.sets)
    }

    @Test
    fun theOtherScreenTakesTheNewStampOnlyWhenItHadTheCurrentOne() {
        val t1 = t0 + h3
        val at = t1 + h1
        // Lock's set failed at the turn's end: it's still on APOD with the old stamp.
        val retry = mapOf(HOME to Record(NASA, pic(NASA), t1), LOCK to Record(APOD, pic(APOD), t0))
        val hand = byHand(EARTH, setOf(HOME), cols(all), retry, clock = t1, now = at)!!
        assertEquals(setOf(HOME), hand.plan.screens.keys)
        // Lock moves on at the next run, Home shows Earth.
        val next = due(cols(all), apply(retry, hand.plan), clock = at, now = at)
        assertEquals(listOf(pic(EARTH) to setOf(HOME), pic(NASA) to setOf(LOCK)), next.sets)
        assertEquals(Record(NASA, pic(NASA), at), next.screens.getValue(LOCK).record)
        // On the current turn: the new stamp, the owner and the picture kept.
        val current = mapOf(HOME to Record(NASA, pic(NASA), t1), LOCK to Record(NASA, pic(NASA), t1))
        val fresh = byHand(EARTH, setOf(HOME), cols(all), current, clock = t1, now = at)!!
        assertEquals(Due(Record(NASA, pic(NASA), at), false), fresh.plan.screens.getValue(LOCK))
    }

    @Test
    fun theOtherScreenLosingTheSourceIsAnEarlyChange() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        val at = t0 + h1
        val hand = byHand(EARTH, setOf(HOME), cols(listOf(APOD, NASA), all), records)!!
        assertEquals(cols(listOf(APOD, NASA, EARTH), listOf(APOD, NASA)), hand.columns)
        val plan = due(hand.columns, apply(records, hand.plan), clock = at, now = at)
        // Lock's owner unticked: the next source for the rest of the turn, its stamp kept.
        assertEquals(listOf(pic(EARTH) to setOf(HOME), pic(APOD) to setOf(LOCK)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), at), plan.screens.getValue(LOCK).record)
        // An emptied column is left alone.
        val alone = byHand(EARTH, setOf(HOME), cols(listOf(APOD), listOf(EARTH)), records)!!
        assertEquals(cols(listOf(APOD, EARTH), emptyList()), alone.columns)
        val run = due(alone.columns, apply(records, alone.plan), clock = at, now = at)
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), run.sets)
        assertNull(run.screens[LOCK])
    }

    @Test
    fun aColumnThatWasEmptyStartsOnTheSource() {
        // Home was emptied with a stale record on NASA.
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0 - h3), LOCK to Record(NASA, pic(NASA), t0))
        val at = t0 + h1
        val hand = byHand(EARTH, both, cols(emptyList(), listOf(APOD, NASA)), records)!!
        assertEquals(cols(listOf(EARTH), all), hand.columns)
        assertEquals(Record(EARTH, null, at), hand.plan.screens.getValue(HOME).record)
        assertEquals(listOf(pic(EARTH) to both), due(hand.columns, apply(records, hand.plan), clock = at, now = at).sets)
    }

    @Test
    fun settingsAlreadySoStartTheSourcesTurnOrSetItAgain() {
        val home = cols(listOf(APOD, NASA), emptyList())
        val at = t0 + h1
        // Another source's turn: the source's turn starts now.
        val nasa = mapOf(HOME to Record(NASA, pic(NASA), t0))
        val turn = byHand(APOD, setOf(HOME), home, nasa)!!
        assertEquals(home, turn.columns)
        assertTrue(turn.plan.restarted)
        val run = due(home, apply(nasa, turn.plan), clock = at, now = at)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), run.sets)
        // At its end the next source, a whole turn later.
        val end = due(home, apply(apply(nasa, turn.plan), run), clock = at, now = at + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), end.sets)
        // Already on the source (✓ with a new part): set again.
        val apod = mapOf(HOME to Record(APOD, pic(APOD), t0))
        val again = byHand(APOD, setOf(HOME), home, apod)!!
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(home, apply(apod, again.plan), clock = at, now = at).sets)
        // A one-source screen: set again, the clock stays.
        val one = cols(listOf(APOD), emptyList())
        val single = byHand(APOD, setOf(HOME), one, apod)!!
        assertEquals(one, single.columns)
        assertFalse(single.plan.restarted)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(one, apply(apod, single.plan)).sets)
    }

    @Test
    fun nothingToSetChangesNothing() {
        val video = posts + (APOD to post(APOD, d2, video = true))
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0))
        // "Skip it" on APOD's video tab, both modes.
        assertNull(byHand(APOD, both, cols(listOf(APOD), emptyList()), records, stored(video, StandIn.SKIP)))
        assertNull(byHand(APOD, both, apodColumns(setOf(HOME)), records, stored(video, StandIn.SKIP), allSources = false))
        // The stand-in or the user's own picture not there yet, a source not stored.
        assertNull(byHand(APOD, both, apodColumns(setOf(HOME)), records, stored(video - NASA, StandIn.NASA), allSources = false))
        assertNull(byHand(APOD, both, apodColumns(setOf(HOME)), records, stored(video, StandIn.OWN, own = false), allSources = false))
        assertNull(byHand(NASA, both, cols(listOf(APOD), emptyList()), records, stored(files = setOf(APOD))))
        // APOD's video tab with a stand-in: set as APOD's.
        val onNasa = mapOf(HOME to Record(NASA, pic(NASA), t0))
        val nasa = byHand(APOD, setOf(LOCK), cols(listOf(NASA), emptyList()), onNasa, stored(video, StandIn.NASA))!!
        assertEquals(cols(listOf(NASA), listOf(APOD)), nasa.columns)
        assertEquals(listOf(pic(NASA) to setOf(LOCK)), due(nasa.columns, apply(onNasa, nasa.plan), stored(video, StandIn.NASA)).sets)
    }

    // The simulation of 2026-10-20: each case as the settings after the set and the due pictures over the next posts

    @Test
    fun homeNasaLockApodThenApodOnBoth() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0), LOCK to Record(APOD, pic(APOD), t0))
        val at = t0 + h1
        val hand = byHand(APOD, both, cols(listOf(NASA), listOf(APOD)), records)!!
        // Home takes turns APOD/NASA with APOD now, Lock is APOD.
        assertEquals(cols(listOf(APOD, NASA), listOf(APOD)), hand.columns)
        assertEquals(at, hand.plan.clock)
        var r = apply(records, hand.plan)
        val now = due(hand.columns, r, clock = at, now = at)
        assertEquals(listOf(pic(APOD) to both), now.sets)
        r = apply(r, now)
        // The turn ends: Home NASA, Lock stays APOD.
        val turn = due(hand.columns, r, clock = at, now = at + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), turn.sets)
        r = apply(r, turn)
        // APOD posts: Lock at once, Home at its next APOD turn.
        val apod2 = stored(posts + (APOD to post(APOD, d2)))
        val post = due(hand.columns, r, apod2, clock = at + h3, now = at + h3 + h1)
        assertEquals(listOf(pic(APOD, d2) to setOf(LOCK)), post.sets)
        r = apply(r, post)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(hand.columns, r, apod2, clock = at + h3, now = at + h3 + h3).sets)
    }

    @Test
    fun earthOnHomeOnlyLeavesLocksPictureWithTheNewStamp() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(NASA, pic(NASA), t0))
        val at = t0 + h1
        val hand = byHand(EARTH, setOf(HOME), cols(all, listOf(NASA, EARTH)), records)!!
        assertEquals(cols(all, listOf(NASA)), hand.columns)
        var r = apply(records, hand.plan)
        assertEquals(Record(NASA, pic(NASA), at), r.getValue(LOCK))
        val now = due(hand.columns, r, clock = at, now = at)
        // Earth now with a new turn; Lock's picture stays.
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), now.sets)
        r = apply(r, now)
        assertEquals(Record(EARTH, pic(EARTH), at), r.getValue(HOME))
        // A whole turn later APOD on Home; NASA posts: Lock at once.
        val nasa2 = stored(posts + (NASA to post(NASA, d2)))
        val turn = due(hand.columns, r, nasa2, clock = at, now = at + h3)
        assertEquals(listOf(pic(APOD) to setOf(HOME), pic(NASA, d2) to setOf(LOCK)), turn.sets)
    }

    @Test
    fun apodOnBothThenAnApodVideoShowsTheStandInByTheSettings() {
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to Record(APOD, pic(APOD), t0))
        val at = t0 + h1
        val hand = byHand(APOD, both, cols(listOf(EARTH), listOf(APOD)), records, stored(standIn = StandIn.NASA))!!
        assertEquals(cols(listOf(APOD, EARTH), listOf(APOD)), hand.columns)
        var r = apply(records, hand.plan)
        val now = due(hand.columns, r, stored(standIn = StandIn.NASA), clock = at, now = at)
        assertEquals(listOf(pic(APOD) to both), now.sets)
        r = apply(r, now)
        // An APOD video: Home has APOD ticked, so its APOD turn shows NASA, and Lock too.
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.NASA)
        val standIn = due(hand.columns, r, video, clock = at, now = at + h1)
        assertEquals(listOf(pic(NASA) to both), standIn.sets)
        r = apply(r, standIn)
        // Home's turn ends: Earth; Lock keeps NASA standing in.
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), due(hand.columns, r, video, clock = at, now = at + h3).sets)
    }

    @Test
    fun apodModeWithLockSetOnHomeLeavesTheLockScreenAlone() {
        val records = mapOf(LOCK to Record(APOD, pic(APOD), t0))
        val hand = byHand(APOD, setOf(HOME), apodColumns(setOf(LOCK)), records, allSources = false)!!
        // "Where" is Home.
        assertEquals(apodColumns(setOf(HOME)), hand.columns)
        assertEquals(setOf(HOME), hand.plan.screens.keys)
        var r = apply(records, hand.plan)
        val now = due(hand.columns, r)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), now.sets)
        r = apply(r, now)
        // APOD posts: Home only; the lock screen keeps its picture.
        val next = due(hand.columns, r, stored(posts + (APOD to post(APOD, d2))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), next.sets)
        assertNull(next.screens[LOCK])
    }

    @Test
    fun apodModeStandInSetByHandFollowsAChangedVideoDayChoice() {
        // The case that failed with the hold: Earth goes on.
        val video = posts + (APOD to post(APOD, d2, video = true))
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val where = apodColumns(both)
        val hand = byHand(NASA, both, where, records, stored(video, StandIn.NASA), allSources = false)!!
        assertEquals(where, hand.columns)
        var r = apply(records, hand.plan)
        val now = due(where, r, stored(video, StandIn.NASA))
        assertEquals(listOf(pic(NASA) to both), now.sets)
        r = apply(r, now)
        assertEquals(Record(APOD, pic(NASA), t0), r.getValue(HOME))
        // The video-day choice changed to Earth: the owner APOD alone decides (Store.settingChanged forgets).
        val earth = stored(video, StandIn.EARTH)
        val forgotten = r.filterValues { it.owner == APOD }.mapValues { it.value.forget() }
        assertEquals(both, forgotten.keys)
        assertEquals(listOf(pic(EARTH) to both), due(where, r + forgotten, earth).sets)
        // Without the forget too.
        assertEquals(listOf(pic(EARTH) to both), due(where, r, earth).sets)
    }

    @Test
    fun skipItOnApodsVideoTabChangesNothing() {
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.SKIP)
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0), LOCK to Record(APOD, pic(APOD), t0))
        assertNull(byHand(APOD, both, cols(listOf(NASA), listOf(APOD)), records, video))
        assertNull(byHand(APOD, setOf(HOME), apodColumns(setOf(LOCK)), records, video, allSources = false))
    }

    // Forgetting and starting

    @Test
    fun aForgottenRecordWithTurnsGetsTheSameOwnersPicture() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0).forget())
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

    // The page's "video day, wallpaper kept"

    private val videoDay = posts + (APOD to post(APOD, d2, video = true))

    @Test
    fun aVideoDayKeepsAnApodScreenWithNothingStandingIn() {
        val kept = Record(APOD, pic(APOD))
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.SKIP)))
        // "Use my picture" with none picked, NASA not stored yet.
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.OWN, own = false)))
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay - NASA, StandIn.NASA)))
        // The stand-in is stored but not set yet.
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.NASA)))
    }

    @Test
    fun aStandInOnTheScreenIsNoKeptPicture() {
        assertFalse(videoDayKept(listOf(APOD), Record(APOD, pic(NASA)), stored(videoDay, StandIn.NASA)))
        assertFalse(videoDayKept(listOf(APOD, EARTH), Record(APOD, Picture.Own), stored(videoDay, StandIn.OWN)))
    }

    @Test
    fun onlyAnApodVideoDayOnAnApodTurnKeeps() {
        // An image day.
        assertFalse(videoDayKept(listOf(APOD), Record(APOD, pic(APOD)), stored()))
        // APOD not ticked there.
        assertFalse(videoDayKept(listOf(NASA), Record(NASA, pic(NASA)), stored(videoDay)))
        // With turns "Skip it" passed the turn on.
        assertFalse(videoDayKept(all, Record(NASA, pic(NASA)), stored(videoDay)))
        // Never set, or forgotten: the next set decides.
        assertFalse(videoDayKept(listOf(APOD), null, stored(videoDay)))
        assertFalse(videoDayKept(listOf(APOD), Record(APOD), stored(videoDay)))
    }
}
