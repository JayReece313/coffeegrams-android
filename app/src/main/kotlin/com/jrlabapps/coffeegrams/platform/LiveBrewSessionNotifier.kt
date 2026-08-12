package com.jrlabapps.coffeegrams.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.jrlabapps.coffeegrams.core.BrewSessionNotifier

/**
 * The live [BrewSessionNotifier] adapter. [start] launches
 * [BrewTimerForegroundService] (which calls the actual `startForeground()`);
 * [update] then talks to `NotificationManagerCompat` directly rather than
 * going back through the service — same process, so there's nothing the
 * service adds for a content-only update. The channel is created in [init],
 * matching [LiveNotificationScheduler]'s pattern.
 */
class LiveBrewSessionNotifier(private val context: Context) : BrewSessionNotifier {
    /** Tracks whether [start] has run, so [update] can honor its documented no-op-before-[start] contract. */
    private var started = false

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Brew in progress",
            // LOW, not DEFAULT: this is an ongoing status notification, updated
            // continuously as the brew advances — it should never make a sound
            // or pop up, unlike the one-shot M5 reminder channel.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the current step and time while a guided brew is running"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun start(title: String, message: String) {
        started = true
        val intent = Intent(context, BrewTimerForegroundService::class.java).apply {
            putExtra(BrewTimerForegroundService.EXTRA_TITLE, title)
            putExtra(BrewTimerForegroundService.EXTRA_MESSAGE, message)
        }
        context.startForegroundService(intent)
    }

    override fun update(title: String, message: String) {
        if (!started) return
        val notification = BrewTimerForegroundService.buildNotification(context, title, message)
        NotificationManagerCompat.from(context).notify(BrewTimerForegroundService.NOTIFICATION_ID, notification)
    }

    override fun stop() {
        started = false
        context.stopService(Intent(context, BrewTimerForegroundService::class.java))
        // Cancelled explicitly rather than relying solely on stopService()'s
        // implicit cleanup — update() posts directly via NotificationManagerCompat,
        // outside the service's own startForeground()/stopForeground() tracking,
        // so a notification it posted needs its own explicit removal here too.
        NotificationManagerCompat.from(context).cancel(BrewTimerForegroundService.NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "brew_in_progress"
    }
}
