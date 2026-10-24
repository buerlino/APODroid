package io.github.buerlino.apodroid.core

/**
 * The daily job's runs going, process-wide (the page and the job share one process), and off's
 * generation. Scheduling a job's id again stops its run, so nothing schedules one while a run
 * goes ([going]). One run at a time (B16): a job that starts while one goes ends at once ([start]
 * says null), as both ids share the one job service's thread and stop flag. Off ([drop]: the
 * daily switch off, the jobs and alarms cancelled) bumps the generation: a run started before it
 * ends without effect ([finished] says so), and the count starts again from nothing, so a run
 * after an on may go beside the old one, which no longer counts. Thread-safe.
 */
class JobRuns {
    private var generation = 0
    private var runs = 0

    /**
     * A run starts, unless one goes: null then (the job ends at once; the going run's end arms
     * the alarms, and a check it missed comes at once). It hands the returned generation to
     * [counts] and [finished].
     */
    @Synchronized
    fun start(): Int? {
        if (runs > 0) return null
        runs++
        return generation
    }

    /** Whether a run of [generation] still counts (no off since its start). */
    @Synchronized
    fun counts(generation: Int): Boolean = generation == this.generation

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
