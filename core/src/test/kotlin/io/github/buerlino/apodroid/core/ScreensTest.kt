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

    // Forgetting and starting

    @Test
    fun aForgottenRecordWithTurnsGetsTheSameOwnersPicture() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0).forget())
        assertEquals(Record(NASA, null, t0), records.getValue(HOME))
        val plan = due(cols(all, emptyList()), records)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
    }

    // Set: the clock restarts, the records forgotten

    @Test
    fun setRestartsTheClockAndForgetsTheRecordsOnTheCurrentTurn() {
        val t1 = t0 + h3
        // Lock is still due a retry from the turn before.
        val stale = mapOf(HOME to Record(NASA, pic(NASA), t0), LOCK to Record(APOD, pic(APOD), t0 - h3))
        val restart = restartAndForget(cols(all), cols(all), t0, stale, t0 + h1)
        assertTrue(restart.plan.restarted)
        assertEquals(t0 + h1, restart.plan.clock)
        assertEquals(emptySet(), restart.drop)
        assertEquals(Record(NASA, null, t0 + h1), restart.plan.screens.getValue(HOME).record)
        assertEquals(Record(APOD, null, t0 - h3), restart.plan.screens.getValue(LOCK).record)
        // At once: Home gets its owner's picture again, Lock moves on to the next source.
        val now = due(cols(all), apply(stale, restart.plan), clock = restart.plan.clock, now = t0 + h1)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), now.sets)
        // The turn is over: both move on together.
        val next = due(cols(all), apply(apply(stale, restart.plan), now), clock = restart.plan.clock, now = t1 + h1)
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), next.sets)
    }

    @Test
    fun setPutsTheSamePictureOnAgainSoAChangedFitOrOwnPictureShows() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        assertEquals(emptyList(), due(cols(listOf(APOD)), records).sets)
        val restart = restartAndForget(cols(listOf(APOD)), cols(listOf(APOD)), t0, records, t0 + h1)
        val plan = due(cols(listOf(APOD)), apply(records, restart.plan), clock = restart.plan.clock, now = t0 + h1)
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), plan.sets)
        assertFalse(plan.restarted)
    }

    @Test
    fun setLeavesAScreenWithAnEmptyColumnAndItsRecordAlone() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        val restart = restartAndForget(cols(listOf(APOD)), cols(listOf(NASA), emptyList()), t0, records, t0 + h1)
        assertEquals(setOf(HOME), restart.plan.screens.keys)
        val plan = due(cols(listOf(NASA), emptyList()), apply(records, restart.plan), clock = restart.plan.clock, now = t0 + h1)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertNull(plan.screens[LOCK])
    }

    @Test
    fun aColumnTickedAgainAtSetStartsOnTheOtherScreensSource() {
        // Home was emptied while its record was on Earth; Lock takes turns and is on NASA.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to Record(NASA, pic(NASA), t0))
        val restart = restartAndForget(cols(emptyList(), all), cols(all), t0, records, t0 + h1)
        assertEquals(setOf(HOME), restart.drop)
        assertEquals(setOf(LOCK), restart.plan.screens.keys)
        val after = apply(records, restart.plan) - restart.drop
        val plan = due(cols(all), after, clock = restart.plan.clock, now = t0 + h1)
        // One wallpaper: Home starts on Lock's source with the new stamp.
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), plan.sets)
        assertEquals(Record(NASA, pic(NASA), t0 + h1), plan.screens.getValue(HOME).record)
    }

    @Test
    fun aColumnTickedAgainAtSetWithoutTheOtherScreensSourceStartsOnItsFirstWithAPicture() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val restart = restartAndForget(cols(emptyList(), listOf(APOD)), cols(listOf(NASA, EARTH), listOf(APOD)), t0, records, t0 + h1)
        val after = apply(records, restart.plan) - restart.drop
        val plan = due(cols(listOf(NASA, EARTH), listOf(APOD)), after, stored(files = setOf(APOD, EARTH)), restart.plan.clock, t0 + h1)
        assertEquals(Record(EARTH, pic(EARTH), t0 + h1), plan.screens.getValue(HOME).record)
        assertEquals(listOf(pic(APOD) to setOf(LOCK), pic(EARTH) to setOf(HOME)), plan.sets)
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
        assertFalse(videoDayKept(listOf(APOD), null, stored(videoDay)))
    }

    @Test
    fun aRecordForgottenBySetAfterASetIsKept() {
        // Set drops the picture, "Skip it" alone sets nothing; the screen was set before.
        assertTrue(videoDayKept(listOf(APOD), Record(APOD), stored(videoDay, StandIn.SKIP), everSet = true))
        assertFalse(videoDayKept(listOf(APOD), null, stored(videoDay), everSet = true))
    }

    @Test
    fun aFreshInstallsFirstVideoDayKeepsNothing() {
        // The record dueWallpapers writes with "Skip it": owner APOD, no picture, never set.
        assertFalse(videoDayKept(listOf(APOD), Record(APOD), stored(videoDay, StandIn.SKIP), everSet = false))
    }

    @Test
    fun theListPlacesASourceOnExactlyItsScreens() {
        val columns = mapOf(HOME to listOf(APOD, NASA), LOCK to listOf(APOD))
        assertEquals(setOf(HOME, LOCK), columns.screensOf(APOD))
        assertEquals(setOf(HOME), columns.screensOf(NASA))
        assertEquals(emptySet(), columns.screensOf(EARTH))
        // A tick on Both: in order in each column.
        assertEquals(mapOf(HOME to listOf(APOD, NASA, EARTH), LOCK to listOf(APOD, EARTH)), columns.placing(EARTH, setOf(HOME, LOCK)))
        // A new dropdown choice moves it; the others stay.
        assertEquals(mapOf(HOME to listOf(APOD), LOCK to listOf(APOD, NASA)), columns.placing(NASA, setOf(LOCK)))
        // An untick takes it off both.
        assertEquals(mapOf(HOME to listOf(NASA), LOCK to emptyList()), columns.placing(APOD, emptySet()))
        // The same screens: nothing changes.
        assertEquals(columns, columns.placing(NASA, setOf(HOME)))
    }
}
