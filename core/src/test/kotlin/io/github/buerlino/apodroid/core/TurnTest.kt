package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnTest {
    private val start = Instant.parse("2026-10-17T06:00:00Z")
    private val h3 = Duration.ofHours(3)
    private val day = Duration.ofHours(24)
    private val all = Source.entries
    private val apod = Source.APOD
    private val nasa = Source.NASA
    private val earth = Source.EARTH

    private fun over(startedAt: Instant? = start, length: Duration = h3, after: Duration = Duration.ofHours(1)) =
        isTurnOver(startedAt, length, start + after)

    private fun next(ticked: List<Source> = all, after: Source = apod, pictures: Set<Source> = all.toSet()) =
        nextSource(ticked, after, pictures::contains)

    @Test
    fun withinTheTurnItIsNotOver() {
        assertFalse(over())
        assertFalse(over(length = day, after = Duration.ofHours(20)))
    }

    @Test
    fun nextSourceGoesInOrderAndRoundsOff() {
        assertEquals(nasa, next())
        assertEquals(earth, next(after = nasa))
        assertEquals(apod, next(after = earth))
        assertEquals(earth, next(ticked = listOf(apod, earth)))
        assertEquals(apod, next(ticked = listOf(apod, earth), after = earth))
    }

    @Test
    fun nextSourceSkipsSourcesWithoutAPicture() {
        assertEquals(earth, next(pictures = setOf(apod, earth)))
        // Round to the current one when it's the only one with a picture.
        assertEquals(apod, next(pictures = setOf(apod)))
    }

    @Test
    fun nextSourceFromAnUntickedSource() {
        assertEquals(earth, next(ticked = listOf(apod, earth), after = nasa))
        assertEquals(apod, next(ticked = listOf(apod, nasa), after = earth))
        assertEquals(earth, next(after = nasa, pictures = setOf(apod, earth)))
    }

    @Test
    fun aVideoApodIsSkipped() {
        // An APOD video has no picture.
        val noApod = setOf(nasa, earth)
        assertEquals(nasa, next(pictures = noApod))
        assertEquals(nasa, next(after = earth, pictures = noApod))
        assertEquals(nasa, next(ticked = listOf(apod, nasa), after = nasa, pictures = noApod))
    }

    @Test
    fun noneWithAPictureIsNull() {
        assertNull(next(pictures = emptySet()))
        assertNull(next(ticked = listOf(apod, nasa), pictures = setOf(earth)))
    }

    @Test
    fun noStartMeansTheTurnIsOver() {
        assertTrue(over(startedAt = null))
        assertTrue(over(startedAt = null, after = Duration.ZERO))
    }

    @Test
    fun aTurnEndsAtItsLength() {
        assertTrue(over(after = h3))
        assertTrue(over(after = h3 + Duration.ofHours(1)))
    }

    @Test
    fun aClockSetBackEndsTheTurn() {
        assertTrue(over(startedAt = start + Duration.ofHours(1) + Duration.ofMinutes(1)))
        assertFalse(over(startedAt = start + Duration.ofHours(1)))
    }

    @Test
    fun aRunALittleEarlyEndsTheTurn() {
        for (length in listOf(h3, day)) {
            assertTrue(over(length = length, after = length - Duration.ofMinutes(20)), "20 min early, $length")
            assertFalse(over(length = length, after = length - Duration.ofMinutes(40)), "40 min early, $length")
            assertTrue(over(length = length, after = length - turnSlack), "at the slack, $length")
        }
    }
}
