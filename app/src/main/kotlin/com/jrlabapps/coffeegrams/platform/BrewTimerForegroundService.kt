package com.jrlabapps.coffeegrams.platform

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jrlabapps.coffeegrams.MainActivity

/**
 * A thin foreground service whose only job is to give Android a reason not
 * to kill this process while a guided brew is running — Doze and background
 * OOM-kill both treat an active foreground service as user-visible work.
 * It owns no timer logic: [com.jrlabapps.coffeegrams.viewmodel.GuidedBrewViewModel]
 * keeps ticking exactly as it already does, and [LiveBrewSessionNotifier]
 * updates this notification's content directly via `NotificationManagerCompat`
 * rather than routing updates back through here.
 *
 * `specialUse` is the foreground service type: none of Android's specific
 * categories (camera, location, mediaPlayback, etc.) describe "an ongoing
 * coffee brew," and `specialUse` exists precisely for that case — see the
 * `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declaration on this service in
 * `AndroidManifest.xml` for the justification string Play reviews.
 */
class BrewTimerForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent?.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val notification = buildNotification(this, title, message)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        // No reason to let the system restart this with a null intent — a
        // restarted service has no brew context to show.
        return START_NOT_STICKY
    }

    companion object {
        const val NOTIFICATION_ID = 4200
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"

        fun buildNotification(context: android.content.Context, title: String, message: String): Notification {
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, LiveBrewSessionNotifier.CHANNEL_ID)
                // Placeholder system icon — a notification small icon must be a
                // monochrome/alpha-only silhouette (Android re-renders it that way
                // regardless), and no such asset exists in the repo yet. Swap for a
                // real branded one alongside M11's other store-facing asset work.
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle(title)
                .setContentText(message)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .build()
        }
    }
}
