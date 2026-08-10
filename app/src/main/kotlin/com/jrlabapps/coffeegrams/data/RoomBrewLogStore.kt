package com.jrlabapps.coffeegrams.data

import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewLogStoring
import java.util.UUID

/** The live [BrewLogStoring] adapter, backed by Room. */
class RoomBrewLogStore(private val dao: BrewLogDao) : BrewLogStoring {
    override suspend fun add(entry: BrewLogEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun entries(): List<BrewLogEntry> = dao.getAll().map { it.toEntry() }

    override suspend fun entry(id: UUID): BrewLogEntry? = dao.getById(id)?.toEntry()

    override suspend fun delete(id: UUID) {
        dao.deleteById(id)
    }

    override suspend fun setRating(rating: Int?, id: UUID) {
        dao.updateRating(id, rating)
    }

    override suspend fun setNotes(notes: String?, id: UUID) {
        dao.updateNotes(id, notes)
    }
}
