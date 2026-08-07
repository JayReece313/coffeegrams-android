package com.jrlabapps.coffeegrams.core

/**
 * The core brewing math (spec §4.1), as pure, side-effect-free functions.
 *
 * This is the single most test-worthy part of the app: every number here is
 * a promise to the user standing at their scale. It is intentionally an
 * object of top-level functions with no stored state — nothing to mock,
 * trivial to test, and callable from anywhere without wiring up an
 * instance.
 */
object BrewCalculator {

    // MARK: Dose-first (enter coffee grams, get water)

    /**
     * Water needed for a given dose and ratio. `ratio` of 16 means 1:16, so
     * 16 g of coffee → 256 g of water.
     *
     * A negative dose is treated as zero (the UI can momentarily hold an
     * invalid value while the user edits a field); we never return a
     * negative weight.
     */
    fun waterGrams(doseGrams: Double, ratio: Double): Double {
        // Written as a negated `>` guard, not `<= 0`, to match iOS's
        // `guard doseGrams > 0, ratio > 0 else { return 0 }` exactly: under
        // IEEE 754, `NaN > 0` is false (so the guard fails and returns 0),
        // but `NaN <= 0` is *also* false, so a `<= 0` check would let NaN
        // fall through to the multiply below instead of being caught here.
        if (!(doseGrams > 0 && ratio > 0)) return 0.0
        return doseGrams * ratio
    }

    // MARK: Yield-first (enter desired finished coffee, get dose)

    /**
     * Coffee dose required to hit a target *finished* yield.
     *
     * Known v1 simplification (documented in the spec): immersion methods
     * (French Press, AeroPress) lose ~2 g of water per gram of coffee to
     * grounds absorption, so the true dose is marginally higher than
     * `targetYield / ratio`. At the precision a home brewer works to, that
     * correction is within scale noise, and modelling it convincingly needs
     * testing against real output — so it is deliberately kept out of v1 and
     * every method uses the simple relationship. The `when` is retained
     * (rather than a one-liner) precisely so the absorption seam is visible
     * and easy to refine in v2 without touching call sites.
     *
     * Espresso is included for completeness — yield-first there is just
     * `yield / ratio` = dose — though the espresso UI is driven dose-first.
     */
    fun doseGrams(targetYieldGrams: Double, ratio: Double, method: BrewMethod): Double {
        // Same NaN-safe negated-`>` guard as waterGrams — see its comment.
        if (!(targetYieldGrams > 0 && ratio > 0)) return 0.0

        return when (method) {
            BrewMethod.V60, BrewMethod.CHEMEX, BrewMethod.COLD_BREW, BrewMethod.ESPRESSO ->
                // Pour-through / pressure: yield ≈ water, so dose = yield / ratio.
                targetYieldGrams / ratio

            BrewMethod.FRENCH_PRESS, BrewMethod.AEROPRESS ->
                // Immersion: absorption offset deferred to v2 (see doc comment).
                targetYieldGrams / ratio
        }
    }

    // MARK: Ratio clamping

    /**
     * Clamp an arbitrary ratio into the method's allowed slider range. The
     * UI slider is already bounded, but clamping here means every code path
     * that consumes a ratio is protected — belt and braces for a number the
     * whole calculation hinges on.
     */
    fun clampRatio(ratio: Double, method: BrewMethod): Double {
        val range = BrewMethodProfile.profile(method).ratioRange
        return ratio.coerceIn(range)
    }
}
