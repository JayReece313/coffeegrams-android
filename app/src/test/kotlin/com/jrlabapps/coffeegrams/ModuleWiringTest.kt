package com.jrlabapps.coffeegrams

import com.jrlabapps.coffeegrams.core.BrewCalculator
import com.jrlabapps.coffeegrams.core.BrewMethod
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves two things that are easy to get silently wrong:
 *
 *  1. `:app` really resolves types from `:core` — the project dependency is
 *     wired up, not just declared.
 *  2. The `:app` unit-test harness discovers and runs `kotlin.test` tests.
 *     `:app` gets Kotlin from AGP's built-in support rather than the
 *     standalone kotlin-jvm plugin, so test discovery is worth asserting
 *     here rather than assuming it behaves the same as `:core`.
 *
 * M6 replaces this with the real ViewModel suites.
 */
class ModuleWiringTest {
    @Test
    fun `app module can resolve types from core`() {
        assertEquals("v60", BrewMethod.V60.rawValue)
        assertEquals(256.0, BrewCalculator.waterGrams(doseGrams = 16.0, ratio = 16.0))
    }
}
