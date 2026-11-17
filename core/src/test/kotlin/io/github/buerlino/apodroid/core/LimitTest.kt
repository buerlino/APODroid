package io.github.buerlino.apodroid.core

import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

// SH0 (sharp-0.9.0.md, Testing 1): the oracle for SH1's `limitedLayout` and `limitedFit`, written from
// sharp-0.9.0.md's Terms and The rules before either exists. `SharpOracle` is a naive reading of them in
// doubles; `LimitTableTest` compares SH1's code with it over Testing 1's generated table, ±1 px;
// `LimitPropertiesTest` checks Testing 1's properties over 100,000 seeded cases. Both are @Ignored until
// SH1 (its stubs in Limit.kt throw). `LimitOracleTest` runs now: it checks the oracle and the table
// against 0.8.0's `wallpaperFrame`, `sidewaysFrame` and `fitFrame` where no limit bites.

/** A rectangle in doubles: the oracle's answer before any rounding (right and bottom exclusive, as [Frame]). */
internal data class SharpBox(val left: Double, val top: Double, val right: Double, val bottom: Double)

/** What the oracle says: the frame in the upright picture's pixels, the dest on the screen, the scale. */
internal data class SharpAnswer(val frame: SharpBox, val dest: SharpBox, val scale: Double)

/**
 * The naive reference, sharp-0.9.0.md read literally, written for clarity, not speed: no rounding but
 * the dest's centring floor, which the plan names.
 */
internal object SharpOracle {
    /** Fill's enlargement of the picture as it goes on, W×H: `max(sw ÷ W, sh ÷ H)`. */
    fun fillScale(bigW: Int, bigH: Int, sw: Int, sh: Int): Double =
        max(sw.toDouble() / bigW, sh.toDouble() / bigH)

    /** The whole picture's fit: `min(sw ÷ W, sh ÷ H)`. */
    fun fitScale(w: Int, h: Int, sw: Int, sh: Int): Double = min(sw.toDouble() / w, sh.toDouble() / h)

    /** s: e within the tolerance (or no limit), else the limit. */
    fun scaleFor(e: Double, limit: Double?): Double =
        if (limit == null || e <= limit * SHARP_TOLERANCE) e else limit

    /**
     * One axis of the oriented picture, [length] long, drawn at [s] on a [screen] long side: larger than
     * the screen, the visible length `screen ÷ s` slides by [position] and the dest spans the screen; else
     * the whole length, centred (`(screen − len·s) ÷ 2`, floor). Returns frame start, frame end, dest
     * start, dest end.
     */
    private fun axis(length: Int, screen: Int, s: Double, position: Double): DoubleArray {
        val drawn = length * s
        return if (drawn > screen) {
            val visible = screen / s
            val start = (length - visible) * position
            doubleArrayOf(start, start + visible, 0.0, screen.toDouble())
        } else {
            val start = floor((screen - drawn) / 2)
            doubleArrayOf(0.0, length.toDouble(), start, start + drawn)
        }
    }

    /**
     * The limited layout of an upright [w]×[h] picture on a [sw]×[sh] screen. Sideways the picture as it
     * goes on is h×w, its top to the screen's right (turned clockwise): the oriented x runs from the
     * upright picture's bottom row (0) to its top, the oriented y from its left column to its right, so
     * the frame maps back as `sidewaysFrame` maps (position 0 the bottom band).
     */
    fun layout(w: Int, h: Int, sw: Int, sh: Int, sideways: Boolean, position: Float, limit: Double?): SharpAnswer {
        val bigW = if (sideways) h else w
        val bigH = if (sideways) w else h
        val s = scaleFor(fillScale(bigW, bigH, sw, sh), limit)
        val x = axis(bigW, sw, s, position.toDouble())
        val y = axis(bigH, sh, s, position.toDouble())
        val frame = if (sideways) SharpBox(y[0], h - x[1], y[1], h - x[0]) else SharpBox(x[0], y[0], x[1], y[1])
        return SharpAnswer(frame, SharpBox(x[2], y[2], x[3], y[3]), s)
    }

    /** The whole picture at its fit scale, at most the limit (with the tolerance), centred. */
    fun fit(w: Int, h: Int, sw: Int, sh: Int, limit: Double?): SharpBox {
        val s = scaleFor(fitScale(w, h, sw, sh), limit)
        val left = floor((sw - w * s) / 2)
        val top = floor((sh - h * s) / 2)
        return SharpBox(left, top, left + w * s, top + h * s)
    }
}

// ---- The generated table (Testing 1's first bullet) ----

internal val sharpScreens = listOf(1116 to 2484, 1080 to 2400)
internal val sharpPositions = listOf(0f, 0.5f, 1f)
internal val sharpLimits = listOf<Double?>(null, SHARP_LIMIT)

/** The table's shapes, upright: a ratio (width ÷ height), or a fixed size. */
internal enum class SharpShape(val label: String, val ratio: Double, val fixed: Pair<Int, Int>? = null) {
    PORTRAIT("portrait 2:3", 2.0 / 3),
    SQUARE("square", 1.0),
    NEAR_SQUARE("1.05:1", 1.05),
    WIDE_147("1.47:1", 1.47),
    WIDE_3_2("3:2", 1.5),
    PANORAMA("4:1", 4.0),
    TINY("tiny 640×480", 640.0 / 480, 640 to 480),
    HUGE("huge 8000×6000", 8000.0 / 6000, 8000 to 6000),
}

/** The table's enlargements: 0.5, 1, exactly L, L × 1.03 (the largest integer size within it), just above, far above. */
internal enum class SharpTarget(val label: String) {
    HALF("e 0.5"), ONE("e 1"), AT_LIMIT("e = L"), AT_TOLERANCE("e ≤ L×1.03"), JUST_ABOVE("e just above L×1.03"), FAR_ABOVE("e 2L"),
}

/**
 * The picture as it goes on (W×H) of [shape] (oriented: the upright ratio, inverted when [turned]) whose
 * Fill enlargement (or, with [fit], fit scale) on [sw]×[sh] is [target]: the binding side is the screen's
 * ÷ the target (rounded so AT_TOLERANCE stays within the tolerance and JUST_ABOVE just over it), the
 * other side by the ratio. A fixed shape keeps its size whatever the target.
 */
internal fun sharpSize(shape: SharpShape, turned: Boolean, target: SharpTarget, sw: Int, sh: Int, fit: Boolean): Pair<Int, Int> {
    shape.fixed?.let { (w, h) -> return if (turned) h to w else w to h }
    val ratio = if (turned) 1 / shape.ratio else shape.ratio
    val wider = ratio * sh > sw
    val bindsHeight = if (fit) !wider else wider
    val screen = (if (bindsHeight) sh else sw).toDouble()
    val edge = SHARP_LIMIT * SHARP_TOLERANCE
    val bind = when (target) {
        SharpTarget.HALF -> (screen * 2).roundToInt()
        SharpTarget.ONE -> screen.roundToInt()
        SharpTarget.AT_LIMIT -> (screen / SHARP_LIMIT).roundToInt()
        SharpTarget.AT_TOLERANCE -> ceil(screen / edge).toInt()
        SharpTarget.JUST_ABOVE -> floor(screen / edge).toInt()
        SharpTarget.FAR_ABOVE -> (screen / (2 * SHARP_LIMIT)).roundToInt()
    }
    return if (bindsHeight) (bind * ratio).roundToInt() to bind else bind to (bind / ratio).roundToInt()
}

/** Whether [e] is what [target] asks (null: the target holds); else why not. */
internal fun sharpTargetMiss(target: SharpTarget, e: Double): String? {
    val edge = SHARP_LIMIT * SHARP_TOLERANCE
    val ok = when (target) {
        SharpTarget.HALF -> e == 0.5
        SharpTarget.ONE -> e == 1.0
        SharpTarget.AT_LIMIT -> e == SHARP_LIMIT
        SharpTarget.AT_TOLERANCE -> e > SHARP_LIMIT && e < edge - 1e-9
        SharpTarget.JUST_ABOVE -> e > edge + 1e-9 && e < edge + 0.01
        SharpTarget.FAR_ABOVE -> abs(e - 2 * SHARP_LIMIT) < 0.02
    }
    return if (ok) null else "${target.label} wanted, e = $e"
}

internal data class SharpLayoutCase(
    val label: String, val w: Int, val h: Int, val sw: Int, val sh: Int,
    val sideways: Boolean, val position: Float, val limit: Double?, val target: SharpTarget?,
) {
    override fun toString() =
        "$label ${w}×$h on ${sw}×$sh, ${if (sideways) "sideways" else "upright"}, p=$position, limit=$limit"
}

internal data class SharpFitCase(
    val label: String, val w: Int, val h: Int, val sw: Int, val sh: Int, val limit: Double?, val target: SharpTarget?,
) {
    override fun toString() = "$label ${w}×$h on ${sw}×$sh, limit=$limit"
}

/** Orientation × shapes × e × position × limit × the two screens: 2 × (6 × 6 + 2) × 3 × 2 × 2 = 912 cases. */
internal fun sharpLayoutTable(): List<SharpLayoutCase> = buildList {
    for ((sw, sh) in sharpScreens) for (sideways in listOf(false, true)) for (shape in SharpShape.entries) {
        val targets: List<SharpTarget?> = if (shape.fixed != null) listOf(null) else SharpTarget.entries
        for (target in targets) {
            val (bigW, bigH) = sharpSize(shape, sideways, target ?: SharpTarget.ONE, sw, sh, fit = false)
            val (w, h) = if (sideways) bigH to bigW else bigW to bigH
            for (position in sharpPositions) for (limit in sharpLimits) {
                add(SharpLayoutCase("${shape.label}, ${target?.label ?: "as is"}:", w, h, sw, sh, sideways, position, limit, target))
            }
        }
    }
}

/** The whole picture: the shapes upright and turned × fit scale × limit × the two screens: 2 × 38 × 2 × 2 = 304 cases. */
internal fun sharpFitTable(): List<SharpFitCase> = buildList {
    for ((sw, sh) in sharpScreens) for (turned in listOf(false, true)) for (shape in SharpShape.entries) {
        val targets: List<SharpTarget?> = if (shape.fixed != null) listOf(null) else SharpTarget.entries
        for (target in targets) {
            val (w, h) = sharpSize(shape, turned, target ?: SharpTarget.ONE, sw, sh, fit = true)
            for (limit in sharpLimits) {
                add(SharpFitCase("${shape.label}${if (turned) " turned" else ""}, fit ${target?.label ?: "as is"}:", w, h, sw, sh, limit, target))
            }
        }
    }
}

// ---- Comparing ----

/** ±1 px (and a hair for the doubles). */
internal const val SHARP_PX = 1.0 + 1e-6

/** Where [got] differs from [want] by more than [tolerance] on a side, else null. */
internal fun sharpMiss(what: String, want: SharpBox, got: Frame, tolerance: Double = SHARP_PX): String? {
    val off = listOf(
        got.left - want.left, got.top - want.top, got.right - want.right, got.bottom - want.bottom,
    ).maxOf { abs(it) }
    return if (off <= tolerance) null else "$what $got, the oracle ${want.rounded()} (off by ${"%.2f".format(off)})"
}

internal fun SharpBox.rounded() = "SharpBox(%.2f, %.2f, %.2f, %.2f)".format(left, top, right, bottom)

internal fun sameScale(a: Double, b: Double) = abs(a - b) <= 1e-9 * max(abs(a), abs(b))

/** Fails with the first [shown] of [failures], if any, out of [total]. */
internal fun sharpReport(failures: List<String>, total: Int, what: String, shown: Int = 25) {
    if (failures.isNotEmpty()) {
        fail("${failures.size} of $total $what:\n" + failures.take(shown).joinToString("\n"))
    }
}

/** The oriented frame (as the picture goes on, W×H) of an upright [frame] of an upright picture [h] high. */
internal fun orientedFrame(frame: Frame, h: Int, sideways: Boolean): Frame =
    if (sideways) Frame(h - frame.bottom, frame.left, h - frame.top, frame.right) else frame

// ---- The oracle's own checks (run now) ----

class LimitOracleTest {
    @Test
    fun theTableHitsItsTargets() {
        val misses = mutableListOf<String>()
        for (c in sharpLayoutTable()) {
            val target = c.target ?: continue
            val e = if (c.sideways) SharpOracle.fillScale(c.h, c.w, c.sw, c.sh) else SharpOracle.fillScale(c.w, c.h, c.sw, c.sh)
            sharpTargetMiss(target, e)?.let { misses += "$c: $it" }
        }
        for (c in sharpFitTable()) {
            val target = c.target ?: continue
            sharpTargetMiss(target, SharpOracle.fitScale(c.w, c.h, c.sw, c.sh))?.let { misses += "$c: $it" }
        }
        sharpReport(misses, sharpLayoutTable().size + sharpFitTable().size, "table cases miss their e")
        assertEquals(912, sharpLayoutTable().size)
        assertEquals(304, sharpFitTable().size)
    }

    /** Without a limit, or within the tolerance, the oracle is 0.8.0's Fill: `wallpaperFrame` / `sidewaysFrame`, the whole screen. */
    @Test
    fun theOracleIsFillWhereNoLimitBites() {
        val misses = mutableListOf<String>()
        var fills = 0
        for (c in sharpLayoutTable()) {
            val e = if (c.sideways) SharpOracle.fillScale(c.h, c.w, c.sw, c.sh) else SharpOracle.fillScale(c.w, c.h, c.sw, c.sh)
            if (c.limit != null && e > c.limit * SHARP_TOLERANCE) continue
            fills++
            val want = SharpOracle.layout(c.w, c.h, c.sw, c.sh, c.sideways, c.position, c.limit)
            val fill = if (c.sideways) sidewaysFrame(c.w, c.h, c.sw, c.sh, c.position) else wallpaperFrame(c.w, c.h, c.sw, c.sh, c.position)
            sharpMiss("Fill's frame", want.frame, fill)?.let { misses += "$c: $it" }
            sharpMiss("the screen", want.dest, Frame(0, 0, c.sw, c.sh))?.let { misses += "$c: $it" }
            if (want.scale != e) misses += "$c: scale ${want.scale}, e $e"
        }
        sharpReport(misses, fills, "Fill cases differ")
        assertTrue(fills > 500, "$fills Fill cases")
    }

    @Test
    fun theOracleIsFillOnRandomSizesToo() {
        val r = Random(9_000_917)
        val misses = mutableListOf<String>()
        repeat(20_000) {
            val sw = 320 + r.nextInt(1681)
            val sh = (sw * (1.0 + 1.6 * r.nextDouble())).toInt()
            val w = 16 + r.nextInt(8000)
            val h = 16 + r.nextInt(8000)
            val sideways = r.nextBoolean()
            val position = sharpPositions[r.nextInt(3)]
            val want = SharpOracle.layout(w, h, sw, sh, sideways, position, null)
            val fill = if (sideways) sidewaysFrame(w, h, sw, sh, position) else wallpaperFrame(w, h, sw, sh, position)
            sharpMiss("Fill's frame", want.frame, fill)?.let { misses += "${w}×$h on ${sw}×$sh sideways=$sideways p=$position: $it" }
            sharpMiss("the screen", want.dest, Frame(0, 0, sw, sh))?.let { misses += "${w}×$h on ${sw}×$sh: $it" }
        }
        sharpReport(misses, 20_000, "random Fill cases differ")
    }

    @Test
    fun theOracleIsFitWhereNoLimitBites() {
        val misses = mutableListOf<String>()
        var fits = 0
        for (c in sharpFitTable()) {
            val f = SharpOracle.fitScale(c.w, c.h, c.sw, c.sh)
            if (c.limit != null && f > c.limit * SHARP_TOLERANCE) continue
            fits++
            sharpMiss("fitFrame", SharpOracle.fit(c.w, c.h, c.sw, c.sh, c.limit), fitFrame(c.w, c.h, c.sw, c.sh))
                ?.let { misses += "$c: $it" }
        }
        sharpReport(misses, fits, "fit cases differ")
        assertTrue(fits > 150, "$fits fit cases")
    }

    /** The worked examples of [LimitTableTest] hold for the oracle: ±1 px, as the Fill ones are 0.8.0's rounded frames. */
    @Test
    fun theOracleGivesTheWorkedExamples() {
        for (x in sharpExamples) {
            val got = SharpOracle.layout(x.w, x.h, x.sw, x.sh, x.sideways, x.position, x.limit)
            assertNull(sharpMiss("frame", got.frame, x.frame), x.label)
            assertNull(sharpMiss("dest", got.dest, x.dest), x.label)
            assertEquals(x.scale, got.scale, 1e-9, x.label)
        }
        for (x in sharpFitExamples) {
            assertNull(sharpMiss("fit", SharpOracle.fit(x.w, x.h, x.sw, x.sh, x.limit), x.dest), x.label)
        }
    }
}

// ---- Worked examples, by hand from sharp-0.9.0.md (±1 px, the Fill ones 0.8.0's own frames) ----

internal data class SharpExample(
    val label: String, val w: Int, val h: Int, val sw: Int, val sh: Int, val sideways: Boolean,
    val position: Float, val limit: Double?, val frame: Frame, val dest: Frame, val scale: Double,
)

internal data class SharpFitExample(val label: String, val w: Int, val h: Int, val sw: Int, val sh: Int, val limit: Double?, val dest: Frame)

internal val sharpExamples = listOf(
    // 7 Nov's original, 1500×1022 on the FP6: e = 2484 ÷ 1022 = 2.43 > 2.06, so 2×: 3000×2044. Across
    // 3000 > 1116 the visible 558 slides, (1500 − 558) × 0.5 = 471; down 2044 < 2484, centred at 220.
    SharpExample("7 Nov, 2×, the middle", 1500, 1022, 1116, 2484, false, 0.5f, 2.0,
        Frame(471, 0, 1029, 1022), Frame(0, 220, 1116, 2264), 2.0),
    SharpExample("7 Nov, 2×, left", 1500, 1022, 1116, 2484, false, 0f, 2.0,
        Frame(0, 0, 558, 1022), Frame(0, 220, 1116, 2264), 2.0),
    SharpExample("7 Nov, 2×, right", 1500, 1022, 1116, 2484, false, 1f, 2.0,
        Frame(942, 0, 1500, 1022), Frame(0, 220, 1116, 2264), 2.0),
    // A tiny picture sideways: as it goes on 480×640, e = 2484 ÷ 640 = 3.88; at 2× 960×1280, smaller both
    // ways: the whole picture centred, (1116 − 960) ÷ 2 = 78, (2484 − 1280) ÷ 2 = 602.
    SharpExample("640×480 sideways, 2×", 640, 480, 1116, 2484, true, 0.5f, 2.0,
        Frame(0, 0, 640, 480), Frame(78, 602, 1038, 1882), 2.0),
    // A panorama sideways: 2400×500 goes on as 500×2400, e = 1116 ÷ 500 = 2.23 > 2.06; at 2× 1000×4800:
    // across centred at 58, down the visible 1242 slides along the upright picture's columns.
    SharpExample("2400×500 sideways, 2×, p 0", 2400, 500, 1116, 2484, true, 0f, 2.0,
        Frame(0, 0, 1242, 500), Frame(58, 0, 1058, 2484), 2.0),
    SharpExample("2400×500 sideways, 2×, p 1", 2400, 500, 1116, 2484, true, 1f, 2.0,
        Frame(1158, 0, 2400, 500), Frame(58, 0, 1058, 2484), 2.0),
    // A portrait picture sideways: 600×1000 goes on as 1000×600, e = 2484 ÷ 600 = 4.14; at 2× 2000×1200:
    // across the visible 558 slides, position 0 the upright picture's bottom band (rows 442–1000); down
    // centred at 642.
    SharpExample("600×1000 sideways, 2×, p 0 (the bottom band)", 600, 1000, 1116, 2484, true, 0f, 2.0,
        Frame(0, 442, 600, 1000), Frame(0, 642, 1116, 1842), 2.0),
    SharpExample("600×1000 sideways, 2×, p 1 (the top band)", 600, 1000, 1116, 2484, true, 1f, 2.0,
        Frame(0, 0, 600, 558), Frame(0, 642, 1116, 1842), 2.0),
    // Within the tolerance: 1809×1206, e = 2484 ÷ 1206 = 2.0597 ≤ 2.06, plain Fill.
    SharpExample("1809×1206, e 2.0597, Fill", 1809, 1206, 1116, 2484, false, 0.5f, 2.0,
        wallpaperFrame(1809, 1206, 1116, 2484, 0.5f), Frame(0, 0, 1116, 2484), 2484.0 / 1206),
    // Just above it: 1807×1205, e = 2484 ÷ 1205 = 2.0614, so 2×: 3614×2410, the visible 558 from the
    // left edge; down centred at (2484 − 2410) ÷ 2 = 37.
    SharpExample("1807×1205, e 2.0614, 2×", 1807, 1205, 1116, 2484, false, 0f, 2.0,
        Frame(0, 0, 558, 1205), Frame(0, 37, 1116, 2447), 2.0),
    // No limit: 0.8.0's Fill whatever the enlargement.
    SharpExample("640×480, no limit", 640, 480, 1116, 2484, false, 0.5f, null,
        wallpaperFrame(640, 480, 1116, 2484, 0.5f), Frame(0, 0, 1116, 2484), 2484.0 / 480),
)

internal val sharpFitExamples = listOf(
    // 500×400: fit = min(1116 ÷ 500, 2484 ÷ 400) = 2.23 > 2.06, so 2×: 1000×800 centred.
    SharpFitExample("500×400, 2×", 500, 400, 1116, 2484, 2.0, Frame(58, 842, 1058, 1642)),
    // 300×1000: fit = min(3.72, 2.484) = 2.484, so 2×: 600×2000 centred.
    SharpFitExample("300×1000, 2×", 300, 1000, 1116, 2484, 2.0, Frame(258, 242, 858, 2242)),
    // 640×480: fit 1.74 ≤ 2.06: fitFrame.
    SharpFitExample("640×480, within", 640, 480, 1116, 2484, 2.0, fitFrame(640, 480, 1116, 2484)),
    SharpFitExample("500×400, no limit", 500, 400, 1116, 2484, null, fitFrame(500, 400, 1116, 2484)),
)

// ---- SH1's code against the oracle ----

@Ignore("SH1 implements limitedLayout")
class LimitTableTest {
    @Test
    fun layoutMatchesTheOracleOverTheTable() {
        val table = sharpLayoutTable()
        val misses = mutableListOf<String>()
        for (c in table) {
            val got = limitedLayout(c.w, c.h, c.sw, c.sh, c.sideways, c.position, c.limit)
            val want = SharpOracle.layout(c.w, c.h, c.sw, c.sh, c.sideways, c.position, c.limit)
            sharpMiss("frame", want.frame, got.frame)?.let { misses += "$c: $it" }
            sharpMiss("dest", want.dest, got.dest)?.let { misses += "$c: $it" }
            if (got.sideways != c.sideways) misses += "$c: sideways ${got.sideways}"
            if (!sameScale(got.scale, want.scale)) misses += "$c: scale ${got.scale}, the oracle ${want.scale}"
        }
        sharpReport(misses, table.size, "layout cases differ from the oracle")
    }

    @Test
    fun fitMatchesTheOracleOverTheTable() {
        val table = sharpFitTable()
        val misses = mutableListOf<String>()
        for (c in table) {
            sharpMiss("fit", SharpOracle.fit(c.w, c.h, c.sw, c.sh, c.limit), limitedFit(c.w, c.h, c.sw, c.sh, c.limit))
                ?.let { misses += "$c: $it" }
        }
        sharpReport(misses, table.size, "fit cases differ from the oracle")
    }

    @Test
    fun theWorkedExamples() {
        for (x in sharpExamples) {
            val got = limitedLayout(x.w, x.h, x.sw, x.sh, x.sideways, x.position, x.limit)
            assertNull(sharpMiss("frame", x.frame.toBox(), got.frame), x.label)
            assertNull(sharpMiss("dest", x.dest.toBox(), got.dest), x.label)
            assertEquals(x.sideways, got.sideways, x.label)
            assertEquals(x.scale, got.scale, 1e-9, x.label)
        }
        for (x in sharpFitExamples) {
            assertNull(sharpMiss("fit", x.dest.toBox(), limitedFit(x.w, x.h, x.sw, x.sh, x.limit)), x.label)
        }
    }

    /** Where no limit bites, exactly 0.8.0's frames (Fill and the whole picture), not only ±1 px. */
    @Test
    fun noLimitIsExactlyFillOverTheTable() {
        val misses = mutableListOf<String>()
        for (c in sharpLayoutTable()) {
            val e = if (c.sideways) SharpOracle.fillScale(c.h, c.w, c.sw, c.sh) else SharpOracle.fillScale(c.w, c.h, c.sw, c.sh)
            if (c.limit != null && e > c.limit * SHARP_TOLERANCE) continue
            val got = limitedLayout(c.w, c.h, c.sw, c.sh, c.sideways, c.position, c.limit)
            val fill = if (c.sideways) sidewaysFrame(c.w, c.h, c.sw, c.sh, c.position) else wallpaperFrame(c.w, c.h, c.sw, c.sh, c.position)
            if (got.frame != fill) misses += "$c: frame ${got.frame}, Fill $fill"
            if (got.dest != Frame(0, 0, c.sw, c.sh)) misses += "$c: dest ${got.dest}, not the screen"
        }
        for (c in sharpFitTable()) {
            val f = SharpOracle.fitScale(c.w, c.h, c.sw, c.sh)
            if (c.limit != null && f > c.limit * SHARP_TOLERANCE) continue
            val got = limitedFit(c.w, c.h, c.sw, c.sh, c.limit)
            if (got != fitFrame(c.w, c.h, c.sw, c.sh)) misses += "$c: fit $got, fitFrame ${fitFrame(c.w, c.h, c.sw, c.sh)}"
        }
        sharpReport(misses, sharpLayoutTable().size + sharpFitTable().size, "cases aren't 0.8.0's frames")
    }
}

internal fun Frame.toBox() = SharpBox(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

// ---- The properties (Testing 1's second bullet) ----

internal data class SharpRandomCase(
    val w: Int, val h: Int, val sw: Int, val sh: Int, val sideways: Boolean, val position: Float, val limit: Double?,
) {
    override fun toString() =
        "${w}×$h on ${sw}×$sh, ${if (sideways) "sideways" else "upright"}, p=$position, limit=$limit"
}

/** A side from 16 to 16,000 px, log-uniform. */
private fun sharpSide(r: Random): Int = exp(ln(16.0) + r.nextDouble() * ln(1000.0)).roundToInt()

/**
 * [n] seeded cases: portrait screens 320 to 2000 px wide and 1 to 2.6 times as tall; half the pictures
 * any size (16 to 16,000 px a side), half with e within 1% of the tolerance's edge; limits none, 2×, or
 * anything from 1× to 4×; positions 0, ½, 1 or anything.
 */
internal fun sharpRandomCases(seed: Long, n: Int): List<SharpRandomCase> {
    val r = Random(seed)
    return List(n) {
        val sw = 320 + r.nextInt(1681)
        val sh = (sw * (1.0 + 1.6 * r.nextDouble())).toInt()
        val sideways = r.nextBoolean()
        val limit = when (r.nextInt(5)) {
            0 -> null
            1 -> SHARP_LIMIT
            else -> 1.0 + 3.0 * r.nextDouble()
        }
        val position = when (r.nextInt(5)) {
            0 -> 0f
            1 -> 1f
            2 -> 0.5f
            else -> r.nextFloat()
        }
        val (w, h) = if (r.nextBoolean()) {
            sharpSide(r) to sharpSide(r)
        } else {
            val ratio = exp(ln(0.2) + r.nextDouble() * ln(25.0)) // as it goes on, W ÷ H, 0.2 to 5
            val e = (limit ?: SHARP_LIMIT) * SHARP_TOLERANCE * (0.99 + 0.02 * r.nextDouble())
            val (bigW, bigH) = if (ratio * sh > sw) {
                val bigH = max(1, (sh / e).roundToInt())
                max(1, (bigH * ratio).roundToInt()) to bigH
            } else {
                val bigW = max(1, (sw / e).roundToInt())
                bigW to max(1, (bigW / ratio).roundToInt())
            }
            if (sideways) bigH to bigW else bigW to bigH
        }
        SharpRandomCase(w, h, sw, sh, sideways, position, limit)
    }
}

private const val SHARP_SEED = 20_261_117L
private const val SHARP_CASES = 100_000

@Ignore("SH1 implements limitedLayout")
class LimitPropertiesTest {
    private val cases = sharpRandomCases(SHARP_SEED, SHARP_CASES)

    /** e as the picture goes on. */
    private fun e(c: SharpRandomCase) =
        if (c.sideways) SharpOracle.fillScale(c.h, c.w, c.sw, c.sh) else SharpOracle.fillScale(c.w, c.h, c.sw, c.sh)

    /** e at the tolerance's edge itself (an integer size can land on it): either answer is fair. */
    private fun onTheEdge(e: Double, limit: Double) = abs(e - limit * SHARP_TOLERANCE) <= 1e-9 * e

    private fun layout(c: SharpRandomCase, position: Float = c.position, limit: Double? = c.limit) =
        limitedLayout(c.w, c.h, c.sw, c.sh, c.sideways, position, limit)

    private fun fill(c: SharpRandomCase, position: Float = c.position) =
        if (c.sideways) sidewaysFrame(c.w, c.h, c.sw, c.sh, position) else wallpaperFrame(c.w, c.h, c.sw, c.sh, position)

    private fun check(what: String, test: (SharpRandomCase) -> String?) {
        val misses = mutableListOf<String>()
        for (c in cases) test(c)?.let { misses += "$c: $it" }
        sharpReport(misses, cases.size, "cases break: $what", shown = 15)
    }

    @Test
    fun theLimitHolds() = check("the scale is at most the limit × 1.03, and the limit itself past it") { c ->
        val limit = c.limit ?: return@check null
        val got = layout(c)
        val e = e(c)
        when {
            got.scale > limit * SHARP_TOLERANCE * (1 + 1e-12) -> "scale ${got.scale} over ${limit * SHARP_TOLERANCE}"
            onTheEdge(e, limit) -> null
            e > limit * SHARP_TOLERANCE && got.scale != limit -> "e $e: scale ${got.scale}, not the limit $limit"
            else -> null
        }
    }

    @Test
    fun noLimitIsFillExactly() = check("limit null is wallpaperFrame/sidewaysFrame on the whole screen") { c ->
        val got = layout(c, limit = null)
        when {
            got.frame != fill(c) -> "frame ${got.frame}, Fill ${fill(c)}"
            got.dest != Frame(0, 0, c.sw, c.sh) -> "dest ${got.dest}"
            got.sideways != c.sideways -> "sideways ${got.sideways}"
            !sameScale(got.scale, e(c)) -> "scale ${got.scale}, e ${e(c)}"
            else -> null
        }
    }

    @Test
    fun withinTheToleranceIsFillExactly() = check("e ≤ limit × 1.03 is Fill exactly") { c ->
        val limit = c.limit ?: return@check null
        val e = e(c)
        if (e > limit * SHARP_TOLERANCE || onTheEdge(e, limit)) return@check null
        val got = layout(c)
        when {
            got.frame != fill(c) -> "e $e: frame ${got.frame}, Fill ${fill(c)}"
            got.dest != Frame(0, 0, c.sw, c.sh) -> "e $e: dest ${got.dest}"
            !sameScale(got.scale, e) -> "scale ${got.scale}, e $e"
            else -> null
        }
    }

    @Test
    fun theFrameIsInsideThePicture() = check("the frame inside the picture") { c ->
        val f = layout(c).frame
        if (f.left in 0..f.right && f.right <= c.w && f.top in 0..f.bottom && f.bottom <= c.h) null else "frame $f"
    }

    @Test
    fun theDestIsInsideTheScreenAndCentredOnABandedAxis() = check("the dest inside the screen, centred where banded") { c ->
        val got = layout(c)
        val d = got.dest
        if (!(d.left in 0..d.right && d.right <= c.sw && d.top in 0..d.bottom && d.bottom <= c.sh)) return@check "dest $d"
        val bigW = if (c.sideways) c.h else c.w
        val bigH = if (c.sideways) c.w else c.h
        axisMiss("across", bigW * got.scale, c.sw, d.left, d.right) ?: axisMiss("down", bigH * got.scale, c.sh, d.top, d.bottom)
    }

    /** An axis drawn [drawn] long on a [screen] side: clearly larger, the dest spans it; clearly smaller, it's [drawn] long ±1, centred ±1. */
    private fun axisMiss(what: String, drawn: Double, screen: Int, start: Int, end: Int): String? = when {
        drawn >= screen + 1 && (start != 0 || end != screen) -> "$what: drawn $drawn, dest $start–$end, not the screen"
        drawn <= screen - 1 && abs((end - start) - drawn) > SHARP_PX -> "$what: drawn $drawn, dest $start–$end"
        drawn <= screen - 1 && abs(start - (screen - end)) > 1 -> "$what: dest $start–$end not centred on $screen"
        else -> null
    }

    @Test
    fun frameAndDestHaveTheSameShape() = check("frame × scale is the dest, ±1 px of the picture") { c ->
        val got = layout(c)
        val o = orientedFrame(got.frame, c.h, c.sideways)
        val across = (got.dest.right - got.dest.left) / got.scale - (o.right - o.left)
        val down = (got.dest.bottom - got.dest.top) / got.scale - (o.bottom - o.top)
        if (abs(across) <= SHARP_PX && abs(down) <= SHARP_PX) null
        else "frame ${got.frame} (as it goes on $o), dest ${got.dest}, scale ${got.scale}"
    }

    @Test
    fun aLargerLimitNeverCoversLess() {
        val r = Random(SHARP_SEED + 1)
        check("a larger limit covers at least as much of the screen") { c ->
            val limit = c.limit ?: return@check null
            val small = layout(c).dest
            val larger = limit * (1 + r.nextDouble())
            val big = layout(c, limit = larger).dest
            val none = layout(c, limit = null).dest
            when {
                big.right - big.left < small.right - small.left - 1 || big.bottom - big.top < small.bottom - small.top - 1 ->
                    "limit $limit: dest $small, limit $larger: $big"
                none != Frame(0, 0, c.sw, c.sh) -> "no limit: dest $none"
                else -> null
            }
        }
    }

    /** Position 0 starts the frame at the picture's start as it goes on (left, top; sideways the bottom row), 1 ends it at its end. */
    @Test
    fun positionsZeroAndOneReachThePicturesEdges() = check("positions 0 and 1 reach the picture's edges") { c ->
        val bigW = if (c.sideways) c.h else c.w
        val bigH = if (c.sideways) c.w else c.h
        val first = orientedFrame(layout(c, position = 0f).frame, c.h, c.sideways)
        val last = orientedFrame(layout(c, position = 1f).frame, c.h, c.sideways)
        when {
            first.left != 0 || first.top != 0 -> "position 0: $first as it goes on, not at 0, 0"
            last.right != bigW || last.bottom != bigH -> "position 1: $last as it goes on, not at $bigW, $bigH"
            else -> null
        }
    }

    @Test
    fun theLimitedFitHolds() = check("limitedFit: fitFrame within the tolerance, else the picture at the limit, centred") { c ->
        val got = limitedFit(c.w, c.h, c.sw, c.sh, c.limit)
        val fit = fitFrame(c.w, c.h, c.sw, c.sh)
        val f = SharpOracle.fitScale(c.w, c.h, c.sw, c.sh)
        val limit = c.limit
        when {
            limit == null || (f <= limit * SHARP_TOLERANCE && !onTheEdge(f, limit)) ->
                if (got == fit) null else "fit scale $f: $got, fitFrame $fit"
            onTheEdge(f, limit) -> null
            !(got.left in 0..got.right && got.right <= c.sw && got.top in 0..got.bottom && got.bottom <= c.sh) -> "$got outside the screen"
            abs((got.right - got.left) - c.w * limit) > SHARP_PX || abs((got.bottom - got.top) - c.h * limit) > SHARP_PX ->
                "$got isn't ${c.w * limit}×${c.h * limit}"
            abs(got.left - (c.sw - got.right)) > 1 || abs(got.top - (c.sh - got.bottom)) > 1 -> "$got not centred"
            else -> null
        }
    }
}
