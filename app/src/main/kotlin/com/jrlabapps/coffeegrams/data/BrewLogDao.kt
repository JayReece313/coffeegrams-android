package com.jrlabapps.coffeegrams.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.UUID

@Dao
interface BrewLogDao {
    @Insert
    suspend fun insert(entity: BrewLogEntity)

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
