package io.github.buerlino.apodroid

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Runs every ~6 hours with network (with turns of 3 hours, every 3): [Store.update], which fetches
 * the newest post of each source in use unless already stored, and sets each screen's wallpaper
 * once per post and turn; then, with "Save every picture", saves their new posts
 * ([Store.saveInUse]). A failed run is retried with the job's backoff.
 *
 * No reschedule stops a running run (S4f3): scheduling the job's id again would stop it, so while
 * one goes [schedule] and [ensure] only note what's wanted, and the run does it after
 * [jobFinished]. [cancel] (the daily switch off) still stops it at once.
 */
class DailyJob : JobService() {
    @Volatile
    private var worker: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        synchronized(lock) { runs++ }
        worker = thread {
            val store = Store(this)
            val result = runCatching { store.update(force = false) }
            val failure = result.exceptionOrNull() ?: result.getOrNull()?.failure
            result.getOrNull()?.done?.let { Log.i("APODroid", "Daily job: $it") }
            if (failure != null) Log.w("APODroid", "Daily job failed: $failure", failure)
            // Save every picture: also after a failed run (a failing source mustn't stop the others'
            // saves), not after a stop. A failed save doesn't retry the job; the next run tries again.
            val stopped = result.exceptionOrNull().let { it is InterruptedException || it?.isStop() == true }
            if (!stopped) {
                runCatching { store.saveInUse() }
                    .onSuccess { if (it != null) Log.i("APODroid", "Daily job: $it") }
                    .onFailure { Log.w("APODroid", "Daily job's saves ended: $it") }
            }
            jobFinished(params, failure != null)
            rescheduleNoted(this)
        }
        return true
    }

    /** Android's deadline or conditions ended the run: interrupt it (see [Store.update]), then retry. */
    override fun onStopJob(params: JobParameters): Boolean {
        worker?.interrupt()
        return true
    }

    /** What a reschedule asked for while a run went: [ensure], or [schedule] afresh (wins). */
    private enum class Noted { ENSURE, SCHEDULE }

    companion object {
        private const val ID = 1

        /** The page and the job share one process: the runs going ([onStartJob] to its end) and what they owe. */
        private val lock = Any()
        private var runs = 0
        private var noted: Noted? = null

        /**
         * Schedules the job afresh (its period starts now). While a run goes, only noted: the
         * run does it when it ends ([rescheduleNoted]); `true` then.
         */
        fun schedule(context: Context): Boolean {
            synchronized(lock) {
                if (runs > 0) {
                    noted = Noted.SCHEDULE
                    return true
                }
            }
            return scheduler(context).schedule(job(context)) == JobScheduler.RESULT_SUCCESS
        }

        /**
         * Schedules the job unless it's there with the right timing: hibernation or a force stop
         * deletes it, and the sources or the interval may change it. Not always: scheduling the
         * same id again stops a running job, so while a run goes it's only noted, as [schedule].
         */
        fun ensure(context: Context): Boolean {
            synchronized(lock) {
                if (runs > 0) {
                    if (noted == null) noted = Noted.ENSURE
                    return true
                }
            }
            val pending = scheduler(context).getPendingJob(ID)
            val job = job(context)
            return pending != null && pending.intervalMillis == job.intervalMillis && pending.flexMillis == job.flexMillis &&
                pending.networkType == job.networkType ||
                scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS
        }

        /**
         * Every 6 hours, with any flex (so scheduling runs it at once). With turns (a column with
         * two or more), as often as a turn lasts, up to 6 hours, with a short flex so the turns
         * come at even times.
         */
        private fun job(context: Context): JobInfo {
            val store = Store(context)
            val sixHours = TimeUnit.HOURS.toMillis(6)
            return JobInfo.Builder(ID, ComponentName(context, DailyJob::class.java))
                .apply {
                    if (!store.hasTurns) {
                        setPeriodic(sixHours)
                    } else {
                        setPeriodic(minOf(TimeUnit.HOURS.toMillis(store.interval.hours), sixHours), TimeUnit.MINUTES.toMillis(15))
                    }
                }
                .setRequiredNetworkType(if (store.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()
        }

        /** Off is off: cancels the job, a running run too, and drops a noted reschedule. */
        fun cancel(context: Context) {
            synchronized(lock) { noted = null }
            scheduler(context).cancel(ID)
        }

        /**
         * A run's end, after [jobFinished]: does what a reschedule noted meanwhile, by the stored
         * setup as it is now, while the daily switch is still on. The process dying first loses
         * it: `onResume`'s [ensure] then covers a changed timing, not a fresh period.
         */
        private fun rescheduleNoted(context: Context) {
            val wanted = synchronized(lock) {
                runs--
                if (runs > 0) null else noted.also { noted = null }
            }
            if (wanted == null || !Store(context).daily) return
            val ok = when (wanted) {
                Noted.SCHEDULE -> schedule(context)
                Noted.ENSURE -> ensure(context)
            }
            if (ok) Log.i("APODroid", "Daily job: rescheduled after the run (${wanted.name.lowercase()})")
            else Log.w("APODroid", "Daily job: couldn't reschedule after the run")
        }

        private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)
    }
}
