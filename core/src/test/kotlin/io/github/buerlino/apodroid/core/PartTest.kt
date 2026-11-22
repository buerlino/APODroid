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
        val previous = Part(date, url, 0.3f, false, false)
        // Not dragged: the previous value, null staying null, so Size and the subject keep deciding.
        assertNull(notedPosition(moved = false, dragged = 0.78f, previous = null, sideways = false))
        assertNull(notedPosition(moved = false, dragged = 0.78f, previous = previous.copy(position = null), sideways = false))
        assertEquals(0.3f, notedPosition(moved = false, dragged = 0.78f, previous = previous, sideways = false))
        // Dragged: where it was left, also back at the middle, in either orientation.
        assertEquals(0.5f, notedPosition(moved = true, dragged = 0.5f, previous = null, sideways = false))
        assertEquals(0.9f, notedPosition(moved = true, dragged = 0.9f, previous = previous, sideways = false))
        assertEquals(0.9f, notedPosition(moved = true, dragged = 0.9f, previous = previous, sideways = true))
    }

    @Test
    fun aPositionBelongsToItsOrientation() {
        // B32: a position noted with an orientation is that orientation's only; in the other, null
        // (Size and the subject).
        val sideways = Part(date, url, 0.893f, false, true)
        assertEquals(0.893f, sideways.positionFor(true))
        assertNull(sideways.positionFor(false))
        val upright = Part(date, url, 0.2f, false, false)
        assertEquals(0.2f, upright.positionFor(false))
        assertNull(upright.positionFor(true))
        // K26: a part from before 0.9.0 (a position, no orientation noted) keeps its number in both.
        val old = Part(date, url, 0.7f, false, null)
        assertEquals(0.7f, old.positionFor(true))
        assertEquals(0.7f, old.positionFor(false))
        assertNull(Part(date, url, null, false, true).positionFor(true))
    }

    @Test
    fun aBandDraggedSidewaysDoesntMoveTheUprightFrame() {
        // B32 (E17b, K18/K31a on the Android 17 emulator): Size Auto, a landscape APOD whose upright
        // subject is at 0.4. ⛶, the pill on, the band dragged to 0.893, ✓.
        val first = Part(
            date, url,
            notedPosition(moved = true, dragged = 0.893f, previous = null, sideways = true), false,
            notedSideways(touched = true, moved = true, shown = true, previous = null),
        )
        assertEquals(Part(date, url, 0.893f, false, true), first)
        // ⛶ again: the pill turned off moves the frame to Size's, the upright subject.
        val frame = framePosition(Size.AUTO, first.positionFor(false), 0.4f)
        assertEquals(0.4f, frame)
        // ✓ without a drag: upright noted, no position for it, so the set is Auto's subject.
        val turned = notedSideways(touched = true, moved = false, shown = false, previous = first.sideways)
        val second = Part(date, url, notedPosition(moved = false, dragged = frame, previous = first, sideways = turned!!), false, turned)
        assertEquals(Part(date, url, null, false, false), second)
        assertEquals(0.4f, framePosition(Size.AUTO, second.positionFor(false), 0.4f))
        // The pill back on, ✓ without a drag: the band's own position again.
        assertEquals(0.893f, framePosition(Size.AUTO, first.positionFor(true), 0.62f))
        assertEquals(0.893f, notedPosition(moved = false, dragged = 0.893f, previous = first, sideways = true))
    }

    @Test
    fun checkNotesTheOrientationWithADrag() {
        // 0.9.0 (sharp-0.9.0.md, The rules): untouched and undragged, the previous value, null
        // (Auto's turn) staying null.
        for (previous in listOf(null, true, false)) {
            for (shown in listOf(true, false)) assertEquals(previous, notedSideways(touched = false, moved = false, shown = shown, previous = previous))
        }
        // A touched pill notes its value, as SW3 did.
        assertEquals(true, notedSideways(touched = true, moved = false, shown = true, previous = null))
        assertEquals(false, notedSideways(touched = true, moved = false, shown = false, previous = true))
        // A drag notes the orientation it was made in, also Auto's turn's (shown sideways, previous null).
        assertEquals(true, notedSideways(touched = false, moved = true, shown = true, previous = null))
        assertEquals(false, notedSideways(touched = false, moved = true, shown = false, previous = null))
        assertEquals(false, notedSideways(touched = true, moved = true, shown = false, previous = true))
    }

    @Test
    fun anUndraggedPartFollowsSize() {
        // A part ✓'d only for "Whole picture" or the pill: Centre the middle, Auto the subject.
        val part = Part(date, url, null, true, null)
        assertEquals(0.5f, framePosition(Size.CENTRE, part.position, 0.62f))
        assertEquals(0.62f, framePosition(Size.AUTO, part.position, 0.62f))
        // A dragged one wins over Auto.
        assertEquals(0.3f, framePosition(Size.AUTO, 0.3f, 0.9f))
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
