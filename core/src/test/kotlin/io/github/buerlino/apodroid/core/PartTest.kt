package io.github.buerlino.apodroid.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartTest {
    private val date = LocalDate.of(2026, 11, 16)
    private val url = "https://example.com/a.jpg"

    @Test
    fun theSavedStateKeepsAnUndraggedPart() {
        // SA3b: null (not dragged) and null sideways (the switch's default) survive as "null".
        val part = Part(date, url, null, true, null)
        assertEquals("2026-11-16|null|true|null|$url", part.toText())
        assertEquals(part, partOf(part.toText()))
    }

    @Test
    fun theSavedStateKeepsADraggedPart() {
        for (part in listOf(Part(date, url, 0.25f, false, true), Part(date, null, 0f, false, false), Part(date, url, 1f, true, null))) {
            assertEquals(part, partOf(part.toText()))
        }
    }

    @Test
    fun aSavedStateFromBeforeSa3bStillReads() {
        // SW3's line, written the same way with a float position.
        assertEquals(Part(date, url, 0.5f, false, null), partOf("2026-11-16|0.5|false|null|$url"))
        // A URL with a "|" stays whole (limit 5).
        assertEquals("a|b", partOf("2026-11-16|0.5|false|true|a|b").url)
    }

    @Test
    fun checkNotesThePositionOnlyWhenDragged() {
        // Not dragged: the previous value, null staying null, so Size and the subject keep deciding.
        assertNull(notedPosition(moved = false, dragged = 0.78f, previous = null))
        assertEquals(0.3f, notedPosition(moved = false, dragged = 0.78f, previous = 0.3f))
        // Dragged: where it was left, also back at the middle.
        assertEquals(0.5f, notedPosition(moved = true, dragged = 0.5f, previous = null))
        assertEquals(0.9f, notedPosition(moved = true, dragged = 0.9f, previous = 0.3f))
    }

    @Test
    fun anUndraggedPartFollowsSizeAndTheFallback() {
        // A part ✓'d only for "Whole picture" or the pill: Centre the middle, Auto the subject.
        val part = Part(date, url, null, true, null)
        assertEquals(0.5f, framePosition(Size.CENTRE, part.position, 0.62f, fallback = true))
        assertEquals(0.5f, framePosition(Size.AUTO, part.position, 0.62f, fallback = true))
        assertEquals(0.62f, framePosition(Size.AUTO, part.position, 0.62f, fallback = false))
        // A dragged one wins over Auto.
        assertEquals(0.3f, framePosition(Size.AUTO, 0.3f, 0.9f, fallback = false))
    }

    @Test
    fun aPartIsForItsOwnPostOnly() {
        val post = Post(Source.APOD, date, "T", url, "https://example.com/p", false, "")
        val parts = mapOf(Source.APOD to Part(date, url, null, false))
        assertEquals(parts[Source.APOD], parts.forPost(post))
        assertNull(parts.forPost(post.copy(imageUrl = "https://example.com/b.jpg")))
        assertNull(parts.forPost(post.copy(date = date.plusDays(1))))
    }
}
