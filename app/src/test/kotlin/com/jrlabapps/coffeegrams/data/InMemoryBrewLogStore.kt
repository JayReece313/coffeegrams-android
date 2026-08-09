package com.jrlabapps.coffeegrams.data

import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewLogStoring
import java.util.UUID

/**
 * A [BrewLogStoring] test double, backed by an in-memory map instead of
 * Room. Lives in `:app`'s test sources (not main) so it never ships, but is
 * visible to every test in this module — including the future M6
 * ViewModel tests it's really for.
 *
 * Matches [RoomBrewLogStore]'s behavior exactly: mutating a nonexistent
 * [id] is a silent no-op, mirroring the iOS `BrewLogStore`'s
 * `if let record = try record(id: id)` guard.
 */
class InMemoryBrewLogStore : BrewLogStoring {
    private val storage = mutableMapOf<UUID, BrewLogEntry>()

    override suspend fun add(entry: BrewLogEntry) {
        storage[entry.id] = entry
    }

    override suspend fun entries(): List<BrewLogEntry> = storage.values.sortedByDescending { it.date }

    override suspend fun delete(id: UUID) {
        storage.remove(id)
    }

    override suspend fun setRating(rating: Int?, id: UUID) {
        storage[id]?.let { storage[id] = it.copy(rating = rating) }
    }

    override suspend fun setNotes(notes: String?, id: UUID) {
        storage[id]?.let { storage[id] = it.copy(notes = notes) }
    }
}
