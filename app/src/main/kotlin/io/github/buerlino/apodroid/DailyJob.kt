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
 * Runs every ~6 hours with network: fetches the newest APOD unless it's already stored, and sets
 * it as wallpaper once per date. A failed run is retried with the job's backoff.
 */
class DailyJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        thread {
            val result = runCatching { update(Store(this)) }
            result.onFailure { Log.w("APODroid", "Daily job failed: $it", it) }
            jobFinished(params, result.isFailure)
        }
        return true
    }

    override fun onStopJob(params: JobParameters) = true

    private fun update(store: Store) {
        if (!store.isCurrent) store.refresh()
        val apod = store.apod ?: return
        if (apod.date == store.wallpaperDate) return
        val file = store.wallpaperFile()
        if (file != null) store.setWallpaper(file)
        Log.i("APODroid", "Daily job: ${apod.date}, ${if (file != null) "wallpaper set" else "video, wallpaper kept"}")
    }

    companion object {
        private const val ID = 1

        fun isScheduled(context: Context) = scheduler(context).getPendingJob(ID) != null

        fun schedule(context: Context): Boolean {
            val job = JobInfo.Builder(ID, ComponentName(context, DailyJob::class.java))
                .setPeriodic(TimeUnit.HOURS.toMillis(6))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()
            return scheduler(context).schedule(job) == JobScheduler.RESULT_SUCCESS
        }

        fun cancel(context: Context) = scheduler(context).cancel(ID)

        private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)
    }
}
