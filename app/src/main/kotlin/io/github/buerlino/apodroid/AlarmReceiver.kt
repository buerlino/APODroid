package io.github.buerlino.apodroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * The alarms' receiver (0.6.0, J2; [DailyJob]): an alarm of the pair ([ACTION_RUN]) starts the
 * run ([DailyJob.runNow]). Android's broadcasts arm the alarms again while the daily switch is on
 * ([DailyJob.ensure]): after a boot (alarms don't survive one), a time zone or clock change (they
 * are absolute times, so "06:00" armed before it is wrong after it) and an update of the app
 * (alarms don't survive that either, and the first morning after it mustn't wait for a page open).
 * Not exported: the system's broadcasts reach it anyway, and the alarms are the app's own.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RUN -> DailyJob.runNow(context)
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (!Store(context).daily) return
                Log.i("APODroid", "Daily job: ${intent.action}, arming again")
                if (!DailyJob.ensure(context)) Log.w("APODroid", "Daily job: couldn't schedule the fallback")
            }
        }
    }

    companion object {
        const val ACTION_RUN = "io.github.buerlino.apodroid.RUN"
    }
}
