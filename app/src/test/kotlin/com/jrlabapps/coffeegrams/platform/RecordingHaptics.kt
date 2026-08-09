package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.Haptics

/** A [Haptics] test double that records which cue fired, in order, instead of vibrating. */
class RecordingHaptics : Haptics {
    enum class Event { STEP_CHANGE, TARGET_REACHED, FINISHED }

    private val _events = mutableListOf<Event>()
    val events: List<Event> get() = _events

    override fun stepChange() {
        _events += Event.STEP_CHANGE
    }

    override fun targetReached() {
        _events += Event.TARGET_REACHED
    }

    override fun finished() {
        _events += Event.FINISHED
    }
}
