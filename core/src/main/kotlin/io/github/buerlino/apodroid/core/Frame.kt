package io.github.buerlino.apodroid.core

import kotlin.math.roundToInt

/** A rectangle in a picture's pixels (with Fit, the screen's), right and bottom exclusive (as Android's `Rect`). */
data class Frame(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * The part of a [width]×[height] picture the wallpaper shows: the screen's shape, as large as
 * fits. It fills the picture's height and slides from the left edge ([position] 0) to the right
 * (1), or on a picture taller than the screen fills its width and slides from top to bottom.
 */
fun wallpaperFrame(width: Int, height: Int, screenWidth: Int, screenHeight: Int, position: Float): Frame {
    val w = width.toLong()
    val h = height.toLong()
    return if (w * screenHeight > h * screenWidth) {
        val frameW = (h * screenWidth / screenHeight).toInt()
        val left = ((width - frameW) * position).roundToInt()
        Frame(left, 0, left + frameW, height)
    } else {
        val frameH = (w * screenHeight / screenWidth).toInt()
        val top = ((height - frameH) * position).roundToInt()
        Frame(0, top, width, top + frameH)
    }
}

/**
 * Where a [width]×[height] picture goes with Fit, on a black canvas of the screen's size: all of
 * it, as large as fits, centred. A picture wider than the screen's shape fills the width, a
 * taller one the height.
 */
fun fitFrame(width: Int, height: Int, screenWidth: Int, screenHeight: Int): Frame {
    val w = width.toLong()
    val h = height.toLong()
    return if (w * screenHeight > h * screenWidth) {
        val fitH = (h * screenWidth / w).toInt()
        val top = (screenHeight - fitH) / 2
        Frame(0, top, screenWidth, top + fitH)
    } else {
        val fitW = (w * screenHeight / h).toInt()
        val left = (screenWidth - fitW) / 2
        Frame(left, 0, left + fitW, screenHeight)
    }
}
