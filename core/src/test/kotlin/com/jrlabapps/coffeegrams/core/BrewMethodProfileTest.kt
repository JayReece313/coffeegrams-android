package com.jrlabapps.coffeegrams.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M1 gate: the hardcoded reference table (spec §3) is internally
 * consistent. These are cheap invariants, but they catch a whole class of
 * data-entry bugs (a typo'd range, a missing method) that would otherwise
 * surface as weird UI behaviour much later.
 *
 * Ported case-for-case from the iOS BrewMethodProfileTests.swift
 * conformance suite (7 cases).
 */
class BrewMethodProfileTest {

    @Test
    fun `every method has exactly one profile`() {
        for (method in BrewMethod.entries) {
            val matches = BrewMethodProfile.all.filter { it.method == method }
            assertEquals(1, matches.size, "$method should have exactly one profile")
        }
        assertEquals(BrewMethod.entries.size, BrewMethodProfile.all.size)
    }

    @Test
    fun `profile(for) returns the matching method`() {
        for (method in BrewMethod.entries) {
            assertEquals(method, BrewMethodProfile.profile(method).method)
        }
    }

    @Test
    fun `default ratio sits inside the allowed range`() {
        for (profile in BrewMethodProfile.all) {
            assertTrue(
                profile.ratioRange.contains(profile.defaultRatio),
                "${profile.method} default ${profile.defaultRatio} outside ${profile.ratioRange}",
            )
        }
    }

    @Test
    fun `ratio ranges are well-formed (lower less than or equal to upper, positive)`() {
        for (profile in BrewMethodProfile.all) {
            assertTrue(profile.ratioRange.start > 0)
            assertTrue(profile.ratioRange.start <= profile.ratioRange.endInclusive)
        }
    }

    @Test
    fun `exact values from the spec section 3 table`() {
        val v60 = BrewMethodProfile.profile(BrewMethod.V60)
        assertEquals(BrewType.PULSE_POUR, v60.brewType)
        assertEquals(16.0, v60.defaultRatio)
        assertEquals(15.0..17.0, v60.ratioRange)
        assertEquals(2.25, v60.bloomMultiplier)
        assertEquals(45, v60.bloomSeconds)
        assertEquals(2, v60.numPours)
        assertEquals(45, v60.pourIntervalSeconds)

        val chemex = BrewMethodProfile.profile(BrewMethod.CHEMEX)
        assertEquals(60, chemex.pourIntervalSeconds) // spaced wider than V60
        assertEquals(2.5, chemex.bloomMultiplier)

        val fp = BrewMethodProfile.profile(BrewMethod.FRENCH_PRESS)
        assertEquals(BrewType.IMMERSION, fp.brewType)
        assertEquals(15.0, fp.defaultRatio)
        assertEquals(240, fp.steepSeconds)
        assertEquals(2.0, fp.bloomMultiplier)

        val aero = BrewMethodProfile.profile(BrewMethod.AEROPRESS)
        assertEquals(18.0, aero.defaultRatio) // Hoffmann default
        assertEquals(12.0..18.0, aero.ratioRange)
        assertNull(aero.bloomMultiplier) // no bloom
        assertEquals(120, aero.steepSeconds)

        val cold = BrewMethodProfile.profile(BrewMethod.COLD_BREW)
        assertEquals(5.0, cold.defaultRatio) // concentrate
        assertNull(cold.steepSeconds) // steep measured in hours

        val esp = BrewMethodProfile.profile(BrewMethod.ESPRESSO)
        assertEquals(BrewType.PRESSURE, esp.brewType)
        assertEquals(2.0, esp.defaultRatio)
        assertEquals(1.0..3.0, esp.ratioRange)
        assertEquals(25..30, esp.shotTimeRangeSeconds)
    }

    @Test
    fun `only espresso defines a shot-time window`() {
        for (profile in BrewMethodProfile.all) {
            if (profile.method == BrewMethod.ESPRESSO) {
                assertNotNull(profile.shotTimeRangeSeconds)
            } else {
                assertNull(profile.shotTimeRangeSeconds)
            }
        }
    }

    @Test
    fun `free tier is exactly French Press`() {
        val free = BrewMethod.entries.filter { it.isFreeTier }
        assertEquals(setOf(BrewMethod.FRENCH_PRESS), free.toSet())
    }
}
