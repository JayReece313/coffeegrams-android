package com.jrlabapps.coffeegrams.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jrlabapps.coffeegrams.core.NotificationScheduling
import com.jrlabapps.coffeegrams.core.ScheduledReminder
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

/**
 * The live [NotificationScheduling] adapter, backed by a notification
 * channel plus `WorkManager` for the delay. The channel is created in
 * [init] so it exists before first use regardless of when this is
 * constructed, matching how [CHANNEL_ID] is referenced by [ReminderWorker].
 */
class LiveNotificationScheduler(private val context: Context) : NotificationScheduling {
    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Brew reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Cold brew and French press steep reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun requestAuthorization(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override suspend fun schedule(reminder: ScheduledReminder) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(reminder.id, ExistingWorkPolicy.REPLACE, buildWorkRequest(reminder))
    }

    /** Cancels pending scheduling only — a reminder already delivered stays delivered. */
    override fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(id)
    }

    companion object {
        const val CHANNEL_ID = "brew_reminders"

        /**
         * Pure — buildable without a [Context] — so it's unit-testable on
         * the plain JVM. [ExistingWorkPolicy.REPLACE] (used by [schedule])
         * also cancels already-*running* work, not just pending work, which
         * is fine here since [ReminderWorker] runs near-instantly.
         *
         * The clamped delay is rounded *up* to whole milliseconds ([ceil],
         * not truncation) so a fractional-second delay never fires earlier
         * than requested.
         */
        fun buildWorkRequest(reminder: ScheduledReminder): OneTimeWorkRequest {
            val data = Data.Builder()
                .putString(ReminderWorker.KEY_ID, reminder.id)
                .putString(ReminderWorker.KEY_TITLE, reminder.title)
                .putString(ReminderWorker.KEY_BODY, reminder.body)
                .build()
            val delayMillis = ceil(max(1.0, reminder.delaySeconds) * 1000.0).toLong()
            return OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(reminder.id)
                .build()
        }
    }
}
