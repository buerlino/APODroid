package io.github.buerlino.apodroid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withClip
import io.github.buerlino.apodroid.core.Frame
import io.github.buerlino.apodroid.core.blurBackground
import io.github.buerlino.apodroid.core.blurSize
import io.github.buerlino.apodroid.core.edgeCopySize
import io.github.buerlino.apodroid.core.edgesBlack
import io.github.buerlino.apodroid.core.fitFrame
import io.github.buerlino.apodroid.core.gray
import io.github.buerlino.apodroid.core.sidewaysFrame as coreSidewaysFrame
import io.github.buerlino.apodroid.core.topThirdMean
import io.github.buerlino.apodroid.core.wallpaperFrame as coreWallpaperFrame
import java.io.File
import kotlin.math.roundToInt

// The drawing (L4): one place that draws a picture onto a canvas of a given size, panned (Fill's
// frame at a position) or whole (on black or blurred). Its users: Store.set's whole picture and a
// bright picture's frame (BR2), and from 0.7.0's Live the engine and ⛶'s preview, both darkened by
// the picture's one factor (BR3). Sideways (0.8.0, SW2, issue #4): a picture turned 90° clockwise,
// its top at the screen's right; every decode here takes the flag, decodes upright (the band
// `:core`'s sidewaysFrame names) and draws it through a rotating Matrix ([drawTurned]).

/**
 * [picture]'s frame at [position] (0–1, [wallpaperFrame]: the [screen]'s portrait shape) drawn to
 * fill [canvas]'s drawing area of [screen]'s size, filtered, darkened by [factor] ([darkeningPaint]).
 */
fun drawPanned(canvas: Canvas, picture: Bitmap, screen: Size, position: Float, factor: Float = 1f) {
    canvas.drawBitmap(
        picture, wallpaperFrame(picture.width, picture.height, screen, position),
        Rect(0, 0, screen.width, screen.height), darkeningPaint(factor),
    )
}

/**
 * The darkening (BR2, issue #2): a filtering [Paint] that scales R, G and B by [factor] (`:core`'s
 * `darkening`, 0.7–1), one linear scale, so the hues stay and black stays black; none at 1.
 */
fun darkeningPaint(factor: Float) = Paint(Paint.FILTER_BITMAP_FLAG).apply {
    if (factor < 1f) colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(factor, factor, factor, 1f) })
}

/** [bitmap] darkened by [factor] ([darkeningPaint]) on a new bitmap of its size; [bitmap] is recycled. */
fun darkened(bitmap: Bitmap, factor: Float): Bitmap =
    createBitmap(bitmap.width, bitmap.height).also {
        Canvas(it).drawBitmap(bitmap, 0f, 0f, darkeningPaint(factor))
        bitmap.recycle()
    }

/** How high the copy is that [topThirdGrey] measures: BR0 measured the frame shrunk to 90×200. */
const val MEASURE_HEIGHT = 200

/**
 * The mean grey (0–255) of [bitmap]'s top third (`:core`'s [topThirdMean], by [gray]'s weights),
 * measured on a copy at most [MEASURE_HEIGHT] high.
 */
fun topThirdGrey(bitmap: Bitmap): Double {
    val h = minOf(bitmap.height, MEASURE_HEIGHT)
    val w = maxOf(1, (bitmap.width.toLong() * h / bitmap.height).toInt())
    val copy = if (h == bitmap.height) bitmap else bitmap.scale(w, h)
    val pixels = IntArray(w * h).also { copy.getPixels(it, 0, w, 0, 0, w, h) }
    if (copy !== bitmap) copy.recycle()
    return topThirdMean(IntArray(pixels.size) { gray(pixels[it]) }, w, h)
}

/** The mean grey of the top third of [file]'s frame at [position] ([topThirdGrey], [decodeFrame] small), turned when [sideways]. */
fun frameGrey(file: File, screen: Size, position: Float, sideways: Boolean = false): Double =
    decodeFrame(file, screen, position, MEASURE_HEIGHT, sideways).let { if (sideways) turned(it) else it }
        .let { frame -> topThirdGrey(frame).also { frame.recycle() } }

/**
 * The mean grey of the top third of [file] as a whole ([topThirdGrey]), upright, decoded straight
 * to at most [MEASURE_HEIGHT] high: Live's measure of a panned picture (BR3), one per picture, so
 * the darkening doesn't change as the frame moves. [sideways]: of the picture turned (SW2), at
 * most [MEASURE_HEIGHT] high turned, so as wide upright.
 */
fun pictureGrey(file: File, sideways: Boolean = false): Double {
    val copy = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = info.size.width to info.size.height
        if (!sideways && h > MEASURE_HEIGHT) decoder.setTargetSize(maxOf(1, (w.toLong() * MEASURE_HEIGHT / h).toInt()), MEASURE_HEIGHT)
        if (sideways && w > MEASURE_HEIGHT) decoder.setTargetSize(MEASURE_HEIGHT, maxOf(1, (h.toLong() * MEASURE_HEIGHT / w).toInt()))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }.let { if (sideways) turned(it) else it }
    return topThirdGrey(copy).also { copy.recycle() }
}

/**
 * The mean grey of the top third of [file]'s whole picture on its background ([drawWhole]),
 * drawn at the [screen]'s shape at most [MEASURE_HEIGHT] high: Live's measure of a whole picture
 * and the preview's (BR3), what the screen shows, as `Store.set` measures it at full size.
 */
fun wholeGrey(file: File, screen: Size, sideways: Boolean = false): Double {
    val h = minOf(screen.height, MEASURE_HEIGHT)
    val small = Size(maxOf(1, (screen.width.toLong() * h / screen.height).toInt()), h)
    val drawn = wholePicture(file, small, sideways)
    return topThirdGrey(drawn).also { drawn.recycle() }
}

/**
 * [file]'s frame at [position] ([pictureFrame], the [screen]'s shape), upright, decoded alone
 * (the decoder's crop: a wide panorama at the screen's height would be hundreds of MB whole) and
 * straight to at most [height] high on the screen: the screen's for drawing, [MEASURE_HEIGHT]
 * for measuring. A picture smaller than that stays its size. The crop and `setTargetSize` are in
 * the upright picture's coordinates, as `info.size` (AOSP's ImageDecoder.cpp). [sideways]: the
 * band the turned picture shows (`:core`'s sidewaysFrame), still upright, so its width is what
 * stands at most [height] high once [drawTurned] turns it.
 */
fun decodeFrame(file: File, screen: Size, position: Float, height: Int, sideways: Boolean = false): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = info.size.width to info.size.height
        val frame = pictureFrame(w, h, screen, position, sideways)
        val scale = minOf(1.0, height.toDouble() / if (sideways) frame.width() else frame.height())
        val (tw, th) = maxOf(1, (w * scale).roundToInt()) to maxOf(1, (h * scale).roundToInt())
        if (scale < 1.0) decoder.setTargetSize(tw, th)
        decoder.crop = pictureFrame(tw, th, screen, position, sideways)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

/**
 * [band], an upright bitmap ([decodeFrame] sideways), turned 90° clockwise (its top at the right)
 * and drawn to fill [canvas]'s drawing area of [screen]'s size, darkened by [factor]: the
 * sideways set's drawing (SW2), as [drawPanned] is Fill's.
 */
fun drawSideways(canvas: Canvas, band: Bitmap, screen: Size, factor: Float = 1f) =
    drawTurned(canvas, band, null, Rect(0, 0, screen.width, screen.height), darkeningPaint(factor))

/**
 * Draws [src] of [bitmap] (null: all of it) turned 90° clockwise onto [dst] of [canvas]: the
 * upright top-left corner at [dst]'s top right, so [src]'s width spans [dst]'s height. One
 * rotating [Matrix], no turned copy; clipped to [dst].
 */
fun drawTurned(canvas: Canvas, bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
    val from = src ?: Rect(0, 0, bitmap.width, bitmap.height)
    val matrix = Matrix().apply {
        setTranslate(-from.left.toFloat(), -from.top.toFloat())
        postScale(dst.height().toFloat() / from.width(), dst.width().toFloat() / from.height())
        postRotate(90f)
        postTranslate(dst.right.toFloat(), dst.top.toFloat())
    }
    canvas.withClip(dst) { drawBitmap(bitmap, matrix, paint) }
}

/** [bitmap] turned 90° clockwise on a new bitmap (its top at the right); [bitmap] is recycled. */
fun turned(bitmap: Bitmap): Bitmap =
    createBitmap(bitmap.height, bitmap.width).also {
        drawTurned(Canvas(it), bitmap, null, Rect(0, 0, it.width, it.height), null)
        bitmap.recycle()
    }

/** The whole picture of [file] on a bitmap of the [screen]'s size ([drawWhole]), turned when [sideways]. */
fun wholePicture(file: File, screen: Size, sideways: Boolean = false): Bitmap =
    createBitmap(screen.width, screen.height).also { drawWhole(Canvas(it), file, screen, sideways) }

/**
 * The whole picture (A3), drawn onto [canvas], whose drawing area is [screen]'s size: [file]
 * upright, all of it as large as fits ([fitFrame]), centred on black when its edges are nearly
 * black ([edgesBlack]: a planet on space blends in), else on the picture blurred and darkened
 * ([blurred]). Decoded straight to the frame's size and drawn 1:1: shrinking the rendition (3726×2484 for a
 * 3:2 post) about 3× in one bilinear step can make stars jagged. `info.size` and `setTargetSize`
 * are both the upright size (Android 10's codec respects EXIF, 11+ swaps for the origin; AOSP's
 * ImageDecoder.cpp). [sideways] (SW2): the picture turned 90° clockwise, fitted as turned, decoded
 * upright at the turned fit's size and drawn 1:1 through [drawTurned]; its background the turned
 * picture's middle. The black edges are measured upright (the strip is the same on all four sides).
 */
fun drawWhole(canvas: Canvas, file: File, screen: Size, sideways: Boolean = false) {
    val background = if (hasBlackEdges(file)) null else blurred(file, screen, sideways)
    lateinit var frame: Frame
    val picture = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = info.size.width to info.size.height
        frame = if (sideways) fitFrame(h, w, screen.width, screen.height) else fitFrame(w, h, screen.width, screen.height)
        val (fw, fh) = maxOf(1, frame.right - frame.left) to maxOf(1, frame.bottom - frame.top)
        if (sideways) decoder.setTargetSize(fh, fw) else decoder.setTargetSize(fw, fh)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    canvas.run {
        drawColor(Color.BLACK)
        background?.let { blur ->
            drawBitmap(blur, null, Rect(0, 0, screen.width, screen.height), Paint(Paint.FILTER_BITMAP_FLAG))
            blur.recycle()
        }
        if (sideways) {
            drawTurned(this, picture, null, Rect(frame.left, frame.top, frame.left + picture.height, frame.top + picture.width), null)
        } else {
            drawBitmap(picture, frame.left.toFloat(), frame.top.toFloat(), null)
        }
    }
    picture.recycle()
}

/** Whether [file]'s edges are nearly black, from an upright grey copy [edgeCopySize] large ([edgesBlack]). */
private fun hasBlackEdges(file: File): Boolean {
    val copy = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = edgeCopySize(info.size.width, info.size.height)
        decoder.setTargetSize(w, h)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val w = copy.width
    val h = copy.height
    val pixels = IntArray(w * h).also { copy.getPixels(it, 0, w, 0, 0, w, h) }
    copy.recycle()
    return edgesBlack(IntArray(pixels.size) { gray(pixels[it]) }, w, h)
}

/**
 * The blurred background ([blurBackground]): [file]'s middle in the [screen]'s shape, upright,
 * at [blurSize] (decoded halved while it still covers that, then scaled), blurred and darkened.
 * The caller draws it enlarged to the screen. [sideways]: the turned picture's middle, turned.
 */
private fun blurred(file: File, screen: Size, sideways: Boolean = false): Bitmap {
    val (bw, bh) = blurSize(screen.width, screen.height)
    val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val frame = pictureFrame(info.size.width, info.size.height, screen, 0.5f, sideways)
        // The band's width stands across the screen's height once turned.
        val (across, high) = if (sideways) frame.height() to frame.width() else frame.width() to frame.height()
        var sample = 1
        while (across / (sample * 2) >= bw && high / (sample * 2) >= bh) sample *= 2
        decoder.setTargetSampleSize(sample)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val copy = createBitmap(bw, bh)
    val middle = pictureFrame(decoded.width, decoded.height, screen, 0.5f, sideways)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    if (sideways) {
        drawTurned(Canvas(copy), decoded, middle, Rect(0, 0, bw, bh), paint)
    } else {
        Canvas(copy).drawBitmap(decoded, middle, Rect(0, 0, bw, bh), paint)
    }
    decoded.recycle()
    val pixels = IntArray(bw * bh).also { copy.getPixels(it, 0, bw, 0, 0, bw, bh) }
    copy.setPixels(blurBackground(pixels, bw, bh), 0, bw, 0, 0, bw, bh)
    return copy
}

/** The part of a [width]×[height] picture the wallpaper shows on [screen] (`:core`'s maths). */
fun wallpaperFrame(width: Int, height: Int, screen: Size, position: Float): Rect =
    coreWallpaperFrame(width, height, screen.width, screen.height, position).run { Rect(left, top, right, bottom) }

/**
 * The part of an upright [width]×[height] picture the screen shows at [position]: [wallpaperFrame],
 * or with [sideways] the band of the picture turned 90° clockwise (`:core`'s sidewaysFrame:
 * position 0 the bottom band), in the upright picture's pixels. The one frame for Fill's set,
 * the measure, Live's keep and ⛶.
 */
fun pictureFrame(width: Int, height: Int, screen: Size, position: Float, sideways: Boolean): Rect =
    if (!sideways) {
        wallpaperFrame(width, height, screen, position)
    } else {
        coreSidewaysFrame(width, height, screen.width, screen.height, position).run { Rect(left, top, right, bottom) }
    }
