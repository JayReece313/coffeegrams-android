package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.platform.RecordingNotificationScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Conformance-matched to iOS's `ColdBrewViewModelTests`, adapted for the
 * Android-specific `canScheduleReminders()` + `startSteep(permissionGranted)`
 * split (iOS's single `startSteep()` can request the system prompt itself;
 * Android can't, so the caller determines the result and passes it in).
 */
class ColdBrewViewModelTest {
    @Test
    fun `starting a steep schedules the reminder when permission is granted`() = runTest {
        val spy = RecordingNotificationScheduler()
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = spy)
        vm.setSteepHours(18.0)

        val granted = vm.canScheduleReminders()
        vm.startSteep(granted)

        assertEquals(1, spy.authRequestCount)
        assertEquals(1, spy.scheduled.size)
        assertEquals(18 * 3600.0, spy.scheduled.first().delaySeconds)
        assertEquals(ColdBrewViewModel.ReminderState.Scheduled(18), vm.reminderState.value)
    }

    @Test
    fun `denied permission degrades gracefully, nothing scheduled`() = runTest {
        val spy = RecordingNotificationScheduler(authorized = false)
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = spy)

        assertFalse(vm.canScheduleReminders())
        vm.startSteep(permissionGranted = false)

        assertTrue(spy.scheduled.isEmpty())
        assertEquals(ColdBrewViewModel.ReminderState.Denied, vm.reminderState.value)
    }

    @Test
    fun `startSteep trusts the passed-in permission result, not the port's own check`() = runTest {
        // The port itself reports "not authorized", but the caller already
        // obtained permission through a fresh system prompt the port can't
        // see -- startSteep must trust the parameter, not re-derive it.
        val spy = RecordingNotificationScheduler(authorized = false)
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = spy)

        vm.startSteep(permissionGranted = true)

        assertEquals(1, spy.scheduled.size)
        assertEquals(ColdBrewViewModel.ReminderState.Scheduled(16), vm.reminderState.value)
    }

    @Test
    fun `water uses the cold-brew ratio`() {
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = RecordingNotificationScheduler())
        assertEquals(500.0, vm.waterGrams)
    }
}
