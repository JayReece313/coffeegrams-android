package com.jrlabapps.coffeegrams.viewmodel

import androidx.lifecycle.ViewModel
import com.jrlabapps.coffeegrams.core.BrewCalculator
import com.jrlabapps.coffeegrams.core.NotificationScheduling
import com.jrlabapps.coffeegrams.platform.BrewReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the cold-brew screen. Cold brew has no live timer — instead,
 * starting a steep schedules a local notification for when it's done.
 */
class ColdBrewViewModel(
    val doseGrams: Double,
    val ratio: Double,
    private val notifications: NotificationScheduling,
) : ViewModel() {

    /** The outcome of trying to start a steep. */
    sealed interface ReminderState {
        data object Idle : ReminderState
        data class Scheduled(val hours: Int) : ReminderState
        data object Denied : ReminderState
    }

    private val _steepHours = MutableStateFlow(16.0)
    val steepHours: StateFlow<Double> = _steepHours.asStateFlow()
    fun setSteepHours(hours: Double) {
        _steepHours.value = hours
    }

    private val _reminderState = MutableStateFlow<ReminderState>(ReminderState.Idle)
    val reminderState: StateFlow<ReminderState> = _reminderState.asStateFlow()

    val waterGrams: Double get() = BrewCalculator.waterGrams(doseGrams, ratio)

    /**
     * Ask permission and schedule the "steep done" reminder. If permission
     * is denied we degrade gracefully — the user can still brew, just
     * without the reminder.
     */
    suspend fun startSteep() {
        val granted = notifications.requestAuthorization()
        if (!granted) {
            _reminderState.value = ReminderState.Denied
            return
        }
        notifications.schedule(BrewReminder.coldBrewReady(_steepHours.value))
        _reminderState.value = ReminderState.Scheduled(_steepHours.value.toInt())
    }
}
