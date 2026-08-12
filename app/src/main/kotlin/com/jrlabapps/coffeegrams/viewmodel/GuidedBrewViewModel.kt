package com.jrlabapps.coffeegrams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrlabapps.coffeegrams.core.BrewSessionNotifier
import com.jrlabapps.coffeegrams.core.BrewStep
import com.jrlabapps.coffeegrams.core.BrewTimeline
import com.jrlabapps.coffeegrams.core.BrewTimerEngine
import com.jrlabapps.coffeegrams.core.BrewTimerEvent
import com.jrlabapps.coffeegrams.core.BrewTimerPhase
import com.jrlabapps.coffeegrams.core.Haptics
import com.jrlabapps.coffeegrams.core.MonotonicClock
import com.jrlabapps.coffeegrams.design.TimeFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Drives the guided-brew timer screen. It owns the tested [BrewTimerEngine]
 * and mirrors its state into [StateFlow]s the screen renders.
 *
 * Unlike the iOS sibling — where the *View* drives a 0.1s `Timer.publish`
 * and calls a passive `tickOnce()` — this VM owns its own tick loop
 * ([TICK_INTERVAL_MS]), started and stopped in lockstep with
 * [BrewTimerEngine.isActive] (see [updateTicker]) rather than running
 * unconditionally for the VM's whole lifetime, so it isn't waking up 10
 * times a second while idle, paused, or finished. That's a deliberate
 * divergence, not drift: on Android the View is exactly what gets
 * destroyed on device rotation, so it can't be the cadence owner if the
 * brew is meant to survive a configuration change. [tick] itself stays
 * exactly as passive and pure as iOS's `tickOnce()` — only *who calls it*
 * moved.
 *
 * [sessionNotifier] (M9) exists for a different survival problem: rotation
 * is covered by [viewModelScope] outliving it, but nothing before M9 stopped
 * Android from killing the whole *process* while backgrounded, which would
 * take [viewModelScope] — and the brew — with it. The foreground service it
 * drives is what keeps the process alive; the actual catch-up on resume
 * needs no new logic here, since [tick] already computes its delta from
 * [MonotonicClock.now] (`elapsedRealtime`-backed, keeps advancing through
 * Doze) and [BrewTimerEngine.advance] was built to fast-forward by exactly
 * that delta.
 */
class GuidedBrewViewModel(
    val timeline: BrewTimeline,
    private val clock: MonotonicClock,
    private val haptics: Haptics,
    private val sessionNotifier: BrewSessionNotifier,
) : ViewModel() {

    private val engine = BrewTimerEngine(timeline)

    private val _phase = MutableStateFlow(engine.phase)
    val phase: StateFlow<BrewTimerPhase> = _phase.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(engine.currentStepIndex)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _fractionComplete = MutableStateFlow(0.0)
    val fractionComplete: StateFlow<Double> = _fractionComplete.asStateFlow()

    /** Seconds the **final** step has run past its target, or `null` when no overrun is showing. */
    private val _overrunSeconds = MutableStateFlow<Int?>(null)
    val overrunSeconds: StateFlow<Int?> = _overrunSeconds.asStateFlow()

    private val _isOnFinalStep = MutableStateFlow(false)
    val isOnFinalStep: StateFlow<Boolean> = _isOnFinalStep.asStateFlow()

    /** The master count-up clock: total wall-clock seconds since [start]. */
    private val _totalElapsedSeconds = MutableStateFlow(0)
    val totalElapsedSeconds: StateFlow<Int> = _totalElapsedSeconds.asStateFlow()

    /** The clock reading at the last tick, used to compute the delta to advance. */
    private var lastTickTime: Double = 0.0

    private var tickerJob: Job? = null

    /**
     * The last notification content actually posted — [syncFromEngine] only
     * calls [sessionNotifier]'s `update` when this changes, since the
     * 100ms tick loop recomputes it far more often than the second-granularity
     * display text ever does, and `NotificationManager.notify()` on every
     * tick is 10 avoidable calls a second for the whole brew.
     */
    private var lastNotifiedMessage: String? = null

    init {
        engine.onEvent = { event -> handle(event) }
        syncFromEngine()
    }

    // MARK: Derived view state

    val steps: List<BrewStep> get() = timeline.steps
    val isIdle: Boolean get() = _phase.value == BrewTimerPhase.IDLE
    val isRunning: Boolean get() = _phase.value == BrewTimerPhase.RUNNING
    val isPaused: Boolean get() = _phase.value == BrewTimerPhase.PAUSED
    val isOverrunning: Boolean get() = _phase.value == BrewTimerPhase.OVERRUNNING
    val isAwaitingManualAdvance: Boolean get() = _phase.value == BrewTimerPhase.AWAITING_MANUAL_ADVANCE
    val isFinished: Boolean get() = _phase.value == BrewTimerPhase.COMPLETED

    /** True whenever a brew is live — running, overrunning, or held on a manual step. */
    val isActive: Boolean get() = isRunning || isOverrunning || isAwaitingManualAdvance

    /** True when the brew has begun and can therefore be finished or stopped. */
    val hasStarted: Boolean get() = !isIdle && !isFinished

    /**
     * The user's action on a step that is holding, or `null` when the brew
     * is flowing on its own. In practice only the final step holds, so this
     * is "Done" — the tap that ends the brew.
     */
    val advanceTitle: String?
        get() {
            if (!(isOverrunning || isAwaitingManualAdvance)) return null
            return if (_isOnFinalStep.value) "Done" else "Next"
        }

    /** True when the step's own button already ends the brew, so a separate "End Brew" would be redundant. */
    val stepActionEndsBrew: Boolean
        get() = _isOnFinalStep.value && (isOverrunning || isAwaitingManualAdvance)

    /** True when the readout is showing a count-*up* rather than a countdown. */
    val isShowingOverrun: Boolean get() = _overrunSeconds.value != null

    val currentStep: BrewStep? get() = steps.getOrNull(_currentStepIndex.value)

    /** The total elapsed time to write to the log when the brew is saved. */
    val actualSeconds: Int get() = _totalElapsedSeconds.value

    /** The brew's planned duration: the sum of the timeline's fixed step durations. */
    val plannedSeconds: Int get() = timeline.totalFixedDuration

    // MARK: Controls

    fun start() {
        if (_phase.value != BrewTimerPhase.IDLE) return
        engine.start()
        lastTickTime = clock.now
        // Reads engine directly, not the not-yet-synced StateFlows below —
        // the notifier's start() must fire before syncFromEngine()'s own
        // update() call, so this can't wait for that sync to happen first.
        // Recording it as already-notified here is what stops the very next
        // syncFromEngine() call from immediately re-posting the same content.
        val message = notificationMessage()
        sessionNotifier.start(timeline.method.displayName, message)
        lastNotifiedMessage = message
        syncFromEngine()
    }

    fun pause() {
        engine.pause()
        syncFromEngine()
    }

    fun resume() {
        engine.resume()
        lastTickTime = clock.now
        syncFromEngine()
    }

    /** The "Next"/"Done" action for a manual step (or a deliberate skip). */
    fun advanceStep() {
        engine.advanceStep()
        lastTickTime = clock.now
        syncFromEngine()
    }

    /** End the brew where it stands — stops the master clock, keeps the time it actually took. */
    fun finish() {
        engine.finish()
        syncFromEngine()
        sessionNotifier.stop()
    }

    /**
     * Defensive stop, covering paths [finish] doesn't: the user backing out
     * of the screen mid-brew (leave-confirmation dialog, then navigation)
     * clears this ViewModel without ever calling [finish]. Safe to call
     * even when no session was ever started — [BrewSessionNotifier.stop]
     * is a no-op in that case.
     */
    override fun onCleared() {
        sessionNotifier.stop()
    }

    /** The action behind the step's own button — ends the brew on the final step, otherwise just advances. */
    fun resolveCurrentStep() {
        if (stepActionEndsBrew) finish() else advanceStep()
    }

    /** One button, two meanings: pause a live brew, resume a paused one. */
    fun togglePause() {
        if (isPaused) resume() else pause()
    }

    fun reset() {
        engine.reset()
        syncFromEngine()
    }

    // MARK: Ticking (called by the internal loop; tests call it directly)

    /**
     * Advance the engine by the real time elapsed since the last tick.
     * Ticks through **every live phase**, not just running — the master
     * clock keeps counting while a hands-on step overruns and while a
     * manual step waits.
     */
    fun tick() {
        if (!engine.isActive) return
        val now = clock.now
        val delta = now - lastTickTime
        lastTickTime = now
        engine.advance(delta)
        syncFromEngine()
    }

    // MARK: Engine bridging

    private fun handle(event: BrewTimerEvent) {
        when (event) {
            is BrewTimerEvent.StepBegan -> haptics.stepChange()
            is BrewTimerEvent.ReachedTarget -> haptics.targetReached()
            BrewTimerEvent.Completed -> haptics.finished()
            BrewTimerEvent.Started, is BrewTimerEvent.StepCompleted, is BrewTimerEvent.AwaitingManualAdvance -> Unit
        }
    }

    private fun syncFromEngine() {
        _phase.value = engine.phase
        _currentStepIndex.value = engine.currentStepIndex
        // Round up so a step with 0.3s left still reads "1", never "0:00" while it's still going.
        _remainingSeconds.value = ceil(engine.remainingInStep ?: 0.0).toInt()
        _fractionComplete.value = engine.fractionComplete
        // Round overrun and the master clock *down*: they count up, so flooring
        // means the display never claims more time than has actually passed.
        _overrunSeconds.value = engine.overrunInStep?.let { floor(it).toInt() }
        _totalElapsedSeconds.value = floor(engine.totalWallElapsed).toInt()
        _isOnFinalStep.value = engine.isOnFinalStep
        updateTicker()
        if (engine.isActive) {
            val message = notificationMessage()
            if (message != lastNotifiedMessage) {
                sessionNotifier.update(timeline.method.displayName, message)
                lastNotifiedMessage = message
            }
        }
    }

    /**
     * "Step 2: Bloom — 0:12" while counting down, "Step 4: Draw down —
     * +0:07" while overrunning. Reads [engine] directly rather than the
     * `_stateFlows` above — [start] needs this before [syncFromEngine] has
     * populated them for the first time.
     */
    private fun notificationMessage(): String {
        val step = engine.currentStep ?: return ""
        val overrun = engine.overrunInStep
        val time = if (overrun != null) {
            "+${TimeFormat.clock(floor(overrun).toInt())}"
        } else {
            TimeFormat.clock(ceil(engine.remainingInStep ?: 0.0).toInt())
        }
        return "Step ${engine.currentStepIndex + 1}: ${step.title} — $time"
    }

    /**
     * Starts the tick loop when the engine becomes active, stops it
     * otherwise — called after every state change so the ticker always
     * matches [BrewTimerEngine.isActive] regardless of which method
     * caused the transition (a direct [advanceStep] on the final step can
     * complete the brew, not just [finish]).
     */
    private fun updateTicker() {
        if (engine.isActive) {
            if (tickerJob == null) {
                tickerJob = viewModelScope.launch {
                    while (true) {
                        delay(TICK_INTERVAL_MS)
                        tick()
                    }
                }
            }
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 100L
    }
}
