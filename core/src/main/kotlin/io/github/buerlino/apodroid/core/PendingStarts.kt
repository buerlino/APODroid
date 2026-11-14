package io.github.buerlino.apodroid.core

/**
 * "Keep running"'s starts not yet delivered (O11, fix round 1): `startForegroundService` owes
 * Android a `startForeground` in the service's `onStartCommand`, and a `stopService` that comes
 * before it takes the service down owing it, which crashes the app ("did not then call
 * startForeground"): a job run's start read the switch on and started it while the page turned
 * it off. So a stop waits while a start is pending ([mayStop]); the pending `onStartCommand` goes
 * foreground, reads the switch off and stops itself. A count, not a flag: two quick starts give
 * two `onStartCommand`s. Thread-safe (the job's thread and the page's).
 */
class PendingStarts {
    private var pending = 0

    /** Before `startForegroundService`. */
    @Synchronized
    fun starting() {
        pending++
    }

    /** `startForegroundService` threw: nothing will be delivered. */
    @Synchronized
    fun failed() {
        if (pending > 0) pending--
    }

    /**
     * At the top of `onStartCommand`; [fromStart]: it carries an intent (a sticky restart has
     * none and owes nothing, also in a process whose count is new).
     */
    @Synchronized
    fun delivered(fromStart: Boolean) {
        if (fromStart && pending > 0) pending--
    }

    /** Whether `stopService` is safe now: no start pending. */
    @Synchronized
    fun mayStop(): Boolean = pending == 0
}
