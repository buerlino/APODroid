package io.github.buerlino.apodroid.core

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class SubjectTest {
    private val sw = 1080
    private val sh = 2408

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun grey(v: Int) = rgb(v, v, v)

    /** A black copy of a [w]×[h] picture's [subjectCopySize] with a white disc at ([fx], [fy]) of its width and height. */
    private fun disc(w: Int, h: Int, fx: Double, fy: Double, screenW: Int = sw, screenH: Int = sh): Triple<IntArray, Int, Int> {
        val (cw, ch) = subjectCopySize(w, h, screenW, screenH)
        val cx = fx * cw
        val cy = fy * ch
        val r = minOf(cw, ch) * 0.08
        val px = IntArray(cw * ch) { i ->
            val x = i % cw + 0.5
            val y = i / cw + 0.5
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) grey(255) else grey(0)
        }
        return Triple(px, cw, ch)
    }

    private fun position(w: Int, h: Int, fx: Double, fy: Double, sideways: Boolean = false): Float {
        val (px, cw, ch) = disc(w, h, fx, fy)
        return subjectPosition(px, cw, ch, w, h, sw, sh, sideways)
    }

    @Test
    fun framePositionTakesThePartThenAutoThenTheMiddle() {
        // ⛶'s part wins with every Size.
        for (size in Size.entries) assertEquals(0.3f, framePosition(size, 0.3f, 0.9f, true))
        // Auto: the subject, through autoPosition's fallback.
        assertEquals(0.9f, framePosition(Size.AUTO, null, 0.9f, true))
        assertEquals(0.5f, framePosition(Size.AUTO, null, 0.62f, true))
        assertEquals(0.62f, framePosition(Size.AUTO, null, 0.62f, false))
        // Auto without a subject (none noted, or a failed one): the middle.
        assertEquals(0.5f, framePosition(Size.AUTO, null, null, false))
        // Centre, and the screens Live leaves still: the middle.
        assertEquals(0.5f, framePosition(Size.CENTRE, null, 0.9f, false))
        assertEquals(0.5f, framePosition(Size.LIVE, null, 0.9f, false))
    }

    @Test
    fun theCopyIs128AcrossTheFramesDirection() {
        // Wider than the screen's shape: the frame moves left–right, the height is 128.
        assertEquals(256 to 128, subjectCopySize(4000, 2000, sw, sh))
        // A 2:3 portrait is still wider than a 1080×2408 screen: the height again.
        assertEquals(85 to 128, subjectCopySize(1024, 1538, sw, sh))
        // Taller than the screen's shape: the frame moves up–down, the width is 128.
        assertEquals(128 to 384, subjectCopySize(1000, 3000, sw, sh))
        // Python's round, half to even: 1001 × 128 / 256 = 500.5 → 500 (roundToInt would say 501).
        assertEquals(500 to 128, subjectCopySize(1001, 256, sw, sh))
        // At least 4 a side.
        assertEquals(4 to 128, subjectCopySize(20, 1000, 10, 1000))
    }

    @Test
    fun boxResizeIsPillowsBox() {
        // Pillow 12's resize(…, Image.BOX), checked in Python: each output pixel the mean of the
        // input pixels whose centres lie in its box (an edge centre exactly on the box's left
        // border is out, on its right border in), not an area average with fractional weights.
        val row = intArrayOf(rgb(0, 0, 255), rgb(90, 45, 165), rgb(180, 90, 75), rgb(30, 15, 225), rgb(60, 30, 195))
        assertArrayEquals(
            intArrayOf(rgb(45, 23, 210), rgb(180, 90, 75), rgb(45, 23, 210)),
            boxResize(row, 5, 1, 3, 1),
        )
        assertArrayEquals(intArrayOf(rgb(90, 45, 165), rgb(45, 23, 210)), boxResize(row, 5, 1, 2, 1))
        // Two passes, each rounded to 8 bits: (10 + 20 + 30 + 41) / 4 = 25.25 comes out 26.
        val sq = intArrayOf(10, 20, 0, 0, 30, 41, 0, 0, 0, 0, 255, 255, 0, 0, 255, 254).map(::grey).toIntArray()
        assertArrayEquals(intArrayOf(grey(26), grey(0), grey(0), grey(255)), boxResize(sq, 4, 4, 2, 2))
        // The same size: the pixels, opaque.
        assertArrayEquals(intArrayOf(grey(7)), boxResize(intArrayOf((grey(7) and 0xFFFFFF) or (0x12 shl 24)), 1, 1, 1, 1))
    }

    @Test
    fun aSubjectLeftRightOrInTheMiddle() {
        // 3:1, the frame a seventh of the width.
        val left = position(3000, 1000, 0.08, 0.5)
        val right = position(3000, 1000, 0.92, 0.5)
        val middle = position(3000, 1000, 0.5, 0.5)
        assertTrue("left $left", left < 0.1f)
        assertTrue("right $right", right > 0.9f)
        assertEquals(0.5f, middle, 0.02f)
        // Taller than the screen's shape: up–down.
        assertTrue(position(1000, 4000, 0.5, 0.1) < 0.1f)
        assertTrue(position(1000, 4000, 0.5, 0.9) > 0.9f)
    }

    @Test
    fun aFlatPictureIsTheMiddle() {
        val (cw, ch) = subjectCopySize(3000, 1000, sw, sh)
        for (v in listOf(0, 128, 255)) {
            assertEquals(0.5f, subjectPosition(IntArray(cw * ch) { grey(v) }, cw, ch, 3000, 1000, sw, sh, false))
        }
    }

    @Test
    fun aPictureNoWiderThanTheFrameIsTheMiddle() {
        // The screen's own shape: the frame is the whole picture.
        val (px, cw, ch) = disc(1080, 2408, 0.5, 0.1)
        assertEquals(0.5f, subjectPosition(px, cw, ch, 1080, 2408, sw, sh, false))
    }

    @Test
    fun percentileIsNumpys() {
        assertEquals(8.955, percentile(DoubleArray(10) { it.toDouble() }, 99.5), 1e-12)
        assertEquals(2.99, percentile(doubleArrayOf(3.0, 1.0, 2.0), 99.5), 1e-12)
    }

    @Test
    fun normWithAZeroPercentileKeepsTheShiftedMap() {
        // One value in 1000 stands out: the 99.5th percentile is 0, so the map is only shifted,
        // not divided and not clipped (5 stays 5).
        val m = DoubleArray(1000) { -2.0 }.also { it[17] = 3.0 }
        val n = norm(m)
        assertEquals(5.0, n[17], 0.0)
        assertEquals(0.0, n[0], 0.0)
        // Else divided by it and clipped to 0–1.
        val d = norm(DoubleArray(1000) { (it % 10).toDouble() })
        assertEquals(1.0, d.max(), 0.0)
        assertEquals(1.0, d[9], 0.0)
        assertEquals(5.0 / 9, d[5], 1e-12)
    }

    @Test
    fun anEdgeFrameNeedsTenPercentMoreThanTheMiddle() {
        // 11 columns, the frame one column: positions 0, 0.1, …, 1; the middle column holds 1.
        fun at(edge: Double) = DoubleArray(11) { if (it == 5) 1.0 else if (it == 0) edge else 0.0 }
        assertEquals(0.5, mapPosition(at(1.09), 11, 1, horizontal = true, frac = 1.0 / 11), 0.0)
        assertEquals(0.0, mapPosition(at(1.11), 11, 1, horizontal = true, frac = 1.0 / 11), 0.0)
        // The pull grows with the square of the distance: 3% more wins at 0.3, not at 0.1.
        val near = DoubleArray(11) { if (it == 5) 1.0 else if (it == 3) 1.03 else 0.0 }
        assertEquals(0.3, mapPosition(near, 11, 1, horizontal = true, frac = 1.0 / 11), 1e-12)
        val far = DoubleArray(11) { if (it == 5) 1.0 else if (it == 1) 1.03 else 0.0 }
        assertEquals(0.5, mapPosition(far, 11, 1, horizontal = true, frac = 1.0 / 11), 0.0)
    }

    @Test
    fun theFirstMaximumWins() {
        // Equal scores at 0 and 1 (the same pull): the first.
        val m = doubleArrayOf(2.0, 0.0, 1.0, 0.0, 2.0)
        assertEquals(0.0, mapPosition(m, 5, 1, horizontal = true, frac = 0.2), 0.0)
        // Up–down the same, by rows.
        assertEquals(0.0, mapPosition(m, 1, 5, horizontal = false, frac = 0.2), 0.0)
    }

    @Test
    fun anEmptyMiddleTakesTheLargestWithoutThePull() {
        // The middle frame holds nothing: the largest sum, no pull (else a division by 0).
        val m = doubleArrayOf(0.0, 1.0, 0.0, 0.0, 1.5)
        assertEquals(1.0, mapPosition(m, 5, 1, horizontal = true, frac = 0.2), 0.0)
        assertEquals(0.5, mapPosition(DoubleArray(5), 5, 1, horizontal = true, frac = 0.2), 0.0)
    }

    @Test
    fun autoFallsBackToTheMiddleForSmallMoves() {
        assertEquals(0.5f, autoPosition(0.65f, true))
        assertEquals(0.75f, autoPosition(0.75f, true))
        assertEquals(0.65f, autoPosition(0.65f, false))
        assertEquals(0.5f, autoPosition(0.35f, true))
        assertEquals(0.25f, autoPosition(0.25f, true))
        assertEquals(0.0f, autoPosition(0.0f, true))
    }

    /** Whether [f] holds the point ([x], [y]) of the picture. */
    private fun holds(f: Frame, x: Double, y: Double) = x >= f.left && x < f.right && y >= f.top && y < f.bottom

    @Test
    fun sidewaysFindsTheBandSidewaysFrameShows() {
        // 3:2, a subject near the bottom, centred across: upright the middle, sideways the bottom band.
        val w = 3000
        val h = 2000
        val up = position(w, h, 0.5, 0.88)
        val side = position(w, h, 0.5, 0.88, sideways = true)
        assertEquals(0.5f, up, 0.05f)
        assertTrue("sideways $side", side < 0.2f)
        assertTrue(holds(sidewaysFrame(w, h, sw, sh, side), w * 0.5, h * 0.88))
        assertTrue(!holds(sidewaysFrame(w, h, sw, sh, 1 - side), w * 0.5, h * 0.88))
        // Near the top: the top band.
        val top = position(w, h, 0.5, 0.12, sideways = true)
        assertTrue("top $top", top > 0.8f)
        assertTrue(holds(sidewaysFrame(w, h, sw, sh, top), w * 0.5, h * 0.12))
    }

    @Test
    fun sidewaysOnAPanoramaMovesAlongIt() {
        // 6:1: turned, it is taller than the screen's shape, so its frame is a strip of columns
        // moving left to right.
        val w = 6000
        val h = 1000
        for (fx in listOf(0.1, 0.5, 0.9)) {
            val p = position(w, h, fx, 0.5, sideways = true)
            assertTrue("fx $fx: $p", abs(p - fx) < 0.12)
            assertTrue("fx $fx", holds(sidewaysFrame(w, h, sw, sh, p), w * fx, h * 0.5))
        }
    }

    /**
     * SA0's positions on the desktop (`private/sa0/`, not in CI): every picture in
     * `private/pictures/` and the round-2 ones of `private/pictures-oct-nov/` from 2 Oct, decoded
     * by ImageIO subsampled (the full 8857×6785 one wouldn't fit the test heap), box-resized to the
     * copy and measured as the app will, against compute.py's `importance_pull` (Pillow's full
     * decode): 95% within 0.03 and none off by more than 0.1.
     */
    @Test
    fun matchesTheDesktopsPositions() {
        val root = File("../private")
        val pics = File(root, "pictures")
        val later = File(root, "pictures-oct-nov")
        val csvs = listOf(File(root, "sa0/positions-253.csv"), File(root, "sa0/positions-round2.csv"))
        assumeTrue(pics.isDirectory && later.isDirectory && csvs.all { it.isFile })
        val expected = HashMap<String, Double>()
        for (csv in csvs) {
            val lines = csv.readLines().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
            val head = lines.first().split(",")
            val col = head.indexOf("importance_pull")
            for (l in lines.drop(1)) l.split(",").let { expected[it[0]] = it[col].toDouble() }
        }
        val files = pics.listFiles()!!.filter { it.name.endsWith(".img") } +
            later.listFiles()!!.filter { it.name.endsWith(".img") && it.name.removeSuffix(".img") >= "2026-10-02" }
        var within = 0
        val misses = mutableListOf<String>()
        val far = mutableListOf<String>()
        for (f in files.sortedBy { it.name }) {
            val date = f.name.removeSuffix(".img")
            val want = expected[date] ?: error("$date: no position in the CSVs")
            val got = desktopPosition(f)
            val d = abs(got - want)
            if (d <= 0.03) within++ else misses += "$date %.4f vs %.4f".format(got, want)
            if (d > 0.1) far += date
        }
        println("Subject positions: ${files.size} pictures, $within within 0.03; misses: $misses")
        assertTrue("within 0.03: $within of ${files.size}; misses $misses", within >= 0.95 * files.size)
        assertTrue("off by more than 0.1: $far", far.isEmpty())
    }

    private fun desktopPosition(f: File): Double {
        ImageIO.createImageInputStream(f).use { input ->
            val reader = ImageIO.getImageReaders(input).next()
            try {
                reader.input = input
                val w = reader.getWidth(0)
                val h = reader.getHeight(0)
                var k = 1
                while (minOf(w, h) / (k * 2) >= 512) k *= 2
                val param = reader.defaultReadParam.apply { setSourceSubsampling(k, k, 0, 0) }
                val raster = reader.read(0, param).raster
                val dw = raster.width
                val dh = raster.height
                val bands = raster.numBands
                val s = IntArray(dw * dh * bands)
                raster.getPixels(0, 0, dw, dh, s)
                // The samples as they are (a grey JPEG has one band, no gamma); alpha dropped, as Pillow's convert("RGB").
                val argb = IntArray(dw * dh) { i ->
                    val o = i * bands
                    if (bands < 3) grey(s[o]) else rgb(s[o], s[o + 1], s[o + 2])
                }
                val (cw, ch) = subjectCopySize(w, h, sw, sh)
                val copy = boxResize(argb, dw, dh, cw, ch)
                return subjectPosition(copy, cw, ch, w, h, sw, sh, false).toDouble()
            } finally {
                reader.dispose()
            }
        }
    }
}
