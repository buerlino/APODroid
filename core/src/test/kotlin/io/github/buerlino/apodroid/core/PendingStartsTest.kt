package io.github.buerlino.apodroid.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingStartsTest {
    @Test
    fun nothingStartedMayStop() {
        assertTrue(PendingStarts().mayStop())
    }

    /** O11's fix round 1: a stop between the start and its onStartCommand waits for it. */
    @Test
    fun aStopWaitsForAPendingStart() {
        val starts = PendingStarts()
        starts.starting()
        assertFalse(starts.mayStop())
        starts.delivered(fromStart = true)
        assertTrue(starts.mayStop())
    }

    @Test
    fun twoQuickStartsWaitForBoth() {
        val starts = PendingStarts()
        starts.starting()
        starts.starting()
        starts.delivered(fromStart = true)
        assertFalse(starts.mayStop())
        starts.delivered(fromStart = true)
        assertTrue(starts.mayStop())
    }

    @Test
    fun aRefusedStartOwesNothing() {
        val starts = PendingStarts()
        starts.starting()
        starts.failed()
        assertTrue(starts.mayStop())
    }

    /** A sticky restart (no intent) doesn't use up a start still pending. */
    @Test
    fun aStickyRestartLeavesAPendingStart() {
        val starts = PendingStarts()
        starts.starting()
        starts.delivered(fromStart = false)
        assertFalse(starts.mayStop())
    }

    @Test
    fun aStickyRestartInANewProcessStaysAtZero() {
        val starts = PendingStarts()
        starts.delivered(fromStart = false)
        starts.starting()
        assertFalse(starts.mayStop())
    }
}
