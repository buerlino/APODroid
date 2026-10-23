package io.github.buerlino.apodroid.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundTest {
    // A 200×134 grey copy, as the app decodes a 3:2 picture.
    private val w = 200
    private val h = 134

    private fun picture(value: (x: Int, y: Int) -> Int) = IntArray(w * h) { value(it % w, it / w) }

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun aPlanetOnBlackHasBlackEdges() {
        val gray = picture { x, y ->
            val dx = x - w / 2
            val dy = y - h / 2
            if (dx * dx + dy * dy < 50 * 50) 200 else 5
        }
        assertTrue(edgesBlack(gray, w, h))
    }

    @Test
    fun aStarFieldWithAFewBrightStarsAtTheEdgeHasBlackEdges() {
        val random = Random(7)
        val gray = picture { _, _ -> random.nextInt(0, 25) } // dark sky with noise
        // Twelve bright stars in the outer strip, each 2×2.
        listOf(0 to 0, 50 to 1, 120 to 2, 199 to 30, 198 to 90, 0 to 70, 3 to 120, 80 to 133, 150 to 131, 100 to 0, 196 to 133, 5 to 10)
            .forEach { (sx, sy) ->
                for (y in sy..sy + 1) for (x in sx..sx + 1) {
                    if (x < w && y < h) gray[y * w + x] = 255
                }
            }
        assertTrue(edgesBlack(gray, w, h))
    }

    @Test
    fun aLitLandscapeHasNoBlackEdges() {
        val gray = picture { x, y -> 90 + (x + y) % 100 }
        assertFalse(edgesBlack(gray, w, h))
    }

    @Test
    fun aNebulaFillingTheFrameHasNoBlackEdges() {
        // Dark in places, but glowing gas reaches every edge.
        val gray = picture { x, y -> if ((x / 10 + y / 10) % 3 == 0) 20 else 70 + (x * y) % 120 }
        assertFalse(edgesBlack(gray, w, h))
    }

    @Test
    fun aBlackSkyOverLitGroundHasNoBlackEdges() {
        // The top two thirds black, the ground lit: the bottom strip and the sides' lower part aren't black.
        val gray = picture { _, y -> if (y < h * 2 / 3) 3 else 120 }
        assertFalse(edgesBlack(gray, w, h))
    }

    @Test
    fun aThinGreyFrameIsNotBlackEdges() {
        // A black picture with a one-pixel grey frame: a sixth of the 6 px strip is grey.
        val gray = picture { x, y -> if (x == 0 || y == 0 || x == w - 1 || y == h - 1) 128 else 0 }
        assertFalse(edgesBlack(gray, w, h))
    }

    @Test
    fun theStripIsAboutThreePercentOfEachSide() {
        // Bright everywhere but the outer 6 px (3% of 200, rounded up) and 5 px (3% of 134): black edges.
        val gray = picture { x, y -> if (x < 6 || x >= w - 6 || y < 5 || y >= h - 5) 0 else 255 }
        assertTrue(edgesBlack(gray, w, h))
        // One pixel less on the left and the left strip's inner column is bright: 1 of 6 columns.
        val narrower = picture { x, y -> if (x < 5 || x >= w - 6 || y < 5 || y >= h - 5) 0 else 255 }
        assertFalse(edgesBlack(narrower, w, h))
    }

    @Test
    fun aTinyCopyStillHasAnEdge() {
        assertTrue(edgesBlack(IntArray(1) { 0 }, 1, 1))
        assertFalse(edgesBlack(IntArray(4) { 200 }, 2, 2))
    }

    @Test
    fun grayWeighsTheColours() {
        assertEquals(0, gray(argb(0, 0, 0)))
        assertEquals(255, gray(argb(255, 255, 255)))
        assertEquals(76, gray(argb(255, 0, 0)))
        assertEquals(150, gray(argb(0, 255, 0)))
        assertEquals(29, gray(argb(0, 0, 255)))
    }

    @Test
    fun theCopyIsASixteenthOfTheScreen() {
        assertEquals(270 to 600, blurSize(1080, 2400))
        assertEquals(279 to 621, blurSize(1116, 2484))
        assertEquals(1 to 1, blurSize(1, 1))
        assertEquals(11, blurRadius(270, 600))
        assertEquals(1, blurRadius(10, 10))
    }

    @Test
    fun theBlurKeepsTheSizeAndDarkensBy35Percent() {
        val bw = 30
        val bh = 50
        val even = IntArray(bw * bh) { argb(200, 100, 40) }
        val out = blurBackground(even, bw, bh)
        assertEquals(bw * bh, out.size)
        // 200 · 0.65 = 130, 100 · 0.65 = 65, 40 · 0.65 = 26: every pixel, also at the edges and corners.
        out.forEach { assertEquals(argb(130, 65, 26), it) }
        assertEquals(argb(200, 100, 40), even[0]) // the input is left as it is
    }

    @Test
    fun theBlurIsOpaque() {
        val transparent = IntArray(16) { 0x00FFFFFF }
        blurBackground(transparent, 4, 4).forEach { assertEquals(0xFF, (it ushr 24)) }
    }

    @Test
    fun theBlurSpreadsABrightDotAndKeepsItsLight() {
        val bw = 41
        val bh = 41
        val dot = IntArray(bw * bh) { argb(0, 0, 0) }
        dot[20 * bw + 20] = argb(255, 255, 255)
        val out = blurBackground(dot, bw, bh, radius = 3)
        val red = out.map { (it shr 16) and 0xFF }
        // The dot is gone, its light spread around it, symmetric, falling off outwards.
        assertTrue(red[20 * bw + 20] < 10)
        assertTrue(red[20 * bw + 20] > 0)
        assertEquals(red[20 * bw + 17], red[20 * bw + 23])
        assertEquals(red[17 * bw + 20], red[23 * bw + 20])
        assertTrue(red[20 * bw + 20] >= red[20 * bw + 25])
        // Three passes of radius 3 reach 9 px; beyond that nothing.
        assertEquals(0, red[20 * bw + 30])
        assertEquals(0, red[0])
    }

    @Test
    fun theEdgesExtendOutwardsSoAHalfAndHalfPictureStaysCleanAtItsBorder() {
        // Left half white, right half black: the outer columns keep their colour (no dark fringe
        // from outside the picture), only the middle blends.
        val bw = 40
        val bh = 10
        val half = IntArray(bw * bh) { if (it % bw < bw / 2) argb(255, 255, 255) else argb(0, 0, 0) }
        val out = blurBackground(half, bw, bh, radius = 2)
        val red = out.map { (it shr 16) and 0xFF }
        for (y in 0 until bh) {
            assertEquals(166, red[y * bw]) // 255 · 0.65, also in the top and bottom rows
            assertEquals(0, red[y * bw + bw - 1])
            assertTrue(red[y * bw + 19] in 1..165)
            assertTrue(red[y * bw + 20] in 1..165)
        }
    }

    @Test
    fun edgeCopyIs200WideInThePicturesShape() {
        assertEquals(200 to 133, edgeCopySize(1600, 1067))
        assertEquals(200 to 251, edgeCopySize(4455, 5592))
        assertEquals(200 to 1, edgeCopySize(6000, 10))
        assertEquals(150 to 90, edgeCopySize(150, 90))
        assertEquals(200 to 300, edgeCopySize(200, 300))
    }
}
