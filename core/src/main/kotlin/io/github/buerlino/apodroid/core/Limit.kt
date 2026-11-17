package io.github.buerlino.apodroid.core

// 0.9.0, "Keep pictures sharp" (sharp-0.9.0.md, Terms and The rules). Stubs from SH0, so its oracle
// tests (LimitTest) compile; SH1 implements them and removes the tests' @Ignore.

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
 * it's whole and centred. [limit] null: plain Fill. SH1 implements it.
 */
fun limitedLayout(
    width: Int, height: Int, screenWidth: Int, screenHeight: Int,
    sideways: Boolean, position: Float, limit: Double?,
): Layout = throw NotImplementedError("SH1 implements limitedLayout")

/** The whole picture's place on the screen ([fitFrame]) at a scale of at most [limit] (with the tolerance), centred. */
fun limitedFit(width: Int, height: Int, screenWidth: Int, screenHeight: Int, limit: Double?): Frame =
    throw NotImplementedError("SH1 implements limitedFit")
