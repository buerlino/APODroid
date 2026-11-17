package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SH2c (sharp-0.9.0.md): when "Keep pictures sharp" changes a set, and Auto's subject for the limited frame.
class SharpTest {
    // The FP6's portrait screen.
    private val sw = 1116
    private val sh = 2484

    private fun grey(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    /** A [w]×[h] picture decoded at [dw]×[dh], black with a white disc at ([fx], [fy]) of its width and height. */
    private fun disc(dw: Int, dh: Int, fx: Double, fy: Double): IntArray {
        val r = minOf(dw, dh) * 0.08
        return IntArray(dw * dh) { i ->
            val x = i % dw + 0.5 - fx * dw
            val y = i / dw + 0.5 - fy * dh
            if (x * x + y * y <= r * r) grey(255) else grey(0)
        }
    }

    @Test
    fun theSeventhOfNovemberBitesUprightNotSideways() {
        // 1500×1022: Fill ×2.43 upright, ×1.66 turned.
        assertEquals(2484.0 / 1022, fillScale(1500, 1022, sw, sh, false), 1e-12)
        assertEquals(2484.0 / 1500, fillScale(1500, 1022, sw, sh, true), 1e-12)
        assertTrue(limitBites(1500, 1022, sw, sh, false, SHARP_LIMIT))
        assertFalse(limitBites(1500, 1022, sw, sh, true, SHARP_LIMIT))
        assertFalse(limitBites(1500, 1022, sw, sh, false, null))
        // At 2×: 3000 wide, 558 of it on the screen; 2044 high, all of it.
        assertEquals(558 to 1022, limitedShape(1500, 1022, sw, sh, false, SHARP_LIMIT))
        assertNull(limitedShape(1500, 1022, sw, sh, true, SHARP_LIMIT))
    }

    @Test
    fun theToleranceIsPlainFill() {
        // e = 2484 / 1220 = 2.036, within 2.06: no bite; 2484 / 1200 = 2.07: bites.
        assertFalse(limitBites(2000, 1220, sw, sh, false, SHARP_LIMIT))
        assertTrue(limitBites(2000, 1200, sw, sh, false, SHARP_LIMIT))
    }

    @Test
    fun aTurnedPanoramaMovesAlongItsLength() {
        // 2000×400 turned is 400×2000: e = 1116 / 400 = 2.79; at 2× 800 wide (whole), 4000 high (1242 shown).
        assertEquals(400 to 1242, limitedShape(2000, 400, sw, sh, true, SHARP_LIMIT))
    }

    @Test
    fun noLimitNoSharpPosition() {
        val px = disc(150, 102, 0.7, 0.5)
        assertNull(limitedSubjectPosition(px, 150, 102, 1500, 1022, sw, sh, sideways = true))
        assertNull(limitedSubjectPosition(disc(400, 100, 0.5, 0.5), 400, 100, 4000, 1000, sw, sh, sideways = false, limit = 3.0))
    }

    @Test
    fun aSmallPictureIsWholeBothWays() {
        // 300×200 at 2×: 600×400, smaller than the screen both ways.
        assertEquals(300 to 200, limitedShape(300, 200, sw, sh, false, SHARP_LIMIT))
        assertEquals(0.5f, limitedSubjectPosition(disc(300, 200, 0.9, 0.5), 300, 200, 300, 200, sw, sh, false))
        assertEquals(0.5f, limitedSubjectPosition(disc(300, 200, 0.9, 0.5), 300, 200, 300, 200, sw, sh, true))
    }

    @Test
    fun theUprightPositionIsTheSubjectsForTheLimitedFrame() {
        val (dw, dh) = 750 to 511
        val px = disc(dw, dh, 0.7, 0.5)
        val got = limitedSubjectPosition(px, dw, dh, 1500, 1022, sw, sh, false)!!
        val (cw, ch) = subjectCopySize(1500, 1022, 558, 1022)
        assertEquals(subjectPosition(boxResize(px, dw, dh, cw, ch), cw, ch, 1500, 1022, 558, 1022), got)
        // The limited frame (558 of 1500) at that position holds the disc's centre.
        val frac = 558.0 / 1500
        val start = got * (1 - frac)
        assertTrue("p $got", got > 0.5f && start <= 0.7 && 0.7 <= start + frac)
    }

    @Test
    fun theSidewaysPositionIsTheTurnedSubjectsForTheLimitedFrame() {
        val (dw, dh) = 1000 to 200
        val px = disc(dw, dh, 0.5, 0.2)
        val got = limitedSubjectPosition(px, dw, dh, 2000, 400, sw, sh, true)!!
        assertEquals(sidewaysSubjectPosition(px, dw, dh, 2000, 400, 400, 1242), got)
    }
}
