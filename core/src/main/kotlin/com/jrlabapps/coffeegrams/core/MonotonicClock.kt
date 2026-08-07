package com.jrlabapps.coffeegrams.core

/**
 * A source of monotonic time, in seconds. Only *differences* between
 * readings are meaningful (it is not wall-clock time).
 *
 * This is a "port" — an abstraction `:core` owns but does not implement.
 * The app provides a live adapter backed by `SystemClock.elapsedRealtime()`,
 * and it drives [BrewTimerEngine.advance] with the delta between ticks. The
 * engine itself never reads the clock, which is why its tests need no clock
 * at all: they call `advance` with exact deltas. Keeping the time source
 * behind an interface is what makes the whole timer deterministically
 * testable.
 */
interface MonotonicClock {
    val now: Double
}
