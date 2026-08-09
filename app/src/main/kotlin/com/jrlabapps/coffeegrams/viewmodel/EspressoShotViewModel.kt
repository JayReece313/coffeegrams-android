package com.jrlabapps.coffeegrams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrlabapps.coffeegrams.core.EspressoTarget
import com.jrlabapps.coffeegrams.core.MonotonicClock
import com.jrlabapps.coffeegrams.core.ShotTimingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the espresso-shot stopwatch: a target yield and a shot timer that
 * reads green inside the target window, amber before, red after.
 *
 * Same VM-owned tick loop as [GuidedBrewViewModel], for the same
 * rotation-survival reason — see that class's doc comment. Started in
 * [start], stopped in [stop], rather than running unconditionally for the
 * VM's whole lifetime, so it isn't waking up 10 times a second before the
 * shot begins or after it ends.
 */
class EspressoShotViewModel(
    val target: EspressoTarget,
    private val clock: MonotonicClock,
) : ViewModel() {

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var startTime: Double = 0.0
    private var tickerJob: Job? = null

    /** Green / amber / red classification of the current elapsed time against the target window. */
    val timingState: ShotTimingState
        get() = ShotTimingState.classify(_elapsedSeconds.value, target.shotTimeRange)

    val hasStarted: Boolean get() = _isRunning.value || _elapsedSeconds.value > 0

    fun startOrStop() {
        if (_isRunning.value) stop() else start()
    }

    fun start() {
        _isRunning.value = true
        _elapsedSeconds.value = 0
        startTime = clock.now
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                tick()
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        tickerJob?.cancel()
        tickerJob = null
    }

    fun reset() {
        stop()
        _elapsedSeconds.value = 0
    }

    /** Recompute elapsed whole seconds from the clock. A no-op when stopped. */
    fun tick() {
        if (!_isRunning.value) return
        _elapsedSeconds.value = maxOf(0, (clock.now - startTime).toInt())
    }

    private companion object {
        const val TICK_INTERVAL_MS = 100L
    }
}
