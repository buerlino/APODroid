package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TurnTest {
    private val start = Instant.parse("2026-10-17T06:00:00Z")
    private val h3 = Duration.ofHours(3)
    private val day = Duration.ofHours(24)
    private val all = Source.entries
    private val apod = Source.APOD
    private val nasa = Source.NASA
    private val earth = Source.EARTH

    private fun turn(
        ticked: List<Source> = all,
        current: Source = apod,
        startedAt: Instant? = start,
        length: Duration = h3,
        after: Duration = Duration.ofHours(1),
        pictures: Set<Source> = all.toSet(),
    ) = nextTurn(ticked, current, startedAt, length, start + after, pictures::contains)

    @Test
    fun withinTheTurnTheWallpapersSourceStays() {
        assertEquals(apod, turn())
        assertEquals(earth, turn(current = earth))
        assertEquals(nasa, turn(current = nasa, length = day, after = Duration.ofHours(20)))
    }

    @Test
    fun aTurnOverGoesToTheNextTickedSourceInOrder() {
        val over = Duration.ofHours(3)
        assertEquals(nasa, turn(after = over))
        assertEquals(earth, turn(current = nasa, after = over))
        assertEquals(apod, turn(current = earth, after = over))
        assertEquals(earth, turn(ticked = listOf(apod, earth), after = over))
        assertEquals(apod, turn(ticked = listOf(apod, earth), current = earth, after = over))
    }

    @Test
    fun aTurnOverSkipsSourcesWithoutAPicture() {
        val over = Duration.ofHours(3)
        assertEquals(earth, turn(after = over, pictures = setOf(apod, earth)))
        // Round to the current one when it's the only one with a picture.
        assertEquals(apod, turn(after = over, pictures = setOf(apod)))
    }

    @Test
    fun theCurrentSourceUntickedOrWithoutAPictureEndsItsTurnAtOnce() {
        assertEquals(earth, turn(ticked = listOf(apod, earth), current = nasa))
        assertEquals(apod, turn(ticked = listOf(apod, nasa), current = earth))
        assertEquals(earth, turn(current = nasa, pictures = setOf(apod, earth)))
    }

    @Test
    fun aVideoApodIsSkipped() {
        // An APOD video has no picture.
        val noApod = setOf(nasa, earth)
        assertEquals(nasa, turn(pictures = noApod))
        assertEquals(nasa, turn(current = earth, after = Duration.ofHours(3), pictures = noApod))
        assertEquals(nasa, turn(ticked = listOf(apod, nasa), current = nasa, after = Duration.ofHours(3), pictures = noApod))
    }

    @Test
    fun noneWithAPictureIsNull() {
        assertNull(turn(pictures = emptySet()))
        assertNull(turn(ticked = listOf(apod, nasa), after = Duration.ofHours(3), pictures = setOf(earth)))
    }

    @Test
    fun noStartMeansTheTurnIsOver() {
        assertEquals(nasa, turn(startedAt = null))
        assertEquals(nasa, turn(startedAt = null, after = Duration.ZERO))
    }

    @Test
    fun aClockSetBackEndsTheTurn() {
        assertEquals(nasa, turn(startedAt = start + Duration.ofHours(1) + Duration.ofMinutes(1)))
        assertEquals(apod, turn(startedAt = start + Duration.ofHours(1)))
    }

    @Test
    fun aRunALittleEarlyEndsTheTurn() {
        for (length in listOf(h3, day)) {
            assertEquals(nasa, turn(length = length, after = length - Duration.ofMinutes(20)), "20 min early, $length")
            assertEquals(apod, turn(length = length, after = length - Duration.ofMinutes(40)), "40 min early, $length")
            assertEquals(nasa, turn(length = length, after = length - turnSlack), "at the slack, $length")
        }
    }
}
