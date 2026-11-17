package io.github.buerlino.apodroid.core

import io.github.buerlino.apodroid.core.Screen.HOME
import io.github.buerlino.apodroid.core.Screen.LOCK
import io.github.buerlino.apodroid.core.Source.APOD
import io.github.buerlino.apodroid.core.Source.EARTH
import io.github.buerlino.apodroid.core.Source.NASA
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        soon: Boolean = false,
    ) = dueWallpapers(columns, records, stored, time, zone, now, pull, soon)

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

    /** t0's number ([PictureTime.turn]) is a multiple of 6: APOD's turn with two or three sources; each 3 h the next. */
    @Test
    fun severalSourcesGoToTheTurnsSourceByTheClock() {
        assertEquals(0L, Math.floorMod(time.turn(t0, zone), 6L))
        for ((k, source) in listOf(APOD, NASA, EARTH, APOD, NASA).withIndex()) {
            val change = t0 + h3.multipliedBy(k.toLong())
            // Whatever was on before, and in whichever order.
            val before = mapOf(HOME to Record(EARTH, pic(EARTH), change - h3))
            val plan = due(cols(all, emptyList()), before, stored(posts + (EARTH to post(EARTH, d2))), now = change + min1)
            val expected = if (source == EARTH) pic(EARTH, d2) else pic(source)
            assertEquals(listOf(expected to setOf(HOME)), plan.sets, "$k")
            assertEquals(source, plan.screens.getValue(HOME).record.owner, "$k")
            assertEquals(change, plan.screens.getValue(HOME).record.stamp, "$k")
        }
        // Two sources: APOD, NASA, APOD.
        for ((k, source) in listOf(APOD, NASA, APOD).withIndex()) {
            val change = t0 + h3.multipliedBy(k.toLong())
            val before = mapOf(HOME to Record(EARTH, pic(EARTH), change - h3))
            assertEquals(listOf(pic(source) to setOf(HOME)), due(cols(listOf(APOD, NASA), emptyList()), before, now = change).sets, "$k")
        }
    }

    @Test
    fun aPictureShownBeforeGoesOnAgainAtItsTurn() {
        // RO1: the screen showed every source's picture; NASA's turn puts NASA's on again.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val plan = due(cols(all, emptyList()), records, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(Record(NASA, pic(NASA), t0 + h3), plan.screens.getValue(HOME).record.bare())
    }

    @Test
    fun aMissedChangeTimeIsOneChange() {
        // The phone was off for a day: one change, to the current change time's turn (NASA's), not
        // one per missed time, nor the next after the one on (APOD).
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - day, shown(pic(EARTH))))
        val plan = due(cols(all, emptyList()), records, now = t0 + h3 + min1)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(t0 + h3, plan.screens.getValue(HOME).record.stamp)
    }

    @Test
    fun aSourceWithNothingToShowPassesTheTurn() {
        // t0 is APOD's turn. Its video with "Skip it", or nothing stored: the next with a picture.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3))
        val video = stored(posts + (APOD to post(APOD, d2, video = true)))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(all, emptyList()), records, video, now = t0).sets)
        assertEquals(NASA, due(cols(all, emptyList()), records, video, now = t0).screens.getValue(HOME).record.owner)
        val noApod = stored(posts - APOD)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(all, emptyList()), records, noApod, now = t0).sets)
        // Round to the one on: only Earth has a picture, on already.
        val onlyEarth = stored(mapOf(EARTH to post(EARTH)))
        assertEquals(emptyList(), due(cols(all, emptyList()), records, onlyEarth, now = t0).sets)
        // None has one: the screen keeps its picture and takes the change time, the turn APOD's.
        val none = stored(emptyMap())
        val plan = due(cols(all, emptyList()), records, none, now = t0)
        assertEquals(emptyList(), plan.sets)
        assertEquals(Record(APOD, pic(EARTH), t0), plan.screens.getValue(HOME).record.bare())
        // APOD's picture stored later in the window, published in it: it goes on (rule 2).
        val later = stored(mapOf(APOD to post(APOD, d2, published = t0 + Duration.ofMinutes(5))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(cols(all, emptyList()), apply(records, plan), later, now = t0 + h1).sets)
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
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val home = cols(all, emptyList())
        // A new Earth post waits for Earth's turn, two change times on (NASA's comes first).
        val newEarth = stored(posts + (EARTH to post(EARTH, d2)))
        assertEquals(emptyList(), due(home, records, newEarth, now = t0 + h1).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, records, newEarth, now = t0 + h3).sets)
        assertEquals(listOf(pic(EARTH, d2) to setOf(HOME)), due(home, records, newEarth, now = t0 + h3 + h3).sets)
        // A newer APOD post replaces it.
        val newApod = stored(posts + (APOD to post(APOD, d2)))
        val plan = due(home, records, newApod, now = t0 + h1)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), plan.sets)
        assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
    }

    // The FP6's day (rotation-0.7.0.md, Why)

    /**
     * 11 Nov 2026 on the FP6: APOD and NASA on both screens, every 24 h until a Set at 07:19 to
     * every 6 h; NASA posted nothing on 11 Nov (US Veterans Day). 0.6.0's rules waited at 18:00
     * (APOD's NGC 4449 shown that morning, NASA's CHAPEA at 12:00, nothing new); the rotation
     * puts APOD on at 18:00, as the user expected.
     */
    @Test
    fun theFp6sDayOf11NovPutsApodOnAt1800() {
        val zurich = ZoneId.of("Europe/Zurich")
        val sources = listOf(APOD, NASA)
        val posts = listOf(apod("2026-11-09"), apod("2026-11-10"), apod("2026-11-11"), apod("2026-11-12"), nasa("2026-11-09"), nasa("2026-11-10"))
        // Tuesday was NASA's day (every 24 h): Monday's NASA post on (Tuesday's, CHAPEA, came out at
        // 17:30, after the window), APOD last shown on Monday.
        val tuesday = Record(NASA, pic(nasa("2026-11-09")), at(zurich, "2026-11-10T06:00"), notes(apod("2026-11-09"), nasa("2026-11-09")))
        val phone = Phone(zurich, mapOf(HOME to sources, LOCK to sources), PictureTime(6, 24), posts, mapOf(HOME to tuesday, LOCK to tuesday))
        phone.run(at(zurich, "2026-11-11T05:00"), at(zurich, "2026-11-11T07:19"))
        phone.time = PictureTime(6, 6)
        phone.set(at(zurich, "2026-11-11T07:19"))
        phone.run(at(zurich, "2026-11-11T07:20"), at(zurich, "2026-11-12T01:00"))
        assertEquals(
            listOf(
                // Every 24 h, 11 Nov is APOD's day: yesterday's at 06:00, NGC 4449 at 06:15.
                "11 06:00 APOD 11-10", "11 06:15 APOD 11-11",
                // The Set to every 6 h: 06:00 is APOD's turn there too, the same picture again.
                "11 07:19 APOD 11-11",
                // NASA's CHAPEA, APOD's NGC 4449 again, CHAPEA again.
                "11 12:00 NASA 11-10", "11 18:00 APOD 11-11", "12 00:00 NASA 11-10",
            ),
            phone.sets,
        )
    }

    // No waiting (RO1: 0.6.0's rule 3 went)

    @Test
    fun withNothingNewTheScreenDoesntWait() {
        // APOD's turn at t0 with its picture on already: nothing set, the change time taken.
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - h3, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val home = cols(all, emptyList())
        val plan = due(home, records, now = t0)
        assertEquals(emptyList(), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t0), plan.screens.getValue(HOME).record.bare())
        // A new NASA post at 07:30 waits for NASA's turn, at the next change time.
        val newNasa = stored(posts + (NASA to post(NASA, d2)))
        assertEquals(emptyList(), due(home, apply(records, plan), newNasa, now = t0 + h1 + h1).sets)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), due(home, apply(records, plan), newNasa, now = t0 + h3).sets)
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
    fun equalColumnsWithoutAHomeRecordStartOnTheTurnsSource() {
        // RO1: home starts by the clock (APOD's turn at t0); the lock screen takes home's.
        val records = mapOf(LOCK to Record(APOD, pic(APOD), t0))
        val plan = due(cols(all), records)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t0), plan.screens.getValue(HOME).record.bare())
        // A lock screen on another source (a record from 0.6.0's rules): both go to the turn's.
        val other = due(cols(all), mapOf(LOCK to Record(NASA, pic(NASA), t0)))
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), other.sets)
    }

    @Test
    fun equalColumnsShareAStandIn() {
        val video = stored(posts + (APOD to post(APOD, d2, video = true)), StandIn.NASA)
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(EARTH, pic(EARTH), t0))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), due(cols(listOf(APOD)), records, video).sets)
    }

    @Test
    fun columnsThatDifferEachTakeTheirTurnByTheClock() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        // Home's change: it moves on; the lock screen with APOD alone changes only at 06:00.
        val plan = due(cols(listOf(APOD, NASA), listOf(APOD)), records, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t0), plan.screens.getValue(LOCK).record.bare())
        // Two columns with turns that differ: each by the number mod its own sources.
        val apart = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val columns = cols(all, listOf(APOD, EARTH))
        val turn = due(columns, apart, now = t0 + h3)
        assertEquals(listOf(pic(NASA) to setOf(HOME), pic(EARTH) to setOf(LOCK)), turn.sets)
        val next = due(columns, apply(apart, turn), now = t0 + h3 + h3)
        assertEquals(listOf(pic(EARTH) to setOf(HOME), pic(APOD) to setOf(LOCK)), next.sets)
    }

    @Test
    fun aFailedSetIsRetriedOnThatScreenOnly() {
        // Columns that differ; at NASA's change (t0 + 3 h) Home's set went through, Lock's failed.
        val columns = cols(listOf(APOD, NASA), listOf(APOD, EARTH))
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0 + h3, shown(pic(NASA))), LOCK to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val plan = due(columns, records, now = t0 + h3 + min1)
        assertEquals(listOf(pic(EARTH) to setOf(LOCK)), plan.sets)
        assertEquals(Record(EARTH, pic(EARTH), t0 + h3), plan.bare(LOCK).record)
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

    // EF1f1

    @Test
    fun theRealPictureForgetsOnlyTheScreensShowingThePost() {
        val earth = pic(EARTH)
        val records = mapOf(
            HOME to Record(EARTH, earth, t0),
            LOCK to Record(NASA, pic(NASA), t0),
        )
        val forgotten = forgetShowing(records, earth)
        assertEquals(mapOf(HOME to Record(EARTH, null, t0)), forgotten)
    }

    @Test
    fun theRealPictureKeepsAScreenWithoutAColumn() {
        val earth = pic(EARTH)
        val r = Record(EARTH, earth, t0)
        assertEquals(mapOf(HOME to r.forget()), forgetShowing(mapOf(HOME to r, LOCK to r), earth, setOf(HOME)))
    }

    @Test
    fun theRealPictureKeepsRecordsOfOtherDatesAndUrls() {
        val earth = pic(EARTH)
        val otherDate = Record(EARTH, pic(EARTH, d2), t0)
        val otherUrl = Record(EARTH, earth.copy(url = "https://x/other.jpg"), t0)
        assertEquals(emptyMap(), forgetShowing(mapOf(HOME to otherDate, LOCK to otherUrl), earth))
    }

    @Test
    fun aRecordWithoutAUrlIsForgottenByDate() {
        val earth = pic(EARTH)
        val records = mapOf(HOME to Record(EARTH, Picture.Of(EARTH, d1, null), t0))
        assertEquals(mapOf(HOME to Record(EARTH, null, t0)), forgetShowing(records, earth))
    }

    @Test
    fun bothScreensShowingThePostAreForgottenWithOwnerStampAndNotesKept() {
        val earth = pic(EARTH)
        val r = Record(EARTH, earth, t0, shown(earth))
        val forgotten = forgetShowing(mapOf(HOME to r, LOCK to r), earth)
        assertEquals(mapOf(HOME to r.forget(), LOCK to r.forget()), forgotten)
        assertEquals(shown(earth), forgotten.getValue(HOME).shown)
        assertEquals(t0, forgotten.getValue(LOCK).stamp)
    }

    @Test
    fun aThumbnailIsReplacedOnlyByARealDownloadOfTheSameUrl() {
        assertTrue(thumbReplaced("u", "u", false))
        assertFalse(thumbReplaced("u", "u", true))
        assertFalse(thumbReplaced(null, "u", false))
        assertFalse(thumbReplaced("old", "u", false))
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
        // To the turn's source (APOD's at t0), else the next after it with a picture.
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(EARTH))).forget())
        val plan = due(cols(all, emptyList()), records, stored(files = setOf(APOD, EARTH)))
        assertEquals(listOf(pic(APOD) to setOf(HOME)), plan.sets)
        // NASA's turn (t0 + 3 h) without NASA's picture: Earth.
        val nasaTurn = mapOf(HOME to Record(NASA, null, t0 + h3))
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), due(cols(all, emptyList()), nasaTurn, stored(files = setOf(APOD, EARTH)), now = t0 + h3 + h1).sets)
    }

    @Test
    fun aScreenWithNoRecordStartsOnTheTurnsSource() {
        // RO1: by the clock, not on the other screen's source: t0 is APOD's turn.
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0))
        val ticked = due(cols(all, listOf(APOD, NASA)), records)
        assertEquals(Due(Record(APOD, pic(APOD), t0), true), ticked.bare(LOCK))
        // At NASA's turn: NASA.
        val later = due(cols(all, listOf(APOD, NASA)), mapOf(HOME to Record(NASA, pic(NASA), t0 + h3)), now = t0 + h3 + h1)
        assertEquals(Record(NASA, pic(NASA), t0 + h3), later.bare(LOCK).record)
        // The turn's source without a picture: the next with one.
        val noApod = due(cols(all, listOf(APOD, EARTH)), records, stored(files = setOf(NASA, EARTH)))
        assertEquals(Record(EARTH, pic(EARTH), t0), noApod.bare(LOCK).record)
    }


    // Set: the pictures forgotten, the change times kept

    @Test
    fun setForgetsThePicturesAndTakesTheCurrentChangeTime() {
        // Lock is still due a retry from the change before; home's owner isn't the clock's turn
        // (a record from 0.6.0's rules): both go to APOD's, t0's.
        val stale = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA))), LOCK to Record(APOD, pic(APOD), t0 - h3))
        val restart = set(cols(all), cols(all), stale, t0 + h1)
        assertEquals(emptySet(), restart.drop)
        assertEquals(Record(APOD, null, t0, shown(pic(NASA))), restart.plan.screens.getValue(HOME).record)
        assertEquals(Record(APOD, null, t0), restart.plan.screens.getValue(LOCK).record)
        assertTrue(restart.plan.screens.values.none { it.set })
        // At once: one wallpaper, the turn's picture.
        val records = apply(stale, restart.plan)
        val now = due(cols(all), records, now = t0 + h1)
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), now.sets)
        // The next run doesn't move it on; the next change time does.
        val after = apply(records, now)
        assertEquals(emptyList(), due(cols(all), after, now = t0 + h1 + min1).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), due(cols(all), after, now = t0 + h3).sets)
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
    }

    @Test
    fun setGoesToTheTurnsSource() {
        // RO1: Set never shifts the rotation; t0 is APOD's turn with two or three sources.
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA), pic(EARTH))))
        val after = cols(listOf(APOD, EARTH), emptyList())
        val restart = set(cols(all, emptyList()), after, records, t0 + h1)
        assertEquals(Record(APOD, null, t0, shown(pic(NASA), pic(EARTH))), restart.plan.screens.getValue(HOME).record)
        assertEquals(listOf(pic(APOD) to setOf(HOME)), due(after, apply(records, restart.plan)).sets)
        // NASA and Earth: NASA's turn (the number mod 2), the owner kept by chance.
        assertEquals(NASA, set(cols(all, emptyList()), cols(listOf(NASA, EARTH), emptyList()), records, t0 + h1).plan.screens.getValue(HOME).record.owner)
        // The turn's source without a picture: the next with one.
        val noApod = stored(files = setOf(NASA, EARTH))
        assertEquals(EARTH, set(cols(all, emptyList()), after, records, t0 + h1, noApod).plan.screens.getValue(HOME).record.owner)
        // A Set at NASA's turn with all three: NASA.
        val late = mapOf(HOME to Record(APOD, pic(APOD), t0 + h3))
        assertEquals(NASA, set(cols(all, emptyList()), cols(all, emptyList()), late, t0 + h3 + h1).plan.screens.getValue(HOME).record.owner)
    }

    /**
     * RO2's fix round (the tablet, 2026-11-14): a Set that changes "If APOD is a video" from
     * "Skip it" to Earth on APOD's video day, at APOD's turn. [restartAndForget] picks the owner by
     * the stand-in of the [Stored] it's given, so `Store.saveSetup` gives it the new setup's: with
     * the stored "Skip it" the turn passed to NASA again and NASA's picture went on, Earth's only
     * at the second Set.
     */
    @Test
    fun setWithANewStandInPicksTheOwnerByTheNewChoice() {
        val video = posts + (APOD to post(APOD, video = true))
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(NASA))))
        val columns = cols(all, emptyList())
        val skip = set(columns, columns, records, t0 + h1, stored(video, StandIn.SKIP))
        assertEquals(NASA, skip.plan.screens.getValue(HOME).record.owner)
        val earth = stored(video, StandIn.EARTH)
        val restart = set(columns, columns, records, t0 + h1, earth)
        assertEquals(APOD, restart.plan.screens.getValue(HOME).record.owner)
        val plan = due(columns, apply(records, restart.plan), earth, now = t0 + h1)
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), plan.sets)
    }

    @Test
    fun setPutsTheSamePictureOnAgainSoAChangedWholePictureOrOwnPictureShows() {
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
    fun aColumnTickedAgainAtSetStartsOnTheTurnsSource() {
        // Home was emptied while its record was on Earth; Lock takes turns and is on NASA.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0), LOCK to Record(NASA, pic(NASA), t0))
        val restart = set(cols(emptyList(), all), cols(all), records, t0 + h1)
        assertEquals(setOf(HOME), restart.drop)
        assertEquals(setOf(LOCK), restart.plan.screens.keys)
        val after = apply(records, restart.plan) - restart.drop
        val plan = due(cols(all), after, now = t0 + h1)
        // One wallpaper: both on the turn's source (APOD's, t0's) with the current change time.
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), plan.sets)
        assertEquals(Record(APOD, pic(APOD), t0), plan.bare(HOME).record)
    }

    @Test
    fun aColumnTickedAgainAtSetWhoseTurnHasNoPictureStartsOnTheNextWithOne() {
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
        // "Skip it": APOD has nothing to show, the turn passes to Earth, on already: nothing set,
        // the change time taken (no waiting).
        val skip = due(home, records, stored(videoDay, StandIn.SKIP), now = t0)
        assertEquals(Due(Record(EARTH, pic(EARTH), t0), false), skip.bare(HOME))
        // Earth standing in with Earth's picture on: APOD's turn, the same picture (RO1: B15's
        // "shown, so the turn passes" went).
        val earth = due(home, records, stored(videoDay, StandIn.EARTH), now = t0)
        assertEquals(Due(Record(APOD, pic(EARTH), t0), false), earth.bare(HOME))
    }

    @Test
    fun aStandInShownBeforeFillsApodsTurnAgain() {
        // RO1 (Open 2: B15's and B22's "not twice" went): NASA stands in, and this screen showed
        // NASA's picture: APOD's turn puts it on again.
        val video = stored(videoDay + (EARTH to post(EARTH, d2)), StandIn.NASA)
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(NASA), pic(EARTH))))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(all, emptyList()), records, video, now = t0).sets)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(cols(listOf(APOD, EARTH), emptyList()), records, video, now = t0).sets)
    }

    @Test
    fun aStandInMayShowAgainInItsOwnSourcesTurn() {
        val video = stored(videoDay, StandIn.NASA)
        val home = cols(all, emptyList())
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3, shown(pic(APOD), pic(EARTH))))
        val apodTurn = due(home, records, video, now = t0)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), apodTurn.sets)
        // Noted: APOD's video post and NASA's picture.
        val noted = apodTurn.screens.getValue(HOME).record
        assertEquals(Record(APOD, pic(NASA), t0, shown(post(APOD, d2, video = true).picture(), pic(NASA), pic(EARTH))), noted)
        // The next change is NASA's turn: the same picture, on already, nothing set; then Earth's.
        val nasaTurn = due(home, apply(records, apodTurn), video, now = t0 + h3)
        assertEquals(Due(Record(NASA, pic(NASA), t0 + h3), false), nasaTurn.bare(HOME))
        assertEquals(listOf(pic(EARTH) to setOf(HOME)), due(home, apply(apply(records, apodTurn), nasaTurn), video, now = t0 + h3 + h3).sets)
        // An APOD video in the window: the stand-in replaces APOD's picture (APOD went on).
        val apodOn = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(home, apodOn, video, now = t0 + h1).sets)
    }

    @Test
    fun aStandInShownBeforeGoesOnWithApodAlone() {
        // RO1 (Open 2): B22's "not twice" went. APOD alone, NASA for video days, and this screen
        // has shown NASA's post: it goes on again at the change time, once.
        val video = stored(videoDay, StandIn.NASA)
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0 - day, shown(pic(APOD), pic(NASA))))
        val home = apodColumns(setOf(HOME))
        val first = due(home, records, video, now = t0)
        assertEquals(listOf(pic(NASA) to setOf(HOME)), first.sets)
        assertEquals(emptyList(), due(home, apply(records, first), video, now = t0 + h1).sets)
        // Both screens ("Where" Both): one set.
        val both = mapOf(HOME to records.getValue(HOME), LOCK to records.getValue(HOME))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), due(apodColumns(setOf(HOME, LOCK)), both, video, now = t0).sets)
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
    fun aVideoInTheWindowPutsTheStandInOnInApodsTurn() {
        // With turns: APOD's turn, APOD's picture on, the video comes out in the window: the
        // stand-in replaces it, also one this screen has shown (RO1: B22 went).
        val video = stored(videoDay, StandIn.NASA)
        val columns = cols(listOf(APOD, EARTH), emptyList())
        val apodOn = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA))))
        assertEquals(listOf(pic(NASA) to setOf(HOME)), due(columns, apodOn, video, now = t0 + h1).sets)
    }


    // The page's "video day, wallpaper kept"

    @Test
    fun aVideoDayKeepsAnApodScreenWithNothingStandingIn() {
        val kept = Record(APOD, pic(APOD))
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.SKIP)))
        // "Use my picture" with none picked, NASA not stored yet.
        assertTrue(videoDayKept(listOf(APOD), kept, stored(videoDay, StandIn.OWN, own = false)))
        // B23's already-shown case (B22) went with RO1: NASA, on before, goes on again.
        assertFalse(videoDayKept(listOf(APOD), Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA))), stored(videoDay, StandIn.NASA)))
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

    // As soon as it's out (SO1, soon-0.7.0.md): rule 2 without the window

    /**
     * 12–13 Nov 2026 on the FP6 (soon-0.7.0.md, Why): APOD on home, NASA on lock, 06:00, Zurich.
     * A Set at 20:57 put NASA's "Heart in the Sky" (out 20:14) on the lock screen; 13 Nov's NASA
     * post came out at 17:41 and the page showed it at 19:24. Off (0.6.0's window): it waits for
     * 14 Nov's 06:00. On: the next hourly check after it, 18:15, puts it on; home as before.
     */
    @Test
    fun theFp6sDayOf13NovPutsNasaOnWithinTheHourWithSoon() {
        val zurich = ZoneId.of("Europe/Zurich")
        val heart = Out(at(zurich, "2026-11-12T20:14"), post(NASA, LocalDate.parse("2026-11-12"), at(zurich, "2026-11-12T20:14")))
        val outbursts = Out(at(zurich, "2026-11-13T17:41"), post(NASA, LocalDate.parse("2026-11-13"), at(zurich, "2026-11-13T17:41")))
        val posts = listOf(apod("2026-11-12"), apod("2026-11-13"), apod("2026-11-14"), nasa("2026-11-10"), heart, outbursts)
        val columns = mapOf(HOME to listOf(APOD), LOCK to listOf(NASA))
        fun day(soon: Boolean): List<String> {
            val records = mapOf(
                HOME to Record(APOD, pic(apod("2026-11-12")), at(zurich, "2026-11-12T06:00"), notes(apod("2026-11-12"))),
                LOCK to Record(NASA, pic(nasa("2026-11-10")), at(zurich, "2026-11-12T06:00"), notes(nasa("2026-11-10"))),
            )
            val phone = Phone(zurich, columns, PictureTime(6, 24), posts, records, soon = soon)
            phone.set(at(zurich, "2026-11-12T20:57"))
            phone.run(at(zurich, "2026-11-12T20:58"), at(zurich, "2026-11-14T07:00"), pages = listOf(at(zurich, "2026-11-13T19:24")))
            return phone.sets
        }
        val set = listOf("12 20:57 APOD 11-12 HOME", "12 20:57 NASA 11-12 LOCK")
        assertEquals(set + listOf("13 06:15 APOD 11-13 HOME", "14 06:00 NASA 11-13 LOCK", "14 06:15 APOD 11-14 HOME"), day(soon = false))
        assertEquals(set + listOf("13 06:15 APOD 11-13 HOME", "13 18:15 NASA 11-13 LOCK", "14 06:15 APOD 11-14 HOME"), day(soon = true))
    }

    @Test
    fun withSoonOneSourcesNewerPictureGoesOnWheneverARunSeesIt() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD))))
        val home = apodColumns(setOf(HOME))
        // Published after the window (and long after it): on at the next run, the change time kept.
        for (published in listOf(t0 + h3, t0 + Duration.ofHours(15))) {
            val late = stored(posts + (APOD to post(APOD, d2, published = published)))
            assertEquals(emptyList(), due(home, records, late, now = published + min1).sets, "off")
            val plan = due(home, records, late, now = published + min1, soon = true)
            assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), plan.sets, "$published")
            assertEquals(t0, plan.screens.getValue(HOME).record.stamp)
            // Once on, nothing more.
            assertEquals(emptyList(), due(home, apply(records, plan), late, now = published + h1, soon = true).sets)
        }
        // Never back.
        val ahead = mapOf(HOME to Record(APOD, pic(APOD, d2), t0, shown(pic(APOD, d2))))
        assertEquals(emptyList(), due(home, ahead, now = t0 + h3 + h1, soon = true).sets)
        // B21's mark changes nothing while it's on.
        val late = post(APOD, d2, published = t0 + h3 + h1)
        for (gaps in listOf(emptySet(), setOf(APOD))) {
            assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), due(home, records, stored(posts + (APOD to late), gaps = gaps), now = t0 + h3 + h3, soon = true).sets)
        }
    }

    @Test
    fun withSoonTheTurnsSourcesNewerPictureGoesOnAnotherSourcesWaits() {
        // APOD and NASA every 24 h, NASA's day from t0: NASA's and APOD's next posts both come
        // out after the window (10 h and 9 h after t0).
        val daily = PictureTime(6, 24)
        val home = cols(listOf(APOD, NASA), emptyList())
        val records = mapOf(HOME to Record(NASA, pic(NASA), t0, shown(pic(APOD), pic(NASA))))
        val newer = stored(posts + (NASA to post(NASA, d2, published = t0 + Duration.ofHours(10))) + (APOD to post(APOD, d2, published = t0 + Duration.ofHours(9))))
        val now = t0 + Duration.ofHours(11)
        assertEquals(emptyList(), dueWallpapers(home, records, newer, daily, zone, now).sets)
        // On: NASA's (the turn's) newer post goes on; APOD's waits for APOD's turn.
        val plan = dueWallpapers(home, records, newer, daily, zone, now, soon = true)
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME)), plan.sets)
        assertEquals(Record(NASA, pic(NASA, d2), t0), plan.bare(HOME).record)
        val after = apply(records, plan)
        assertEquals(emptyList(), dueWallpapers(home, after, newer, daily, zone, now + h1, soon = true).sets)
        assertEquals(emptyList(), dueWallpapers(home, after, newer, daily, zone, t0 + day - min1, soon = true).sets)
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME)), dueWallpapers(home, after, newer, daily, zone, t0 + day, soon = true).sets)
        // Every 3 h: Earth's newer post waits for Earth's turn, also with the switch on.
        val all3 = cols(all, emptyList())
        val apodOn = mapOf(HOME to Record(APOD, pic(APOD), t0, shown(pic(APOD), pic(NASA), pic(EARTH))))
        val newEarth = stored(posts + (EARTH to post(EARTH, d2, published = t0 + h1)))
        assertEquals(emptyList(), due(all3, apodOn, newEarth, now = t0 + h1 + min1, soon = true).sets)
        // Never back with turns.
        val ahead = mapOf(HOME to Record(NASA, pic(NASA, d2), t0))
        assertEquals(emptyList(), dueWallpapers(home, ahead, stored(), daily, zone, now, soon = true).sets)
    }

    @Test
    fun withSoonTheStandInsNewerPictureGoesOnInApodsTurn() {
        // APOD alone, NASA for video days: the stand-in on; NASA's next post comes out in the
        // afternoon, after the window: off it waits for the next change time, on it goes on.
        val records = mapOf(HOME to Record(APOD, pic(NASA), t0, shown(post(APOD, d2, video = true).picture(), pic(NASA))))
        val home = apodColumns(setOf(HOME))
        val afternoon = post(NASA, d2, published = t0 + Duration.ofHours(10))
        val video = stored(videoDay + (NASA to afternoon), StandIn.NASA)
        val now = t0 + Duration.ofHours(10) + min1
        assertEquals(emptyList(), due(home, records, video, now = now).sets)
        assertEquals(listOf(afternoon.picture() to setOf(HOME)), due(home, records, video, now = now, soon = true).sets)
        // With turns, in APOD's turn the same.
        val turns = cols(listOf(APOD, EARTH), emptyList())
        assertEquals(emptyList(), due(turns, records, video, now = t0 + h1).sets)
        assertEquals(listOf(afternoon.picture() to setOf(HOME)), due(turns, records, video, now = t0 + h1, soon = true).sets)
    }

    @Test
    fun withSoonEqualColumnsStayTogether() {
        val records = mapOf(HOME to Record(APOD, pic(APOD), t0), LOCK to Record(APOD, pic(APOD), t0))
        val late = stored(posts + (APOD to post(APOD, d2, published = t0 + h3 + h1)))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME, LOCK)), due(apodColumns(setOf(HOME, LOCK)), records, late, now = t0 + h3 + h3, soon = true).sets)
    }

    // The list's order (OR1, order-0.7.0.md): the turns in the list's order, the anchor A (the
    // "New picture at" of `orderFrom`), Set and the switch on keeping what's on until it.

    private val d3 = LocalDate.parse("2026-10-21")
    private val nae = listOf(NASA, APOD, EARTH)

    /** [date] at [hour]:00 UTC. */
    private fun on(date: LocalDate, hour: Int, minute: Int = 0): Instant = date.atTime(hour, minute).toInstant(zone)

    /**
     * Two screens running the rules with a [rotation]: [run] is a job or page run ([dueWallpapers]),
     * [set] Set or the daily switch on ([restartAndForget], then the update). Every set works.
     */
    private inner class Turns(
        var columns: Map<Screen, List<Source>>,
        var time: PictureTime,
        var rotation: Rotation,
        var records: Map<Screen, Record>,
        var stored: Stored = stored(),
        val zone: ZoneId = this@ScreensTest.zone,
        var soon: Boolean = false,
    ) {
        fun run(now: Instant): List<Pair<Picture, Set<Screen>>> {
            val plan = dueWallpapers(columns, records, stored, time, zone, now, soon = soon, rotation = rotation)
            records = apply(records, plan)
            return plan.sets
        }

        fun set(
            now: Instant,
            after: Map<Screen, List<Source>> = columns,
            time: PictureTime = this.time,
            rotation: Rotation = this.rotation,
            orderOnly: Boolean = false,
            daily: Boolean = true,
        ): List<Pair<Picture, Set<Screen>>> {
            val restart = restartAndForget(columns, after, records, stored, time, zone, now, rotation, orderOnly, daily)
            records = records + restart.plan.screens.mapValues { it.value.record } - restart.drop
            columns = after
            this.time = time
            this.rotation = rotation
            return run(now)
        }

        fun owner(screen: Screen = HOME) = records.getValue(screen).owner
    }

    /** APOD on both screens since [date]'s 06:00 (its stamp), all three ticked, every 24 h, the default order without an anchor. */
    private fun apodOn(date: LocalDate, columns: Map<Screen, List<Source>> = cols(all), time: PictureTime = PictureTime(6, 24)) = Turns(
        columns, time, Rotation(),
        mapOf(HOME to Record(APOD, pic(APOD), on(date, 6)), LOCK to Record(APOD, pic(APOD), on(date, 6))),
    )

    @Test
    fun theUsersExampleKeepsApodUntilTheAnchorThenTheList() {
        // NASA, APOD, Earth sorted, Set at 20:00: nothing set, APOD stays; at the next 06:00 NASA,
        // then APOD, then Earth (RO1's clock would put NASA on at once, then Earth, APOD, NASA).
        val phone = apodOn(d2)
        assertEquals(emptyList(), phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true))
        assertEquals(Record(APOD, pic(APOD), on(d2, 6)), phone.records.getValue(HOME).bare())
        assertEquals(Record(APOD, pic(APOD), on(d2, 6)), phone.records.getValue(LOCK).bare())
        assertEquals(emptyList(), phone.run(on(d2, 23)))
        assertEquals(emptyList(), phone.run(on(d3, 5, 59)))
        for ((k, source) in listOf(NASA, APOD, EARTH, NASA).withIndex()) {
            val change = on(d3.plusDays(k.toLong()), 6)
            val sets = phone.run(change)
            assertEquals(source, phone.owner(), "day $k")
            assertEquals(listOf(pic(source) to setOf(HOME, LOCK)), sets, "day $k")
        }
    }

    @Test
    fun apodStaysAtASetWithoutANewOrderBeforeTheAnchor() {
        val phone = apodOn(d2)
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        // At 21:00 a Set changing only "If APOD is a video": APOD again (forgotten, set once), not
        // Earth (the clock's turn before the anchor, N − N(A) = −1), nor RO1's NASA.
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.set(on(d2, 21)))
        assertEquals(APOD, phone.owner())
        assertEquals(APOD, phone.owner(LOCK))
        assertEquals(on(d2, 6), phone.records.getValue(HOME).stamp)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d3, 6)))
    }

    @Test
    fun apodStaysAtTheDailySwitchOnBeforeTheAnchor() {
        // The daily switch off and on at 21:00 (`Store.startAfresh`: the stored setup, the anchor
        // as stored): the same as a Set without a new order.
        val phone = apodOn(d2)
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        val restart = restartAndForget(phone.columns, phone.columns, phone.records, phone.stored, phone.time, zone, on(d2, 21), phone.rotation)
        assertEquals(emptySet(), restart.drop)
        phone.records = phone.records + restart.plan.screens.mapValues { it.value.record }
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.run(on(d2, 21)))
        assertEquals(APOD, phone.owner())
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d3, 6)))
    }

    @Test
    fun fromTheAnchorOnASetIsRo1sWithTheFormula() {
        // The next day at 07:00, APOD on (a record as a pull could leave it): a Set without a new
        // order goes to the formula's turn at once, NASA (the top at A); before A it kept APOD.
        val phone = apodOn(d2)
        phone.rotation = Rotation(nae, d3)
        phone.records = phone.records.mapValues { it.value.copy(stamp = on(d3, 6)) }
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.set(on(d3, 7)))
        assertEquals(NASA, phone.owner())
        assertEquals(on(d3, 6), phone.records.getValue(HOME).stamp)
        // The day after, APOD's turn by the formula.
        val next = apodOn(d2).apply { rotation = Rotation(nae, d3) }
        next.records = next.records.mapValues { Record(EARTH, pic(EARTH), on(d3.plusDays(1), 6)) }
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), next.set(on(d3.plusDays(1), 7)))
    }

    @Test
    fun anOrderSetBeforeAMissedRunTakesTheChangeTime() {
        // 24 h, an offset of 7 min: APOD on since yesterday's 06:00; the order Set at 06:03 (A is
        // tomorrow's 06:00), then the 06:07 run: today's 06:00 is the anchor's −1 (Earth), but the
        // Set took it, so nothing from the order goes on; APOD's own newer picture by rule 2 does.
        val phone = apodOn(d1)
        assertEquals(emptyList(), phone.set(on(d2, 6, 3), rotation = Rotation(nae, d3), orderOnly = true))
        assertEquals(on(d2, 6), phone.records.getValue(HOME).stamp)
        assertEquals(APOD, phone.owner())
        phone.stored = stored(posts + (APOD to post(APOD, d2, published = on(d2, 6, 5))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME, LOCK)), phone.run(on(d2, 6, 7)))
        assertEquals(APOD, phone.owner())
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d3, 6, 7)))
    }

    @Test
    fun theFirstSetWithNoAnchorKeepsTheOwnerAndSetsItAgain() {
        // An update from 0.6.0 (no `orderFrom`), Set tapped with the order unchanged: `Store` writes
        // the anchor (the next "New picture at"), and the owner is kept and set again, not RO1's NASA.
        val phone = apodOn(d2)
        val anchor = phone.time.anchorDate(on(d2, 20), zone)
        assertEquals(d3, anchor)
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.set(on(d2, 20), rotation = Rotation(all, anchor)))
        assertEquals(APOD, phone.owner())
        // At the anchor the top of the default order, APOD (kept), then NASA.
        assertEquals(emptyList(), phone.run(on(d3, 6)))
        assertEquals(APOD, phone.owner())
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d3.plusDays(1), 6)))
    }

    @Test
    fun everySixHoursTheTopLandsAtTheAnchor() {
        // The plan's table: NASA, APOD, Earth set at 20:00, every 6 h.
        val three = apodOn(d2, time = PictureTime(6, 6))
        three.records = three.records.mapValues { it.value.copy(stamp = on(d2, 18)) }
        assertEquals(emptyList(), three.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true))
        val expected = listOf(EARTH, NASA, APOD, EARTH, NASA, APOD)
        for ((k, source) in expected.withIndex()) {
            three.run(on(d3, 0) + Duration.ofHours(6L * k))
            assertEquals(source, three.owner(), "change $k")
        }
        // NASA and APOD: the top at every 06:00 and 18:00.
        val two = apodOn(d2, cols(listOf(APOD, NASA)), PictureTime(6, 6))
        two.records = two.records.mapValues { it.value.copy(stamp = on(d2, 18)) }
        assertEquals(emptyList(), two.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true))
        for ((k, source) in listOf(APOD, NASA, APOD, NASA, APOD, NASA).withIndex()) {
            two.run(on(d3, 0) + Duration.ofHours(6L * k))
            assertEquals(source, two.owner(), "change $k")
        }
    }

    @Test
    fun aPhoneOffOverTheAnchorTakesTheCurrentTurn() {
        // 24 h: off from 05:00 to 08:00, the top at 08:00.
        val daily = apodOn(d2)
        daily.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), daily.run(on(d3, 8)))
        // 6 h: off until 13:00, 12:00's source, the second, APOD.
        val six = apodOn(d2, time = PictureTime(6, 6))
        six.records = six.records.mapValues { it.value.copy(stamp = on(d2, 18)) }
        six.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        six.records = six.records.mapValues { it.value.copy(owner = EARTH, picture = pic(EARTH), stamp = on(d3, 0)) }
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), six.run(on(d3, 13)))
        assertEquals(APOD, six.owner())
    }

    @Test
    fun theTopWithNothingToShowPassesToTheNextInTheList() {
        // Earth, NASA, APOD with nothing of Earth's: at the anchor NASA, the next in the list's
        // order (APOD → NASA → Earth would give APOD).
        val phone = apodOn(d2)
        phone.stored = stored(posts - EARTH)
        phone.set(on(d2, 20), rotation = Rotation(listOf(EARTH, NASA, APOD), d3), orderOnly = true)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d3, 6)))
        assertEquals(NASA, phone.owner())
    }

    @Test
    fun beforeTheAnchorKeepCantHoldGivesTheTop() {
        val rotation = Rotation(nae, d3)
        // The owner unticked: the top, NASA.
        val unticked = apodOn(d2)
        unticked.set(on(d2, 20), rotation = rotation, orderOnly = true)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), unticked.set(on(d2, 21), after = cols(listOf(NASA, EARTH))))
        assertEquals(NASA, unticked.owner())
        // The owner with nothing to show (APOD's video, "Skip it"), the top too: Earth, NASA, APOD
        // with nothing of Earth's gives NASA.
        val video = apodOn(d2)
        video.stored = stored(posts - EARTH + (APOD to post(APOD, d2, video = true)))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), video.set(on(d2, 20), rotation = Rotation(listOf(EARTH, NASA, APOD), d3)))
        assertEquals(NASA, video.owner())
        // No record (a fresh install, the switch on): the top.
        val fresh = Turns(cols(all), PictureTime(6, 24), rotation, emptyMap())
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), fresh.set(on(d2, 20)))
        assertEquals(Record(NASA, pic(NASA), on(d2, 6)), fresh.records.getValue(LOCK).bare())
        // A screen whose column was empty: its stale record goes, it starts on the top.
        val stale = apodOn(d2, cols(all, emptyList()))
        stale.records = stale.records + (LOCK to Record(EARTH, pic(EARTH), on(d1, 6)))
        assertEquals(listOf(pic(APOD) to setOf(HOME), pic(NASA) to setOf(LOCK)), stale.set(on(d2, 20), after = cols(all, listOf(NASA, EARTH)), rotation = rotation))
        assertEquals(NASA, stale.owner(LOCK))
    }

    @Test
    fun beforeTheAnchorASetWithTheDailySwitchOffPutsTheTopOn() {
        // Night default 2: nothing turns with the switch off, so Set puts the top on at once.
        val phone = apodOn(d2)
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true, daily = false))
        assertEquals(NASA, phone.owner())
        // Also a Set without a new order before A.
        val other = apodOn(d2).apply { rotation = Rotation(nae, d3) }
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), other.set(on(d2, 21), daily = false))
        // From A on RO1's Set: the formula's turn (the day after A, APOD).
        val later = apodOn(d2).apply { rotation = Rotation(nae, d3) }
        later.records = later.records.mapValues { Record(EARTH, pic(EARTH), on(d3.plusDays(1), 6)) }
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), later.set(on(d3.plusDays(1), 7), daily = false))
    }

    @Test
    fun columnsThatDifferEachGetTheirTopAtTheAnchor() {
        val phone = Turns(
            cols(listOf(APOD, NASA), listOf(APOD, EARTH)), PictureTime(6, 24), Rotation(),
            mapOf(HOME to Record(APOD, pic(APOD), on(d2, 6)), LOCK to Record(APOD, pic(APOD), on(d2, 6))),
        )
        assertEquals(emptyList(), phone.set(on(d2, 20), rotation = Rotation(listOf(EARTH, NASA, APOD), d3), orderOnly = true))
        assertEquals(listOf(pic(NASA) to setOf(HOME), pic(EARTH) to setOf(LOCK)), phone.run(on(d3, 6)))
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.run(on(d3.plusDays(1), 6)))
    }

    @Test
    fun fromTheAnchorAChangedChangeEveryKeepsTheAnchorsDate() {
        // NASA, APOD, Earth from 21 Oct; at 07:00 that day "Change every" 24 h → 6 h: the
        // formula with 4 change times a day, S[(N(c) − N(A)) mod 3], each 06:00 k days after the
        // anchor S[4k mod 3].
        val phone = apodOn(d2)
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        phone.run(on(d3, 6))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.set(on(d3, 7), time = PictureTime(6, 6)))
        assertEquals(NASA, phone.owner())
        val expected = listOf(APOD, EARTH, NASA, APOD, EARTH, NASA, APOD, EARTH)
        for ((k, source) in expected.withIndex()) {
            val change = on(d3, 12) + Duration.ofHours(6L * k)
            phone.run(change)
            assertEquals(source, phone.owner(), "change $k at $change")
        }
    }

    @Test
    fun fromTheAnchorAChangedNewPictureAtKeepsTheDaysPositions() {
        // 06:00 → 08:00, the anchor's date kept: the day after A at 08:00 APOD, then Earth.
        val phone = apodOn(d2)
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        phone.run(on(d3, 6))
        val d4 = d3.plusDays(1)
        phone.run(on(d4, 6))
        assertEquals(APOD, phone.owner())
        phone.records = phone.records.mapValues { Record(EARTH, pic(EARTH), on(d4, 6)) }
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.set(on(d4, 9), time = PictureTime(8, 24)))
        assertEquals(emptyList(), phone.run(on(d4.plusDays(1), 7, 59)))
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), phone.run(on(d4.plusDays(1), 8)))
        // A change of hour can move A after now: 06:00 → 08:00 at 07:00 on A's day keeps what's on.
        val early = apodOn(d2)
        early.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        early.run(on(d3, 6))
        early.records = early.records.mapValues { Record(EARTH, pic(EARTH), on(d3, 6)) }
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), early.set(on(d3, 7), time = PictureTime(8, 24)))
        assertEquals(EARTH, early.owner())
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), early.run(on(d3, 8)))
    }

    @Test
    fun fromTheAnchorATickUsesTheFormulaWithTheNewCount() {
        // NASA and APOD from 21 Oct; on 23 Oct (k = 2) Earth ticked: S[2 mod 3] = Earth at once
        // (with two, NASA).
        val phone = apodOn(d2, cols(listOf(APOD, NASA)))
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        val d5 = d3.plusDays(2)
        phone.run(on(d5, 6))
        assertEquals(NASA, phone.owner())
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), phone.set(on(d5, 7), after = cols(all)))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(on(d5.plusDays(1), 6)))
    }

    @Test
    fun withSoonAKeptOwnersNewerPictureGoesOnAnothersWaits() {
        val phone = apodOn(d2)
        phone.soon = true
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        // Out at 20:30, after the window: APOD's goes on, NASA's waits for its turn.
        phone.stored = stored(posts + (APOD to post(APOD, d2, published = on(d2, 20, 30))) + (NASA to post(NASA, d2, published = on(d2, 20, 30))))
        assertEquals(listOf(pic(APOD, d2) to setOf(HOME, LOCK)), phone.run(on(d2, 21)))
        assertEquals(APOD, phone.owner())
        assertEquals(listOf(pic(NASA, d2) to setOf(HOME, LOCK)), phone.run(on(d3, 6)))
        // Off: APOD's waits too.
        val off = apodOn(d2)
        off.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        off.stored = phone.stored
        assertEquals(emptyList(), off.run(on(d2, 21)))
    }

    @Test
    fun theStandInFillsApodsPlaceInTheOrder() {
        // NASA, APOD, Earth every 6 h, APOD a video with Earth for video days: 12:00 after the
        // anchor is APOD's turn, Earth's picture with APOD as the owner.
        val phone = apodOn(d2, time = PictureTime(6, 6))
        phone.records = phone.records.mapValues { it.value.copy(stamp = on(d2, 18)) }
        phone.set(on(d2, 20), rotation = Rotation(nae, d3), orderOnly = true)
        phone.stored = stored(posts + (APOD to post(APOD, d3, video = true)), StandIn.EARTH)
        phone.run(on(d3, 6))
        assertEquals(NASA, phone.owner())
        assertEquals(listOf(pic(EARTH) to setOf(HOME, LOCK)), phone.run(on(d3, 12)))
        assertEquals(APOD, phone.owner())
    }

    @Test
    fun aDstDayAsTheAnchor() {
        // Zurich, "New picture at" 02:00: on 28 Mar 2027 it comes at 03:00 (the gap's end).
        val zurich = ZoneId.of("Europe/Zurich")
        val time = PictureTime(2, 24)
        val set = at(zurich, "2027-03-27T20:00")
        val anchor = time.anchorDate(set, zurich)
        assertEquals(LocalDate.parse("2027-03-28"), anchor)
        val records = mapOf(HOME to Record(APOD, pic(APOD), at(zurich, "2027-03-27T02:00")), LOCK to Record(APOD, pic(APOD), at(zurich, "2027-03-27T02:00")))
        val phone = Turns(cols(all), time, Rotation(), records, zone = zurich)
        assertEquals(emptyList(), phone.set(set, rotation = Rotation(nae, anchor), orderOnly = true))
        assertEquals(emptyList(), phone.run(at(zurich, "2027-03-28T01:59")))
        assertEquals(listOf(pic(NASA) to setOf(HOME, LOCK)), phone.run(at(zurich, "2027-03-28T03:00")))
        assertEquals(listOf(pic(APOD) to setOf(HOME, LOCK)), phone.run(at(zurich, "2027-03-29T02:00")))
    }

    @Test
    fun withoutAnAnchorTheDefaultOrderIsRo1s() {
        // Rotation() is RO1's rotation: the same plan as without it.
        val records = mapOf(HOME to Record(EARTH, pic(EARTH), t0 - h3))
        for (k in 0..5L) {
            val now = t0 + h3.multipliedBy(k)
            assertEquals(
                dueWallpapers(cols(all, emptyList()), records, stored(), time, zone, now),
                dueWallpapers(cols(all, emptyList()), records, stored(), time, zone, now, rotation = Rotation()),
            )
            assertEquals(
                restartAndForget(cols(all), cols(all), records, stored(), time, zone, now),
                restartAndForget(cols(all), cols(all), records, stored(), time, zone, now, Rotation(), orderOnly = true, daily = false),
            )
        }
    }
}
