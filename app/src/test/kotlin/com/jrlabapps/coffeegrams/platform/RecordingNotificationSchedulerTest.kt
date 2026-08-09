package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.ScheduledReminder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingNotificationSchedulerTest {
    private fun reminder(id: String = "r1") =
        ScheduledReminder(id = id, title = "t", body = "b", delaySeconds = 60.0)

    @Test
    fun `schedule records the reminder`() = runTest {
        val scheduler = RecordingNotificationScheduler()
        val reminder = reminder()
        scheduler.schedule(reminder)
        assertEquals(listOf(reminder), scheduler.scheduled)
    }

    @Test
    fun `cancel records the id`() {
        val scheduler = RecordingNotificationScheduler()
        scheduler.cancel("r1")
        assertEquals(listOf("r1"), scheduler.cancelled)
    }

    @Test
    fun `requestAuthorization reports the configured state`() {
        assertTrue(RecordingNotificationScheduler(authorized = true).requestAuthorization())
        assertFalse(RecordingNotificationScheduler(authorized = false).requestAuthorization())
    }
}
