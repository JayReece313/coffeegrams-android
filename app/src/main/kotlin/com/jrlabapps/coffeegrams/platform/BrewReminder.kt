package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.ScheduledReminder

/**
 * Builds the specific reminders CoffeeGrams schedules. Pure — no
 * [android.content.Context] involved — so the content/timing is
 * unit-testable without touching the notification system.
 */
object BrewReminder {
    const val COLD_BREW_ID = "cold_brew_ready"
    const val FRENCH_PRESS_ID = "french_press_plunge"

    /** Fires when a cold-brew steep finishes. */
    fun coldBrewReady(steepHours: Double): ScheduledReminder = ScheduledReminder(
        id = COLD_BREW_ID,
        title = "Cold brew ready ☕️",
        body = "Your cold brew has finished steeping — strain it and enjoy.",
        delaySeconds = steepHours * 3600.0,
    )

    /** Fires when a French-press steep ends, so it isn't left to over-extract. */
    fun frenchPressPlunge(steepEndsInSeconds: Int): ScheduledReminder = ScheduledReminder(
        id = FRENCH_PRESS_ID,
        title = "Time to plunge",
        body = "Your French press is done steeping — plunge and serve so it doesn't turn bitter.",
        delaySeconds = steepEndsInSeconds.toDouble(),
    )
}
