package com.jrlabapps.coffeegrams.core

/**
 * The six brewing methods CoffeeGrams supports in v1.
 *
 * [rawValue] is a **stable identifier** — it is what gets persisted in the
 * brew log. Never rename an existing case's raw value, or old log entries
 * would fail to decode; add new cases instead.
 */
enum class BrewMethod(val rawValue: String) {
    V60("v60"),
    CHEMEX("chemex"),
    FRENCH_PRESS("french_press"),
    AEROPRESS("aeropress"),
    COLD_BREW("cold_brew"),
    ESPRESSO("espresso");

    val id: String get() = rawValue

    /** Human-facing name for pickers and titles. */
    val displayName: String
        get() = when (this) {
            V60 -> "V60"
            CHEMEX -> "Chemex"
            FRENCH_PRESS -> "French Press"
            AEROPRESS -> "AeroPress"
            COLD_BREW -> "Cold Brew"
            ESPRESSO -> "Espresso"
        }

    /**
     * The method(s) available for free; the rest are unlocked by the
     * one-time in-app purchase. Encoded here (not in the paywall UI) so the
     * free/paid split has a single source of truth the tests can assert.
     *
     * v1: only French Press is free. Widening the free tier later is a
     * one-line change here.
     */
    val isFreeTier: Boolean
        get() = this == FRENCH_PRESS

    companion object {
        /**
         * Decode from a persisted raw value, matching the iOS app's
         * `BrewMethod(rawValue:) ?? .v60` fallback in `BrewLogRecord.method`
         * exactly — an unrecognized string (e.g. a future or removed
         * method) falls back to V60 rather than throwing.
         */
        fun fromRawValue(value: String): BrewMethod = entries.firstOrNull { it.rawValue == value } ?: V60
    }
}
