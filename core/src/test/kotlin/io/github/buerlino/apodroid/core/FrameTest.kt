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

    /**
     * 0.9.0's rule (sharp-0.9.0.md, Testing 1): pill {true, false, none} × Size × source {APOD, NASA,
     * Earth, my picture} × shape {portrait, square, landscape} × up {true, false}, each against The rules.
     */
    @Test
    fun sidewaysOfsTruthTable() {
        val shapes = mapOf("portrait" to (1000 to 1500), "square" to (1200 to 1200), "landscape" to (1500 to 1022))
        val sources: List<Source?> = Source.entries + null
        var cases = 0
        var turned = 0
        for (pill in listOf(true, false, null)) for (size in Size.entries) for (source in sources) {
            for ((shape, wh) in shapes) for (up in listOf(true, false)) {
                val want = when {
                    source == null -> false // my picture: no part, never turned
                    pill != null -> pill // ⛶'s pill, under every Size
                    else -> size == Size.AUTO && source == Source.APOD && shape == "landscape" && !up
                }
                val got = sidewaysOf(pill, size, source, { wh }, { up })
                assertEquals(want, got, "pill $pill, $size, ${source ?: "my picture"}, $shape, up $up")
                cases++
                if (got) turned++
            }
        }
        assertEquals(3 * 3 * 4 * 3 * 2, cases)
        // The pill's true for the three sources (54), and Auto's one turn.
        assertEquals(3 * 3 * 3 * 2 + 1, turned)
    }

    @Test
    fun sidewaysOfReadsOnlyWhatItNeeds() {
        var sizes = 0
        var texts = 0
        fun of(noted: Boolean?, size: Size, source: Source?, wh: Pair<Int, Int>?) =
            sidewaysOf(noted, size, source, { sizes++; wh }, { texts++; false })
        assert(of(true, Size.AUTO, Source.APOD, 3000 to 1000))
        assert(!of(null, Size.CENTRE, Source.APOD, 3000 to 1000))
        assert(!of(null, Size.AUTO, Source.NASA, 3000 to 1000))
        assert(!of(null, Size.AUTO, null, 3000 to 1000))
        assertEquals(0 to 0, sizes to texts)
        assert(!of(null, Size.AUTO, Source.APOD, 1000 to 3000))
        assert(!of(null, Size.AUTO, Source.APOD, null))
        assertEquals(2 to 0, sizes to texts, "no text read for a portrait or unknown size")
        assert(of(null, Size.AUTO, Source.APOD, 3000 to 1000))
        assertEquals(3 to 1, sizes to texts)
        // Any landscape turns, not only 3:2 (W > H by one pixel).
        assert(of(null, Size.AUTO, Source.APOD, 1001 to 1000) && !of(null, Size.AUTO, Source.APOD, 1000 to 1000))
    }

    /** Auto's turn with the word table: a galaxy text turns, a horizon text doesn't (SH2a's device case, in :core). */
    @Test
    fun autosTurnByTheText() {
        fun turns(title: String, size: Size = Size.AUTO) =
            sidewaysOf(null, size, Source.APOD, { 3000 to 1000 }) { hasUp(title, null, "") }
        assert(turns("A spiral galaxy and its nebulae in a field of stars"))
        assert(!turns("Trees and a lake below the horizon at sunset"))
        assert(!turns("A spiral galaxy and its nebulae in a field of stars", Size.CENTRE))
        assert(!turns("A spiral galaxy and its nebulae in a field of stars", Size.LIVE))
    }

    @Test
    fun theUprightSizeSwapsOnlyForAQuarterTurn() {
        for (o in listOf(0, 1, 2, 3, 4, 9, -1)) assertEquals(4000 to 3000, uprightOf(4000, 3000, o), "orientation $o")
        for (o in 5..8) assertEquals(3000 to 4000, uprightOf(4000, 3000, o), "orientation $o")
        // A camera photo stored 4000×2400 with orientation 6 stands upright: not landscape, so no turn.
        assert(!uprightOf(4000, 2400, 6).let { (w, h) -> sidewaysHelps(w, h) } && sidewaysHelps(4000, 2400))
    }
}
