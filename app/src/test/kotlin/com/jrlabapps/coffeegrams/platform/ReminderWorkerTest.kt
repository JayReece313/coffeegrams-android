package com.jrlabapps.coffeegrams.platform

import androidx.work.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises [ReminderWorker.contentFrom] — the pure part of the worker,
 * testable without Android. [ReminderWorker.doWork] itself needs a real
 * `NotificationManager` and stays unautomated.
 */
class ReminderWorkerTest {
    @Test
    fun `contentFrom derives a stable notification id from the reminder id`() {
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_ID, "cold_brew_ready")
            .putString(ReminderWorker.KEY_TITLE, "Cold brew ready")
            .putString(ReminderWorker.KEY_BODY, "Strain it and enjoy")
            .build()

        val content = ReminderWorker.contentFrom(data)

        assertEquals("cold_brew_ready".hashCode(), content?.notificationId)
        assertEquals("Cold brew ready", content?.title)
        assertEquals("Strain it and enjoy", content?.body)
    }

    @Test
    fun `contentFrom returns null when required data is missing`() {
        val data = Data.Builder().putString(ReminderWorker.KEY_ID, "id").build()
        assertNull(ReminderWorker.contentFrom(data))
    }
}
