package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethod

/**
 * A named recipe preset. AeroPress culture is genuinely split on ratio and
 * technique, so rather than pick one "correct" number the calculator offers
 * a couple of well-known starting points on top of the raw ratio slider.
 */
data class BrewPreset(
    val id: String,
    val name: String,
    val doseGrams: Double,
    val ratio: Double,
) {
    constructor(name: String, doseGrams: Double, ratio: Double) : this(name, name, doseGrams, ratio)
}

object BrewPresets {
    /** Presets to offer for a given method, or empty if it has none. */
    fun presets(method: BrewMethod): List<BrewPreset> = when (method) {
        BrewMethod.AEROPRESS -> aeroPress
        else -> emptyList()
    }

    /** AeroPress: Hoffmann's clean 1:18 and a classic strong 1:12. */
    val aeroPress: List<BrewPreset> = listOf(
        BrewPreset(name = "Hoffmann", doseGrams = 11.0, ratio = 18.0),
        BrewPreset(name = "Classic Strong", doseGrams = 15.0, ratio = 12.0),
    )
}
