package com.jrlabapps.coffeegrams.core

/** A reminder to schedule: what to say and how far in the future, in seconds from now. */
data class ScheduledReminder(
    val id: String,
    val title: String,
    val body: String,
    val delaySeconds: Double,
)

/**
 * Local-reminder scheduling the app needs (cold-brew ready, French-press
 * plunge). A port — an abstraction `:core` owns but does not implement — so
 * `:app` can back it with `WorkManager` and tests can back it with a
 * recording double, mirroring the iOS app's `NotificationScheduling`
 * protocol and its `ScheduledReminder` shape.
 *
 * [requestAuthorization] is `Boolean`, not `suspend`, unlike the iOS
 * protocol's `async` version: Android's `POST_NOTIFICATIONS` permission can
 * only be *requested* from an Activity/Compose composition via
 * `rememberLauncherForActivityResult`, never from a plain adapter class —
 * there is no code path by which this port could show the system prompt
 * itself. On Android this method only reports current permission state; the
 * real request is wired up in the UI layer, not routed through this port.
 * `schedule` stays `suspend` because it does touch a system service.
 */
interface NotificationScheduling {
    /** Whether reminders are currently allowed to be shown — does not itself prompt the user. */
    fun requestAuthorization(): Boolean

    /** Schedule (or replace, by id) a one-shot reminder. */
    suspend fun schedule(reminder: ScheduledReminder)

    /** Cancel a pending (not yet delivered) reminder by id. */
    fun cancel(id: String)
}
