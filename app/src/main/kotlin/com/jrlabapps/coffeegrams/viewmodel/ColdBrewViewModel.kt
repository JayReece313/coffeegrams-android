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
     * Whether reminders are currently allowed, per the OS's current
     * permission state. Does **not** prompt — on Android the system
     * permission dialog can only be shown from an Activity/Compose
     * composition, never from a plain class (see
     * [NotificationScheduling.requestAuthorization]). The caller (the
     * cold-brew screen, M7) calls this first; if it's `false`, the screen
     * shows the real system prompt itself (e.g. via
     * `rememberLauncherForActivityResult`) and passes the result to
     * [startSteep].
     */
    fun canScheduleReminders(): Boolean = notifications.requestAuthorization()

    /**
     * Schedule the "steep done" reminder if [permissionGranted], else
     * degrade gracefully — the user can still brew, just without the
     * reminder. Takes the permission result as a parameter rather than
     * checking it itself, so this class never implies it can request the
     * system permission — only the caller, from a real UI context, can
     * actually obtain a "yes" it didn't already have.
     */
    suspend fun startSteep(permissionGranted: Boolean) {
        if (!permissionGranted) {
            _reminderState.value = ReminderState.Denied
            return
        }
        notifications.schedule(BrewReminder.coldBrewReady(_steepHours.value))
        _reminderState.value = ReminderState.Scheduled(_steepHours.value.toInt())
    }
}
