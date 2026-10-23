package io.github.buerlino.apodroid.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// The background behind a whole picture (0.6.0, A1): black when the picture's edges are nearly
// black (a planet or a moon on space blends in), else the picture blurred and darkened.

/** The share of each side that counts as the edge: the outer strip, about 3%. */
const val EDGE_STRIP = 0.03

/** A grey value (0–255) up to this is nearly black. */
const val NEARLY_BLACK = 40

/** The share of the edge's pixels that must be nearly black. */
const val EDGE_BLACK_SHARE = 0.97

/** How much darker the blurred background is than the picture: 35%. */
const val BLUR_DARKEN = 0.35

/** The grey value (0–255) of an ARGB pixel, by the usual luma weights. */
fun gray(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (r * 299 + g * 587 + b * 114 + 500) / 1000
}

/**
 * Whether a [w]×[h] picture's edges are nearly black, from a small grey copy (about 200 px
 * wide; [gray] holds 0–255 per pixel, row by row): true when at least [EDGE_BLACK_SHARE] of the
 * outer strip ([EDGE_STRIP] of each side, at least one pixel) is [NEARLY_BLACK]. Each pixel
 * counts once, also in the corners.
 */
fun edgesBlack(gray: IntArray, w: Int, h: Int): Boolean {
    require(w > 0 && h > 0 && gray.size >= w * h)
    val sx = ceil(w * EDGE_STRIP).toInt().coerceIn(1, w)
    val sy = ceil(h * EDGE_STRIP).toInt().coerceIn(1, h)
    var total = 0
    var black = 0
    for (y in 0 until h) {
        val edgeRow = y < sy || y >= h - sy
        for (x in 0 until w) {
            if (edgeRow || x < sx || x >= w - sx) {
                total++
                if (gray[y * w + x] <= NEARLY_BLACK) black++
            }
        }
    }
    return black >= EDGE_BLACK_SHARE * total
}

/**
 * The size of the copy the blurred background is made from: a quarter of the screen's width
 * and height (1/16 of its pixels), at least 1×1. The app decodes the picture's middle in the
 * screen's shape (`wallpaperFrame` at 0.5) at this size and draws the result enlarged.
 */
fun blurSize(screenWidth: Int, screenHeight: Int): Pair<Int, Int> =
    max(1, (screenWidth / 4.0).roundToInt()) to max(1, (screenHeight / 4.0).roundToInt())

/** The box blur's radius for a [w]×[h] copy: 1/25 of its shorter side, at least 1 px. */
fun blurRadius(w: Int, h: Int): Int = max(1, (minOf(w, h) / 25.0).roundToInt())

/**
 * The blurred background from a [w]×[h] ARGB copy ([blurSize]): three box-blur passes of
 * [radius] each way (close to a Gaussian; the edge pixels extend outwards, so the edges don't
 * darken), then each colour [BLUR_DARKEN] darker. Opaque; [argb] is left as it is.
 */
fun blurBackground(argb: IntArray, w: Int, h: Int, radius: Int = blurRadius(w, h)): IntArray {
    require(w > 0 && h > 0 && argb.size >= w * h && radius >= 0)
    val n = w * h
    val channels = Array(3) { c -> IntArray(n) { (argb[it] shr (16 - 8 * c)) and 0xFF } }
    val tmp = IntArray(n)
    for (ch in channels) repeat(3) {
        boxPass(ch, tmp, w, h, radius, horizontal = true)
        boxPass(tmp, ch, w, h, radius, horizontal = false)
    }
    val keep = 1 - BLUR_DARKEN
    return IntArray(n) { i ->
        var out = 0xFF shl 24
        for (c in 0..2) out = out or (((channels[c][i] * keep).roundToInt()) shl (16 - 8 * c))
        out
    }
}

/** One box blur of [radius] along rows ([horizontal]) or columns, [from] into [to], edges clamped. */
private fun boxPass(from: IntArray, to: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
    val len = if (horizontal) w else h
    val lines = if (horizontal) h else w
    val size = 2 * radius + 1
    for (line in 0 until lines) {
        fun at(i: Int): Int {
            val k = i.coerceIn(0, len - 1)
            return if (horizontal) from[line * w + k] else from[k * w + line]
        }
        var sum = 0
        for (i in -radius..radius) sum += at(i)
        for (i in 0 until len) {
            val value = (sum + size / 2) / size
            if (horizontal) to[line * w + i] = value else to[i * w + line] = value
            sum += at(i + radius + 1) - at(i - radius)
        }
    }
}
