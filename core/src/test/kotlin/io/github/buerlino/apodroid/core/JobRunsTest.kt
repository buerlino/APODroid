package io.github.buerlino.apodroid.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobRunsTest {
    @Test
    fun aRunGoesFromItsStartToItsEnd() {
        val runs = JobRuns()
        assertFalse(runs.going)
        val run = runs.start()!!
        assertTrue(runs.going)
        assertTrue(runs.finished(run))
        assertFalse(runs.going)
    }

    /** One run at a time (B16): a job that starts while one goes ends at once; after its end the next starts. */
    @Test
    fun aSecondRunWhileOneGoesDoesntStart() {
        val runs = JobRuns()
        val first = runs.start()!!
        assertNull(runs.start())
        assertTrue(runs.going)
        assertTrue(runs.finished(first))
        assertFalse(runs.going)
        assertNotNull(runs.start())
    }

    /** Off during a run: nothing goes any more, and the run ends without effect. */
    @Test
    fun offDuringARun() {
        val runs = JobRuns()
        val run = runs.start()!!
        assertTrue(runs.counts(run))
        runs.drop()
        assertFalse(runs.going)
        assertFalse(runs.counts(run))
        assertFalse(runs.finished(run))
        assertFalse(runs.going)
    }

    /** Off, on and a new run; the old run ends after the new one: it counts no more. */
    @Test
    fun aRunFromBeforeOffDoesNothingAtItsEnd() {
        val runs = JobRuns()
        val old = runs.start()!!
        runs.drop()
        val new = runs.start()!!
        assertTrue(runs.finished(new))
        assertFalse(runs.finished(old))
        assertFalse(runs.going)
    }

    /** The old run ending first doesn't end the new one's going. */
    @Test
    fun aRunFromBeforeOffEndingFirstDoesNothing() {
        val runs = JobRuns()
        val old = runs.start()!!
        runs.drop()
        val new = runs.start()!!
        assertFalse(runs.finished(old))
        assertTrue(runs.going)
        assertTrue(runs.finished(new))
        assertFalse(runs.going)
    }
}
