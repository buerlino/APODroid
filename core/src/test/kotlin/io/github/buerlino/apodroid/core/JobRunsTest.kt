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
        val run = runs.started()
        assertTrue(runs.note(Reschedule.ENSURE))
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertTrue(runs.note(Reschedule.ENSURE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(run, failed = false))
    }

    @Test
    fun theLastRunToEndDoesIt() {
        val runs = JobRuns()
        val first = runs.started()
        val second = runs.started()
        runs.note(Reschedule.ENSURE)
        assertNull(runs.finished(first, failed = false))
        assertEquals(Reschedule.ENSURE, runs.finished(second, failed = false))
    }

    /** B5: a failed run keeps the note for its retry, which does it at its end when it works. */
    @Test
    fun aFailedRunKeepsTheNoteForTheRetry() {
        val runs = JobRuns()
        assertNull(runs.finished(runs.started().also { runs.note(Reschedule.SCHEDULE) }, failed = true))
        assertEquals(Reschedule.SCHEDULE, runs.finished(runs.started(), failed = false))
        assertNull(runs.finished(runs.started(), failed = false))
    }

    /** B7, one retry at most (replaces B5's case that let the note wait through two failed runs). */
    @Test
    fun theRetryDoesAKeptNoteAlsoWhenItFails() {
        val runs = JobRuns()
        assertNull(runs.finished(runs.started().also { runs.note(Reschedule.SCHEDULE) }, failed = true))
        assertEquals(Reschedule.SCHEDULE, runs.finished(runs.started(), failed = true))
        assertNull(runs.finished(runs.started(), failed = false))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    /**
     * B7, a Set just after a failed run (replaces B5's aDirectRescheduleDropsAKeptNote, which did it
     * at once and so replaced the backoff retry): noted, done at the retry's end.
     */
    @Test
    fun aNoteAfterAFailedRunWaitsForTheRetry() {
        val runs = JobRuns()
        runs.finished(runs.started(), failed = true)
        assertTrue(runs.note(Reschedule.ENSURE))
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertTrue(runs.note(Reschedule.ENSURE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(runs.started(), failed = false))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    @Test
    fun aNoteAfterAFailedRunIsDoneWhenTheRetryFailsToo() {
        val runs = JobRuns()
        runs.finished(runs.started(), failed = true)
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(runs.started(), failed = true))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    /**
     * B12, only the first retry of a failing series is waited for (replaces B7's
     * aFailedRetryWithoutANoteLeavesItsRetryDue, which kept a retry due after a failed retry, so
     * while a source kept failing every Set waited for Android's ever longer backoff): after a
     * failed retry with nothing noted a reschedule is done at once. A failed run after that
     * starts a new series: a note waits for its retry again.
     */
    @Test
    fun aFailedRetryWithoutANoteLeavesNoRetryDue() {
        val runs = JobRuns()
        runs.finished(runs.started(), failed = true)
        assertNull(runs.finished(runs.started(), failed = true))
        assertFalse(runs.note(Reschedule.SCHEDULE))
        assertFalse(runs.note(Reschedule.ENSURE))
        assertNull(runs.finished(runs.started(), failed = true))
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(runs.started(), failed = true))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    /** B7: off during a run, which then ends stopped (failed): no retry due, so the next on schedules at once. */
    @Test
    fun offDuringARunLeavesNoRetryDue() {
        val runs = JobRuns()
        val run = runs.started()
        runs.note(Reschedule.SCHEDULE)
        runs.drop()
        assertNull(runs.finished(run, failed = true))
        assertFalse(runs.note(Reschedule.SCHEDULE))
    }

    /** B7: off, on and a new run; the old run ends failed after the new one: it counts no more. */
    @Test
    fun aRunFromBeforeOffDoesNothingAtItsEnd() {
        val runs = JobRuns()
        val old = runs.started()
        runs.drop()
        val new = runs.started()
        assertTrue(runs.note(Reschedule.ENSURE))
        assertEquals(Reschedule.ENSURE, runs.finished(new, failed = false))
        assertNull(runs.finished(old, failed = true))
        assertFalse(runs.note(Reschedule.SCHEDULE))
    }

    @Test
    fun aRunFromBeforeOffEndingFirstDoesNothing() {
        val runs = JobRuns()
        val old = runs.started()
        runs.drop()
        val new = runs.started()
        assertNull(runs.finished(old, failed = true))
        assertTrue(runs.note(Reschedule.SCHEDULE))
        assertEquals(Reschedule.SCHEDULE, runs.finished(new, failed = false))
        assertFalse(runs.note(Reschedule.ENSURE))
    }

    @Test
    fun offDropsTheNote() {
        val runs = JobRuns()
        val run = runs.started()
        runs.note(Reschedule.SCHEDULE)
        runs.drop()
        assertNull(runs.finished(run, failed = false))
        runs.finished(runs.started().also { runs.note(Reschedule.ENSURE) }, failed = true)
        runs.drop()
        assertFalse(runs.note(Reschedule.ENSURE))
        assertNull(runs.finished(runs.started(), failed = false))
    }
}
