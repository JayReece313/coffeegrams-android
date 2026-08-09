package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.ScheduledReminder
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises [LiveNotificationScheduler.buildWorkRequest] — the one piece of
 * the live adapter with real logic and no [android.content.Context]
 * dependency, so it's testable on the plain JVM. The rest of the adapter
 * (channel creation, `WorkManager` enqueueing) needs a real Android
 * environment and is left to manual/instrumented verification, matching the
 * iOS sibling's precedent of never unit-testing `LiveNotificationService`
 * directly.
 */
class LiveNotificationSchedulerTest {
    @Test
    fun `buildWorkRequest carries the reminder content as input data`() {
        val reminder = ScheduledReminder(
            id = "cold_brew_ready",
            title = "Cold brew ready",
            body = "Strain it and enjoy",
            delaySeconds = 120.0,
        )
        val data = LiveNotificationScheduler.buildWorkRequest(reminder).workSpec.input
        assertEquals(reminder.id, data.getString(ReminderWorker.KEY_ID))
        assertEquals(reminder.title, data.getString(ReminderWorker.KEY_TITLE))
        assertEquals(reminder.body, data.getString(ReminderWorker.KEY_BODY))
    }

    @Test
    fun `buildWorkRequest clamps the delay to at least one second`() {
        val reminder = ScheduledReminder(id = "x", title = "t", body = "b", delaySeconds = 0.2)
        val request = LiveNotificationScheduler.buildWorkRequest(reminder)
        assertEquals(TimeUnit.SECONDS.toMillis(1), request.workSpec.initialDelay)
    }

    @Test
    fun `buildWorkRequest preserves fractional-second precision, rounded up`() {
        val reminder = ScheduledReminder(id = "x", title = "t", body = "b", delaySeconds = 1.5)
        val request = LiveNotificationScheduler.buildWorkRequest(reminder)
        // Rounds up (not truncates) so the reminder never fires earlier than requested.
        assertEquals(1500L, request.workSpec.initialDelay)
    }

    @Test
    fun `buildWorkRequest tags the request with the reminder id`() {
        val reminder = ScheduledReminder(id = "french_press_plunge", title = "t", body = "b", delaySeconds = 90.0)
        val request = LiveNotificationScheduler.buildWorkRequest(reminder)
        // WorkManager auto-adds the worker's class name as a tag too, alongside ours.
        assertTrue(request.tags.contains(reminder.id))
    }
}
