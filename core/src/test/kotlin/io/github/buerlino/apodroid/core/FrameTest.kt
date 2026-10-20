package io.github.buerlino.apodroid.core

import kotlin.test.Test
import kotlin.test.assertEquals

class FrameTest {
    // A 1116×2484 phone screen.
    private fun frame(width: Int, height: Int, position: Float) = wallpaperFrame(width, height, 1116, 2484, position)

    @Test
    fun slidesAlongAWidePicture() {
        // 854 · 1116 / 2484 = 383.7 px wide; the middle is at 448.5, rounded up.
        assertEquals(Frame(0, 0, 383, 854), frame(1280, 854, 0f))
        assertEquals(Frame(449, 0, 832, 854), frame(1280, 854, 0.5f))
        assertEquals(Frame(897, 0, 1280, 854), frame(1280, 854, 1f))
    }

    @Test
    fun slidesAlongATallPicture() {
        // 400 · 2484 / 1116 = 890.3 px tall.
        assertEquals(Frame(0, 0, 400, 890), frame(400, 1200, 0f))
        assertEquals(Frame(0, 155, 400, 1045), frame(400, 1200, 0.5f))
        assertEquals(Frame(0, 310, 400, 1200), frame(400, 1200, 1f))
    }

    @Test
    fun aPictureInTheScreensShapeIsTheWholeFrame() {
        assertEquals(Frame(0, 0, 558, 1242), frame(558, 1242, 0f))
        assertEquals(Frame(0, 0, 558, 1242), frame(558, 1242, 1f))
    }

    @Test
    fun tinyPicturesGiveAnEmptyFrameInside() {
        assertEquals(Frame(1, 0, 1, 1), frame(1, 1, 0.5f))
        assertEquals(Frame(100_000, 0, 100_000, 1), frame(100_000, 1, 1f))
    }

    @Test
    fun hugePicturesDontOverflow() {
        // 1 000 000 · 2484 is past Int.MAX_VALUE.
        assertEquals(Frame(999_551, 0, 1_000_000, 1000), frame(1_000_000, 1000, 1f))
    }
}
