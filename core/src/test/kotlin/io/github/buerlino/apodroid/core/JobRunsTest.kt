package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRunsTest {
    @Test
    fun noRunGoingSchedulesAtOnce() {
        val runs = JobRuns()
        assertFalse(runs.note(Reschedule.SCHEDULE))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    @Test
    fun aGoodRunDoesTheNoteAtItsEnd() {
        val runs = JobRuns()
        runs.started()
        assertTrue(runs.note(Reschedule.ENSURE))
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertTrue(runs.note(Reschedule.ENSURE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(failed = false))
    }

    @Test
    fun theLastRunToEndDoesIt() {
        val runs = JobRuns()
        runs.started()
        runs.started()
        runs.note(Reschedule.ENSURE)
        assertNull(runs.finished(failed = false))
        assertEquals(Reschedule.ENSURE, runs.finished(failed = false))
    }

    /** B5: S4f3 did the note right after a failed run, replacing its backoff retry. */
    @Test
    fun aFailedRunKeepsTheNoteForTheRetry() {
        val runs = JobRuns()
        runs.started()
        runs.note(Reschedule.SCHEDULE)
        assertNull(runs.finished(failed = true))
        runs.started()
        assertNull(runs.finished(failed = true))
        runs.started()
        assertEquals(Reschedule.SCHEDULE, runs.finished(failed = false))
        runs.started()
        assertNull(runs.finished(failed = false))
    }

    /** A Set with no run going after a failed one: the kept note mustn't follow the next good run. */
    @Test
    fun aDirectRescheduleDropsAKeptNote() {
        val runs = JobRuns()
        runs.started()
        runs.note(Reschedule.SCHEDULE)
        runs.finished(failed = true)
        assertFalse(runs.note(Reschedule.ENSURE))
        runs.started()
        assertNull(runs.finished(failed = false))
    }

    @Test
    fun offDropsTheNote() {
        val runs = JobRuns()
        runs.started()
        runs.note(Reschedule.SCHEDULE)
        runs.drop()
        assertNull(runs.finished(failed = false))
        runs.started()
        runs.note(Reschedule.ENSURE)
        runs.finished(failed = true)
        runs.drop()
        runs.started()
        assertNull(runs.finished(failed = false))
    }
}
