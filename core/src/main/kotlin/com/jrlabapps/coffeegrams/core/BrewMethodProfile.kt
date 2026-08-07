package com.jrlabapps.coffeegrams.core

/**
 * The static, hardcoded brewing parameters for one method — the encoded
 * form of the reference table in spec §3.
 *
 * These are **defaults**, not gospel: the UI lets users drag the ratio
 * within [ratioRange], because taste and bean freshness shift the "right"
 * number. Everything a timeline generator needs to lay out a brew lives
 * here, so the generators stay free of magic numbers.
 */
data class BrewMethodProfile(
    val method: BrewMethod,
    val brewType: BrewType,
    /** Default water:coffee ratio (for espresso, yield:dose). `16.0` means 1:16. */
    val defaultRatio: Double,
    /** The inclusive range the ratio slider allows. */
    val ratioRange: ClosedRange<Double>,
    /**
     * Bloom water as a multiple of dose grams (`multiplier * dose` = bloom
     * water). `null` for methods with no bloom.
     */
    val bloomMultiplier: Double? = null,
    /** How long the bloom rests, in seconds. `null` if there is no bloom. */
    val bloomSeconds: Int? = null,
    /**
     * Steep time for immersion methods, in seconds. `null` for pour-over and
     * for cold brew (whose steep is measured in hours — see [ColdBrewPlan]).
     */
    val steepSeconds: Int? = null,
    /** Number of pours for pulse-pour methods. `null` otherwise. */
    val numPours: Int? = null,
    /** Seconds between the start of consecutive pours (pulse-pour only). */
    val pourIntervalSeconds: Int? = null,
    /** Target shot-time window for espresso. `null` for every other method. */
    val shotTimeRangeSeconds: IntRange? = null,
) {
    companion object {

        // MARK: The reference table (spec §3)

        /** V60 — pulse pour. Bloom 2.25×, 45s; 2 pours, 45s apart. */
        val v60 = BrewMethodProfile(
            method = BrewMethod.V60,
            brewType = BrewType.PULSE_POUR,
            defaultRatio = 16.0,
            ratioRange = 15.0..17.0,
            bloomMultiplier = 2.25,
            bloomSeconds = 45,
            numPours = 2,
            pourIntervalSeconds = 45,
        )

        /**
         * Chemex — pulse pour. Thicker filter drains slower, so pours are
         * spaced further apart (60s) than the V60.
         */
        val chemex = BrewMethodProfile(
            method = BrewMethod.CHEMEX,
            brewType = BrewType.PULSE_POUR,
            defaultRatio = 16.0,
            ratioRange = 15.0..17.0,
            bloomMultiplier = 2.5,
            bloomSeconds = 45,
            numPours = 2,
            pourIntervalSeconds = 60,
        )

        /**
         * French Press — immersion. Optional 2× bloom (30s), then fill and
         * steep 4:00 before plunging.
         */
        val frenchPress = BrewMethodProfile(
            method = BrewMethod.FRENCH_PRESS,
            brewType = BrewType.IMMERSION,
            defaultRatio = 15.0,
            ratioRange = 12.0..17.0,
            bloomMultiplier = 2.0,
            bloomSeconds = 30,
            steepSeconds = 240,
        )

        /**
         * AeroPress — immersion + press. Ratio is a strength slider (1:12
         * strong → 1:18 clean), defaulting to Hoffmann's 1:18. No bloom.
         */
        val aeropress = BrewMethodProfile(
            method = BrewMethod.AEROPRESS,
            brewType = BrewType.IMMERSION,
            defaultRatio = 18.0,
            ratioRange = 12.0..18.0,
            steepSeconds = 120,
        )

        /**
         * Cold Brew — immersion, no heat, no live timer. [defaultRatio] is
         * the concentrate ratio (1:5); the ready-to-drink style (1:8) is
         * selected via [ColdBrewStyle]. The range spans both styles so the
         * slider can reach either. Steep is measured in hours, so
         * [steepSeconds] stays null and the long steep is modelled by
         * [ColdBrewPlan].
         */
        val coldBrew = BrewMethodProfile(
            method = BrewMethod.COLD_BREW,
            brewType = BrewType.IMMERSION,
            defaultRatio = 5.0,
            ratioRange = 4.0..8.0,
        )

        /**
         * Espresso — pressure. Ratio is yield:dose (1:1 ristretto → 1:3
         * lungo), default 1:2 "normale". Target shot window 25–30s.
         */
        val espresso = BrewMethodProfile(
            method = BrewMethod.ESPRESSO,
            brewType = BrewType.PRESSURE,
            defaultRatio = 2.0,
            ratioRange = 1.0..3.0,
            shotTimeRangeSeconds = 25..30,
        )

        /** All profiles, in the order they appear in the picker. */
        val all: List<BrewMethodProfile> = listOf(v60, chemex, frenchPress, aeropress, coldBrew, espresso)

        /**
         * The profile for a given method. Total by construction — [all]
         * covers every [BrewMethod] entry (asserted in tests), so this never
         * throws.
         */
        fun profile(method: BrewMethod): BrewMethodProfile = all.first { it.method == method }
    }
}
