package com.jrlabapps.coffeegrams.core

import java.time.Instant

/**
 * Generates the guided timeline (or plan/target) for a brew from a dose and
 * ratio, following spec §4.2–4.6. Pure and deterministic — the cold-brew
 * builder takes the start time as a parameter rather than reading the
 * clock, so its notification time is testable.
 */
object BrewTimelineBuilder {

    // MARK: Public entry point

    /**
     * The step timeline for the four pour-based methods. Returns `null` for
     * Cold Brew and Espresso, which have their own dedicated builders
     * ([buildColdBrewPlan], [buildEspressoTarget]) because they are not
     * step-by-step pours.
     */
    fun timeline(method: BrewMethod, doseGrams: Double, ratio: Double): BrewTimeline? =
        when (method) {
            BrewMethod.V60, BrewMethod.CHEMEX ->
                buildPulsePourTimeline(BrewMethodProfile.profile(method), doseGrams, ratio)
            BrewMethod.FRENCH_PRESS -> buildFrenchPressTimeline(doseGrams, ratio)
            BrewMethod.AEROPRESS -> buildAeroPressTimeline(doseGrams, ratio)
            BrewMethod.COLD_BREW, BrewMethod.ESPRESSO -> null
        }

    // MARK: V60 / Chemex — pulse pour (spec §4.2)

    /**
     * Bloom, then N evenly-sized pours spaced `pourIntervalSeconds` apart,
     * then a drawdown the user ends when the drips stop. Each pour's
     * `targetCumulativeGrams` is the running scale reading, and the final
     * pour lands exactly on `dose × ratio`.
     */
    fun buildPulsePourTimeline(profile: BrewMethodProfile, doseGrams: Double, ratio: Double): BrewTimeline {
        val totalWater = BrewCalculator.waterGrams(doseGrams, ratio)

        // Fall back to sane defaults if a profile ever omits these; the
        // pulse-pour profiles (V60/Chemex) always supply them.
        val bloomMultiplier = profile.bloomMultiplier ?: 2.25
        val bloomSeconds = profile.bloomSeconds ?: 45
        val numPours = maxOf(1, profile.numPours ?: 2)
        val pourInterval = profile.pourIntervalSeconds ?: 45

        // doseGrams/ratio are used raw here, matching iOS exactly — this
        // builder never clamps them (only buildEspressoTarget does).
        // Bounding to sane ranges is the caller's job (UI slider bounds
        // before :core is ever reached), same contract on both platforms.
        val bloomWater = doseGrams * bloomMultiplier
        val remainingWater = maxOf(0.0, totalWater - bloomWater)
        val pourAmount = remainingWater / numPours

        val steps = mutableListOf<BrewStep>()
        steps.add(BrewStep.Bloom(targetGrams = bloomWater, durationSeconds = bloomSeconds))

        var cumulative = bloomWater
        for (pourNumber in 1..numPours) {
            cumulative += pourAmount
            steps.add(
                BrewStep.Pour(
                    pourNumber = pourNumber,
                    targetCumulativeGrams = cumulative,
                    durationSeconds = pourInterval,
                ),
            )
        }

        steps.add(BrewStep.Drawdown(untilDripsStop = true))
        return BrewTimeline(method = profile.method, steps = steps, totalWaterGrams = totalWater)
    }

    // MARK: French Press — bloom + fill + steep + plunge (spec §4.3)

    /**
     * Optional 2× bloom, one fill pour to the full weight, a 4:00 steep
     * that starts once the pour is complete, then plunge. The fill pour is
     * given a short nominal duration so the guided timer shows a "pour to
     * full" beat before the steep clock begins.
     */
    fun buildFrenchPressTimeline(doseGrams: Double, ratio: Double): BrewTimeline {
        val profile = BrewMethodProfile.frenchPress
        val totalWater = BrewCalculator.waterGrams(doseGrams, ratio)
        val bloomWater = doseGrams * (profile.bloomMultiplier ?: 2.0)
        val bloomSeconds = profile.bloomSeconds ?: 30
        val steepSeconds = profile.steepSeconds ?: 240

        val steps = listOf(
            BrewStep.Bloom(targetGrams = bloomWater, durationSeconds = bloomSeconds),
            BrewStep.Pour(pourNumber = 1, targetCumulativeGrams = totalWater, durationSeconds = fillPourSeconds),
            BrewStep.Steep(durationSeconds = steepSeconds),
            BrewStep.Plunge,
        )
        return BrewTimeline(method = BrewMethod.FRENCH_PRESS, steps = steps, totalWaterGrams = totalWater)
    }

    // MARK: AeroPress — pour, steep, stir, settle, press (spec §4.4)

    /**
     * Pour all the water at once, steep 2:00, stir/swirl, let the grounds
     * settle, then plunge. Ratio doubles as a strength slider (handled by
     * the caller); this builder just lays out the fixed sequence.
     */
    fun buildAeroPressTimeline(doseGrams: Double, ratio: Double): BrewTimeline {
        val profile = BrewMethodProfile.aeropress
        val totalWater = BrewCalculator.waterGrams(doseGrams, ratio)
        val steepSeconds = profile.steepSeconds ?: 120

        val steps = listOf(
            BrewStep.Pour(pourNumber = 1, targetCumulativeGrams = totalWater, durationSeconds = fillPourSeconds),
            BrewStep.Steep(durationSeconds = steepSeconds),
            BrewStep.Stir(durationSeconds = 10),
            BrewStep.Wait(durationSeconds = 30),
            BrewStep.Plunge,
        )
        return BrewTimeline(method = BrewMethod.AEROPRESS, steps = steps, totalWaterGrams = totalWater)
    }

    // MARK: Cold Brew — no live timer, a scheduled notification (spec §4.5)

    /**
     * A dose/water amount plus the wall-clock time the steep finishes.
     * [now] is injected so the notify time is deterministic in tests. Steep
     * hours are clamped to the sensible 12–24 h band.
     */
    fun buildColdBrewPlan(
        doseGrams: Double,
        style: ColdBrewStyle,
        steepHours: Double = 16.0,
        now: Instant,
    ): ColdBrewPlan {
        val clampedHours = steepHours.coerceIn(12.0, 24.0)
        val waterGrams = BrewCalculator.waterGrams(doseGrams, style.ratio)
        return ColdBrewPlan(
            doseGrams = doseGrams,
            waterGrams = waterGrams,
            style = style,
            steepHours = clampedHours,
            notifyAt = now.plusMillis((clampedHours * 3600.0 * 1000.0).toLong()),
        )
    }

    // MARK: Espresso — dose, ratio, target yield, shot window (spec §4.6)

    /**
     * Four numbers only. Ratio is clamped to the espresso band (1:1–1:3)
     * and the shot window comes from the profile (25–30 s).
     */
    fun buildEspressoTarget(doseGrams: Double, ratio: Double): EspressoTarget {
        val clampedRatio = BrewCalculator.clampRatio(ratio, BrewMethod.ESPRESSO)
        val yield = BrewCalculator.waterGrams(doseGrams, clampedRatio)
        val window = BrewMethodProfile.espresso.shotTimeRangeSeconds ?: (25..30)
        return EspressoTarget(
            doseGrams = doseGrams,
            ratio = clampedRatio,
            targetYieldGrams = yield,
            shotTimeRange = window,
        )
    }

    // MARK: Tuning constants

    /**
     * Nominal seconds allotted to a single "fill" pour (French Press /
     * AeroPress). Not a brewing constant from the spec — it's a UI pacing
     * choice so the timer shows a distinct pour beat before the steep
     * clock, kept here as one named value rather than a magic number in two
     * builders.
     */
    private const val fillPourSeconds = 15
}
