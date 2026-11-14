package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PendingStartsTest {
    private val events = mutableListOf<String>()

    private fun PendingStarts.startIf(on: Boolean) = start({ on }) { events += "start" }

    private fun PendingStarts.tryStop() = stopIf { events += "stop" }

    @Test
    fun nothingStartedMayStop() {
        assertTrue(PendingStarts().tryStop())
        assertEquals(listOf("stop"), events)
    }

    @Test
    fun theSwitchOffStartsNothing() {
        val starts = PendingStarts()
        assertFalse(starts.startIf(false))
        assertTrue(events.isEmpty())
        assertTrue(starts.tryStop())
    }

    /** O11's fix round 1: a stop between the start and its onStartCommand waits for it. */
    @Test
    fun aStopWaitsForAPendingStart() {
        val starts = PendingStarts()
        assertTrue(starts.startIf(true))
        assertFalse(starts.tryStop())
        starts.delivered(fromStart = true)
        assertTrue(starts.tryStop())
        assertEquals(listOf("start", "stop"), events)
    }

    /**
     * O11's fix round 3: onStartCommand has begun (building the notification, before
     * startForeground) when a stop comes: still pending, so the stop is skipped; only once the
     * start has gone foreground (delivered) may a stop run.
     */
    @Test
    fun aStopBeforeStartForegroundIsSkipped() {
        val starts = PendingStarts()
        assertTrue(starts.startIf(true))
        // onStartCommand runs; notification() makes its binder calls; the job's ensure reads off.
        assertFalse(starts.tryStop())
        // startForeground's try ends, then delivered.
        starts.delivered(fromStart = true)
        assertTrue(starts.tryStop())
        assertEquals(listOf("start", "stop"), events)
    }

    @Test
    fun twoQuickStartsWaitForBoth() {
        val starts = PendingStarts()
        starts.startIf(true)
        starts.startIf(true)
        starts.delivered(fromStart = true)
        assertFalse(starts.tryStop())
        starts.delivered(fromStart = true)
        assertTrue(starts.tryStop())
    }

    @Test
    fun aRefusedStartOwesNothing() {
        val starts = PendingStarts()
        val refused = runCatching { starts.start({ true }) { throw IllegalStateException("refused") } }
        assertTrue(refused.isFailure)
        assertTrue(starts.tryStop())
    }

    /** A sticky restart (no intent) doesn't use up a start still pending. */
    @Test
    fun aStickyRestartLeavesAPendingStart() {
        val starts = PendingStarts()
        starts.startIf(true)
        starts.delivered(fromStart = false)
        assertFalse(starts.tryStop())
    }

    @Test
    fun aStickyRestartInANewProcessStaysAtZero() {
        val starts = PendingStarts()
        starts.delivered(fromStart = false)
        starts.startIf(true)
        assertFalse(starts.tryStop())
    }

    /**
     * O11's fix round 2: the job's start reads the switch on, then the page's stop comes, then
     * the start. The stop waits for the whole start and then sees it pending: no `stopService`
     * while that start is pending (with separate steps the stop found nothing pending and ran
     * between the read and `startForegroundService`).
     */
    @Test
    fun aStopDuringTheStartsReadWaitsAndLeavesTheStart() {
        val starts = PendingStarts()
        val log = Collections.synchronizedList(mutableListOf<String>())
        val read = CountDownLatch(1)
        val goOn = CountDownLatch(1)
        val job = Thread {
            starts.start({
                log += "read on"
                read.countDown()
                goOn.await(5, TimeUnit.SECONDS)
                true
            }) { log += "start" }
        }
        job.start()
        assertTrue(read.await(5, TimeUnit.SECONDS))
        var stopped: Boolean? = null
        val page = Thread { stopped = starts.stopIf { log += "stop" } }
        page.start()
        // The page's stop is held at the lock while the job is between its read and its start.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (page.state != Thread.State.BLOCKED && System.nanoTime() < deadline) Thread.sleep(1)
        assertEquals(Thread.State.BLOCKED, page.state)
        goOn.countDown()
        job.join(5000)
        page.join(5000)
        assertEquals(false, stopped)
        assertEquals(listOf("read on", "start"), log.toList())
        // The pending onStartCommand reads the switch off and stops itself; a later stop is free.
        starts.delivered(fromStart = true)
        assertTrue(starts.stopIf { log += "stop" })
    }

    /** The other order: the stop first, then the start reads the switch off and starts nothing. */
    @Test
    fun aStopBeforeTheStartsReadStartsNothing() {
        val starts = PendingStarts()
        val on = false // the page wrote the switch off, then stops
        assertTrue(starts.tryStop())
        assertFalse(starts.start({ on }) { events += "start" })
        assertEquals(listOf("stop"), events)
    }
}
