package io.github.buerlino.apodroid.core

/**
 * The daily job's runs going, process-wide (the page and the job share one process), and off's
 * generation. Scheduling a job's id again stops its run, so nothing schedules one while a run
 * goes ([going]). Off ([drop]: the daily switch off, the jobs and alarms cancelled) bumps the
 * generation: a run started before it ends without effect ([finished] says so), and the count
 * starts again from nothing. Thread-safe.
 */
class JobRuns {
    private var generation = 0
    private var runs = 0

    /** A run starts; it hands the returned generation to [finished]. */
    @Synchronized
    fun started(): Int {
        runs++
        return generation
    }

    /** A run's end: `true` when it counts (started since the last [drop]), `false` for a run from before an off. */
    @Synchronized
    fun finished(generation: Int): Boolean {
        if (generation != this.generation) return false
        runs--
        return true
    }

    /** A run goes (one started since the last [drop] and not finished). */
    val going: Boolean
        @Synchronized get() = runs > 0

    /** The daily switch off: the runs going now count no more. */
    @Synchronized
    fun drop() {
        generation++
        runs = 0
    }
}
