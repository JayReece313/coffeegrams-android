package com.jrlabapps.coffeegrams.core

/**
 * The target for an espresso shot. Espresso is deliberately shallow in v1
 * (spec §4.6): four numbers only — dose in, ratio, target yield out, and a
 * shot-time window. No dial-in, extraction %, or TDS tooling.
 */
data class EspressoTarget(
    val doseGrams: Double,
    val ratio: Double,
    val targetYieldGrams: Double,
    val shotTimeRange: IntRange,
)

/**
 * How the elapsed shot time compares to the target window — drives the
 * green/amber/red timer state in the UI. Kept in `:core` so the thresholds
 * are testable rather than buried in a view.
 */
enum class ShotTimingState {
    /** Before the window opens — shot is still young. */
    TOO_EARLY,

    /** Inside the target window (green). */
    ON_TARGET,

    /** Past the window — pull is running long (red). */
    TOO_LATE;

    companion object {
        /** Classify an elapsed shot time against a target window. */
        fun classify(elapsedSeconds: Int, window: IntRange): ShotTimingState = when {
            elapsedSeconds < window.first -> TOO_EARLY
            elapsedSeconds > window.last -> TOO_LATE
            else -> ON_TARGET
        }
    }
}
