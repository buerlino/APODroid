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

    private fun fit(width: Int, height: Int) = fitFrame(width, height, 1116, 2484)

    @Test
    fun fitPutsAWidePictureAcrossTheMiddle() {
        // Earth's 1440×600 map: 600 · 1116 / 1440 = 465 px tall, (2484 − 465) / 2 = 1009.5 from the top.
        assertEquals(Frame(0, 1009, 1116, 1474), fit(1440, 600))
    }

    @Test
    fun fitPutsATallPictureInTheMiddleOfTheWidth() {
        // 400 · 2484 / 1200 = 828 px wide, (1116 − 828) / 2 = 144 from the left.
        assertEquals(Frame(144, 0, 972, 2484), fit(400, 1200))
    }

    @Test
    fun fitFillsTheScreenWithAPictureInItsShape() {
        assertEquals(Frame(0, 0, 1116, 2484), fit(558, 1242))
        assertEquals(Frame(0, 0, 1116, 2484), fit(2232, 4968))
    }

    @Test
    fun fitDoesntOverflowOnHugePictures() {
        // 1 000 000 · 2484 is past Int.MAX_VALUE; 1000 · 1116 / 1 000 000 rounds down to 1 px.
        assertEquals(Frame(0, 1241, 1116, 1242), fit(1_000_000, 1000))
    }

    @Test
    fun sidewaysFrameOfNov9sPicture() {
        // Turned: 2408×3630; the frame is 3630 · 1116 / 2484 = 1630 wide, sliding 778 px.
        fun f(p: Float) = sidewaysFrame(3630, 2408, 1116, 2484, p)
        assertEquals(Frame(0, 778, 3630, 2408), f(0f))
        assertEquals(Frame(0, 389, 3630, 2019), f(0.5f))
        assertEquals(Frame(0, 0, 3630, 1630), f(1f))
    }

    @Test
    fun sidewaysFrameMapsBackToTheTurnedFrame() {
        for ((w, h) in listOf(3630 to 2408, 1280 to 854, 6000 to 1000, 500 to 500, 400 to 1200, 5000 to 5000)) {
            for (p in listOf(0f, 0.13f, 0.5f, 0.77f, 1f)) {
                val b = sidewaysFrame(w, h, 1116, 2484, p)
                val t = wallpaperFrame(h, w, 1116, 2484, p)
                // Turned clockwise: upright (x, y) goes to (h - y, x).
                assertEquals(Frame(h - b.bottom, b.left, h - b.top, b.right), t, "$w×$h at $p")
            }
        }
    }

    @Test
    fun sidewaysFrameOnAPanoramaIsAStripOfColumns() {
        // Turned 1000×6000 is taller than the screen: 1000 · 2484 / 1116 = 2225 high, sliding 3775.
        assertEquals(Frame(0, 0, 2225, 1000), sidewaysFrame(6000, 1000, 1116, 2484, 0f))
        assertEquals(Frame(3775, 0, 6000, 1000), sidewaysFrame(6000, 1000, 1116, 2484, 1f))
    }

    @Test
    fun sidewaysFrameOfASquareAndRounding() {
        // Turned square: fills its height... 500 · 1116 / 2484 = 224 wide.
        assertEquals(Frame(0, 276, 500, 500), sidewaysFrame(500, 500, 1116, 2484, 0f))
        for (p in listOf(0f, 0.3f, 0.999f, 1f)) for (w in 1000..1040 step 7) for (h in 700..740 step 5) {
            val b = sidewaysFrame(w, h, 1116, 2484, p)
            assert(b.left >= 0 && b.top >= 0 && b.right <= w && b.bottom <= h && b.right > b.left && b.bottom > b.top) { "$w×$h $p $b" }
        }
        assert(sidewaysHelps(3630, 2408) && !sidewaysHelps(500, 500) && !sidewaysHelps(400, 1200))
    }

    @Test
    fun wideSidewaysTurns3To2AndWider() {
        // 3:2 exactly and wider turn; 1.2:1, a square, a portrait and an unknown size don't.
        assert(sidewaysByDefault(3000, 2000) && sidewaysByDefault(3630, 2408) && sidewaysByDefault(6000, 1000))
        assert(!sidewaysByDefault(2999, 2000) && !sidewaysByDefault(1200, 1000) && !sidewaysByDefault(500, 500))
        assert(!sidewaysByDefault(1000, 1500) && !sidewaysByDefault(0, 0) && !sidewaysByDefault(10, 0))
        // No overflow on huge sizes.
        assert(sidewaysByDefault(Int.MAX_VALUE, Int.MAX_VALUE / 2) && !sidewaysByDefault(Int.MAX_VALUE / 2, Int.MAX_VALUE / 2))
    }

    @Test
    fun aNotedPartWinsElseTheSwitchsRule() {
        val wide = { 3630 to 2408 }
        val narrow = { 1200 to 1000 }
        // Noted by ⛶'s pill: as noted, whatever the switch and the size.
        for (on in listOf(true, false)) {
            assert(sidewaysOf(true, on, narrow) && sidewaysOf(true, on, wide))
            assert(!sidewaysOf(false, on, wide) && !sidewaysOf(false, on, narrow))
        }
        // Not noted (null): the rule while the switch is on, else upright.
        assert(sidewaysOf(null, true, wide) && !sidewaysOf(null, true, narrow) && !sidewaysOf(null, true) { null })
        assert(!sidewaysOf(null, false, wide))
        // The size is read only when the rule needs it.
        var read = 0
        sidewaysOf(true, true) { read++; 3630 to 2408 }
        sidewaysOf(null, false) { read++; 3630 to 2408 }
        assertEquals(0, read)
        assert(sidewaysOf(null, true) { read++; 3630 to 2408 })
        assertEquals(1, read)
    }

    @Test
    fun theUprightSizeSwapsOnlyForAQuarterTurn() {
        for (o in listOf(0, 1, 2, 3, 4, 9, -1)) assertEquals(4000 to 3000, uprightOf(4000, 3000, o), "orientation $o")
        for (o in 5..8) assertEquals(3000 to 4000, uprightOf(4000, 3000, o), "orientation $o")
        // A camera photo stored 4000×3000 with orientation 6 stands upright: no wide rule.
        assert(!uprightOf(4000, 2400, 6).let { (w, h) -> sidewaysByDefault(w, h) } && sidewaysByDefault(4000, 2400))
    }
}
