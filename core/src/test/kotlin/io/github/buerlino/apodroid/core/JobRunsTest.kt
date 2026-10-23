package io.github.buerlino.apodroid.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRunsTest {
    @Test
    fun aRunGoesFromItsStartToItsEnd() {
        val runs = JobRuns()
        assertFalse(runs.going)
        val run = runs.started()
        assertTrue(runs.going)
        assertTrue(runs.finished(run))
        assertFalse(runs.going)
    }

    @Test
    fun theLastRunToEndEndsTheGoing() {
        val runs = JobRuns()
        val first = runs.started()
        val second = runs.started()
        assertTrue(runs.finished(first))
        assertTrue(runs.going)
        assertTrue(runs.finished(second))
        assertFalse(runs.going)
    }

    /** Off during a run: nothing goes any more, and the run ends without effect. */
    @Test
    fun offDuringARun() {
        val runs = JobRuns()
        val run = runs.started()
        runs.drop()
        assertFalse(runs.going)
        assertFalse(runs.finished(run))
        assertFalse(runs.going)
    }

    /** Off, on and a new run; the old run ends after the new one: it counts no more. */
    @Test
    fun aRunFromBeforeOffDoesNothingAtItsEnd() {
        val runs = JobRuns()
        val old = runs.started()
        runs.drop()
        val new = runs.started()
        assertTrue(runs.finished(new))
        assertFalse(runs.finished(old))
        assertFalse(runs.going)
    }

    /** The old run ending first doesn't end the new one's going. */
    @Test
    fun aRunFromBeforeOffEndingFirstDoesNothing() {
        val runs = JobRuns()
        val old = runs.started()
        runs.drop()
        val new = runs.started()
        assertFalse(runs.finished(old))
        assertTrue(runs.going)
        assertTrue(runs.finished(new))
        assertFalse(runs.going)
    }
}
