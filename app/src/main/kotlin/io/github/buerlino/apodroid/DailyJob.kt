package io.github.buerlino.apodroid

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.buerlino.apodroid.core.JobRuns
import io.github.buerlino.apodroid.core.firstCheck
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
 *   schedules a one-off job ([RUN], no delay, any network) unless it's pending or a run goes; the
 *   second finds it so and does nothing.
 * - **The fallback** ([PERIODIC]): every 6 hours with a 6 hour flex, a timing that never changes,
 *   so after the first start's [ensure] nothing schedules it again while it runs.
 *
 * Every run's start notes when (`lastRunAt`) and arms the alarms 15 min ahead, so a run that never
 * reaches its end (a frozen or killed process) is retried by them (B16). Every run's end arms the
 * alarms again: by the clock after a good run (at once for a check that passed during the run),
 * 15 min later after a failed or stopped one, then hourly. So no run asks Android for a retry
 * (`jobFinished(…, false)`, [onStopJob] `false`): in Doze a backoff retry waits for a maintenance
 * window, and while it's pending the alarms can't start a run. One run at a time (B16): a job of
 * either id that starts while a run goes ends at once ([JobRuns.start]); the going run's end arms
 * again. Scheduling a job's id again stops its run, so nothing schedules one while a run goes
 * ([JobRuns.going], process-wide). [cancel] (the daily switch off) cancels both jobs and both
 * alarms, a running run too, which then ends without effect.
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
        worker = thread {
            val store = Store(this)
            started(this, store, generation)
            // The stop flag besides the interrupt: an interrupted thread's commit() clears it (CR2's finding 2).
            val result = runCatching { store.update(force = false, stopped = { stoppedBySystem }) }
            val failure = result.exceptionOrNull() ?: result.getOrNull()?.failure
            result.getOrNull()?.done?.let { Log.i("APODroid", "Daily job: $it") }
            if (failure != null) Log.w("APODroid", "Daily job failed: $failure", failure)
            // Save every picture: also after a failed run (a failing source mustn't stop the others'
            // saves), not after a stop. A failed save doesn't retry the job; the next run tries again.
            val stopped = stoppedBySystem || result.exceptionOrNull().let { it is InterruptedException || it?.isStop() == true }
            if (!stopped) {
                runCatching { store.saveInUse(stopped = { stoppedBySystem }) }
                    .onSuccess { if (it != null) Log.i("APODroid", "Daily job: $it") }
                    .onFailure { Log.w("APODroid", "Daily job's saves ended: $it") }
            }
            // The alarms retry, not Android (it would hold a retry back in Doze).
            jobFinished(params, false)
            Log.i("APODroid", "Daily job: jobFinished (job ${params.jobId})")
            // A stop leaves the thread interrupted (Store.update), and an interrupted thread's
            // commit() skips the prefs listeners (B16): the page wouldn't see ranJob's note.
            Thread.interrupted()
            ended(this, generation, failed = failure != null || stoppedBySystem)
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
         * Arms the alarms for the next run ([Store.nextAlarm]) and schedules the fallback unless it's
         * there with the right timing: hibernation or a force stop deletes it, and a 0.5.0 or
         * 0.6.0-test install has a turns job (3 h, 15 min flex) to replace once. The fallback not
         * while a run goes: scheduling its id again would stop it (the run's end calls this
         * again). The caller checks [Store.daily]. `false` when the fallback couldn't be scheduled.
         */
        fun ensure(context: Context): Boolean {
            arm(context)
            if (runs.going) return true
            val pending = scheduler(context).getPendingJob(PERIODIC)
            val job = periodic(context)
            return pending != null && pending.intervalMillis == job.intervalMillis && pending.flexMillis == job.flexMillis ||
                scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS
        }

        /**
         * The pair of alarms for [Store.nextAlarm]'s time, replacing the pair armed before; while a
         * run goes no later than its start's pair ([started], B16f1): a page open, a Set or a
         * clock change mid-run would else arm the next change time, and a process killed before
         * the run's end would wait for it.
         */
        private fun arm(context: Context) = arm(context, Store(context).nextAlarm(running = runs.going).toEpochMilli())

        /** The pair of alarms for [at] (epoch ms), replacing the pair armed before. */
        private fun arm(context: Context, at: Long) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, window, alarm(context, WINDOW_ALARM))
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm(context, IDLE_ALARM))
            Log.i("APODroid", "Daily job: alarms for ${Instant.ofEpochMilli(at)}")
        }

        private fun alarm(context: Context, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RUN),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * An alarm fired: schedules the run ([RUN], no delay, any network) while the daily switch
         * is on, unless a run goes in this process (scheduling it again would stop it) or this
         * process scheduled it and it hasn't started (the second alarm of the pair lands here; it
         * may be about to start). A pending [RUN] from before the process died (Android may
         * reschedule a run that died with it, with its own backoff, B16) is replaced. An alarm
         * that lands in a going run arms the pair [firstCheck] ahead, as the run's start did
         * (B18, CR2's finding 1): it had replaced that pair (an arming during the run, or the run
         * outlasting it), so a process killed before the run's end would else have no alarm left.
         * The run's end arms them again.
         */
        fun runNow(context: Context) {
            if (!Store(context).daily) return
            if (runs.going) {
                synchronized(lock) {
                    if (runs.going && Store(context).daily) arm(context, System.currentTimeMillis() + firstCheck.toMillis())
                }
                Log.i("APODroid", "Daily job: alarm, a run is going")
                return
            }
            if (runScheduled && scheduler(context).getPendingJob(RUN) != null) {
                Log.i("APODroid", "Daily job: alarm, a run is pending")
                return
            }
            val job = JobInfo.Builder(RUN, ComponentName(context, DailyJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            runScheduled = true
            if (scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS) Log.i("APODroid", "Daily job: alarm, run scheduled")
            else {
                runScheduled = false
                Log.w("APODroid", "Daily job: alarm, couldn't schedule the run")
            }
        }

        /** Every 6 hours with any flex (0.4.0's; so scheduling runs it at once). */
        private fun periodic(context: Context): JobInfo =
            JobInfo.Builder(PERIODIC, ComponentName(context, DailyJob::class.java))
                .setPeriodic(TimeUnit.HOURS.toMillis(6))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()

        /**
         * Off is off: cancels both jobs, a running run too (it then ends without effect), and both
         * alarms, so the next on arms afresh.
         */
        fun cancel(context: Context) = synchronized(lock) {
            runs.drop()
            runScheduled = false
            scheduler(context).cancel(PERIODIC)
            scheduler(context).cancel(RUN)
            val alarms = context.getSystemService(AlarmManager::class.java)
            alarms.cancel(alarm(context, WINDOW_ALARM))
            alarms.cancel(alarm(context, IDLE_ALARM))
        }

        /**
         * A run's start, on its thread: unless an off came since or the switch is off, notes it
         * ([Store.runStarts]) and arms the alarms [firstCheck] ahead, so a run that never reaches
         * its end (the process frozen or killed) is retried by them; its end arms them again.
         */
        private fun started(context: Context, store: Store, generation: Int) = synchronized(lock) {
            if (!runs.counts(generation) || !store.daily) return
            store.runStarts()
            arm(context, System.currentTimeMillis() + firstCheck.toMillis())
        }

        /**
         * A run's end, after [jobFinished]: unless an off came since it started or the switch is
         * off, notes how it went ([Store.ranJob]) and arms the alarms again ([ensure]: after a
         * failed or stopped run for the retry, 15 min later, then hourly).
         */
        private fun ended(context: Context, generation: Int, failed: Boolean) = synchronized(lock) {
            if (!runs.finished(generation)) return
            val store = Store(context)
            if (!store.daily) return
            store.ranJob(failed)
            if (!ensure(context)) Log.w("APODroid", "Daily job: couldn't schedule the fallback after the run")
        }

        private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)
    }
}
