package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.NotificationScheduling
import com.jrlabapps.coffeegrams.core.ScheduledReminder

/** A [NotificationScheduling] test double that records calls instead of touching WorkManager. */
class RecordingNotificationScheduler(private var authorized: Boolean = true) : NotificationScheduling {
    private val _scheduled = mutableListOf<ScheduledReminder>()
    val scheduled: List<ScheduledReminder> get() = _scheduled

    private val _cancelled = mutableListOf<String>()
    val cancelled: List<String> get() = _cancelled

    override fun requestAuthorization(): Boolean = authorized

    override suspend fun schedule(reminder: ScheduledReminder) {
        _scheduled += reminder
    }

    override fun cancel(id: String) {
        _cancelled += id
    }
}
