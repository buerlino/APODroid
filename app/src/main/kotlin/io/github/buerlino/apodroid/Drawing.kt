package io.github.buerlino.apodroid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.createBitmap
import io.github.buerlino.apodroid.core.Frame
import io.github.buerlino.apodroid.core.blurBackground
import io.github.buerlino.apodroid.core.blurSize
import io.github.buerlino.apodroid.core.edgeCopySize
import io.github.buerlino.apodroid.core.edgesBlack
import io.github.buerlino.apodroid.core.fitFrame
import io.github.buerlino.apodroid.core.gray
import io.github.buerlino.apodroid.core.topThirdMean
import io.github.buerlino.apodroid.core.wallpaperFrame as coreWallpaperFrame
import java.io.File
import kotlin.math.roundToInt

// The drawing (L4): one place that draws a picture onto a canvas of a given size, panned (Fill's
// frame at a position) or whole (on black or blurred). Its users: Store.set's whole picture and a
// bright picture's frame (BR2), and from 0.7.0's Live the engine and ⛶'s preview.

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
    val copy = if (h == bitmap.height) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true)
    val pixels = IntArray(w * h).also { copy.getPixels(it, 0, w, 0, 0, w, h) }
    if (copy !== bitmap) copy.recycle()
    return topThirdMean(IntArray(pixels.size) { gray(pixels[it]) }, w, h)
}

/** The mean grey of the top third of [file]'s frame at [position] ([topThirdGrey], [decodeFrame] small). */
fun frameGrey(file: File, screen: Size, position: Float): Double =
    decodeFrame(file, screen, position, MEASURE_HEIGHT).let { frame -> topThirdGrey(frame).also { frame.recycle() } }

/**
 * [file]'s frame at [position] ([wallpaperFrame], the [screen]'s shape), upright, decoded alone
 * (the decoder's crop: a wide panorama at the screen's height would be hundreds of MB whole) and
 * straight to at most [height] high: the screen's for drawing, [MEASURE_HEIGHT] for measuring.
 * A picture smaller than that stays its size. The crop and `setTargetSize` are in the upright
 * picture's coordinates, as `info.size` (AOSP's ImageDecoder.cpp).
 */
fun decodeFrame(file: File, screen: Size, position: Float, height: Int): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val (w, h) = info.size.width to info.size.height
        val scale = minOf(1.0, height.toDouble() / wallpaperFrame(w, h, screen, position).height())
        val (tw, th) = maxOf(1, (w * scale).roundToInt()) to maxOf(1, (h * scale).roundToInt())
        if (scale < 1.0) decoder.setTargetSize(tw, th)
        decoder.crop = wallpaperFrame(tw, th, screen, position)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

/** The whole picture of [file] on a bitmap of the [screen]'s size ([drawWhole]). */
fun wholePicture(file: File, screen: Size): Bitmap =
    createBitmap(screen.width, screen.height).also { drawWhole(Canvas(it), file, screen) }

/**
 * The whole picture (A3), drawn onto [canvas], whose drawing area is [screen]'s size: [file]
 * upright, all of it as large as fits ([fitFrame]), centred on black when its edges are nearly
 * black ([edgesBlack]: a planet on space blends in), else on the picture blurred and darkened
 * ([blurred]). Decoded straight to the frame's size and drawn 1:1: shrinking the rendition (3726×2484 for a
 * 3:2 post) about 3× in one bilinear step can make stars jagged. `info.size` and `setTargetSize`
 * are both the upright size (Android 10's codec respects EXIF, 11+ swaps for the origin; AOSP's
 * ImageDecoder.cpp).
 */
fun drawWhole(canvas: Canvas, file: File, screen: Size) {
    val background = if (hasBlackEdges(file)) null else blurred(file, screen)
    lateinit var frame: Frame
    val picture = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        frame = fitFrame(info.size.width, info.size.height, screen.width, screen.height)
        decoder.setTargetSize(maxOf(1, frame.right - frame.left), maxOf(1, frame.bottom - frame.top))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    canvas.run {
        drawColor(Color.BLACK)
        background?.let { blur ->
            drawBitmap(blur, null, Rect(0, 0, screen.width, screen.height), Paint(Paint.FILTER_BITMAP_FLAG))
            blur.recycle()
        }
        drawBitmap(picture, frame.left.toFloat(), frame.top.toFloat(), null)
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
 * The caller draws it enlarged to the screen.
 */
private fun blurred(file: File, screen: Size): Bitmap {
    val (bw, bh) = blurSize(screen.width, screen.height)
    val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val frame = wallpaperFrame(info.size.width, info.size.height, screen, 0.5f)
        var sample = 1
        while (frame.width() / (sample * 2) >= bw && frame.height() / (sample * 2) >= bh) sample *= 2
        decoder.setTargetSampleSize(sample)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val copy = createBitmap(bw, bh)
    Canvas(copy).drawBitmap(decoded, wallpaperFrame(decoded.width, decoded.height, screen, 0.5f), Rect(0, 0, bw, bh), Paint(Paint.FILTER_BITMAP_FLAG))
    decoded.recycle()
    val pixels = IntArray(bw * bh).also { copy.getPixels(it, 0, bw, 0, 0, bw, bh) }
    copy.setPixels(blurBackground(pixels, bw, bh), 0, bw, 0, 0, bw, bh)
    return copy
}

/** The part of a [width]×[height] picture the wallpaper shows on [screen] (`:core`'s maths). */
fun wallpaperFrame(width: Int, height: Int, screen: Size, position: Float): Rect =
    coreWallpaperFrame(width, height, screen.width, screen.height, position).run { Rect(left, top, right, bottom) }
