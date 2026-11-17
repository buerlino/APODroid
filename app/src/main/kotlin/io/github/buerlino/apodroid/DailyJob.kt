package io.github.buerlino.apodroid

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.buerlino.apodroid.core.JobRuns
import io.github.buerlino.apodroid.core.UserExit
import io.github.buerlino.apodroid.core.isForceStopSummary
import io.github.buerlino.apodroid.core.readsExitRecords
import io.github.buerlino.apodroid.core.stoppedSinceArmed
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * The daily change's run, with network: [Store.update], which fetches the newest post of each
 * source in use unless already stored and sets what each screen is due by the change times; then,
 * with "Save every picture", saves their new posts ([Store.saveInUse]). Two ways in (0.6.0, J2):
 * - **The alarms** ([AlarmReceiver]): at each next run ([Store.nextAlarm]: the change times and
 *   their checks, or a failed run's retry) a pair, `setWindow(T, 10 min)` and
 *   `setAndAllowWhileIdle(T)`, the only alarms that land in Doze (J0). The first that fires
 *   schedules a one-off job ([RUN], no delay, any network, expedited on 12+) unless it's pending or a run goes; the
 *   second finds it so and does nothing.
 * - **The fallback** ([PERIODIC]): every 6 hours with a 6 hour flex, a timing that never changes,
 *   so after the first start's [ensure] nothing schedules it again while it runs.
 *
 * A run counts as failed from its start until it ends well (O4b): its start counts it (`failedRuns`)
 * and notes when (`lastRunAt`), and arms the alarms 15 min ahead (an hour after a failed run), so
 * a run that never reaches its end (a frozen or killed process) is retried by them (B16), also
 * when a new process arms them before the next run (B19). A good run forgets the failures before
 * [jobFinished] (B24), and its end arms the alarms again: by the clock after a good run (at once
 * for a check that passed during the run), 15 min after a failed or stopped one's start, then
 * hourly. So no run asks Android for a retry
 * (`jobFinished(…, false)`, [onStopJob] `false`): in Doze a backoff retry waits for a maintenance
 * window, and while it's pending the alarms can't start a run. One run at a time (B16): a job of
 * either id that starts while a run goes ends at once ([JobRuns.start]); the going run's end arms
 * again. Scheduling a job's id again stops its run, so nothing schedules one while a run goes
 * ([JobRuns.going], process-wide). [cancel] (the daily switch off) cancels both jobs and both
 * alarms, a running run too, which then ends without effect. A force stop deletes the alarms and
 * the fallback: the first arming in the next process notices it for the detector ([checkAlarms],
 * O14).
 */
class DailyJob : JobService() {
    @Volatile
    private var worker: Thread? = null

    /** The id of the job [worker] runs, so a stop for another id interrupts nothing. */
    @Volatile
    private var workerJob = 0

    /** Set by [onStopJob]: the run ends as failed, and the alarms retry it. Reset at each start. */
    @Volatile
    private var stoppedBySystem = false

    override fun onStartJob(params: JobParameters): Boolean {
        if (params.jobId == RUN) runScheduled = false
        val generation = runs.start() ?: run {
            Log.i("APODroid", "Daily job: job ${params.jobId} ends at once, a run goes")
            return false
        }
        stoppedBySystem = false
        workerJob = params.jobId
        // O10: whether the alarms' run got its expedited quota (12+; 10–11 have none).
        val kind = if (Build.VERSION.SDK_INT >= 31) {
            if (params.isExpeditedJob) ", expedited" else ", not expedited"
        } else ""
        Log.i("APODroid", "Daily job: job ${params.jobId} starts$kind")
        worker = thread {
            val store = Store(this)
            started(this, store, generation)
            // Keep running (O11) back, if it went: allowed from a job only while Unrestricted.
            // After started() and outside the lock, in its own catch (review-0.7.0-runs, 3): a
            // throw here would kill every run at its start. A refusal is logged, once a run.
            runCatching { KeepRunning.ensure(this) }
            // The stop flag besides the interrupt: an interrupted thread's commit() clears it (CR2's finding 2).
            val result = runCatching { store.update(force = false, stopped = { stoppedBySystem }) }
            val failure = result.exceptionOrNull() ?: result.getOrNull()?.failure
            result.getOrNull()?.done?.let { Log.i("APODroid", "Daily job: $it") }
            // The switch off (a run from before it, B19): not a failure, it isn't counted or retried.
            if (!runs.counts(generation)) Log.i("APODroid", "Daily job: stopped (the switch off)")
            else if (failure != null) Log.w("APODroid", "Daily job failed: $failure", failure)
            // Save every picture: also after a failed run (a failing source mustn't stop the others'
            // saves), not after a stop. A failed save doesn't retry the job; the next run tries again.
            val stopped = stoppedBySystem || result.exceptionOrNull().let { it is InterruptedException || it?.isStop() == true }
            if (!stopped) {
                runCatching { store.saveInUse(stopped = { stoppedBySystem }) }
                    .onSuccess { if (it != null) Log.i("APODroid", "Daily job: $it") }
                    .onFailure { Log.w("APODroid", "Daily job's saves ended: $it") }
            }
            // A stop leaves the thread interrupted (Store.update), and an interrupted thread's
            // commit() skips the prefs listeners (B16): the page wouldn't see ranWell's note.
            Thread.interrupted()
            // The bookkeeping before jobFinished (B24): a death between them left a good run
            // counted as failed, its retry an hour ahead at the next start.
            noted(this, generation, failed = failure != null || stoppedBySystem)
            // The alarms retry, not Android (it would hold a retry back in Doze).
            jobFinished(params, false)
            Log.i("APODroid", "Daily job: jobFinished (job ${params.jobId})")
            ended(this, generation)
        }
        return true
    }

    /** Android's deadline or conditions ended the run: interrupt it (see [Store.update]); the alarms retry it. */
    override fun onStopJob(params: JobParameters): Boolean {
        if (params.jobId != workerJob) return false
        stoppedBySystem = true
        worker?.interrupt()
        return false
    }

    companion object {
        /** The fallback, every 6 hours. */
        private const val PERIODIC = 1

        /** The run the alarms start. */
        private const val RUN = 2

        /** The pair of alarms, one request code each (one code would make the second replace the first). */
        private const val WINDOW_ALARM = 1
        private const val IDLE_ALARM = 2

        /** The window of the first alarm: it fires at T + 10 min at the latest (J0). */
        private val window = TimeUnit.MINUTES.toMillis(10)

        /** The page and the job share one process: the runs going ([onStartJob] to its end) and off's generation. */
        private val runs = JobRuns()

        /** A run's start and end against [cancel]: a run that read the switch on doesn't arm the alarms after off cancelled them. */
        private val lock = Any()

        /**
         * [runNow] scheduled [RUN] in this process and it hasn't started: a pending [RUN] is then
         * this process's (waiting for the network, or about to start), not a stale one Android
         * rescheduled after a process died during its run (B16).
         */
        @Volatile
        private var runScheduled = false

        /**
         * This process armed or cancelled the alarms: it knows their state, so [ensure] no longer
         * asks whether they're gone ([checkAlarms], O14).
         */
        @Volatile
        private var armedHere = false

        /**
         * Arms the alarms for the next run ([Store.nextAlarm]) and schedules the fallback unless it's
         * there with the right timing: hibernation or a force stop deletes it, and a 0.5.0 or
         * 0.6.0-test install has a turns job (3 h, 15 min flex) to replace once. The fallback not
         * while a run goes: scheduling its id again would stop it (the run's end calls this
         * again). The callers check [Store.daily] and it's read again here (off: nothing, `true`). `false` when the fallback couldn't be scheduled.
         * The first arming in a process first asks whether the alarms are gone ([checkAlarms]).
         * Under [lock] (FP6 tests' review, 2026-11-15, N1): else a page's or a receiver's arming
         * could read [Store.nextAlarm] just before a run's start ([started]) armed its retry and
         * then replace that pair with the clock's later time; and an off between the caller's check
         * and the arming would leave alarms armed. So the switch is read again here.
         */
        fun ensure(context: Context): Boolean = synchronized(lock) {
            if (!Store(context).daily) return true
            checkAlarms(context)
            arm(context)
            if (runs.going) return true
            val pending = scheduler(context).getPendingJob(PERIODIC)
            val job = periodic(context)
            return pending != null && pending.intervalMillis == job.intervalMillis && pending.flexMillis == job.flexMillis ||
                scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS
        }

        /**
         * The pair of alarms for [Store.nextAlarm]'s time, replacing the pair armed before; while a
         * run goes no later than its start's pair ([started], B16f1: the run counts as failed from
         * its start, O4b): a page open, a Set or a clock change mid-run would else arm the next
         * change time, and a process killed before the run's end would wait for it.
         */
        private fun arm(context: Context) = arm(context, Store(context).nextAlarm().toEpochMilli())

        /** The pair of alarms for [at] (epoch ms), replacing the pair armed before. */
        private fun arm(context: Context, at: Long) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, window, alarm(context, WINDOW_ALARM))
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(context, IDLE_ALARM))
            armedHere = true
            Store(context).noteArmed()
            Log.i("APODroid", "Daily job: alarms for ${Instant.ofEpochMilli(at)}")
        }

        private fun alarm(context: Context, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context,
            code,
            alarmIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * This app's process exits the user asked for (`REASON_USER_REQUESTED`: a force stop, `pm
         * clear`, a Recents swipe), from Android's records (API 30–34 only, [readsExitRecords]: none
         * elsewhere, or on a failure). Only user-requested records; [UserExit.forceStop] marks those
         * whose summary names the force stop's subreason.
         */
        private fun userExits(context: Context, armedAt: Instant?): List<UserExit> {
            if (!readsExitRecords(Build.VERSION.SDK_INT)) return emptyList()
            return runCatching {
                // All of them (0: no cap), TP1f1: 16 dropped none on T4's emulator, but costs nothing.
                val records = context.getSystemService(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(context.packageName, 0, 0)
                // TP1f1: what Android returns, for the check on a device (T4 saw no `Detector:` line).
                Log.i("APODroid", "Detector: ${records.size} exit records, armed $armedAt" + records.sortedByDescending { it.timestamp }.take(3)
                    .joinToString("") { "; ${Instant.ofEpochMilli(it.timestamp)} reason ${it.reason} ${it.description}${if (isForceStopSummary(it.toString())) " (force stop)" else ""}" })
                records
                    .filter { it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
                    .map { UserExit(Instant.ofEpochMilli(it.timestamp), it.description, isForceStopSummary(it.toString())) }
            }.onFailure { Log.w("APODroid", "Detector: couldn't read the exit records: $it") }.getOrDefault(emptyList())
        }

        private fun alarmIntent(context: Context) = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RUN)

        /**
         * The detector's sign (O14; on-time-0.7.0.md, The detector): before the first arming in a
         * process, while the daily switch is on (the caller's check), whether the pair's
         * `PendingIntent`s are gone. A force stop deletes them (`am force-stop`, O7; ZUI's cleaner,
         * O8, which leaves no other sign on the tablet), a process's death doesn't; a boot, an
         * update and a pending run explain it ([Store.noteAlarmsGone]). Within a process that armed
         * or cancelled them they're known, so it asks only once. On 11–14 (TP1) a force stop leaves
         * the `PendingIntent`s until system_server's GC, so a force stop's exit record after the
         * last arming counts too ([stoppedSinceArmed], [userExits]).
         */
        private fun checkAlarms(context: Context) {
            if (armedHere) return
            val gone = listOf(WINDOW_ALARM, IDLE_ALARM).all { code ->
                PendingIntent.getBroadcast(context, code, alarmIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE) == null
            }
            if (!gone) {
                val armedAt = Store(context).armedAt
                if (!stoppedSinceArmed(Build.VERSION.SDK_INT, userExits(context, armedAt), armedAt)) return
                Log.i("APODroid", "Detector: stopped by the user since the arming (exit record)")
            }
            Store(context).noteAlarmsGone(runPending = scheduler(context).getPendingJob(RUN) != null)
        }

        /**
         * An alarm fired: schedules the run ([RUN], no delay, any network, expedited on 12+: O10) while the daily switch
         * is on, unless a run goes in this process (scheduling it again would stop it) or this
         * process scheduled it and it hasn't started (the second alarm of the pair lands here; it
         * may be about to start). A pending [RUN] from before the process died (Android may
         * reschedule a run that died with it, with its own backoff, B16) is replaced. An alarm
         * that lands in a going run arms the pair [Store.runRetry] ahead, as the run's start did
         * (B18, CR2's finding 1): it had replaced that pair (an arming during the run, or the run
         * outlasting it), so a process killed before the run's end would else have no alarm left.
         * The run's end arms them again. A [RUN] that can't be scheduled (`RESULT_FAILURE` or a
         * throw, O4a) arms the pair [Store.runRetry] ahead too, so the alarms aren't lost while the
         * switch is on.
         */
        fun runNow(context: Context) {
            if (!Store(context).daily) return
            if (runs.going) {
                synchronized(lock) {
                    val store = Store(context)
                    if (runs.going && store.daily) armRetry(context, store)
                }
                Log.i("APODroid", "Daily job: alarm, a run is going")
                return
            }
            if (runScheduled && scheduler(context).getPendingJob(RUN) != null) {
                Log.i("APODroid", "Daily job: alarm, a run is pending")
                return
            }
            runScheduled = true
            // O10: expedited on 12+, so Doze and battery saver let it run with the network on an
            // Optimized phone. RESULT_FAILURE there means no expedited quota left: scheduled again
            // as a plain job, as on 10–11 (a throw is tried again the same way).
            if (Build.VERSION.SDK_INT >= 31) {
                val expedited = runCatching { scheduler(context).schedule(runJob(context, expedited = true)) }
                if (expedited.getOrNull() == JobScheduler.RESULT_SUCCESS) {
                    Log.i("APODroid", "Daily job: alarm, run scheduled")
                    return
                }
                Log.i("APODroid", "Daily job: alarm, not expedited: ${expedited.exceptionOrNull() ?: "RESULT_FAILURE"}")
            }
            val result = runCatching { scheduler(context).schedule(runJob(context, expedited = false)) }
            if (result.getOrNull() == JobScheduler.RESULT_SUCCESS) {
                Log.i("APODroid", if (Build.VERSION.SDK_INT >= 31) "Daily job: alarm, run scheduled, not expedited" else "Daily job: alarm, run scheduled")
                return
            }
            // No run and the alarm used up (O4a): the pair Store.runRetry ahead, else nothing would
            // start a run until a page open, a boot or the fallback.
            runScheduled = false
            Log.w("APODroid", "Daily job: alarm, couldn't schedule the run: ${result.exceptionOrNull() ?: "RESULT_FAILURE"}")
            synchronized(lock) {
                val store = Store(context)
                if (store.daily) armRetry(context, store)
            }
        }

        /** [RUN]: no delay, any network, not persisted; expedited on request (12+ only, O10). */
        private fun runJob(context: Context, expedited: Boolean): JobInfo =
            JobInfo.Builder(RUN, ComponentName(context, DailyJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .apply { if (expedited && Build.VERSION.SDK_INT >= 31) setExpedited(true) }
                .build()

        /**
         * The pair [Store.runRetry] ahead (15 min, an hour after one or more failed runs in a row):
         * a run's start ([started]), an alarm that lands in a going run, and a run [runNow]
         * couldn't schedule. The caller holds [lock] and has checked [Store.daily].
         */
        private fun armRetry(context: Context, store: Store) =
            arm(context, System.currentTimeMillis() + store.runRetry().toMillis())

        /** Every 6 hours with any flex (0.4.0's; so scheduling runs it at once). */
        private fun periodic(context: Context): JobInfo =
            JobInfo.Builder(PERIODIC, ComponentName(context, DailyJob::class.java))
                .setPeriodic(TimeUnit.HOURS.toMillis(6))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()

        /**
         * The daily switch turned on (LG): its own arming explains the missing alarms (as a boot does),
         * so the first arming in this process doesn't ask ([checkAlarms]) and logs nothing.
         */
        fun switchedOn() = synchronized(lock) { armedHere = true }

        /**
         * Off is off: cancels both jobs, a running run too (it then ends without effect), and both
         * alarms, so the next on arms afresh.
         */
        fun cancel(context: Context) = synchronized(lock) {
            runs.drop()
            runScheduled = false
            armedHere = true
            scheduler(context).cancel(PERIODIC)
            scheduler(context).cancel(RUN)
            val alarms = context.getSystemService(AlarmManager::class.java)
            alarms.cancel(alarm(context, WINDOW_ALARM))
            alarms.cancel(alarm(context, IDLE_ALARM))
        }

        /**
         * A run's start, on its thread: unless an off came since or the switch is off, counts it as
         * failed until it ends well and notes when ([Store.runStarts], O4b) and arms the alarms
         * [Store.runRetry] ahead (15 min, an hour after a failed run), so a run that never reaches
         * its end (the process frozen or killed) is retried by them; its end arms them again.
         */
        private fun started(context: Context, store: Store, generation: Int): Unit = synchronized(lock) {
            if (!runs.counts(generation) || !store.daily) return
            store.runStarts()
            armRetry(context, store)
        }

        /**
         * A run's bookkeeping, before [jobFinished] (B24: a death between them must not leave a
         * good run counted as failed): unless it [failed], an off came since it started or the
         * switch is off, forgets the failed runs ([Store.ranWell]); a failed or stopped run stays
         * counted from its start. A death after it and before [jobFinished] counts the run as
         * ended; Android may re-run job 1 (it finds nothing new).
         */
        private fun noted(context: Context, generation: Int, failed: Boolean): Unit = synchronized(lock) {
            if (failed || !runs.counts(generation)) return
            val store = Store(context)
            if (!store.daily) return
            store.ranWell()
        }

        /**
         * A run's end, after [jobFinished] (scheduling an id Android still counts as running would
         * stop it): unless an off came since it started or the switch is off, arms the alarms
         * again ([ensure]: by the clock after a good run, 15 min after a failed or stopped one's
         * start, then hourly).
         */
        private fun ended(context: Context, generation: Int): Unit = synchronized(lock) {
            if (!runs.finished(generation)) return
            if (!Store(context).daily) return
            if (!ensure(context)) Log.w("APODroid", "Daily job: couldn't schedule the fallback after the run")
        }

        private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)
    }
}
