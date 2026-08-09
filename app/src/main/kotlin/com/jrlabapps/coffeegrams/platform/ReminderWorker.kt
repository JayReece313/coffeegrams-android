package com.jrlabapps.coffeegrams.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jrlabapps.coffeegrams.R

/**
 * Delivers a single scheduled reminder as a notification. Runs via
 * [LiveNotificationScheduler]'s `WorkManager` request, never constructed
 * directly.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val content = contentFrom(inputData) ?: return Result.failure()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Reminders were never authorized (or were revoked) — nothing to deliver.
            return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, LiveNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(content.notificationId, notification)
        return Result.success()
    }

    /** What to show, derived from the [Data] a [LiveNotificationScheduler] request carries. */
    data class Content(val notificationId: Int, val title: String, val body: String)

    companion object {
        const val KEY_ID = "reminder_id"
        const val KEY_TITLE = "reminder_title"
        const val KEY_BODY = "reminder_body"

        /**
         * Pure — no [Context] involved — so it's unit-testable on the plain
         * JVM even though [doWork] itself needs a real Android environment.
         */
        fun contentFrom(data: Data): Content? {
            val id = data.getString(KEY_ID) ?: return null
            val title = data.getString(KEY_TITLE) ?: return null
            val body = data.getString(KEY_BODY) ?: return null
            return Content(notificationId = id.hashCode(), title = title, body = body)
        }
    }
}
