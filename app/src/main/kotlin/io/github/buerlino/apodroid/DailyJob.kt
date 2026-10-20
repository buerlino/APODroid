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
 */
class DailyJob : JobService() {
    @Volatile
    private var worker: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
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
        }
        return true
    }

    /** Android's deadline or conditions ended the run: interrupt it (see [Store.update]), then retry. */
    override fun onStopJob(params: JobParameters): Boolean {
        worker?.interrupt()
        return true
    }

    companion object {
        private const val ID = 1

        fun schedule(context: Context): Boolean =
            scheduler(context).schedule(job(context)) == JobScheduler.RESULT_SUCCESS

        /**
         * Schedules the job unless it's there with the right timing: hibernation or a force stop
         * deletes it, and the sources or the interval may change it. Not always: scheduling the
         * same id again stops a running job.
         */
        fun ensure(context: Context): Boolean {
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

        fun cancel(context: Context) = scheduler(context).cancel(ID)

        private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)
    }
}
