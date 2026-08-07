package com.jrlabapps.coffeegrams.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M2 gate: the brewing math. Every assertion here is a number the user
 * will read off the app while standing at their scale, so the coverage is
 * deliberately exhaustive — canonical examples, every method's default,
 * both directions, boundaries, and the invalid-input guards.
 *
 * Ported case-for-case from the iOS BrewCalculatorTests.swift conformance
 * suite (11 cases).
 */
class BrewCalculatorTest {

    private val epsilon = 1e-9

    // MARK: Dose-first

    @Test
    fun `canonical example 16 g at 1 16 = 256 g water`() {
        assertEquals(256.0, BrewCalculator.waterGrams(doseGrams = 16.0, ratio = 16.0))
    }

    @Test
    fun `water = dose times ratio across a spread of values`() {
        assertEquals(300.0, BrewCalculator.waterGrams(doseGrams = 20.0, ratio = 15.0))
        assertEquals(198.0, BrewCalculator.waterGrams(doseGrams = 11.0, ratio = 18.0)) // Hoffmann 11:200-ish
        assertEquals(36.0, BrewCalculator.waterGrams(doseGrams = 18.0, ratio = 2.0)) // espresso 18→36
        assertEquals(500.0, BrewCalculator.waterGrams(doseGrams = 100.0, ratio = 5.0)) // cold brew concentrate
    }

    @Test
    fun `water for each method at its default ratio`() {
        val dose = 18.0
        for (profile in BrewMethodProfile.all) {
            val expected = dose * profile.defaultRatio
            val actual = BrewCalculator.waterGrams(doseGrams = dose, ratio = profile.defaultRatio)
            assertTrue(kotlin.math.abs(actual - expected) < epsilon)
        }
    }

    // MARK: Yield-first

    @Test
    fun `yield-first inverts dose-first 256 g yield at 1 16 = 16 g dose`() {
        val dose = BrewCalculator.doseGrams(targetYieldGrams = 256.0, ratio = 16.0, method = BrewMethod.V60)
        assertTrue(kotlin.math.abs(dose - 16.0) < epsilon)
    }

    @Test
    fun `round-trip dose to water to dose is stable`() {
        val startDose = 17.0
        for (method in BrewMethod.entries) {
            val ratio = BrewMethodProfile.profile(method).defaultRatio
            val water = BrewCalculator.waterGrams(doseGrams = startDose, ratio = ratio)
            val backToDose = BrewCalculator.doseGrams(targetYieldGrams = water, ratio = ratio, method = method)
            // v1 uses the simple relationship for every method, so the round
            // trip is exact. When absorption modelling lands in v2,
            // immersion methods will intentionally diverge and this test
            // should be updated.
            assertTrue(kotlin.math.abs(backToDose - startDose) < epsilon)
        }
    }

    @Test
    fun `yield-first for each method equals yield over ratio (v1 simplification)`() {
        val ratio = 16.0
        val yield = 480.0
        for (method in BrewMethod.entries) {
            val dose = BrewCalculator.doseGrams(targetYieldGrams = yield, ratio = ratio, method = method)
            assertTrue(kotlin.math.abs(dose - (yield / ratio)) < epsilon)
        }
    }

    // MARK: Invalid input guards

    @Test
    fun `zero and negative dose yield zero water, never negative`() {
        assertEquals(0.0, BrewCalculator.waterGrams(doseGrams = 0.0, ratio = 16.0))
        assertEquals(0.0, BrewCalculator.waterGrams(doseGrams = -5.0, ratio = 16.0))
    }

    @Test
    fun `non-positive ratio is guarded (no divide-by-zero or NaN)`() {
        assertEquals(0.0, BrewCalculator.waterGrams(doseGrams = 18.0, ratio = 0.0))
        assertEquals(0.0, BrewCalculator.doseGrams(targetYieldGrams = 300.0, ratio = 0.0, method = BrewMethod.V60))
        val d = BrewCalculator.doseGrams(targetYieldGrams = 300.0, ratio = -2.0, method = BrewMethod.V60)
        assertEquals(0.0, d)
    }

    @Test
    fun `zero target yield yields zero dose`() {
        assertEquals(0.0, BrewCalculator.doseGrams(targetYieldGrams = 0.0, ratio = 16.0, method = BrewMethod.V60))
    }

    // MARK: Ratio clamping

    @Test
    fun `clampRatio pins values into the method range`() {
        for (profile in BrewMethodProfile.all) {
            val range = profile.ratioRange
            assertEquals(range.start, BrewCalculator.clampRatio(range.start - 5, profile.method))
            assertEquals(
                range.endInclusive,
                BrewCalculator.clampRatio(range.endInclusive + 5, profile.method),
            )
            assertEquals(profile.defaultRatio, BrewCalculator.clampRatio(profile.defaultRatio, profile.method))
        }
    }

    @Test
    fun `clampRatio leaves in-range espresso ristretto lungo untouched`() {
        assertEquals(1.0, BrewCalculator.clampRatio(1.0, BrewMethod.ESPRESSO)) // ristretto edge
        assertEquals(2.5, BrewCalculator.clampRatio(2.5, BrewMethod.ESPRESSO)) // lungo-ish
        assertEquals(1.0, BrewCalculator.clampRatio(0.5, BrewMethod.ESPRESSO)) // clamped up
        assertEquals(3.0, BrewCalculator.clampRatio(4.0, BrewMethod.ESPRESSO)) // clamped down
    }
}
