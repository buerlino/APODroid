package io.github.buerlino.apodroid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrightnessTest {
    @Test
    fun aDarkPictureKeepsItsBrightness() = assertEquals(1f, darkening(68.0))

    @Test
    fun atTheTargetNothingChanges() = assertEquals(1f, darkening(BRIGHT_TARGET.toDouble()))

    @Test
    fun justOverTheTargetIsJustUnderOne() {
        val k = darkening(101.0)
        assertTrue(k < 1f && k > 0.98f)
    }

    @Test
    fun theFactorIsTargetOverMean() = assertEquals(100f / 130f, darkening(130.0), 1e-6f)

    @Test
    fun cosmicLatteGoesToTheFloor() = assertEquals(DARKEN_FLOOR, darkening(246.0))

    @Test
    fun anEvenPictureStaysEven() {
        val g = IntArray(30 * 30) { 150 }
        assertEquals(150.0, topThirdMean(g, 30, 30), 1e-9)
    }

    @Test
    fun onlyTheTopThirdCounts() {
        // Bright sky over dark ground: the top 10 of 30 rows are 200, the rest 0.
        val g = IntArray(30 * 30) { if (it / 30 < 10) 200 else 0 }
        assertEquals(200.0, topThirdMean(g, 30, 30), 1e-9)
        assertEquals(DARKEN_FLOOR, darkening(topThirdMean(g, 30, 30)))
        // Dark sky over bright ground stays untouched.
        val h = IntArray(30 * 30) { if (it / 30 < 10) 20 else 250 }
        assertEquals(1f, darkening(topThirdMean(h, 30, 30)))
    }

    @Test
    fun aShortCopyMeasuresAtLeastOneRow() {
        assertEquals(10.0, topThirdMean(intArrayOf(10, 10, 99, 99), 2, 2), 1e-9)
    }
}
