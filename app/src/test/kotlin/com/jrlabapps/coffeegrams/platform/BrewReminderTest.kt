package com.jrlabapps.coffeegrams.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Conformance-matched to iOS's `BrewReminderTests`. */
class BrewReminderTest {
    @Test
    fun `cold brew reminder fires after the steep hours`() {
        val reminder = BrewReminder.coldBrewReady(steepHours = 16.0)
        assertEquals(BrewReminder.COLD_BREW_ID, reminder.id)
        assertEquals(16 * 3600.0, reminder.delaySeconds)
        assertTrue(reminder.title.contains("Cold brew"))
    }

    @Test
    fun `french press reminder fires when the steep ends`() {
        val reminder = BrewReminder.frenchPressPlunge(steepEndsInSeconds = 285)
        assertEquals(BrewReminder.FRENCH_PRESS_ID, reminder.id)
        assertEquals(285.0, reminder.delaySeconds)
        assertTrue(reminder.body.contains("plunge"))
    }
}
