package io.github.buerlino.apodroid.core

import kotlin.math.ceil

// Bright pictures darker (0.7.0, BR1; issue #2): a bright picture washes out the clock and the
// widgets, so a picture whose top third is brighter than a target is darkened a little.

/**
 * The mean grey (0–255) up to which a picture is left as it is: 100. Why (BR0, 2026-11-14): the
 * best fit of the user's 50 picks, 23 of 50 exactly on the ladder, 44 within one step.
 */
const val BRIGHT_TARGET = 100

/** The least the factor goes: ×0.7, at most 30% darker. Floors 0.6 and 0.5 fitted the picks worse. */
const val DARKEN_FLOOR = 0.7f

/** The share of the picture, from its top, that is measured: the third where the clock sits. */
const val MEASURED_SHARE = 1.0 / 3

/**
 * The mean grey of the top third of a [w]×[h] grey copy ([gray] holds 0–255 per pixel, row by
 * row): the first ceil(h / 3) rows, at least one.
 */
fun topThirdMean(gray: IntArray, w: Int, h: Int): Double {
    require(w > 0 && h > 0 && gray.size >= w * h)
    val rows = ceil(h * MEASURED_SHARE).toInt().coerceIn(1, h)
    var sum = 0L
    for (i in 0 until rows * w) sum += gray[i]
    return sum.toDouble() / (rows * w)
}

/** The factor for a picture of mean grey [mean]: 1 up to [BRIGHT_TARGET], else target ÷ mean, at least [DARKEN_FLOOR]. */
fun darkening(mean: Double): Float {
    if (mean <= BRIGHT_TARGET) return 1f
    return (BRIGHT_TARGET / mean).toFloat().coerceIn(DARKEN_FLOOR, 1f)
}
