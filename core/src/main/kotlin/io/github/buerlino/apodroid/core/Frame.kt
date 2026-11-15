package io.github.buerlino.apodroid.core

import kotlin.math.roundToInt

/** A rectangle in a picture's pixels (with [fitFrame], the screen's), right and bottom exclusive (as Android's `Rect`). */
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
 * Where a [width]×[height] picture goes as the whole picture, on a canvas of the screen's size: all of
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

/**
 * The part of an upright [width]×[height] picture the screen shows when the picture is turned 90°
 * clockwise (its top goes to the screen's right), in the upright picture's pixels. It is
 * [wallpaperFrame] of the turned picture (`height`×`width`) mapped back: [position] 0 is the turned
 * picture's left edge, which is the upright picture's bottom band (rows), 1 its top band. On a
 * normal wide picture the band spans the full width; on a panorama whose turned picture is taller
 * than the screen it is a strip of whole columns' height, moving left to right as the position
 * goes 0 to 1.
 */
fun sidewaysFrame(width: Int, height: Int, screenWidth: Int, screenHeight: Int, position: Float): Frame {
    val f = wallpaperFrame(height, width, screenWidth, screenHeight, position)
    return Frame(f.top, height - f.right, f.bottom, height - f.left)
}

/** Whether turning a [width]×[height] picture sideways can help: only a landscape one gains. */
fun sidewaysHelps(width: Int, height: Int): Boolean = width > height

/**
 * Whether "Wide pictures sideways" (SW3) turns a [width]×[height] picture (upright, EXIF applied)
 * by default: 3:2 or wider (width ≥ 1.5 × height; night default, the alternative 1.4:1).
 */
fun sidewaysByDefault(width: Int, height: Int): Boolean = height > 0 && width.toLong() * 2 >= height.toLong() * 3

/**
 * Whether a picture goes on sideways (SW3): its part's [noted] value when there is one (⛶'s
 * pill, true or false), else [sidewaysByDefault] of its upright [size] while [wideSideways]
 * ("Wide pictures sideways", the stored switch) is on, else upright. [size] is read only then
 * (null: unknown, upright).
 */
fun sidewaysOf(noted: Boolean?, wideSideways: Boolean, size: () -> Pair<Int, Int>?): Boolean =
    noted ?: (wideSideways && size()?.let { (w, h) -> sidewaysByDefault(w, h) } == true)

/**
 * A [width]×[height] picture's upright size by its EXIF [orientation] (SW3, the app's
 * `uprightSize`): 5 to 8 transpose it (a camera photo turned a quarter), so width and height
 * swap; 1 to 4 (as stored, mirrored, turned half) and anything else keep them.
 */
fun uprightOf(width: Int, height: Int, orientation: Int): Pair<Int, Int> =
    if (orientation in 5..8) height to width else width to height
