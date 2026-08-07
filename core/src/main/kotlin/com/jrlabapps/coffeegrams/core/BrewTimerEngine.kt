package com.jrlabapps.coffeegrams.core

/** Where the guided brew currently is. */
enum class BrewTimerPhase {
    /** Not started yet. */
    IDLE,

    /** A fixed-duration step is counting down. */
    RUNNING,

    /**
     * The brew's **final** step has reached its target and is now counting
     * *up* past it, waiting for the user to finish. Intermediate steps flow
     * straight into the next one; only the last step holds, because there
     * is nothing to advance to and the brew is not over until you say so.
     */
    OVERRUNNING,

    /** Temporarily paused by the user. */
    PAUSED,

    /**
     * On a manual step (plunge / drawdown-until-drips) — the clock is held
     * and we are waiting for the user to tap "next".
     */
    AWAITING_MANUAL_ADVANCE,

    /** The whole timeline is finished. */
    COMPLETED,
}

/**
 * Something worth reacting to — the app turns these into haptics / sounds /
 * UI updates. Emitted synchronously as the engine's state changes.
 */
sealed class BrewTimerEvent {
    data object Started : BrewTimerEvent()
    data class StepBegan(val index: Int, val step: BrewStep) : BrewTimerEvent()

    /**
     * The final step hit its target and is now overrunning. Worth a
     * haptic: it is the cue that the brew has reached its planned time.
     */
    data class ReachedTarget(val index: Int, val step: BrewStep) : BrewTimerEvent()
    data class StepCompleted(val index: Int, val step: BrewStep) : BrewTimerEvent()
    data class AwaitingManualAdvance(val index: Int, val step: BrewStep) : BrewTimerEvent()
    data object Completed : BrewTimerEvent()
}

/**
 * Walks a [BrewTimeline]'s steps in real time.
 *
 * Design: the engine is a **pure state machine driven by time deltas**, not
 * by reading a clock. The app feeds it [advance] from a repeating timer
 * that measures elapsed seconds via a [MonotonicClock]; tests feed it exact
 * deltas. This inversion is the entire reason the timer is testable without
 * waiting in real time — a full four-minute French Press brew is verified
 * in microseconds.
 *
 * Manual steps (null duration) *hold* the machine in
 * [BrewTimerPhase.AWAITING_MANUAL_ADVANCE]: [advance] deliberately does
 * nothing until [advanceStep] is called, so a plunge or drawdown never
 * times out on the user.
 *
 * This is mutable reference state intended to live alongside the UI layer;
 * the app wraps it in an observable ViewModel. `:core` keeps it free of any
 * UI framework.
 *
 * [advance] takes a delta rather than reading a clock itself — that is the
 * mechanism the app's background-timer hardening depends on for continuity
 * across backgrounding (fast-forwarding by a large delta on resume). Do not
 * change this to read a clock internally.
 */
class BrewTimerEngine(val timeline: BrewTimeline) {

    var phase: BrewTimerPhase = BrewTimerPhase.IDLE
        private set

    var currentStepIndex: Int = 0
        private set

    /**
     * Seconds elapsed within the current step. For a soft-target step this
     * may exceed the step's duration — that excess is the overrun.
     */
    var elapsedInStep: Double = 0.0
        private set

    /**
     * Seconds of time counted **against the timeline's fixed durations**.
     * Manual holds and overruns add none, so this stays a clean measure of
     * progress through the plan — it is what [fractionComplete] divides.
     */
    var totalElapsed: Double = 0.0
        private set

    /**
     * Seconds of **wall-clock** time since [start], the master "total
     * elapsed" clock. Unlike [totalElapsed] this keeps running through
     * overruns and manual holds, and stops only while paused — so it
     * answers "how long did this brew actually take?" rather than "how far
     * through the plan are we?". Pairing the two is what gives us planned
     * vs actual.
     */
    var totalWallElapsed: Double = 0.0
        private set

    /**
     * The phase to return to on [resume]. A brew can be paused from any
     * live phase, so "unpause" is not simply "go back to running".
     */
    private var phaseBeforePause: BrewTimerPhase? = null

    /** Fired synchronously on every state transition. Wire haptics/sound here. */
    var onEvent: ((BrewTimerEvent) -> Unit)? = null

    // MARK: Derived state (for the UI)

    private val steps: List<BrewStep> get() = timeline.steps

    val currentStep: BrewStep?
        get() = steps.getOrNull(currentStepIndex)

    /**
     * Seconds left in the current fixed-duration step, or `null` for
     * manual / finished states. Clamped at 0 — once a soft-target step
     * passes its target the overrun is reported by [overrunInStep], not as
     * a negative remainder, so existing countdown UI needs no
     * special-casing.
     */
    val remainingInStep: Double?
        get() {
            val duration = currentStep?.duration ?: return null
            return maxOf(0.0, duration.toDouble() - elapsedInStep)
        }

    /** True when the brew is on its last step — the only step that holds. */
    val isOnFinalStep: Boolean
        get() = steps.isNotEmpty() && currentStepIndex == steps.size - 1

    /**
     * Seconds the **final** step has run past its target — what the UI
     * shows as `+0:07`, and, because every earlier step auto-advances
     * exactly on time, also precisely how far the whole brew is past its
     * plan.
     *
     * `null` on any step that isn't holding. A manual final step (plunge,
     * drawdown) has no target time at all, so every second spent on it
     * counts.
     *
     * While **paused** this reports against the phase the brew will resume
     * into, so the reading freezes rather than disappearing. Without that,
     * the view falls through to the countdown — and a manual step has no
     * duration to count down, so pausing on the drawdown would read a
     * misleading "0:00" instead of holding at "+0:12".
     */
    val overrunInStep: Double?
        get() = when (effectivePhase) {
            BrewTimerPhase.OVERRUNNING -> {
                val duration = currentStep?.duration
                if (duration == null) null else maxOf(0.0, elapsedInStep - duration.toDouble())
            }
            BrewTimerPhase.AWAITING_MANUAL_ADVANCE -> if (isOnFinalStep) elapsedInStep else null
            else -> null
        }

    /**
     * The phase that describes what the brew *is doing*, seeing through a
     * pause to the phase it will resume into. Use this for anything the
     * user reads; use [phase] for anything that decides whether time
     * advances.
     */
    private val effectivePhase: BrewTimerPhase
        get() = if (phase == BrewTimerPhase.PAUSED) (phaseBeforePause ?: BrewTimerPhase.PAUSED) else phase

    /**
     * True while a live brew is in progress in any form — running,
     * overrunning or held on a manual step. Distinguishes "the brew is
     * going" from paused, idle and completed, which is exactly what a
     * Start/Stop toggle needs.
     */
    val isActive: Boolean
        get() = phase == BrewTimerPhase.RUNNING ||
            phase == BrewTimerPhase.OVERRUNNING ||
            phase == BrewTimerPhase.AWAITING_MANUAL_ADVANCE

    /** 0…1 progress across the brew's fixed duration. Completed reads as 1. */
    val fractionComplete: Double
        get() {
            if (phase == BrewTimerPhase.COMPLETED) return 1.0
            val total = timeline.totalFixedDuration
            if (total <= 0) return 0.0
            return minOf(1.0, totalElapsed / total.toDouble())
        }

    val isFinished: Boolean get() = phase == BrewTimerPhase.COMPLETED

    // MARK: Controls

    /** Begin from idle. No-op if already started. */
    fun start() {
        if (phase != BrewTimerPhase.IDLE) return
        if (steps.isEmpty()) {
            phase = BrewTimerPhase.COMPLETED
            onEvent?.invoke(BrewTimerEvent.Completed)
            return
        }
        currentStepIndex = 0
        elapsedInStep = 0.0
        totalElapsed = 0.0
        totalWallElapsed = 0.0
        phaseBeforePause = null
        phase = BrewTimerPhase.RUNNING
        onEvent?.invoke(BrewTimerEvent.Started)
        beginCurrentStep()
    }

    /**
     * Advance the brew by [delta] seconds of real time.
     *
     * Two clocks move here, and they move differently:
     *
     * - [totalWallElapsed] accrues in **every** live phase — running,
     *   overrunning, and holding on a manual step — because it measures how
     *   long the brew has actually taken.
     * - [totalElapsed] accrues **only** against a step's fixed duration,
     *   and never past it, so the progress bar cannot exceed the plan.
     *
     * Ignored while idle, paused or completed.
     */
    fun advance(delta: Double) {
        if (delta <= 0) return

        when (phase) {
            BrewTimerPhase.IDLE, BrewTimerPhase.PAUSED, BrewTimerPhase.COMPLETED -> return

            BrewTimerPhase.OVERRUNNING, BrewTimerPhase.AWAITING_MANUAL_ADVANCE -> {
                // Held for the user. The plan makes no progress, but time
                // passes: the master clock and the step's own elapsed
                // counter keep going so we can show "+0:07" and log an
                // honest actual duration.
                elapsedInStep += delta
                totalWallElapsed += delta
            }

            BrewTimerPhase.RUNNING -> {
                var remaining = delta
                while (remaining > 0 && phase == BrewTimerPhase.RUNNING) {
                    // Defensive: a manual step should already have moved us
                    // to AWAITING_MANUAL_ADVANCE in beginCurrentStep.
                    val duration = currentStep?.duration ?: return
                    val left = duration.toDouble() - elapsedInStep
                    if (remaining < left) {
                        elapsedInStep += remaining
                        totalElapsed += remaining
                        totalWallElapsed += remaining
                        remaining = 0.0
                    } else {
                        elapsedInStep = duration.toDouble()
                        totalElapsed += left
                        totalWallElapsed += left
                        remaining -= left
                        reachTargetOfCurrentStep()
                    }
                }
                // A coarse tick can carry time past the point where we
                // stopped making plan progress (we hit a soft target, or
                // landed on a manual step). That leftover is still real
                // time the user spent brewing.
                if (remaining > 0 &&
                    (phase == BrewTimerPhase.OVERRUNNING || phase == BrewTimerPhase.AWAITING_MANUAL_ADVANCE)
                ) {
                    elapsedInStep += remaining
                    totalWallElapsed += remaining
                }
            }
        }
    }

    /**
     * Complete the current step immediately — the user's "next" tap on a
     * manual or overrunning step, or a deliberate skip of a timed step.
     */
    fun advanceStep() {
        if (!isActive || currentStep == null) return
        phase = BrewTimerPhase.RUNNING // so the shared transition path runs uniformly
        completeCurrentStepAndAdvance()
    }

    /**
     * Pause any live phase. Overruns and manual holds are pausable too —
     * the master clock is running during both, and stopping it is the
     * whole point.
     */
    fun pause() {
        if (!isActive) return
        phaseBeforePause = phase
        phase = BrewTimerPhase.PAUSED
    }

    fun resume() {
        if (phase != BrewTimerPhase.PAUSED) return
        phase = phaseBeforePause ?: BrewTimerPhase.RUNNING
        phaseBeforePause = null
    }

    /**
     * End the brew wherever it is — the "Done" button. Unlike
     * [advanceStep] this does not walk the remaining steps: the user has
     * decided the coffee is made, so it finishes immediately and keeps the
     * elapsed time they took. No-op when there is nothing to finish.
     */
    fun finish() {
        if (phase == BrewTimerPhase.IDLE || phase == BrewTimerPhase.COMPLETED) return
        phaseBeforePause = null
        phase = BrewTimerPhase.COMPLETED
        onEvent?.invoke(BrewTimerEvent.Completed)
    }

    /** Return to the un-started state so the same timeline can be run again. */
    fun reset() {
        phase = BrewTimerPhase.IDLE
        currentStepIndex = 0
        elapsedInStep = 0.0
        totalElapsed = 0.0
        totalWallElapsed = 0.0
        phaseBeforePause = null
    }

    // MARK: Transitions

    private fun beginCurrentStep() {
        val step = currentStep ?: return
        onEvent?.invoke(BrewTimerEvent.StepBegan(currentStepIndex, step))
        if (step.requiresManualAdvance) {
            phase = BrewTimerPhase.AWAITING_MANUAL_ADVANCE
            onEvent?.invoke(BrewTimerEvent.AwaitingManualAdvance(currentStepIndex, step))
        }
    }

    /**
     * Called the instant a timed step's duration is used up.
     *
     * Intermediate steps flow straight into the next one, exactly as in
     * 1.0 — a guided brew shouldn't need a tap between every pour. The
     * **final** step instead holds and counts up, because there is nothing
     * to flow into and ending the brew is the user's call, not the clock's.
     */
    private fun reachTargetOfCurrentStep() {
        val step = currentStep ?: return
        if (isOnFinalStep) {
            phase = BrewTimerPhase.OVERRUNNING
            onEvent?.invoke(BrewTimerEvent.ReachedTarget(currentStepIndex, step))
        } else {
            completeCurrentStepAndAdvance()
        }
    }

    private fun completeCurrentStepAndAdvance() {
        val finished = currentStep ?: return
        onEvent?.invoke(BrewTimerEvent.StepCompleted(currentStepIndex, finished))

        val nextIndex = currentStepIndex + 1
        if (nextIndex >= steps.size) {
            phase = BrewTimerPhase.COMPLETED
            onEvent?.invoke(BrewTimerEvent.Completed)
            return
        }
        currentStepIndex = nextIndex
        elapsedInStep = 0.0
        beginCurrentStep()
    }
}
