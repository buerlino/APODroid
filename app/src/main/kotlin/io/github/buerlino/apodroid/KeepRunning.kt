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
import io.github.buerlino.apodroid.core.PendingStarts

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
        // The start counts as pending until startForeground's try has ended, foreground or
        // refused (fix round 3): delivered at the top left a gap (the notification's binder
        // calls) in which a stop found nothing pending and stopService crashed the app.
        val foreground = try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION,
                notification(this),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
            true
        } catch (e: Exception) {
            Log.w("APODroid", "Keep running: refused: $e")
            false
        } finally {
            pending.delivered(fromStart = intent != null)
        }
        if (!foreground) {
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

        /** Starts whose [onStartCommand] hasn't yet gone foreground or been refused: [stop] waits for them (fix rounds 1 to 3). */
        private val pending = PendingStarts()

        /**
         * Starts the service while the daily switch and "Keep running" are on, else stops it.
         * Android's refusal of a start from the background is caught and logged (the job's run
         * calls this once, at its start).
         */
        fun ensure(context: Context) {
            val store = Store(context)
            // The switch's read and the start under one lock with [stop]'s (fix round 2).
            val started = try {
                pending.start({ store.daily && store.keepRunning }) {
                    context.startForegroundService(Intent(context, KeepRunning::class.java))
                }
            } catch (e: Exception) {
                Log.w("APODroid", "Keep running: couldn't start: $e")
                return
            }
            if (!started) stop(context)
        }

        /**
         * Stops the service (the switch off, the daily switch off), unless a start is still
         * pending: a `stopService` before its `onStartCommand` takes the service down owing
         * `startForeground`, and the app crashes. The pending `onStartCommand` goes foreground,
         * reads the switch off and stops itself (callers write the switch before this). The
         * check and `stopService` run under the lock [ensure]'s start holds (fix round 2).
         */
        fun stop(context: Context) {
            pending.stopIf { context.stopService(Intent(context, KeepRunning::class.java)) }
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
