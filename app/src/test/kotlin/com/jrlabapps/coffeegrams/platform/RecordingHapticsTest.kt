package com.jrlabapps.coffeegrams.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingHapticsTest {
    @Test
    fun `starts with no recorded events`() {
        assertTrue(RecordingHaptics().events.isEmpty())
    }

    @Test
    fun `records each cue in call order`() {
        val haptics = RecordingHaptics()
        haptics.stepChange()
        haptics.finished()
        haptics.targetReached()
        assertEquals(
            listOf(RecordingHaptics.Event.STEP_CHANGE, RecordingHaptics.Event.FINISHED, RecordingHaptics.Event.TARGET_REACHED),
            haptics.events,
        )
    }
}
