package io.github.buerlino.apodroid.core

/**
 * The daily switch's runs on the page (B6): turned on while a load, a Set or a switch-on run
 * goes, one run is owed after it. A switch-on run that got past its set's check of the switch
 * (it set, or tried to) pays what was owed: quick on, off, on taps set once. One that gave up
 * (the switch was off at its check) leaves it, so the last on still sets. Off drops it. Not
 * thread-safe: the page's main thread only.
 */
class SwitchRuns {
    private var owed = false

    /** The switch on: a run is owed; the caller runs it once nothing else goes ([take]). */
    fun on() {
        owed = true
    }

    /** The switch off: nothing is owed. */
    fun off() {
        owed = false
    }

    /** A switch-on run ended; [checkedOn]: it found the switch on at its set's check. */
    fun ended(checkedOn: Boolean) {
        if (checkedOn) owed = false
    }

    /** Whether a run is owed now; taking it clears it. */
    fun take(): Boolean = owed.also { owed = false }
}
