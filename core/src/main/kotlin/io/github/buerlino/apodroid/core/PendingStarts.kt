package io.github.buerlino.apodroid.core

/**
 * "Keep running"'s starts not yet delivered (O11, fix rounds 1 and 2): `startForegroundService`
 * owes Android a `startForeground` in the service's `onStartCommand`, and a `stopService` that
 * comes before it takes the service down owing it, which crashes the app ("did not then call
 * startForeground"): a job run's start read the switch on and started it while the page turned
 * it off. So a stop waits while a start is pending; the pending `onStartCommand` goes foreground,
 * reads the switch off and stops itself. A count, not a flag: two quick starts give two
 * `onStartCommand`s.
 *
 * Each whole step runs under one lock (fix round 2): [start]'s read of the switch, the count and
 * `startForegroundService`; [stopIf]'s check and `stopService`. Callers write the switch before
 * they stop, so either the start comes first and the stop sees it pending, or the stop comes
 * first and the start reads the switch off and starts nothing. Separate steps let a start read
 * the switch on, a stop find nothing pending, and then the start and the stop cross.
 * Thread-safe (the job's thread and the page's).
 */
class PendingStarts {
    private var pending = 0

    /**
     * Runs [start] (`startForegroundService`) and counts it pending while [wanted] (the switch's
     * read) says so, under the lock; false when not wanted. A [start] that throws counts nothing
     * (nothing will be delivered) and rethrows.
     */
    @Synchronized
    fun start(wanted: () -> Boolean, start: () -> Unit): Boolean {
        if (!wanted()) return false
        pending++
        try {
            start()
        } catch (e: Throwable) {
            pending--
            throw e
        }
        return true
    }

    /** Runs [stop] (`stopService`) under the lock unless a start is pending; whether it ran. */
    @Synchronized
    fun stopIf(stop: () -> Unit): Boolean {
        if (pending != 0) return false
        stop()
        return true
    }

    /**
     * At the top of `onStartCommand`; [fromStart]: it carries an intent (a sticky restart has
     * none and owes nothing, also in a process whose count is new).
     */
    @Synchronized
    fun delivered(fromStart: Boolean) {
        if (fromStart && pending > 0) pending--
    }
}
