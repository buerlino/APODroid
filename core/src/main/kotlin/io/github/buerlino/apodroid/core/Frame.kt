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
 * Whether a picture goes on sideways (0.9.0, sharp-0.9.0.md, The rules, Orientation; replaces SW3's
 * "Wide pictures sideways"): its part's [noted] value when there is one (⛶'s pill, true or false,
 * under every Size); else Auto's turn: sideways when the stored [size] is [Size.AUTO], the [source] is
 * APOD, the picture is landscape by its upright size ([picture], W > H, [sidewaysHelps]) and NASA's
 * text doesn't say it has an up ([hasUp], `:core`'s word table); else upright. Centre and Live never
 * turn by the rule, nor NASA's and Earth's pictures. [source] null is "my picture": never sideways
 * (it has no part). [picture] and [hasUp] are read only when the rule needs them (a size null:
 * unknown, upright). The one reading for the app's sets, Live's copy and ⛶'s pill (`Store.turnsSideways`).
 */
fun sidewaysOf(
    noted: Boolean?, size: Size, source: Source?,
    picture: () -> Pair<Int, Int>?, hasUp: () -> Boolean,
): Boolean = when {
    source == null -> false
    noted != null -> noted
    size != Size.AUTO || source != Source.APOD -> false
    else -> picture()?.let { (w, h) -> sidewaysHelps(w, h) } == true && !hasUp()
}

/**
 * A [width]×[height] picture's upright size by its EXIF [orientation] (SW3, the app's
 * `uprightSize`): 5 to 8 transpose it (a camera photo turned a quarter), so width and height
 * swap; 1 to 4 (as stored, mirrored, turned half) and anything else keep them.
 */
fun uprightOf(width: Int, height: Int, orientation: Int): Pair<Int, Int> =
    if (orientation in 5..8) height to width else width to height
