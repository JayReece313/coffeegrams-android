package com.jrlabapps.coffeegrams.core

/**
 * An ordered set of guided-brew steps for one brew, plus the totals a UI
 * header wants to show. Produced by [BrewTimelineBuilder]; consumed by
 * [BrewTimerEngine] and the guided-brew screen.
 *
 * Cold Brew and Espresso do **not** produce a [BrewTimeline] — cold brew is
 * a long unattended steep ([ColdBrewPlan]) and espresso is a single timed
 * pull ([EspressoTarget]). Only the four "pour water over/through grounds"
 * methods have a step-by-step timeline.
 */
data class BrewTimeline(
    val method: BrewMethod,
    val steps: List<BrewStep>,
    /** Final water weight the brew reaches (dose × ratio). */
    val totalWaterGrams: Double,
) {
    /**
     * Sum of the fixed-duration steps, in seconds. Manual steps (plunge,
     * drawdown-until-drips) contribute nothing because they have no set
     * length. Useful for an "≈ 3:15" estimate in the UI header.
     */
    val totalFixedDuration: Int
        get() = steps.sumOf { it.duration ?: 0 }

    /**
     * The absolute second each step *starts*, derived by prefix-summing
     * durations. A manual step (null duration) holds the clock until the
     * user advances, so every following step's start is measured from that
     * release point; here the running clock simply carries forward by the
     * step's fixed duration (0 for manual steps). The engine layers real
     * manual-advance behaviour on top of this.
     */
    val stepStartTimes: List<Int>
        get() {
            val starts = mutableListOf<Int>()
            var clock = 0
            for (step in steps) {
                starts.add(clock)
                clock += step.duration ?: 0
            }
            return starts
        }
}
