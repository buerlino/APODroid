package io.github.buerlino.apodroid.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// 0.9.0, "Keep pictures sharp" (sharp-0.9.0.md, Terms and The rules): Fill's enlargement held to a
// limit, the rest of the screen the whole picture's background. SH1; the app in SH2c.

/** The limit on Fill's enlargement with "Keep pictures sharp" on: at most 2×. */
const val SHARP_LIMIT = 2.0

/** An enlargement up to the limit × this (2.06 at 2×) is still plain Fill. */
const val SHARP_TOLERANCE = 1.03

/** [frame]: the part of the upright picture shown, in its pixels; [dest]: where it's drawn on the screen. */
data class Layout(val frame: Frame, val dest: Frame, val sideways: Boolean, val scale: Double)

/**
 * Where an upright [width]×[height] picture goes on a [screenWidth]×[screenHeight] screen, turned
 * clockwise when [sideways], at Fill's enlargement e or, when e is over [limit] × [SHARP_TOLERANCE],
 * at [limit]: along an axis larger than the screen the frame slides by [position], along one smaller
 * it's whole and centred. [limit] null, or e within the tolerance: exactly 0.8.0's Fill
 * ([wallpaperFrame], or [sidewaysFrame] sideways, on the whole screen), [Layout.scale] e.
 */
fun limitedLayout(
    width: Int, height: Int, screenWidth: Int, screenHeight: Int,
    sideways: Boolean, position: Float, limit: Double?,
): Layout {
    // The picture as it goes on, W×H: turned, its top at the screen's right.
    val bigW = if (sideways) height else width
    val bigH = if (sideways) width else height
    val e = max(screenWidth.toDouble() / bigW, screenHeight.toDouble() / bigH)
    if (limit == null || e <= limit * SHARP_TOLERANCE) {
        val fill = if (sideways) {
            sidewaysFrame(width, height, screenWidth, screenHeight, position)
        } else {
            wallpaperFrame(width, height, screenWidth, screenHeight, position)
        }
        return Layout(fill, Frame(0, 0, screenWidth, screenHeight), sideways, e)
    }
    val x = limitedAxis(bigW, screenWidth, limit, position)
    val y = limitedAxis(bigH, screenHeight, limit, position)
    // Sideways the oriented x runs from the upright picture's bottom row up, the oriented y along its
    // columns, so the frame maps back as sidewaysFrame maps (position 0 the bottom band).
    val frame = if (sideways) Frame(y[0], height - x[1], y[1], height - x[0]) else Frame(x[0], y[0], x[1], y[1])
    return Layout(frame, Frame(x[2], y[2], x[3], y[3]), sideways, limit)
}

/**
 * One axis of the oriented picture, [length] long, drawn at [scale] on a [screen] long side: frame
 * start, frame end (in the picture's pixels), dest start, dest end (on the screen). Larger than the
 * screen: the visible length `screen ÷ scale` slides by [position] (0 the start, 1 the end) and the
 * dest is the whole side; else the whole length, centred.
 */
private fun limitedAxis(length: Int, screen: Int, scale: Double, position: Float): IntArray {
    val drawn = length * scale
    return if (drawn > screen) {
        val visible = min(length, (screen / scale).roundToInt())
        val start = ((length - visible) * position).roundToInt()
        intArrayOf(start, start + visible, 0, screen)
    } else {
        val dest = min(screen, drawn.roundToInt())
        val start = (screen - dest) / 2
        intArrayOf(0, length, start, start + dest)
    }
}

/**
 * The whole picture's place on the screen ([fitFrame]) at a scale of at most [limit] (with the
 * tolerance), centred. [limit] null, or the fit scale within the tolerance: exactly [fitFrame].
 */
fun limitedFit(width: Int, height: Int, screenWidth: Int, screenHeight: Int, limit: Double?): Frame {
    val fit = min(screenWidth.toDouble() / width, screenHeight.toDouble() / height)
    if (limit == null || fit <= limit * SHARP_TOLERANCE) return fitFrame(width, height, screenWidth, screenHeight)
    val w = min(screenWidth, (width * limit).roundToInt())
    val h = min(screenHeight, (height * limit).roundToInt())
    val left = (screenWidth - w) / 2
    val top = (screenHeight - h) / 2
    return Frame(left, top, left + w, top + h)
}
