package io.github.buerlino.apodroid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    // The list's order (OR1)

    @Test
    fun nextSourceGoesInTheListsOrder() {
        val order = listOf(earth, nasa, apod)
        fun next(ticked: List<Source> = all, after: Source, pictures: Set<Source> = all.toSet()) = nextSource(ticked, after, pictures::contains, order)
        assertEquals(nasa, next(after = earth))
        assertEquals(apod, next(after = nasa))
        assertEquals(earth, next(after = apod))
        // Earth without a picture: from APOD to NASA (APOD → NASA → Earth would give NASA too, from Earth APOD).
        assertEquals(nasa, next(after = earth, pictures = setOf(apod, nasa)))
        assertEquals(nasa, next(after = apod, pictures = setOf(apod, nasa)))
        // An unticked source: from its place in the order.
        assertEquals(apod, next(ticked = listOf(earth, apod), after = nasa))
        assertEquals(earth, next(ticked = listOf(earth, nasa), after = apod))
        assertNull(next(after = apod, pictures = emptySet()))
    }

    @Test
    fun theOrdersPrefIsReadTolerantly() {
        assertEquals(all, orderOf(null))
        assertEquals(all, orderOf(""))
        assertEquals(listOf(nasa, apod, earth), orderOf("NASA,APOD,EARTH"))
        assertEquals(listOf(earth, nasa, apod), orderOf(" EARTH , NASA,APOD"))
        // Unknown names and duplicates dropped, missing ones appended in the default order.
        assertEquals(listOf(earth, apod, nasa), orderOf("EARTH,MARS,EARTH"))
        assertEquals(listOf(nasa, apod, earth), orderOf("nasa,NASA"))
        assertEquals(all, orderOf(",,"))
    }

    @Test
    fun aRotationNeedsEverySourceOnce() {
        assertEquals(listOf(nasa, earth), Rotation(listOf(earth, nasa, apod)).ordered(listOf(nasa, earth)).reversed())
        assertEquals(all, Rotation().order)
        assertFailsWith<IllegalArgumentException> { Rotation(listOf(apod, nasa)) }
        assertFailsWith<IllegalArgumentException> { Rotation(listOf(apod, nasa, nasa)) }
    }

    /** A setup as the app's: the list's columns, ⛶'s parts, another field, the order. */
    private data class Draft(
        val list: Map<Screen, List<Source>>,
        val parts: Map<Source, Float> = emptyMap(),
        val hours: Int = 24,
        val order: List<Source> = Source.entries,
    )

    private fun alone(was: Draft, now: Draft) =
        orderAlone(was, now, Draft::order, { d, o -> d.copy(order = o) }, { it.copy(list = columnsInOneOrder(it.list)) })

    @Test
    fun theOrderAloneIsOnlyAnotherOrder() {
        // OR2's fix round: Store compared the setups with their different orders still in, so it was never true.
        val both = mapOf(Screen.HOME to all, Screen.LOCK to all)
        val was = Draft(both, mapOf(nasa to 0.3f))
        val nae = listOf(nasa, apod, earth)
        assertTrue(alone(was, was.copy(order = nae)))
        assertFalse(alone(was, was))
        assertFalse(alone(was, was.copy(order = nae, parts = mapOf(nasa to 0.4f))))
        assertFalse(alone(was, was.copy(order = nae, parts = emptyMap())))
        assertFalse(alone(was, was.copy(order = nae, hours = 6)))
        assertFalse(alone(was, was.copy(order = nae, list = mapOf(Screen.HOME to all, Screen.LOCK to listOf(apod)))))
        // The list's columns in another sequence still count as equal.
        val shuffled = mapOf(Screen.LOCK to listOf(earth, apod, nasa), Screen.HOME to listOf(nasa, earth, apod))
        assertTrue(alone(was, was.copy(order = nae, list = shuffled)))
        assertFalse(alone(was, was.copy(list = shuffled)))
    }
}
