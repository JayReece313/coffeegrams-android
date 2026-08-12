package com.jrlabapps.coffeegrams.core

/**
 * The ongoing "brew in progress" notification a guided brew keeps visible
 * while it runs. A port — an abstraction `:core` owns but does not
 * implement — so `:app` can back it with a real foreground service and
 * tests can back it with a recording double.
 *
 * Distinct from [NotificationScheduling]: that port schedules a one-shot
 * alert for later (a cold-brew or French-press steep timer); this one is
 * shown immediately and updated continuously while [start] and [stop]
 * bracket a live session — the same-instant, ongoing counterpart to
 * `NotificationScheduling`'s delayed, one-shot reminder.
 *
 * No `:core` timing logic depends on this — it exists purely so the app
 * process has a reason for Android not to kill it mid-brew. `BrewTimerEngine`
 * and `MonotonicClock` are unaffected by whether this is wired up or not.
 */
interface BrewSessionNotifier {
    /** Shows the ongoing notification, starting the session. */
    fun start(title: String, message: String)

    /** Updates the ongoing notification's content. No-op before [start]. */
    fun update(title: String, message: String)

    /** Clears the notification, ending the session. Safe to call even if not started. */
    fun stop()
}
