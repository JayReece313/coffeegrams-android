package com.jrlabapps.coffeegrams.core

/**
 * Marker for the pure-logic module.
 *
 * The real contents arrive in M2, when the 12 CoffeeGramsCore Swift sources are
 * transliterated here: BrewMethod, BrewMethodProfile, BrewStep, BrewTimeline +
 * BrewTimelineBuilder, BrewCalculator, BrewTimerEngine, EspressoTarget, ColdBrew,
 * BrewLogEntry and the MonotonicClock port.
 *
 * Nothing in this module may import from `android.*` or `androidx.*`.
 */
object CoffeeGramsCore {
    /** Matches the iOS app version this port targets parity with. */
    const val PARITY_TARGET: String = "iOS 1.1"
}
