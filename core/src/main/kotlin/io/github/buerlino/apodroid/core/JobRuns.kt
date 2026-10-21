package io.github.buerlino.apodroid.core

/** What a reschedule asked for while a run went: ensure, or schedule afresh (wins). */
enum class Reschedule { ENSURE, SCHEDULE }

/**
 * The daily job's runs going and the reschedule they owe (S4f3, B5, B7). Scheduling the job's id
 * again stops a running run and replaces a failed run's backoff retry, so while a run goes, and
 * after a failed run until the next run starts (its retry is due), a reschedule is only noted
 * (afresh wins). The last run to end does it, unless it failed: then the note waits for the
 * retry, **one retry at most**: the retry's end does it, good or failed (a source can fail every
 * run for days). With no run going and no retry due a reschedule is done at once.
 *
 * [drop] (the daily switch off, the job cancelled) leaves nothing owed and no retry due: the runs
 * started before it end without effect ([started]'s generation), so the next on schedules at once.
 * Thread-safe.
 */
class JobRuns {
    private var generation = 0
    private var runs = 0
    private var noted: Reschedule? = null
    private var retryDue = false
    private var retrying = false

    /** A run starts; it hands the returned generation to [finished]. */
    @Synchronized
    fun started(): Int {
        if (retryDue) retrying = true
        retryDue = false
        runs++
        return generation
    }

    /** `true`: a run goes or a retry is due, and [wanted] is noted. `false`: do it now. */
    @Synchronized
    fun note(wanted: Reschedule): Boolean {
        if (runs == 0 && !retryDue) {
            noted = null
            return false
        }
        if (wanted == Reschedule.SCHEDULE || noted == null) noted = wanted
        return true
    }

    /**
     * A run's end: what to do now, `null` when nothing (another run goes, a failed run that wasn't
     * a retry, no note, a run from before [drop]).
     */
    @Synchronized
    fun finished(generation: Int, failed: Boolean): Reschedule? {
        if (generation != this.generation) return null
        runs--
        if (runs > 0) return null
        if (failed && !retrying) {
            retryDue = true
            return null
        }
        retrying = false
        val wanted = noted
        noted = null
        // A failed retry with nothing noted keeps its own backoff retry; a note done replaces it.
        retryDue = failed && wanted == null
        return wanted
    }

    /** The daily switch off: nothing is owed, no retry is due, the runs going now count no more. */
    @Synchronized
    fun drop() {
        generation++
        runs = 0
        noted = null
        retryDue = false
        retrying = false
    }
}
