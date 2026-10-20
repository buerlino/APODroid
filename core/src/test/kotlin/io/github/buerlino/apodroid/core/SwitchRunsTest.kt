package io.github.buerlino.apodroid.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchRunsTest {
    @Test
    fun anOnRunsOnce() {
        val runs = SwitchRuns()
        runs.on()
        assertTrue(runs.take())
        assertFalse(runs.take())
    }

    /** B6: on, off, on while the first run, past its check, sets: no second set. */
    @Test
    fun quickTapsDuringARunThatSetsSetOnce() {
        val runs = SwitchRuns()
        runs.on()
        assertTrue(runs.take())
        runs.off()
        runs.on()
        runs.ended(checkedOn = true)
        assertFalse(runs.take())
    }

    /** The first run's check fell inside the short off: the last on still runs. */
    @Test
    fun aRunThatGaveUpLeavesTheLastOn() {
        val runs = SwitchRuns()
        runs.on()
        assertTrue(runs.take())
        runs.off()
        runs.on()
        runs.ended(checkedOn = false)
        assertTrue(runs.take())
    }

    @Test
    fun offIsOff() {
        val runs = SwitchRuns()
        runs.on()
        assertTrue(runs.take())
        runs.off()
        runs.on()
        runs.off()
        runs.ended(checkedOn = false)
        assertFalse(runs.take())
    }

    /** B6 fix round 1: an off and on after the run's end owe a run (it schedules the job). */
    @Test
    fun anOffAndOnAfterTheEndRunAgain() {
        val runs = SwitchRuns()
        runs.on()
        assertTrue(runs.take())
        runs.ended(checkedOn = true)
        runs.off()
        runs.on()
        assertTrue(runs.take())
    }
}
