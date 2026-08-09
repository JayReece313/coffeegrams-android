package com.jrlabapps.coffeegrams.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface BrewLogDao {
    /**
     * Upsert, not a plain insert: iOS's SwiftData `id` is deliberately not
     * unique-constrained (a fresh UUID per brew makes a collision
     * impossible by construction, so `add()` never needs to crash there).
     * Room's `id` *is* a `@PrimaryKey`, so a plain `@Insert` would throw on
     * a duplicate id instead of replacing the row — [Upsert] matches
     * [InMemoryBrewLogStore]'s overwrite-by-id semantics and is strictly
     * safer than either side's alternative.
     */
    @Upsert
    suspend fun upsert(entity: BrewLogEntity)

    /** All saved brews, newest first. */
    @Query("SELECT * FROM brew_log ORDER BY date DESC")
    suspend fun getAll(): List<BrewLogEntity>

    @Query("DELETE FROM brew_log WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("UPDATE brew_log SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: UUID, rating: Int?)

    @Query("UPDATE brew_log SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: UUID, notes: String?)
}
