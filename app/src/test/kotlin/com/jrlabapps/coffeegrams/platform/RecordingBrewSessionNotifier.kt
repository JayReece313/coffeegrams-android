package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.BrewSessionNotifier

/** A [BrewSessionNotifier] test double that records calls instead of touching a real service/notification. */
class RecordingBrewSessionNotifier : BrewSessionNotifier {
    sealed interface Event {
        data class Start(val title: String, val message: String) : Event
        data class Update(val title: String, val message: String) : Event
        data object Stop : Event
    }

    private val _events = mutableListOf<Event>()
    val events: List<Event> get() = _events

    val startCount: Int get() = _events.count { it is Event.Start }
    val stopCount: Int get() = _events.count { it is Event.Stop }

    override fun start(title: String, message: String) {
        _events += Event.Start(title, message)
    }

    override fun update(title: String, message: String) {
        _events += Event.Update(title, message)
    }

    override fun stop() {
        _events += Event.Stop
    }
}
