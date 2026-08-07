package com.jrlabapps.coffeegrams.core

/**
 * A single instruction in a guided brew timeline.
 *
 * Design note: the spec sketched steps using a mix of start-second (for
 * pours) and duration-second (for bloom/steep). We deliberately unify on
 * **duration** here — each step knows only how long *it* lasts, and the
 * absolute start time is derived by prefix-summing the sequence (see
 * [BrewTimeline]). A single source of truth for timing means the timer
 * engine never has to reconcile two representations, and reordering /
 * inserting a step can't desync a hardcoded start time.
 *
 * `targetCumulativeGrams` is kept on [Pour] because it is *not* derivable
 * from timing — it tells the user the scale reading to pour up to.
 */
sealed class BrewStep {

    /**
     * Wet the grounds and let them de-gas. [targetGrams] is the scale
     * reading to reach; [durationSeconds] is how long to let the bloom rest.
     */
    data class Bloom(val targetGrams: Double, val durationSeconds: Int) : BrewStep()

    /**
     * A pour (or single fill). [pourNumber] is 1-based.
     * [targetCumulativeGrams] is the *total* scale reading to reach by the
     * end of this pour, not the amount added.
     */
    data class Pour(
        val pourNumber: Int,
        val targetCumulativeGrams: Double,
        val durationSeconds: Int,
    ) : BrewStep()

    /** Grounds sit submerged (immersion methods). */
    data class Steep(val durationSeconds: Int) : BrewStep()

    /** Stir or swirl to knock down floating grounds. */
    data class Stir(val durationSeconds: Int) : BrewStep()

    /** A passive pause (e.g. let grounds settle before plunging). */
    data class Wait(val durationSeconds: Int) : BrewStep()

    /**
     * Press the plunger (French Press / AeroPress). A manual action — the
     * timer waits for the user rather than auto-advancing.
     */
    data object Plunge : BrewStep()

    /**
     * Let the bed drain. When [untilDripsStop] is true this is
     * user-advanced (there is no fixed time); false would make it a timed
     * step.
     */
    data class Drawdown(val untilDripsStop: Boolean) : BrewStep()

    /**
     * How long this step runs, in seconds. `null` means the step has no
     * fixed duration and the user must advance it manually (plunge,
     * drawdown-until-drips-stop). The timer engine keys its behaviour off
     * this.
     */
    val duration: Int?
        get() = when (this) {
            is Bloom -> durationSeconds
            is Pour -> durationSeconds
            is Steep -> durationSeconds
            is Stir -> durationSeconds
            is Wait -> durationSeconds
            is Plunge -> null
            // 0, not a real timed duration, matches iOS exactly. No shipping
            // timeline on either platform ever constructs Drawdown(false) —
            // every builder only produces the manual (untilDripsStop = true)
            // case — so this branch is reachable in the type but dead in
            // practice on both platforms.
            is Drawdown -> if (untilDripsStop) null else 0
        }

    /** True when the step has no fixed duration and the user taps to continue. */
    val requiresManualAdvance: Boolean get() = duration == null

    /**
     * Short imperative title for the timer UI. Kept in `:core` (it is pure
     * text, no UI dependency) so it can be unit-tested and localized
     * centrally.
     */
    val title: String
        get() = when (this) {
            is Bloom -> "Bloom"
            is Pour -> "Pour $pourNumber"
            is Steep -> "Steep"
            is Stir -> "Stir"
            is Wait -> "Wait"
            is Plunge -> "Plunge"
            is Drawdown -> "Drawdown"
        }
}
