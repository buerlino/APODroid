package io.github.buerlino.apodroid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TurnTest {
    private val all = Source.entries
    private val apod = Source.APOD
    private val nasa = Source.NASA
    private val earth = Source.EARTH

    private fun next(ticked: List<Source> = all, after: Source = apod, pictures: Set<Source> = all.toSet()) =
        nextSource(ticked, after, pictures::contains)

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
}
