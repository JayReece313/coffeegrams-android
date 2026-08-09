package com.jrlabapps.coffeegrams.data

import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the [BrewLogStoring] contract against [InMemoryBrewLogStore].
 * [RoomBrewLogStoreTest] (`androidTest`) asserts the identical behavior
 * against the real Room-backed implementation — the same operations must
 * mean the same thing regardless of which one backs a ViewModel.
 */
class InMemoryBrewLogStoreTest {

    private fun entry(
        id: UUID = UUID.randomUUID(),
        date: Instant = Instant.now(),
        method: BrewMethod = BrewMethod.V60,
        rating: Int? = null,
        notes: String? = null,
    ) = BrewLogEntry(
        id = id,
        date = date,
        method = method,
        doseGrams = 18.0,
        waterGrams = 288.0,
        ratio = 16.0,
        rating = rating,
        notes = notes,
    )

    @Test
    fun `add then entries round-trips`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        assertEquals(listOf(saved), store.entries())
    }

    @Test
    fun `add with a reused id upserts rather than duplicating`() = runTest {
        val store = InMemoryBrewLogStore()
        val id = UUID.randomUUID()
        store.add(entry(id = id, rating = null))
        val updated = entry(id = id, rating = 4)
        store.add(updated)
        assertEquals(listOf(updated), store.entries())
    }

    @Test
    fun `entries returns newest first`() = runTest {
        val store = InMemoryBrewLogStore()
        val older = entry(date = Instant.ofEpochSecond(1_000))
        val newer = entry(date = Instant.ofEpochSecond(2_000))
        store.add(older)
        store.add(newer)
        assertEquals(listOf(newer, older), store.entries())
    }

    @Test
    fun `delete removes the entry`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        store.delete(saved.id)
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun `delete of a nonexistent id is a no-op`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        store.delete(UUID.randomUUID())
        assertEquals(listOf(saved), store.entries())
    }

    @Test
    fun `setRating updates rating in place`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry(rating = null)
        store.add(saved)
        store.setRating(5, saved.id)
        assertEquals(5, store.entries().single().rating)
    }

    @Test
    fun `setRating on a nonexistent id is a no-op`() = runTest {
        val store = InMemoryBrewLogStore()
        store.setRating(5, UUID.randomUUID())
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun `setNotes updates notes in place`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry(notes = null)
        store.add(saved)
        store.setNotes("bloomed a bit long", saved.id)
        assertEquals("bloomed a bit long", store.entries().single().notes)
    }

    @Test
    fun `setNotes to null clears existing notes`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry(notes = "first note")
        store.add(saved)
        store.setNotes(null, saved.id)
        assertNull(store.entries().single().notes)
    }
}
