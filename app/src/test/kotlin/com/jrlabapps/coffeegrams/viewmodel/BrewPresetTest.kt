package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrewPresetTest {
    @Test
    fun `AeroPress offers presets, other methods offer none`() {
        assertEquals(2, BrewPresets.presets(BrewMethod.AEROPRESS).size)
        assertTrue(BrewPresets.presets(BrewMethod.V60).isEmpty())
        assertTrue(BrewPresets.presets(BrewMethod.ESPRESSO).isEmpty())
    }
}
