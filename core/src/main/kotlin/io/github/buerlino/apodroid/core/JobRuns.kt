package io.github.buerlino.apodroid.core

/** What a reschedule asked for while a run went: ensure, or schedule afresh (wins). */
enum class Reschedule { ENSURE, SCHEDULE }

/**
 * The daily job's runs going and the reschedule they owe (S4f3, B5). Scheduling the job's id
 * again stops a running run, so while one goes a reschedule is only noted, and the last run to
 * end does it. A failed run keeps the note for the next run that doesn't fail (its backoff retry
 * would be replaced otherwise). A reschedule with no run going is done at once and drops a kept
 * note (else the old note would follow it later). Thread-safe.
 */
class JobRuns {
    private var runs = 0
    private var noted: Reschedule? = null

    @Synchronized
    fun started() {
        runs++
    }

    /** `true`: a run goes and [wanted] is noted. `false`: do it now; a kept note is dropped. */
    @Synchronized
    fun note(wanted: Reschedule): Boolean {
        if (runs == 0) {
            noted = null
            return false
        }
        if (wanted == Reschedule.SCHEDULE || noted == null) noted = wanted
        return true
    }

    /** A run's end: what to do now, `null` when nothing (another run goes, the run failed, no note). */
    @Synchronized
    fun finished(failed: Boolean): Reschedule? {
        runs--
        if (runs > 0 || failed) return null
        return noted.also { noted = null }
    }

    /** The daily switch off: nothing is owed. */
    @Synchronized
    fun drop() {
        noted = null
    }
}
