package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rules per screen (J1). In UTC, "New picture at" 06:00, "Change every" 3 h: a column with
 * turns changes at 00:00, 03:00, 06:00, …, one with one source at 06:00. [t0] is a change time
 * of both; the window after it ends at [t0] + 3 h, the next turn's change.
 */
class ScreensTest {
    private val zone = ZoneOffset.UTC
    private val time = PictureTime(6, 3)
    private val t0 = Instant.parse("2026-10-19T06:00:00Z")
    private val min1 = Duration.ofMinutes(1)
    private val h1 = Duration.ofHours(1)
    private val h3 = Duration.ofHours(3)
    private val day = Duration.ofHours(24)
    private val d1 = LocalDate.parse("2026-10-19")
    private val d2 = LocalDate.parse("2026-10-20")
    private val all = Source.entries

    /**
     * A post of [date], published a minute before that date's 06:00 UTC less a day ([d2]'s just
     * before [t0], [d1]'s a day earlier) unless [published] says otherwise (B15).
     */
    private fun post(source: Source, date: LocalDate = d1, video: Boolean = false, published: Instant? = date.minusDays(1).atTime(6, 0).toInstant(zone) - min1) =
        Post(source, date, "$source $date", if (video) null else "https://x/$source-$date.jpg", "https://p/$source-$date", video, published = published)

    private val posts = all.associateWith { post(it) }

    private fun stored(
        posts: Map<Source, Post> = this.posts,
        standIn: StandIn = StandIn.SKIP,
        own: Boolean = true,
        files: Set<Source> = posts.keys,
        gaps: Set<Source> = emptySet(),
    ) = Stored(posts, files, own, standIn, gaps)

    private fun pic(source: Source, date: LocalDate = d1) = post(source, date).picture()

    /** The notes of a screen that has shown these posts. */
    private fun shown(vararg pictures: Picture.Of) = pictures.associateBy { it.source }

    private fun cols(home: List<Source>, lock: List<Source> = home) = mapOf(HOME to home, LOCK to lock)

    private fun due(
        columns: Map<Screen, List<Source>>,
        records: Map<Screen, Record>,
        stored: Stored = stored(),
        now: Instant = t0 + h1,
        pull: Boolean = false,
    ) = dueWallpapers(columns, records, stored, time, zone, now, pull)

    private fun set(before: Map<Screen, List<Source>>, after: Map<Screen, List<Source>>, records: Map<Screen, Record>, now: Instant, stored: Stored = stored()) =
        restartAndForget(before, after, records, stored, time, zone, now)

    /** The records after [plan], all its sets done. */
    private fun apply(records: Map<Screen, Record>, plan: Plan) = records + plan.screens.mapValues { it.value.record }

    /** A record without its notes, to compare owner, picture and stamp. */
    private fun Record.bare() = copy(shown = emptyMap())

    private fun Plan.bare(screen: Screen) = screens.getValue(screen).let { it.copy(record = it.record.bare()) }

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

    // Rule 1: at a change time

    @Test
    fun oneSourceGoesToItsNewestPictureAtTheChangeTime() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day))
        val home = apodColumns(setOf(HOME))
        val newer = stored(posts + (APOD to post(APOD, d2)))
        // Before the change time nothing.
        assertEquals(emptyList(), due(home, records, newer, now = t0 - min1).sets)
        val plan = due(home, records, newer, now = t0)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), plan.sets)
        assertEquals(Record(APOD, pic(APOD, d2), t0, shown(pic(APOD, d2))), plan.screens.getValue(HOME).record)
        // Already on: no set, the change time taken.
        val same = due(home, records, now = t0)
        assertEquals(emptyList(), same.sets)
        assertEquals(t0, same.screens.getValue(HOME).record.stamp)
    }

    @Test
    fun severalSourcesGoToTheNextWithAPictureTheScreenHasntShown() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - h3, shown(pic(APOD), pic(NASA))))
        val plan = due(cols(all, emptyList()), records, now = t0)
        // NASA's is shown already: Earth.
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), plan.sets)
        assertEquals(Record(EARTH, pic(EARTH), t0, shown(pic(APOD), pic(NASA), pic(EARTH))), plan.screens.getValue(HOME).record)
        // Round to itself: only APOD has a new picture.
        val onlyApod = mapOf(HOME to Record(APOD, pic(APOD), t0 - h3, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val newApod = stored(posts + (APOD to post(APOD, d2)))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(cols(all, emptyList()), onlyApod, newApod, now = t0).sets)
    }

    @Test
    fun aMissedChangeTimeIsOneChange() {
        // The phone was off for a day: one change, to the next new source, not one per missed time.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD))))
        val plan = due(cols(all, emptyList()), records, now = t0 + min1)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
    }

    // Rule 2: the window

    @Test
    fun aNewerPictureOfTheSourceThatWentOnReplacesItInTheWindow() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        // Published in the window (B15): set whenever a run sees it, in the window or after it.
        val newer = stored(posts + (APOD to post(APOD, d2, published = t0 + Duration.ofMinutes(5))))
        for (now in listOf(t0 + h1, t0 + h3 - min1, t0 + h3 + min1, t0 + day - min1)) {
            val plan = due(home, records, newer, now = now)
            assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), plan.sets, "$now")
            // The change time kept.
            assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
        }
    }

    @Test
    fun aPicturePublishedAfterTheWindowWaitsForTheNextChangeTime() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        val late = stored(posts + (APOD to post(APOD, d2, published = t0 + h3)))
        assertEquals(emptyList(), due(home, records, late, now = t0 + h3 + h1).sets)
        assertEquals(emptyList(), due(home, records, late, now = t0 + day - min1).sets)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(home, records, late, now = t0 + day).sets)
        // A pull puts it on at once, whenever it was published.
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(home, records, late, now = t0 + h3 + h1, pull = true).sets)
    }

    // B21: one source after a day offline

    /** At 21:05 the evening before its date, as Los Angeles sees APOD (here in UTC, change times at 06:00). */
    private fun evening(source: Source, date: LocalDate) = post(source, date, published = date.minusDays(1).atTime(21, 5).toInstant(zone))

    @Test
    fun afterADayOfflineTheNewestGoesOnWhenARunSeesIt() {
        // CR2's finding 3: APOD alone; D−1's post (out at 21:05 on D−2) went on at 06:00 on D−1,
        // then the phone was offline from 20:00 on D−1 to 22:00 on D. The 22:00 fetch stores
        // D+1's post (out at 21:05 on D, after D's window); D's is gone (only the newest is stored).
        val dm1 = d1
        val d = d2
        val records = mapOf(HOME to Record(APOD, evening(APOD, dm1).picture(), t0, shown(evening(APOD, dm1).picture())))
        val home = apodColumns(setOf(HOME))
        val lastCheck = t0 + Duration.ofHours(14) // 20:00 on D−1
        val now = d.atTime(22, 0).toInstant(zone)
        val newest = evening(APOD, d.plusDays(1))
        assertTrue(afterGap(lastCheck, now))
        val marked = stored(posts + (APOD to newest), gaps = setOf(APOD))
        val plan = due(home, records, marked, now = now)
        assertEquals(listOf(newest.picture() to setOf(HOME)), plan.sets)
        // The change time kept (D's 06:00).
        assertEquals(t0 + day, plan.screens.getValue(HOME).record.stamp)
        // Once on, it isn't set again.
        assertEquals(emptyList(), due(home, apply(records, plan), marked, now = now + h1).sets)
        // A failed set (the record stays): the next run still sets it.
        assertEquals(listOf(newest.picture() to setOf(HOME)), due(home, records, marked, now = now + h1).sets)
        // Never back: a marked post older than the one on the screen isn't set.
        val ahead = mapOf(HOME to Record(APOD, evening(APOD, d.plusDays(2)).picture(), t0 + day))
        assertEquals(emptyList(), due(home, ahead, marked, now = now).sets)
    }

    @Test
    fun theNormalEveningStaysUnset() {
        // The same evening online: checked at 20:00 on D, D+1's post stored at 21:15, not marked.
        val d = d2
        val records = mapOf(HOME to Record(APOD, evening(APOD, d).picture(), t0 + day, shown(evening(APOD, d).picture())))
        val home = apodColumns(setOf(HOME))
        val now = d.atTime(21, 15).toInstant(zone)
        assertFalse(afterGap(d.atTime(20, 0).toInstant(zone), now))
        val newest = stored(posts + (APOD to evening(APOD, d.plusDays(1))))
        assertEquals(emptyList(), due(home, records, newest, now = now).sets)
        assertEquals(emptyList(), due(home, records, newest, now = now + h1).sets)
        // It goes on at the next change time.
        assertEquals(listOf(evening(APOD, d.plusDays(1)).picture() to setOf(HOME)), due(home, records, newest, now = t0 + day + day).sets)
    }

    @Test
    fun nasaFridayToMondayWithDailyChecksStaysUnsetUntilTheChangeTime() {
        // NASA alone in Europe (UTC here): Friday's post on the screen; the checks at each change
        // time (and the window's, the fallback's) succeed over the weekend with nothing new.
        // Monday's comes out at 16:00 and the 18:00 fallback stores it: no gap, so Tuesday 06:00.
        val friday = LocalDate.parse("2026-10-23")
        val monday = LocalDate.parse("2026-10-26")
        val nasa = cols(listOf(NASA), emptyList())
        val fridayPost = post(NASA, friday, published = friday.atTime(16, 0).toInstant(zone))
        val mondayPost = post(NASA, monday, published = monday.atTime(16, 0).toInstant(zone))
        val mondayAt6 = monday.atTime(6, 0).toInstant(zone)
        val records = mapOf(HOME to Record(NASA, fridayPost.picture(), mondayAt6, shown(fridayPost.picture())))
        val lastCheck = monday.atTime(9, 0).toInstant(zone)
        val stored = monday.atTime(18, 0).toInstant(zone)
        val gaps = if (afterGap(lastCheck, stored)) setOf(NASA) else emptySet()
        assertEquals(emptySet(), gaps)
        val withMonday = stored(posts + (NASA to mondayPost), gaps = gaps)
        assertEquals(emptyList(), due(nasa, records, withMonday, now = stored).sets)
        assertEquals(emptyList(), due(nasa, records, withMonday, now = mondayAt6 + day - min1).sets)
        assertEquals(listOf(mondayPost.picture() to setOf(HOME)), due(nasa, records, withMonday, now = mondayAt6 + day).sets)
        // Saturday's and Sunday's checks noted the check, so Monday's isn't after a gap either.
        assertFalse(afterGap(monday.minusDays(1).atTime(9, 0).toInstant(zone), mondayAt6))
    }

    @Test
    fun theGapIsMoreThanADayWithoutASuccessfulCheck() {
        val at = Instant.parse("2026-10-20T22:00:00Z")
        assertTrue(afterGap(at - Duration.ofHours(25), at))
        assertFalse(afterGap(at - Duration.ofHours(23), at))
        assertFalse(afterGap(at - day, at))
        assertTrue(afterGap(at - day - Duration.ofSeconds(1), at))
        // No check yet (a fresh install, a source's first check): no gap.
        assertFalse(afterGap(null, at))
    }

    @Test
    fun theStandInsOwnMarkCountsOnAVideoDay() {
        // APOD alone, NASA stands in. After a day offline APOD's video (out at 21:05) and NASA's
        // post are both stored after the gap: NASA's picture goes on.
        val d = d2
        val records = mapOf(HOME to Record(APOD, evening(APOD, d1).picture(), t0, shown(evening(APOD, d1).picture())))
        val home = apodColumns(setOf(HOME))
        val now = d.atTime(22, 0).toInstant(zone)
        val video = post(APOD, d.plusDays(1), video = true, published = d.atTime(21, 5).toInstant(zone))
        val nasa = post(NASA, d, published = d.atTime(16, 0).toInstant(zone))
        val newest = posts + (APOD to video) + (NASA to nasa)
        val both = stored(newest, StandIn.NASA, files = setOf(NASA, EARTH), gaps = setOf(APOD, NASA))
        assertEquals(listOf(nasa.picture() to setOf(HOME)), due(home, records, both, now = now).sets)
        // The normal evening on a video day: APOD checked in the evening (no mark), NASA's first
        // fetch this video day marked: APOD's video came out after the window, so it waits.
        val standInOnly = stored(newest, StandIn.NASA, files = setOf(NASA, EARTH), gaps = setOf(NASA))
        assertEquals(emptyList(), due(home, records, standInOnly, now = now).sets)
        // APOD's video marked, NASA's not but published after the window: it waits too.
        val apodOnly = stored(newest, StandIn.NASA, files = setOf(NASA, EARTH), gaps = setOf(APOD))
        assertEquals(emptyList(), due(home, records, apodOnly, now = now).sets)
    }

    @Test
    fun turnsIgnoreTheMark() {
        // Several sources: a marked post of the owner published after the window waits for its next turn.
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val late = stored(posts + (NASA to post(NASA, d2, published = t0 + h3 + min1)), gaps = setOf(NASA))
        val daily = dueWallpapers(cols(listOf(APOD, NASA), emptyList()), records, late, PictureTime(6, 24), zone, t0 + h3 + h1)
        assertEquals(emptyList(), daily.sets)
    }

    @Test
    fun aStoredPostWithoutAPublishTimeCountsFromItsDatesMidnightUsEastern() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        // d2's 00:00 US Eastern is 04:00 UTC on 20 Oct: after [t0]'s window, before the next one's end.
        val old = stored(posts + (APOD to post(APOD, d2, published = null)))
        assertEquals(emptyList(), due(home, records, old, now = t0 + h1).sets)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(home, records, old, now = t0 + day).sets)
        // d1's 00:00 US Eastern (04:00 UTC on 19 Oct) is before [t0]: in its window.
        val yesterday = mapOf(HOME to Record(APOD, post(APOD, d1.minusDays(1)).picture(), t0))
        val d1Old = stored(posts + (APOD to post(APOD, d1, published = null)))
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(home, yesterday, d1Old, now = t0 + h3 + h1).sets)
    }

    @Test
    fun aScreenNeverGoesBackToAnOlderPicture() {
        // A stored post older than the one on the screen (its picture file was gone, so refresh took it).
        val records = mapOf(HOME to Record(APOD, pic(APOD, d2), t0, shown(pic(APOD, d2))))
        val home = apodColumns(setOf(HOME))
        assertEquals(emptyList(), due(home, records, now = t0 + h1).sets)
        assertEquals(emptyList(), due(home, records, now = t0 + day).sets)
        assertEquals(emptyList(), due(home, records, now = t0 + h1, pull = true).sets)
    }

    @Test
    fun aFailedSetIsRetriedAfterTheWindow() {
        // B15, finding 6: the set of a picture published in the window failed (the record stays);
        // the alarms' retry after 09:00 still sets it. One source, and turns every 24 h.
        val newer = stored(posts + (NASA to post(NASA, d2, published = t0 + Duration.ofMinutes(10))) + (APOD to post(APOD, d2, published = t0 + Duration.ofMinutes(5))))
        val one = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(apodColumns(setOf(HOME)), one, newer, now = t0 + h3 + h1).sets)
        val turns = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(APOD), pic(NASA))))
        val daily = dueWallpapers(cols(listOf(APOD, NASA), emptyList()), turns, newer, PictureTime(6, 24), zone, t0 + h3 + h1)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), daily.sets)
    }

    @Test
    fun inTheWindowOnlyTheSourceThatWentOnReplacesIt() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val home = cols(all, emptyList())
        // A new Earth post waits for the next change time.
        val newEarth = stored(posts + (EARTH to post(EARTH, d2)))
        assertEquals(emptyList(), due(home, records, newEarth, now = t0 + h1).sets)
        assertEquals(listOf(pic(EARTH, d2) to setOf(HOME)), due(home, records, newEarth, now = t0 + h3).sets)
        // A newer NASA post replaces it.
        val newNasa = stored(posts + (NASA to post(NASA, d2)))
        val plan = due(home, records, newNasa, now = t0 + h1)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), plan.sets)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
    }

    // Rule 3: nothing new, the screen waits

    @Test
    fun withNothingNewTheScreenWaitsAndTakesTheFirstNewPicture() {
        val before = t0 - h3
        val records = mapOf(HOME to Record(APOD, pic(APOD), before, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val home = cols(all, emptyList())
        val nothing = due(home, records, now = t0)
        assertEquals(emptyMap(), nothing.screens)
        assertTrue(waiting(home, records, time, zone, t0))
        // A new post at 07:30, seen at the next check, goes on with the change time of then.
        val newNasa = stored(posts + (NASA to post(NASA, d2)))
        val plan = due(home, records, newNasa, now = t0 + h1 + h1)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), plan.sets)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
        assertFalse(waiting(home, apply(records, plan), time, zone, t0 + h1 + h1))
        // Still waiting at the next change time, then a new post: the change time of then.
        val later = due(home, records, newNasa, now = t0 + h3 + h1)
        assertEquals(t0 + h3, later.screens.getValue(HOME).record.stamp)
    }

    @Test
    fun aWaitingScreenTakesTheNextInOrderWhenTwoAreNew() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0 - h3, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val two = stored(posts + (APOD to post(APOD, d2)) + (EARTH to post(EARTH, d2)))
        assertEquals(listOf(pic(EARTH, d2) to setOf(HOME)), due(cols(all, emptyList()), records, two, now = t0 + h1).sets)
    }

    @Test
    fun aScreenWithOneSourceNeverWaits() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        val plan = due(home, records, now = t0)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
        assertFalse(waiting(home, apply(records, plan), time, zone, t0))
        assertFalse(waiting(home, records, time, zone, t0))
    }

    // A stamp that isn't a change time

    @Test
    fun aMigratedStampCountsAsTheCurrentChangeTime() {
        // 0.6.0's Set clock or 0.5.0's turns: any moment.
        val clock = Instant.parse("2026-10-19T04:23:17.123Z")
        val newer = stored(posts + (APOD to post(APOD, d2)) + (NASA to post(NASA, d2)))
        // Turns: nothing until the next change time (one source: the publish time decides, below).
        for (columns in listOf(cols(all, emptyList()))) {
            val records = mapOf(HOME to Record(APOD, pic(APOD), clock))
            val plan = due(columns, records, newer, now = t0 + h1)
            assertEquals(emptyList(), plan.sets, "$columns")
            // 06:00 is the current change time of both.
            assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
            // The next change time changes.
            val next = if (columns.getValue(HOME).size == 1) t0 + day else t0 + h3
            assertEquals(1, due(columns, apply(records, plan), newer, now = next).sets.size, "$columns")
            // A pull sets at once.
            assertEquals(1, due(columns, records, newer, now = t0 + h1, pull = true).sets.size, "$columns")
        }
        // One source (B15): the stamp taken, the newest published before its window's end goes on.
        val home = apodColumns(setOf(HOME))
        val one = due(home, mapOf(HOME to Record(APOD, pic(APOD), clock)), newer, now = t0 + h1)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), one.sets)
        assertEquals(t0, one.screens.getValue(HOME).record.stamp)
        val late = stored(posts + (APOD to post(APOD, d2, published = t0 + h3)))
        assertEquals(emptyList(), due(home, mapOf(HOME to Record(APOD, pic(APOD), clock)), late, now = t0 + h3 + h1).sets)
        // No stamp at all (a record from before turns): the same.
        assertEquals(emptyList(), due(home, mapOf(HOME to Record(APOD, pic(APOD))), late, now = t0 + h3 + h1).sets)
        assertEquals(emptyList(), due(cols(all, emptyList()), mapOf(HOME to Record(APOD, pic(APOD))), newer).sets)
    }

    @Test
    fun aStampInTheFutureCountsAsTheCurrentChangeTime() {
        // The clock set back.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 + day))
        val plan = due(cols(all, emptyList()), records, now = t0 + h1)
        assertEquals(emptyList(), plan.sets)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
    }

    // Equal columns change together (B2)

    @Test
    fun equalColumnsMatchAfterASetFromColumnsThatDiffered() {
        // APOD on Home, NASA on Lock, set.
        val before = cols(listOf(APOD), listOf(NASA))
        var records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(NASA, pic(NASA), t0))
        // Both on Both with turns, Set.
        val after = cols(listOf(APOD, NASA))
        val at = t0 + h1
        records = apply(records, set(before, after, records, at).plan)
        val afterSet = due(after, records, now = at)
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), afterSet.sets)
        records = apply(records, afterSet)
        // The next two changes: both screens move on together, one set each time.
        val turn1 = due(after, records, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), turn1.sets)
        records = apply(records, turn1)
        val newApod = stored(posts + (APOD to post(APOD, d2)))
        val turn2 = due(after, records, newApod, now = t0 + h3 + h3)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME, LOCK)), turn2.sets)
        assertEquals(apply(records, turn2).getValue(HOME), apply(records, turn2).getValue(LOCK))
    }

    @Test
    fun aFailedSetOnTheLockScreenRetriesHomesPicture() {
        // At the change NASA was shown, so home went APOD → Earth; the lock screen's set failed.
        // NASA has a new picture now: the lock screen still takes Earth, as home did.
        val notes = shown(pic(APOD), pic(EARTH))
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0, notes), LOCK to Record(APOD, pic(APOD), t0 - h3))
        val plan = due(cols(all), records, stored(posts + (NASA to post(NASA, d2))))
        assertEquals(Due(Record(EARTH, pic(EARTH), t0, notes), true), plan.screens.getValue(LOCK))
        assertEquals(listOf(pic(EARTH) to setOf(LOCK)), plan.sets)
        assertNull(plan.screens[HOME])
    }

    @Test
    fun equalColumnsFollowHomesPictureNotTheNewest() {
        // Home keeps yesterday's APOD after the window; the lock screen, set again, takes the same.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))), LOCK to Record(APOD))
        // The newer post came out after the window (B15: by its publish time).
        val plan = due(cols(listOf(APOD)), records, stored(posts + (APOD to post(APOD, d2, published = t0 + h3 + min1))), now = t0 + h3 + h1)
        assertEquals(listOf(pic(APOD) to setOf(LOCK)), plan.sets)
    }

    @Test
    fun equalColumnsRetryTheLockScreensFailedSetAfterTheWindow() {
        // In the window home got today's APOD, the lock screen's set failed (its record stays); after it the retry still sets it.
        val records = mapOf(HOME to Record(APOD, pic(APOD, d2), t0, shown(pic(APOD, d2))), LOCK to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD))))
        val plan = due(cols(listOf(APOD)), records, stored(posts + (APOD to post(APOD, d2))), now = t0 + h3 + h1)
        assertEquals(listOf(pic(APOD, d2) to setOf(LOCK)), plan.sets)
        assertNull(plan.screens[HOME])
    }

    @Test
    fun equalColumnsWithoutAHomeRecordStartOnTheLockScreens() {
        val records = mapOf(LOCK to Record(NASA, pic(NASA), t0))
        val plan = due(cols(all), records)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(Record(NASA, pic(NASA), t0), plan.screens.getValue(HOME).record.bare())
    }

    @Test
    fun equalColumnsShareAStandIn() {
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.NASA)
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), due(cols(listOf(APOD)), records, video).sets)
    }

    @Test
    fun columnsThatDifferKeepTheirOwnOwners() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        // Home's change: it moves on; the lock screen with APOD alone changes only at 06:00.
        val plan = due(cols(listOf(APOD, NASA), listOf(APOD)), records, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t0), plan.screens.getValue(LOCK).record.bare())
        // Two columns with turns that differ: each from its own owner.
        val apart = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(NASA, pic(NASA), t0))
        val turn = due(cols(all, listOf(APOD, NASA)), apart, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME), pic(APOD) to setOf(LOCK)), turn.sets)
    }

    @Test
    fun aFailedSetIsRetriedOnThatScreenOnly() {
        // Columns that differ; at the change Home's set went through, Lock's failed.
        val columns = cols(listOf(APOD, NASA), listOf(APOD, EARTH))
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA))), LOCK to Record(APOD, pic(APOD), t0 - h3, shown(pic(APOD))))
        val plan = due(columns, records, now = t0 + min1)
        assertEquals(listOf(pic(EARTH) to setOf(LOCK)), plan.sets)
        assertEquals(Record(EARTH, pic(EARTH), t0), plan.bare(LOCK).record)
        assertNull(plan.screens[HOME])
    }

    @Test
    fun aColumnWithNothingToShowSetsNoUntickedSource() {
        // CR finding 1: APOD on Both, Set to NASA and Earth while neither has a picture.
        val before = cols(listOf(APOD))
        val after = cols(listOf(NASA, EARTH))
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val onlyApod = stored(files = setOf(APOD))
        val at = t0 + h1
        val forgotten = apply(records, set(before, after, records, at, onlyApod).plan)
        assertTrue(forgotten.values.all { it.owner in listOf(NASA, EARTH) })
        val plan = due(after, forgotten, onlyApod, now = at)
        assertEquals(emptyList(), plan.sets)
        assertTrue(plan.screens.values.all { it.record.owner in listOf(NASA, EARTH) })
        // A new APOD post later doesn't go on either.
        val newApod = stored(posts + (APOD to post(APOD, d2)), files = setOf(APOD))
        assertEquals(emptyList(), due(after, apply(forgotten, plan), newApod, now = at + h1).sets)
        assertEquals(emptyList(), due(after, apply(forgotten, plan), newApod, now = t0 + h3).sets)
        // Columns that differ, at the change: the same.
        val apart = mapOf(HOME to Record(APOD, pic(APOD), t0))
        assertEquals(emptyList(), due(cols(listOf(NASA, EARTH), emptyList()), apart, onlyApod, now = t0 + h3).sets)
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
        val record = Record(APOD, pic(APOD), t0, shown(pic(APOD)))
        assertEquals(emptyMap(), due(cols(listOf(APOD, NASA)), mapOf(HOME to record, LOCK to record)).screens)
    }

    @Test
    fun aRecordWithoutAUrlMatchesByDate() {
        // 0.5.0's prefs from 0.4.0: the date alone.
        val records = mapOf(HOME to Record(APOD, Picture.Of(APOD, d1, null), t0))
        val plan = due(apodColumns(setOf(HOME)), records)
        assertEquals(emptyList(), plan.sets)
        assertEquals(pic(APOD), plan.screens.getValue(HOME).record.picture)
    }

    // Starting

    @Test
    fun aForgottenRecordWithTurnsGetsTheSameOwnersPicture() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0).forget())
        assertEquals(Record(NASA, null, t0), records.getValue(HOME))
        val plan = due(cols(all, emptyList()), records)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
    }

    @Test
    fun aForgottenRecordWhoseOwnerHasNothingToShowGivesWay() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(EARTH))).forget())
        val plan = due(cols(all, emptyList()), records, stored(files = setOf(APOD, EARTH)))
        // The next new one after NASA: Earth is shown, so APOD.
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        // None new: the next with any picture.
        val seen = mapOf(HOME to Record(NASA, null, t0, shown(pic(APOD), pic(EARTH))))
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), due(cols(all, emptyList()), seen, stored(files = setOf(APOD, EARTH))).sets)
    }

    @Test
    fun aScreenWithNoRecordStartsOnTheOtherScreensSource() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0))
        val ticked = due(cols(all, listOf(APOD, NASA)), records)
        assertEquals(Due(Record(NASA, pic(NASA), t0), true), ticked.bare(LOCK))
        // Not ticked there: the first ticked source with a picture.
        val first = due(cols(all, listOf(APOD, EARTH)), records)
        assertEquals(Record(APOD, pic(APOD), t0), first.bare(LOCK).record)
        val noApod = due(cols(all, listOf(APOD, EARTH)), records, stored(files = setOf(NASA, EARTH)))
        assertEquals(Record(EARTH, pic(EARTH), t0), noApod.bare(LOCK).record)
    }

    // Set: the pictures forgotten, the change times kept

    @Test
    fun setForgetsThePicturesAndTakesTheCurrentChangeTime() {
        // Lock is still due a retry from the change before.
        val stale = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA))), LOCK to Record(APOD, pic(APOD), t0 - h3))
        val restart = set(cols(all), cols(all), stale, t0 + h1)
        assertEquals(emptySet(), restart.drop)
        assertEquals(Record(NASA, null, t0, shown(pic(NASA))), restart.plan.screens.getValue(HOME).record)
        assertEquals(Record(APOD, null, t0), restart.plan.screens.getValue(LOCK).record)
        assertTrue(restart.plan.screens.values.none { it.set })
        // At once: one wallpaper, home's owner's picture again.
        val records = apply(stale, restart.plan)
        val now = due(cols(all), records, now = t0 + h1)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), now.sets)
        // The next run doesn't move it on; the next change time does.
        val after = apply(records, now)
        assertEquals(emptyList(), due(cols(all), after, now = t0 + h1 + min1).sets)
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), due(cols(all), after, now = t0 + h3).sets)
    }

    @Test
    fun setLateInATurnKeepsTheChangeTimes() {
        // Set at 08:59: the change at 09:00 comes as always.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val restart = set(cols(all, emptyList()), cols(all, emptyList()), records, t0 + h3 - min1)
        assertEquals(t0, restart.plan.screens.getValue(HOME).record.stamp)
        val afterSet = apply(records, restart.plan)
        val plan = due(cols(all, emptyList()), afterSet, now = t0 + h3 - min1)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(all, emptyList()), apply(afterSet, plan), now = t0 + h3).sets)
        // A screen with one source takes 06:00, its own change time.
        val one = set(apodColumns(setOf(HOME)), apodColumns(setOf(HOME)), records, t0 + h3 + h3)
        assertEquals(t0, one.plan.screens.getValue(HOME).record.stamp)
        // And a waiting screen stops waiting: it takes the current change time.
        val waited = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD), pic(NASA), pic(EARTH))))
        assertTrue(waiting(cols(all, emptyList()), waited, time, zone, t0 + h1))
        val unwait = apply(waited, set(cols(all, emptyList()), cols(all, emptyList()), waited, t0 + h1).plan)
        assertFalse(waiting(cols(all, emptyList()), unwait, time, zone, t0 + h1))
    }

    @Test
    fun setWithTheOwnerUntickedGoesToTheNextNewSource() {
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA), pic(EARTH))))
        val after = cols(listOf(APOD, EARTH), emptyList())
        val restart = set(cols(all, emptyList()), after, records, t0 + h1)
        // After NASA: Earth is shown, so APOD.
        assertEquals(Record(APOD, null, t0, shown(pic(NASA), pic(EARTH))), restart.plan.screens.getValue(HOME).record)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(after, apply(records, restart.plan)).sets)
        // None new: the next with any picture.
        val seen = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(APOD), pic(EARTH))))
        assertEquals(EARTH, set(cols(all, emptyList()), after, seen, t0 + h1).plan.screens.getValue(HOME).record.owner)
        // The owner still ticked: kept.
        assertEquals(NASA, set(cols(all, emptyList()), cols(listOf(NASA, EARTH), emptyList()), records, t0 + h1).plan.screens.getValue(HOME).record.owner)
    }

    @Test
    fun setPutsTheSamePictureOnAgainSoAChangedFitOrOwnPictureShows() {
        val record = Record(APOD, pic(APOD), t0, shown(pic(APOD)))
        val records = mapOf(HOME to record, LOCK to record)
        assertEquals(emptyList(), due(cols(listOf(APOD)), records).sets)
        val restart = set(cols(listOf(APOD)), cols(listOf(APOD)), records, t0 + h1)
        val plan = due(cols(listOf(APOD)), apply(records, restart.plan), now = t0 + h1)
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), plan.sets)
    }

    @Test
    fun setLeavesAScreenWithAnEmptyColumnAndItsRecordAlone() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        val restart = set(cols(listOf(APOD)), cols(listOf(NASA), emptyList()), records, t0 + h1)
        assertEquals(setOf(HOME), restart.plan.screens.keys)
        val plan = due(cols(listOf(NASA), emptyList()), apply(records, restart.plan), now = t0 + h1)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertNull(plan.screens[LOCK])
    }

    @Test
    fun aColumnTickedAgainAtSetStartsOnTheOtherScreensSource() {
        // Home was emptied while its record was on Earth; Lock takes turns and is on NASA.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to Record(NASA, pic(NASA), t0))
        val restart = set(cols(emptyList(), all), cols(all), records, t0 + h1)
        assertEquals(setOf(HOME), restart.drop)
        assertEquals(setOf(LOCK), restart.plan.screens.keys)
        val after = apply(records, restart.plan) - restart.drop
        val plan = due(cols(all), after, now = t0 + h1)
        // One wallpaper: Home starts on Lock's source with the current change time.
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), plan.sets)
        assertEquals(Record(NASA, pic(NASA), t0), plan.bare(HOME).record)
    }

    @Test
    fun aColumnTickedAgainAtSetWithoutTheOtherScreensSourceStartsOnItsFirstWithAPicture() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val restart = set(cols(emptyList(), listOf(APOD)), cols(listOf(NASA, EARTH), listOf(APOD)), records, t0 + h1)
        val after = apply(records, restart.plan) - restart.drop
        val plan = due(cols(listOf(NASA, EARTH), listOf(APOD)), after, stored(files = setOf(APOD, EARTH)), now = t0 + h1)
        assertEquals(Record(EARTH, pic(EARTH), t0), plan.bare(HOME).record)
        assertEquals(listOf(pic(APOD) to setOf(LOCK), pic(EARTH) to setOf(HOME)), plan.sets)
    }

    // The stand-in

    private val videoDay = posts + (APOD to post(APOD, d2, video = true))

    @Test
    fun theStandInWithApodAlone() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day))
        val home = apodColumns(setOf(HOME))
        assertEquals(emptyList(), due(home, records, stored(videoDay, StandIn.SKIP), now = t0).sets)
        for ((choice, picture) in listOf(StandIn.NASA to pic(NASA), StandIn.EARTH to pic(EARTH), StandIn.OWN to Picture.Own)) {
            val plan = due(home, records, stored(videoDay, choice), now = t0)
            assertEquals(Due(Record(APOD, picture, t0), true), plan.bare(HOME), "$choice")
        }
        // Nothing stored for it yet: kept.
        assertEquals(emptyList(), due(home, records, stored(videoDay, StandIn.OWN, own = false), now = t0).sets)
        assertEquals(emptyList(), due(home, records, stored(videoDay - NASA, StandIn.NASA), now = t0).sets)
    }

    @Test
    fun theStandInWithTurns() {
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(EARTH))))
        val home = cols(listOf(APOD, EARTH), emptyList())
        for ((choice, picture) in listOf(StandIn.NASA to pic(NASA), StandIn.OWN to Picture.Own)) {
            val plan = due(home, records, stored(videoDay, choice), now = t0)
            // APOD's turn, the stand-in's picture.
            assertEquals(Record(APOD, picture, t0), plan.bare(HOME).record, "$choice")
        }
        // "Skip it", or Earth standing in with Earth's picture shown (B15): APOD has nothing new,
        // Earth's is shown: the screen waits.
        assertEquals(emptyMap(), due(home, records, stored(videoDay, StandIn.SKIP), now = t0).screens)
        assertEquals(emptyMap(), due(home, records, stored(videoDay, StandIn.EARTH), now = t0).screens)
    }

    @Test
    fun aStandInAlreadyShownOnTheScreenPassesApodsTurnOn() {
        // B15, finding 3: NASA stands in, and this screen showed NASA's picture: APOD's turn passes
        // on, as with "Skip it", to the next new source.
        val video = stored(videoDay + (EARTH to post(EARTH, d2)), StandIn.NASA)
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0 - h3, shown(pic(NASA), pic(EARTH))))
        val columns = cols(listOf(APOD, NASA, EARTH), emptyList())
        assertEquals(listOf(pic(EARTH, d2) to setOf(HOME)), due(columns, records, video, now = t0).sets)
        // Nothing else new: the screen waits rather than putting NASA's picture on again.
        val waits = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(NASA), pic(EARTH))))
        val nothingNew = stored(videoDay, StandIn.NASA)
        assertEquals(emptyMap(), due(cols(listOf(APOD, EARTH), emptyList()), waits, nothingNew, now = t0).screens)
        assertTrue(waiting(cols(listOf(APOD, EARTH), emptyList()), waits, time, zone, t0))
        // Not shown yet: APOD's turn with NASA's picture.
        val fresh = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(EARTH))))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(listOf(APOD, EARTH), emptyList()), fresh, nothingNew, now = t0).sets)
    }

    @Test
    fun aStandInShownCountsAsShownForItsOwnSource() {
        val video = stored(videoDay, StandIn.NASA)
        val home = cols(all, emptyList())
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(APOD), pic(EARTH))))
        val apodTurn = due(home, records, video, now = t0)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), apodTurn.sets)
        // Noted: APOD's video post and NASA's picture.
        val noted = apodTurn.screens.getValue(HOME).record
        assertEquals(Record(APOD, pic(NASA), t0, shown(post(APOD, d2, video = true).picture(), pic(NASA), pic(EARTH))), noted)
        // The next change: NASA isn't put on twice, nothing else is new: the screen waits.
        assertEquals(emptyMap(), due(home, apply(records, apodTurn), video, now = t0 + h3).screens)
        // An APOD video in the window: the stand-in replaces APOD's picture (APOD went on).
        val apodOn = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, apodOn, video, now = t0 + h1).sets)
    }

    @Test
    fun aStandInAlreadyShownDoesntGoOnWithApodAlone() {
        // B22 (V12b): APOD alone, NASA for video days, and this screen has shown NASA's post: the
        // run sets nothing, as with "Skip it" (the old code put NASA's picture on again).
        val video = stored(videoDay, StandIn.NASA)
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD), pic(NASA))))
        val home = apodColumns(setOf(HOME))
        for (now in listOf(t0, t0 + h1, t0 + h3 + h1)) assertEquals(emptyList(), due(home, records, video, now = now).sets, "$now")
        // Both screens ("Where" Both): neither is set.
        val both = mapOf(HOME to records.getValue(HOME), LOCK to records.getValue(HOME))
        assertEquals(emptyList(), due(apodColumns(setOf(HOME, LOCK)), both, video, now = t0).sets)
        // Earth stands in, not shown yet: it goes on.
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), due(home, records, stored(videoDay, StandIn.EARTH), now = t0).sets)
        // A newer NASA post, published in the window: it goes on by the usual rules.
        val newer = stored(videoDay + (NASA to post(NASA, d2)), StandIn.NASA)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), due(home, records, newer, now = t0).sets)
    }

    @Test
    fun aStandInNotShownGoesOnOnceWithApodAlone() {
        // The day the stand-in goes on doesn't change: not noted, it goes on at the change time,
        // then counts as shown and stays.
        val video = stored(videoDay, StandIn.NASA)
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        val first = due(home, records, video, now = t0)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), first.sets)
        val after = apply(records, first)
        assertEquals(Record(APOD, pic(NASA), t0, shown(post(APOD, d2, video = true).picture(), pic(NASA))), after.getValue(HOME))
        for (now in listOf(t0 + h1, t0 + h3 + h1)) {
            val plan = due(home, after, video, now = now)
            assertEquals(emptyList(), plan.sets, "$now")
            assertEquals(after.getValue(HOME), apply(after, plan).getValue(HOME), "$now")
        }
    }

    @Test
    fun aPullSetAForgottenPictureAndANewScreenPutAShownStandInOn() {
        // B22: they put the newest on at once, whatever it was.
        val video = stored(videoDay, StandIn.NASA)
        val notes = shown(pic(APOD), pic(NASA))
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, notes))
        val home = apodColumns(setOf(HOME))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, records, video, now = t0 + h1, pull = true).sets)
        // Set: restartAndForget, then the run.
        val restart = set(home, home, records, t0 + h1, video)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, apply(records, restart.plan), video, now = t0 + h1).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, mapOf(HOME to Record(APOD, null, t0, notes)), video, now = t0 + h1).sets)
        // A new screen (the lock screen with no record).
        val lockToo = apodColumns(setOf(HOME, LOCK))
        assertEquals(listOf(pic(NASA) to setOf(LOCK)), due(mapOf(HOME to emptyList(), LOCK to listOf(APOD)), emptyMap(), video, now = t0 + h1).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), due(lockToo, emptyMap(), video, now = t0 + h1).sets)
        // The user's own picture always stands in.
        assertEquals(listOf(Picture.Own to setOf(HOME)), due(home, records, stored(videoDay, StandIn.OWN), now = t0).sets)
    }

    @Test
    fun aStandInAlreadyShownDoesntReplaceApodsTurnInTheWindow() {
        // B22 with turns: APOD's turn, APOD's picture on, the video comes out in the window and
        // NASA's picture was shown: the screen keeps APOD's (it did put NASA on before).
        val video = stored(videoDay, StandIn.NASA)
        val columns = cols(listOf(APOD, EARTH), emptyList())
        val apodOn = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA))))
        assertEquals(emptyList(), due(columns, apodOn, video, now = t0 + h1).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(columns, apodOn, video, now = t0 + h1, pull = true).sets)
    }

    // The page's video notice: the stand-in skipped (B22)

    @Test
    fun aStandInShownBeforeAndNotOnIsSkipped() {
        val video = stored(videoDay, StandIn.NASA)
        val notes = shown(pic(APOD), pic(NASA))
        assertTrue(standInSkipped(listOf(APOD), Record(APOD, pic(APOD), t0, notes), video))
        // With turns, the turn passed on to Earth.
        assertTrue(standInSkipped(listOf(APOD, EARTH), Record(EARTH, pic(EARTH), t0, notes + shown(pic(EARTH))), video))
    }

    @Test
    fun aStandInOnOrNotShownIsNotSkipped() {
        val video = stored(videoDay, StandIn.NASA)
        val notes = shown(pic(APOD), pic(NASA))
        // Not shown yet: it takes APOD's place at the next run.
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, pic(APOD), t0, shown(pic(APOD))), video))
        // On the screen now.
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, pic(NASA), t0, notes), video))
        // It filled APOD's turn this video day and the turn moved on since.
        val filled = notes + shown(post(APOD, d2, video = true).picture(), pic(EARTH))
        assertFalse(standInSkipped(listOf(APOD, EARTH), Record(EARTH, pic(EARTH), t0 + h3, filled), video))
        // Forgotten by Set, or no record: the next run puts it on.
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, null, t0, notes), video))
        assertFalse(standInSkipped(listOf(APOD), null, video))
        // APOD not ticked there, an image day, "Skip it", the user's own picture.
        assertFalse(standInSkipped(listOf(EARTH), Record(EARTH, pic(EARTH), t0, notes), video))
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, pic(APOD), t0, notes), stored(standIn = StandIn.NASA)))
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, pic(APOD), t0, notes), stored(videoDay, StandIn.SKIP)))
        assertFalse(standInSkipped(listOf(APOD), Record(APOD, pic(APOD), t0, notes), stored(videoDay, StandIn.OWN)))
    }

    // The page's "video day, wallpaper kept"

    @Test
    fun aVideoDayKeepsAnApodScreenWithNothingStandingIn() {
        val kept = Record(APOD, pic(APOD))
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.SKIP)))
        // "Use my picture" with none picked, NASA not stored yet.
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.OWN, own = false)))
        // B23's already-shown case (B22): NASA was on before and isn't now.
        assertTrue(videoDayKept(listOf(APOD), Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA))), stored(videoDay, StandIn.NASA)))
    }

    @Test
    fun aStandInThatWillGoOnIsNoKeptPicture() {
        // B23: the stand-in stored but not on yet (the next check or change time puts it on).
        val kept = Record(APOD, pic(APOD), t0, shown(pic(APOD)))
        assertFalse(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.NASA)))
        assertFalse(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.OWN)))
        // Still loading ("… takes its place once it has loaded.").
        assertFalse(videoDayKept(listOf(APOD), kept, stored(videoDay - NASA, StandIn.NASA)))
        // Forgotten by Set: the stand-in goes on.
        assertFalse(videoDayKept(listOf(APOD), Record(APOD), stored(videoDay, StandIn.OWN), everSet = true))
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
