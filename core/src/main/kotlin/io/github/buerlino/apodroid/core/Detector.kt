package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant

/*
 * The detector (O14; on-time-0.7.0.md, The detector): the page notices that the job didn't run
 * because the phone stopped the app. A force stop (the user's, or a maker's cleaner: ZUI's, O8)
 * deletes the alarms and the fallback job, so nothing starts a run until the page opens, a boot
 * or an update. Two steps:
 * 1. **The alarms found gone** ([alarmsLost]): the first arming in a process finds the pair's
 *    `PendingIntent` gone while the daily switch is on. A boot or an update since they were armed
 *    explains it (neither is a stopped app), and so does a pending run: the alarm fired, the pair
 *    is used up, and the run waits for the network or its quota (Android's layer). Noted with the
 *    last run's start before it ([Lost]). A bucket that only holds an alarm leaves it there: no
 *    note, no box (the pause hint covers the buckets).
 * 2. **A miss** ([missedChange]): a set of the next ordinary run (the page's, or the run its
 *    arming started) due by a change time T after that last run's start, the alarms found gone
 *    at least [missAfter] after T + the device's offset. A run that started and failed after T
 *    isn't one (the problem box covers it), nor a missed run with nothing to set (nothing lost).
 */

/** How long after a change time + the device's offset its run may come before a late one is a miss. */
val missAfter: Duration = Duration.ofMinutes(30)

/** The alarms were found gone [at], after the last run started at [lastRun] (none started since). */
data class Lost(val at: Instant, val lastRun: Instant)

/**
 * The alarms found gone ([gone]: the pair's `PendingIntent`s missing while the daily switch is on)
 * at [now], unless something other than a stopped app explains it: a boot ([booted]) or an update
 * ([updated]) since they were armed, a pending run ([runPending]: the pair fired and was used up),
 * or no run's start noted yet ([lastRun] null: nothing is owed). Null: nothing to note.
 */
fun alarmsLost(gone: Boolean, booted: Boolean, updated: Boolean, runPending: Boolean, lastRun: Instant?, now: Instant): Lost? =
    if (!gone || booted || updated || runPending || lastRun == null) null else Lost(now, lastRun)

/**
 * The latest of [stamps] (the change times the sets of one run were due by) that was missed: after
 * [lost]'s last run start, and its run ([offset] after it) more than [missAfter] overdue when the
 * alarms were found gone. Null: none (also without [lost]).
 */
fun missedChange(stamps: Collection<Instant>, offset: Duration, lost: Lost?): Instant? {
    if (lost == null) return null
    return stamps.filter { it > lost.lastRun && it + offset + missAfter <= lost.at }.maxOrNull()
}

/**
 * What the detector's box calls the missed change (O14b): "06:00 change", or "06:00 change on 16 Nov" when its
 * local date isn't today's. [time] is the clock part, already formatted as the system shows it.
 */
fun missedWhen(missed: Instant, now: Instant, zone: java.time.ZoneId, time: String): String {
    val day = missed.atZone(zone).toLocalDate()
    if (day == now.atZone(zone).toLocalDate()) return "$time change"
    return "$time change on ${day.dayOfMonth} ${day.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}"
}

/**
 * A process exit the user asked for (`ApplicationExitInfo.REASON_USER_REQUESTED`): [at] its time,
 * [description] Android's, [forceStop] when the record names its subreason a force stop (`subreason=21
 * (FORCE STOP)` in its summary: the subreason is hidden in the SDK, TP1f1).
 */
data class UserExit(val at: Instant, val description: String?, val forceStop: Boolean = false)

/** Whether an exit record's summary (`ApplicationExitInfo.toString()`) names a force stop's subreason (TP1f1). */
fun isForceStopSummary(summary: String?): Boolean =
    summary != null && Regex("""subreason=21\b|\(FORCE STOP\)""").containsMatchIn(summary)

/**
 * Whether the detector reads the exit records at all (TP1f1's fix round 1): only where they can count,
 * Android 11–14 (API 30–34, [stoppedSinceArmed]); elsewhere nothing is read or logged.
 */
fun readsExitRecords(sdk: Int): Boolean = sdk in 30..34

/**
 * The second sign of a force stop (TP1, `release-0.8.0.md`), on Android 11–14 (API 30–34) only: there
 * a force stop deletes the alarms but leaves their `PendingIntent`s until system_server's GC (AOSP's
 * `forceStopPackageLocked` cancels them only on uninstall), so the `FLAG_NO_CREATE` check mostly
 * finds them. A force stop's exit record ([exits]: AOSP's `forceStopPackageLocked` describes it
 * "stop <package> due to …", or its subreason [UserExit.forceStop], TP1f1) after the last arming ([armedAt]) counts as the alarms gone. No other
 * user-requested exit counts: a Recents swipe's ("remove task") and `killBackgroundProcesses`' ("kill
 * background", "kill all background": `am kill`, task killers, makers' cleaners) delete no alarm, and
 * `pm clear` wipes [armedAt] anyway. 15+ cancel the `PendingIntent`s themselves, and 29 has no exit records: false, as
 * is no arming noted yet. A force stop with no process alive leaves no record (accepted).
 */
fun stoppedSinceArmed(sdk: Int, exits: Collection<UserExit>, armedAt: Instant?): Boolean =
    readsExitRecords(sdk) && armedAt != null &&
        exits.any { it.at > armedAt && (it.forceStop || it.description?.startsWith("stop ") == true) }
