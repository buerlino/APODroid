package io.github.buerlino.apodroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat

/**
 * "Keep running" (0.7.0, O11): a foreground service that does nothing but be one, so the app's
 * process isn't cached and a maker's cleaner that force-stops cached apps (ZUI's, O8) leaves it
 * and its alarms alone. No thread, no wake lock, no network: the runs stay the alarms' and the
 * fallback's ([DailyJob]), in this same process. Started by [ensure] while the daily switch and
 * the setting are on; sticky, so a process the low-memory killer took comes back (O7, O8).
 *
 * Android may refuse a start from the background (`ForegroundServiceStartNotAllowedException`,
 * 12+: a run's start while Optimized, a sticky restart): caught, logged, and the service stops;
 * the next page open, boot or update starts it again. The page shows nothing for it.
 */
class KeepRunning : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION,
                notification(this),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        } catch (e: Exception) {
            Log.w("APODroid", "Keep running: refused: $e")
            stopSelf()
            return START_NOT_STICKY
        }
        // A start that crossed an off (the job's ensure read the switch on just before the page
        // turned it off): foreground first, as startForegroundService requires, then stop.
        val store = Store(this)
        if (!store.daily || !store.keepRunning) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION = 1
        private const val CHANNEL = "keep"

        /**
         * Starts the service while the daily switch and "Keep running" are on, else stops it.
         * Android's refusal of a start from the background is caught and logged (the job's run
         * calls this once, at its start).
         */
        fun ensure(context: Context) {
            val store = Store(context)
            if (!store.daily || !store.keepRunning) return stop(context)
            try {
                context.startForegroundService(Intent(context, KeepRunning::class.java))
            } catch (e: Exception) {
                Log.w("APODroid", "Keep running: couldn't start: $e")
            }
        }

        /** Stops the service (the switch off, the daily switch off). */
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepRunning::class.java))
        }

        /** The channel "Keep running" (low: no sound, no peek; no badge) and its notification; a tap opens the page. */
        private fun notification(context: Context): Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Keep running", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
            )
            val page = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            return Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_keep_running)
                .setContentText("Keeps the wallpaper changing on time")
                .setContentIntent(page)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        }
    }
}
