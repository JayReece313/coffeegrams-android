package com.jrlabapps.coffeegrams.core

import java.util.UUID

/**
 * The operations the app needs against the brew log. A port — an
 * abstraction `:core` owns but does not implement — so `:app` can back it
 * with Room and tests can back it with an in-memory double, mirroring the
 * iOS app's `BrewLogStoring` protocol.
 *
 * `suspend` rather than synchronous `throws`, unlike the iOS protocol: Room
 * is coroutine-native, and blocking the caller for disk I/O is not the
 * idiomatic Kotlin shape for this port. The operations and their behavior
 * are what must match iOS; the calling convention does not.
 */
interface BrewLogStoring {
    suspend fun add(entry: BrewLogEntry)

    /** All saved brews, newest first. */
    suspend fun entries(): List<BrewLogEntry>

    suspend fun delete(id: UUID)
    suspend fun setRating(rating: Int?, id: UUID)
    suspend fun setNotes(notes: String?, id: UUID)
}
