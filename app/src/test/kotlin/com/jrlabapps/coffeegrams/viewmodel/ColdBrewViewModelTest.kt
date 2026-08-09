package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.platform.RecordingNotificationScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Conformance-matched to iOS's `ColdBrewViewModelTests`. */
class ColdBrewViewModelTest {
    @Test
    fun `starting a steep asks permission and schedules the reminder`() = runTest {
        val spy = RecordingNotificationScheduler()
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = spy)
        vm.setSteepHours(18.0)

        vm.startSteep()

        assertEquals(1, spy.authRequestCount)
        assertEquals(1, spy.scheduled.size)
        assertEquals(18 * 3600.0, spy.scheduled.first().delaySeconds)
        assertEquals(ColdBrewViewModel.ReminderState.Scheduled(18), vm.reminderState.value)
    }

    @Test
    fun `denied permission degrades gracefully, nothing scheduled`() = runTest {
        val spy = RecordingNotificationScheduler(authorized = false)
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = spy)

        vm.startSteep()

        assertTrue(spy.scheduled.isEmpty())
        assertEquals(ColdBrewViewModel.ReminderState.Denied, vm.reminderState.value)
    }

    @Test
    fun `water uses the cold-brew ratio`() {
        val vm = ColdBrewViewModel(doseGrams = 100.0, ratio = 5.0, notifications = RecordingNotificationScheduler())
        assertEquals(500.0, vm.waterGrams)
    }
}
