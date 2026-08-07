package com.jrlabapps.coffeegrams.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M3 gate: timeline generation (spec §4.2–4.6). Asserts step shape, water
 * targets, bloom amounts, cadence, the espresso window, and the cold-brew
 * notify time.
 *
 * Ported case-for-case from the iOS BrewTimelineBuilderTests.swift
 * conformance suite (11 cases).
 */
class BrewTimelineBuilderTest {

    private val epsilon = 1e-9

    // Small helpers to read a step's associated values in assertions.
    private fun cumulative(step: BrewStep): Double? = (step as? BrewStep.Pour)?.targetCumulativeGrams
    private fun bloomTarget(step: BrewStep): Double? = (step as? BrewStep.Bloom)?.targetGrams

    // MARK: V60 / Chemex

    @Test
    fun `V60 timeline bloom + 2 pours + drawdown, cumulative lands on total`() {
        val dose = 20.0
        val ratio = 16.0
        val t = BrewTimelineBuilder.buildPulsePourTimeline(BrewMethodProfile.v60, dose, ratio)
        assertEquals(BrewMethod.V60, t.method)
        assertEquals(320.0, t.totalWaterGrams)
        assertEquals(4, t.steps.size) // bloom + 2 pours + drawdown

        assertEquals(dose * 2.25, bloomTarget(t.steps[0])) // 45 g

        // Pours are evenly sized and the last lands exactly on total water.
        val pourTargets = t.steps.mapNotNull(::cumulative)
        assertEquals(2, pourTargets.size)
        assertTrue(kotlin.math.abs(pourTargets[0] - 182.5) < epsilon)
        assertTrue(kotlin.math.abs(pourTargets[1] - 320.0) < epsilon)

        // Last step is a user-ended drawdown.
        assertEquals(BrewStep.Drawdown(untilDripsStop = true), t.steps.last())
        assertTrue(t.steps.last().requiresManualAdvance)
    }

    @Test
    fun `V60 pour cadence is 45s Chemex is spaced wider at 60s`() {
        val v60 = BrewTimelineBuilder.buildPulsePourTimeline(BrewMethodProfile.v60, 18.0, 16.0)
        val chemex = BrewTimelineBuilder.buildPulsePourTimeline(BrewMethodProfile.chemex, 18.0, 16.0)
        // Every pour uses the profile's interval as its duration.
        for (step in v60.steps) if (step is BrewStep.Pour) assertEquals(45, step.duration)
        for (step in chemex.steps) if (step is BrewStep.Pour) assertEquals(60, step.duration)
        assertEquals(18.0 * 2.5, bloomTarget(chemex.steps[0]))
    }

    // MARK: French Press

    @Test
    fun `French Press bloom, fill, 4 00 steep, plunge`() {
        val dose = 30.0
        val ratio = 15.0
        val t = BrewTimelineBuilder.buildFrenchPressTimeline(dose, ratio)
        assertEquals(450.0, t.totalWaterGrams)
        assertEquals(4, t.steps.size)
        assertEquals(dose * 2.0, bloomTarget(t.steps[0])) // 60 g
        assertEquals(450.0, cumulative(t.steps[1])) // fill to full
        assertEquals(BrewStep.Steep(durationSeconds = 240), t.steps[2]) // 4:00
        assertEquals(BrewStep.Plunge, t.steps[3])
        assertTrue(t.steps[3].requiresManualAdvance)
    }

    // MARK: AeroPress

    @Test
    fun `AeroPress pour, 2 00 steep, stir, settle, plunge, no bloom`() {
        val t = BrewTimelineBuilder.buildAeroPressTimeline(11.0, 18.0)
        assertTrue(kotlin.math.abs(t.totalWaterGrams - 198.0) < epsilon)
        assertEquals(5, t.steps.size)
        assertFalse(t.steps.any { it is BrewStep.Bloom })
        assertEquals(BrewStep.Steep(durationSeconds = 120), t.steps[1])
        assertEquals(BrewStep.Stir(durationSeconds = 10), t.steps[2])
        assertEquals(BrewStep.Wait(durationSeconds = 30), t.steps[3])
        assertEquals(BrewStep.Plunge, t.steps[4])
    }

    // MARK: Cold Brew

    @Test
    fun `Cold brew concentrate = 1 5, ready-to-drink = 1 8`() {
        val start = Instant.ofEpochSecond(1_000_000)
        val conc = BrewTimelineBuilder.buildColdBrewPlan(doseGrams = 100.0, style = ColdBrewStyle.CONCENTRATE, now = start)
        val rtd = BrewTimelineBuilder.buildColdBrewPlan(doseGrams = 100.0, style = ColdBrewStyle.READY_TO_DRINK, now = start)
        assertEquals(500.0, conc.waterGrams)
        assertEquals(800.0, rtd.waterGrams)
    }

    @Test
    fun `Cold brew notify time = start plus steepHours, hours clamped to 12-24`() {
        val start = Instant.EPOCH
        val plan = BrewTimelineBuilder.buildColdBrewPlan(
            doseGrams = 100.0,
            style = ColdBrewStyle.CONCENTRATE,
            steepHours = 16.0,
            now = start,
        )
        assertEquals(16.0, plan.steepHours)
        assertEquals(start.plusSeconds(16 * 3600), plan.notifyAt)

        // Out-of-band requests are clamped, not honoured verbatim.
        val tooShort = BrewTimelineBuilder.buildColdBrewPlan(
            doseGrams = 100.0,
            style = ColdBrewStyle.CONCENTRATE,
            steepHours = 2.0,
            now = start,
        )
        assertEquals(12.0, tooShort.steepHours)
        val tooLong = BrewTimelineBuilder.buildColdBrewPlan(
            doseGrams = 100.0,
            style = ColdBrewStyle.CONCENTRATE,
            steepHours = 48.0,
            now = start,
        )
        assertEquals(24.0, tooLong.steepHours)
    }

    // MARK: Espresso

    @Test
    fun `Espresso 18 g at 1 2 to 36 g yield, 25-30s window`() {
        val target = BrewTimelineBuilder.buildEspressoTarget(doseGrams = 18.0, ratio = 2.0)
        assertEquals(18.0, target.doseGrams)
        assertEquals(2.0, target.ratio)
        assertEquals(36.0, target.targetYieldGrams)
        assertEquals(25..30, target.shotTimeRange)
    }

    @Test
    fun `Espresso ratio is clamped into the 1-1 to 1-3 band`() {
        assertEquals(3.0, BrewTimelineBuilder.buildEspressoTarget(doseGrams = 18.0, ratio = 5.0).ratio)
        assertEquals(1.0, BrewTimelineBuilder.buildEspressoTarget(doseGrams = 18.0, ratio = 0.2).ratio)
    }

    @Test
    fun `Shot timing state too early, on target, too late`() {
        val window = 25..30
        assertEquals(ShotTimingState.TOO_EARLY, ShotTimingState.classify(20, window))
        assertEquals(ShotTimingState.ON_TARGET, ShotTimingState.classify(25, window))
        assertEquals(ShotTimingState.ON_TARGET, ShotTimingState.classify(30, window))
        assertEquals(ShotTimingState.TOO_LATE, ShotTimingState.classify(31, window))
    }

    // MARK: Dispatcher

    @Test
    fun `timeline(for) routes pour methods and returns null for cold brew, espresso`() {
        assertEquals(BrewMethod.V60, BrewTimelineBuilder.timeline(BrewMethod.V60, 18.0, 16.0)?.method)
        assertEquals(BrewMethod.CHEMEX, BrewTimelineBuilder.timeline(BrewMethod.CHEMEX, 18.0, 16.0)?.method)
        assertEquals(
            BrewMethod.FRENCH_PRESS,
            BrewTimelineBuilder.timeline(BrewMethod.FRENCH_PRESS, 18.0, 15.0)?.method,
        )
        assertEquals(BrewMethod.AEROPRESS, BrewTimelineBuilder.timeline(BrewMethod.AEROPRESS, 18.0, 18.0)?.method)
        assertEquals(null, BrewTimelineBuilder.timeline(BrewMethod.COLD_BREW, 100.0, 5.0))
        assertEquals(null, BrewTimelineBuilder.timeline(BrewMethod.ESPRESSO, 18.0, 2.0))
    }

    // MARK: Derived timing

    @Test
    fun `stepStartTimes prefix-sums fixed durations`() {
        // French Press: bloom 30, fill 15, steep 240, plunge(manual)
        val t = BrewTimelineBuilder.buildFrenchPressTimeline(30.0, 15.0)
        assertEquals(listOf(0, 30, 45, 285), t.stepStartTimes)
        assertEquals(285, t.totalFixedDuration) // plunge adds nothing
    }
}
