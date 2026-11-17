package io.github.buerlino.apodroid.core

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// Fill's frame on the subject (0.8.0, SA2; issue #3): an exact port of SA0's importance map and
// its position (`private/sa0/compute.py`'s `map_importance` and `position`, the pull on), so the
// app frames each picture as the user's two picking rounds judged it. Not retuned: every
// constant below is compute.py's. Double throughout; Python's round (half to even) where it
// rounds.

/** The copy's side across the frame's moving direction, in pixels (compute.py's `WORK_H`). */
const val SUBJECT_COPY_SIDE = 128

/**
 * How far from the middle the subject's position must be for Auto to use it (0.2 of the travel);
 * nearer, the middle. Why (SA0, the user's picks, 2026-11-15): round 1 (123 pictures) auto 23 :
 * middle 31 where it moved the frame less than 0.2, 17 : 9 where 0.2 or more; round 2 (92 fresh
 * pictures, the split fixed before) 23 : 20 below, 18 : 6 at or above (p 0.02). Over both rounds
 * small moves auto 46 : middle 51, clear ones auto 35 : middle 15.
 */
const val SUBJECT_MIN_MOVE = 0.2f

/** An edge frame must hold this much more of the map than the middle one (compute.py's `PULL`). */
private const val SUBJECT_PULL = 0.10

/**
 * Whether the frame of a [w]×[h] picture moves left–right (true) or up–down: the picture is wider
 * than the screen's shape, as [wallpaperFrame] decides.
 */
private fun movesSideways(w: Int, h: Int, screenW: Int, screenH: Int): Boolean =
    w.toLong() * screenH > h.toLong() * screenW

/**
 * The size of the copy the map is computed on, for a [w]×[h] picture on a [screenW]×[screenH]
 * screen: the side across the frame's moving direction 128 px (the height of a picture wider than
 * the screen's shape, else the width), the other in proportion, each at least 4, rounded half to
 * even (Python's round).
 */
fun subjectCopySize(w: Int, h: Int, screenW: Int, screenH: Int): Pair<Int, Int> {
    require(w > 0 && h > 0)
    val sc = SUBJECT_COPY_SIDE.toDouble() / (if (movesSideways(w, h, screenW, screenH)) h else w)
    return max(4, round(w * sc).toInt()) to max(4, round(h * sc).toInt())
}

// --- Pillow's BOX resize, byte for byte ---

private const val PRECISION_BITS = 32 - 8 - 2

/** Pillow's `precompute_coeffs` for its BOX filter, then `normalize_coeffs_8bpc`: per output pixel its first input pixel, their count and fixed-point weights. */
private class BoxCoeffs(inSize: Int, outSize: Int) {
    val ksize: Int
    val first = IntArray(outSize)
    val count = IntArray(outSize)
    val k: IntArray

    init {
        val scale = inSize.toDouble() / outSize
        val filterscale = max(scale, 1.0)
        val support = 0.5 * filterscale
        ksize = ceil(support).toInt() * 2 + 1
        val pre = DoubleArray(outSize * ksize)
        val ss = 1.0 / filterscale
        for (xx in 0 until outSize) {
            val center = (xx + 0.5) * scale
            var xmin = (center - support + 0.5).toInt()
            if (xmin < 0) xmin = 0
            var xmax = (center + support + 0.5).toInt()
            if (xmax > inSize) xmax = inSize
            xmax -= xmin
            var ww = 0.0
            for (x in 0 until xmax) {
                val a = (x + xmin - center + 0.5) * ss
                val wgt = if (a > -0.5 && a <= 0.5) 1.0 else 0.0
                pre[xx * ksize + x] = wgt
                ww += wgt
            }
            if (ww != 0.0) for (x in 0 until xmax) pre[xx * ksize + x] /= ww
            first[xx] = xmin
            count[xx] = xmax
        }
        k = IntArray(pre.size) { i ->
            val v = pre[i] * (1 shl PRECISION_BITS)
            if (pre[i] < 0) (-0.5 + v).toInt() else (0.5 + v).toInt()
        }
    }
}

private fun clip8(v: Int): Int = if (v >= 255) 255 else if (v <= 0) 0 else v

/**
 * A [w]×[h] picture ([argb] row by row, alpha ignored) scaled to [newW]×[newH] as Pillow's
 * `resize(…, Image.BOX)` does it, byte for byte (Pillow 12): each output pixel the plain mean of
 * the input pixels whose centres fall inside its box, in Pillow's fixed-point arithmetic, the
 * width first, then the height, each pass rounded to 8 bits. compute.py made its copies so; a
 * one-step bilinear scale is off by up to 0.69 in the position. The result is opaque.
 */
fun boxResize(argb: IntArray, w: Int, h: Int, newW: Int, newH: Int): IntArray {
    require(w > 0 && h > 0 && newW > 0 && newH > 0 && argb.size >= w * h)
    val half = 1 shl (PRECISION_BITS - 1)
    // The width: w×h to newW×h, per channel.
    var src = argb
    var sw = w
    if (newW != w) {
        val c = BoxCoeffs(w, newW)
        val out = IntArray(newW * h)
        for (y in 0 until h) {
            val row = y * w
            for (xx in 0 until newW) {
                var r = half
                var g = half
                var b = half
                val off = xx * c.ksize
                val x0 = row + c.first[xx]
                for (x in 0 until c.count[xx]) {
                    val p = src[x0 + x]
                    val kk = c.k[off + x]
                    r += (p shr 16 and 0xFF) * kk
                    g += (p shr 8 and 0xFF) * kk
                    b += (p and 0xFF) * kk
                }
                out[y * newW + xx] = (0xFF shl 24) or (clip8(r shr PRECISION_BITS) shl 16) or
                    (clip8(g shr PRECISION_BITS) shl 8) or clip8(b shr PRECISION_BITS)
            }
        }
        src = out
        sw = newW
    }
    if (newH != h) {
        val c = BoxCoeffs(h, newH)
        val out = IntArray(sw * newH)
        for (yy in 0 until newH) {
            val off = yy * c.ksize
            for (x in 0 until sw) {
                var r = half
                var g = half
                var b = half
                for (y in 0 until c.count[yy]) {
                    val p = src[(c.first[yy] + y) * sw + x]
                    val kk = c.k[off + y]
                    r += (p shr 16 and 0xFF) * kk
                    g += (p shr 8 and 0xFF) * kk
                    b += (p and 0xFF) * kk
                }
                out[yy * sw + x] = (0xFF shl 24) or (clip8(r shr PRECISION_BITS) shl 16) or
                    (clip8(g shr PRECISION_BITS) shl 8) or clip8(b shr PRECISION_BITS)
            }
        }
        src = out
    }
    return if (src === argb) IntArray(w * h) { argb[it] or (0xFF shl 24) } else src
}

// --- scipy's filters, mode "reflect" ---

/** An index outside 0 until [n] reflected as scipy's mode "reflect" (d c b a | a b c d), with period 2n. */
private fun reflect(i: Int, n: Int): Int {
    if (i in 0 until n) return i
    val p = 2 * n
    var j = i % p
    if (j < 0) j += p
    return if (j < n) j else p - 1 - j
}

/**
 * scipy's `correlate1d` of a [w]×[h] map with an odd, centred [weights], along x ([alongX]) or y,
 * summed as scipy sums a symmetric or antisymmetric kernel (the centre first, then each pair).
 */
private fun correlate1d(m: DoubleArray, w: Int, h: Int, weights: DoubleArray, alongX: Boolean): DoubleArray {
    val r = weights.size / 2
    val sym = (1..r).all { kotlin.math.abs(weights[r + it] - weights[r - it]) <= Math.ulp(1.0) }
    val anti = !sym && (1..r).all { kotlin.math.abs(weights[r + it] + weights[r - it]) <= Math.ulp(1.0) }
    val out = DoubleArray(w * h)
    for (y in 0 until h) for (x in 0 until w) {
        fun at(j: Int) = if (alongX) m[y * w + reflect(x + j, w)] else m[reflect(y + j, h) * w + x]
        var s: Double
        if (sym || anti) {
            s = at(0) * weights[r]
            for (j in -r until 0) s += (if (sym) at(j) + at(-j) else at(j) - at(-j)) * weights[r + j]
        } else {
            s = at(r) * weights[2 * r]
            for (j in -r until r) s += at(j) * weights[r + j]
        }
        out[y * w + x] = s
    }
    return out
}

/** scipy's `gaussian_filter(m, sigma)`: the normalised kernel exp(−x²/2σ²), radius int(4σ + 0.5), along y, then x. */
private fun gaussian(m: DoubleArray, w: Int, h: Int, sigma: Double): DoubleArray {
    val radius = (4.0 * sigma + 0.5).toInt()
    val f = -0.5 / (sigma * sigma)
    val k = DoubleArray(2 * radius + 1) { exp(f * ((it - radius) * (it - radius)).toDouble()) }
    val sum = k.sum()
    for (i in k.indices) k[i] /= sum
    return correlate1d(correlate1d(m, w, h, k, alongX = false), w, h, k, alongX = true)
}

private val SOBEL_D = doubleArrayOf(-1.0, 0.0, 1.0)
private val SOBEL_S = doubleArrayOf(1.0, 2.0, 1.0)

/** numpy's `percentile(m, q)`, the default linear method. */
internal fun percentile(m: DoubleArray, q: Double): Double {
    val s = m.sortedArray()
    val n = s.size
    val quantile = q / 100
    val vi = n * quantile + (1.0 + quantile * (1.0 - 1.0 - 1.0)) - 1.0
    val prev = floor(vi).coerceIn(0.0, (n - 1).toDouble())
    val next = (prev + 1).coerceAtMost((n - 1).toDouble())
    val t = vi - floor(vi)
    val a = s[prev.toInt()]
    val b = s[next.toInt()]
    val diff = b - a
    return if (t >= 0.5) b - diff * (1 - t) else a + diff * t
}

/** compute.py's `norm`: shifted to 0, divided by its 99.5th percentile and clipped to 0–1; as shifted when that percentile is 0. */
internal fun norm(m: DoubleArray): DoubleArray {
    val lo = m.min()
    val shifted = DoubleArray(m.size) { m[it] - lo }
    val mx = percentile(shifted, 99.5)
    if (!(mx > 0)) return shifted
    for (i in shifted.indices) shifted[i] = (shifted[i] / mx).coerceIn(0.0, 1.0)
    return shifted
}

/**
 * The importance map of a [w]×[h] copy (compute.py's `map_importance`): 0.4 smoothed brightness
 * + 0.4 smoothed detail (the Sobel gradient of the smoothed grey) + 0.2 saturation × value, each
 * normalised, then smoothed by σ = 4% of the shorter side. Row by row.
 */
internal fun subjectMap(argb: IntArray, w: Int, h: Int): DoubleArray {
    val n = w * h
    val short = min(w, h).toDouble()
    val grey = DoubleArray(n)
    val satv = DoubleArray(n)
    for (i in 0 until n) {
        val p = argb[i]
        val r = (p shr 16 and 0xFF) / 255.0
        val g = (p shr 8 and 0xFF) / 255.0
        val b = (p and 0xFF) / 255.0
        grey[i] = r * .299 + g * .587 + b * .114
        val mx = max(max(r, g), b)
        val mn = min(min(r, g), b)
        satv[i] = (if (mx > 0.05) (mx - mn) / max(mx, 1e-6) else 0.0) * mx
    }
    val gb = gaussian(grey, w, h, max(0.7, 0.01 * short))
    val bright = norm(gb)
    val sy = correlate1d(correlate1d(gb, w, h, SOBEL_D, alongX = false), w, h, SOBEL_S, alongX = true)
    val sx = correlate1d(correlate1d(gb, w, h, SOBEL_D, alongX = true), w, h, SOBEL_S, alongX = false)
    val gm = DoubleArray(n) { hypot(sy[it], sx[it]) }
    val s = 0.04 * short
    val detail = norm(gaussian(gm, w, h, s))
    val sat = norm(gaussian(satv, w, h, s))
    val mix = DoubleArray(n) { 0.4 * bright[it] + 0.4 * detail[it] + 0.2 * sat[it] }
    return gaussian(mix, w, h, s)
}

/**
 * compute.py's `position` with the pull: where the frame holds the most of the [w]×[h] [map],
 * 0–1 along its moving direction (left–right when [horizontal], else top–bottom); [frac] the
 * frame's length ÷ the picture's along it. An edge frame must beat the middle one by 10%; the
 * first maximum wins.
 */
internal fun mapPosition(map: DoubleArray, w: Int, h: Int, horizontal: Boolean, frac: Double): Double {
    val n = if (horizontal) w else h
    val prof = DoubleArray(n)
    for (y in 0 until h) for (x in 0 until w) prof[if (horizontal) x else y] += map[y * w + x]
    val fl = max(1, round(frac * n).toInt())
    if (fl >= n) return 0.5
    val c = DoubleArray(n + 1)
    for (i in 0 until n) c[i + 1] = c[i] + prof[i]
    val len = n - fl + 1
    val sums = DoubleArray(len) { c[it + fl] - c[it] }
    val ps = DoubleArray(len) { it.toDouble() / (len - 1) }
    if (prof.sum() <= 0) return 0.5
    val mid = sums[round(0.5 * (len - 1)).toInt()]
    var best = 0
    if (mid <= 0) {
        for (i in 1 until len) if (sums[i] > sums[best]) best = i
        return ps[best]
    }
    val score = DoubleArray(len) { sums[it] / mid - SUBJECT_PULL * (2 * (ps[it] - 0.5)).let { d -> d * d } }
    for (i in 1 until len) if (score[i] > score[best]) best = i
    return ps[best]
}

/**
 * The subject's position (0–1, raw) for Fill's frame on a [w]×[h] picture shown upright on a
 * [screenW]×[screenH] screen, from its [copyW]×[copyH] copy ([argb], made by [boxResize] to
 * [subjectCopySize]). The sideways position: [sidewaysSubjectPosition].
 */
fun subjectPosition(
    argb: IntArray, copyW: Int, copyH: Int, w: Int, h: Int, screenW: Int, screenH: Int,
): Float {
    require(copyW > 0 && copyH > 0 && argb.size >= copyW * copyH && w > 0 && h > 0)
    val horizontal = movesSideways(w, h, screenW, screenH)
    val frac = if (horizontal) h.toDouble() * screenW / screenH / w else w.toDouble() * screenH / screenW / h
    return mapPosition(subjectMap(argb, copyW, copyH), copyW, copyH, horizontal, frac).toFloat()
}

/**
 * A [w]×[h] picture ([argb] row by row) turned 90° clockwise, its top to the right: [h]×[w]
 * (PIL's `transpose(ROTATE_270)`).
 */
internal fun turnClockwise(argb: IntArray, w: Int, h: Int): IntArray =
    // turned(x', y') = picture(x = y', y = h − 1 − x'), the turned picture h wide.
    IntArray(w * h) { i -> argb[(h - 1 - i % h) * w + i / h] }

/**
 * The subject's position (0–1, raw) of [sidewaysFrame]'s band on a [w]×[h] picture (SA3c), from
 * the decoded picture's [pixels] ([dw]×[dh], any scale, upright): the picture turned clockwise and
 * treated as its own [h]×[w] picture, as SA0's method on the turned picture (`sideways.py`): its
 * own [subjectCopySize] (128 across the turned frame's moving direction), its own [boxResize]d
 * copy (turned first, then resized, PIL's order), its own map and position with the pull. Since
 * [sidewaysFrame] is [wallpaperFrame] of the turned picture, the position is that band's directly.
 */
fun sidewaysSubjectPosition(pixels: IntArray, dw: Int, dh: Int, w: Int, h: Int, screenW: Int, screenH: Int): Float {
    require(dw > 0 && dh > 0 && pixels.size >= dw * dh && w > 0 && h > 0)
    val (cw, ch) = subjectCopySize(h, w, screenW, screenH)
    val copy = boxResize(turnClockwise(pixels, dw, dh), dh, dw, cw, ch)
    return subjectPosition(copy, cw, ch, h, w, screenW, screenH)
}

/**
 * The position Auto uses: the subject's [raw] position, or with [fallback] (Advanced's switch
 * off) the middle when it would move the frame less than [SUBJECT_MIN_MOVE] from it.
 */
fun autoPosition(raw: Float, fallback: Boolean): Float =
    if (fallback && kotlin.math.abs(raw - 0.5f) < SUBJECT_MIN_MOVE) 0.5f else raw

/**
 * Where a still set's frame stands (SA3a): the part's [partPosition] when ⛶ noted one; else with
 * [Size.AUTO] [autoPosition] of the subject's [subjectRaw] position (the one for the picture's
 * orientation, upright or sideways; none noted or none found: the middle); else (Centre, and the
 * screens Live leaves still) the middle.
 */
fun framePosition(size: Size, partPosition: Float?, subjectRaw: Float?, fallback: Boolean): Float = when {
    partPosition != null -> partPosition
    size == Size.AUTO && subjectRaw != null -> autoPosition(subjectRaw, fallback)
    else -> 0.5f
}

/**
 * Auto's position for "Keep pictures sharp"'s limited frame (SH2c): [subjectPosition] (or, [sideways],
 * [sidewaysSubjectPosition]) of the same decoded [pixels] ([dw]×[dh] of a [w]×[h] picture) with the
 * limited frame's shape ([limitedShape]) as its "screen", so the window is the limited frame's length.
 * Null when the limit doesn't bite; 0.5 when no axis moves (the picture whole both ways).
 */
fun limitedSubjectPosition(
    pixels: IntArray, dw: Int, dh: Int, w: Int, h: Int, screenW: Int, screenH: Int, sideways: Boolean,
    limit: Double = SHARP_LIMIT,
): Float? {
    val (fw, fh) = limitedShape(w, h, screenW, screenH, sideways, limit) ?: return null
    val (bigW, bigH) = if (sideways) h to w else w to h
    if (fw >= bigW && fh >= bigH) return 0.5f
    if (sideways) return sidewaysSubjectPosition(pixels, dw, dh, w, h, fw, fh)
    val (cw, ch) = subjectCopySize(w, h, fw, fh)
    return subjectPosition(boxResize(pixels, dw, dh, cw, ch), cw, ch, w, h, fw, fh)
}
