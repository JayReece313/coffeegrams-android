package com.jrlabapps.coffeegrams.core

import java.time.Instant

/** The two cold-brew strengths, each with its own fixed ratio (spec §3/§4.5). */
enum class ColdBrewStyle(val rawValue: String) {
    /** Strong concentrate, diluted before drinking. 1:5. */
    CONCENTRATE("concentrate"),

    /** Brewed at drinking strength. 1:8. */
    READY_TO_DRINK("ready_to_drink");

    /** Water:coffee ratio for this style. */
    val ratio: Double
        get() = when (this) {
            CONCENTRATE -> 5.0
            READY_TO_DRINK -> 8.0
        }

    val displayName: String
        get() = when (this) {
            CONCENTRATE -> "Concentrate"
            READY_TO_DRINK -> "Ready to Drink"
        }
}

/**
 * The plan for a cold brew. Unlike the other five methods there is no live
 * pour timer — it is a 12–24 hour steep — so instead of a step list we
 * produce a dose/water amount and the wall-clock time the steep finishes,
 * which the app turns into a single local notification.
 */
data class ColdBrewPlan(
    val doseGrams: Double,
    val waterGrams: Double,
    val style: ColdBrewStyle,
    val steepHours: Double,
    /** Absolute time the steep completes = start + steepHours. */
    val notifyAt: Instant,
)
