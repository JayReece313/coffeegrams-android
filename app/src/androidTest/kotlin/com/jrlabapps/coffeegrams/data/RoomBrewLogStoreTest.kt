package com.jrlabapps.coffeegrams.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Exercises the identical [BrewLogStoring] contract as
 * [InMemoryBrewLogStoreTest], but against a real (in-memory) Room database —
 * the actual SQL, `TypeConverters`, and DAO wiring are only proven correct
 * here. Needs a device or emulator (`connectedAndroidTest`); see
 * `testing.md` for a known local-emulator system-image gotcha.
 */
@RunWith(AndroidJUnit4::class)
class RoomBrewLogStoreTest {

    private lateinit var database: BrewLogDatabase
    private lateinit var store: RoomBrewLogStore

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BrewLogDatabase::class.java).build()
        store = RoomBrewLogStore(database.brewLogDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

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
    fun addThenEntriesRoundTrips() = runTest {
        val saved = entry()
        store.add(saved)
        assertEquals(listOf(saved), store.entries())
    }

    @Test
    fun addWithAReusedIdUpsertsRatherThanCrashingOnThePrimaryKey() = runTest {
        val id = UUID.randomUUID()
        store.add(entry(id = id, rating = null))
        val updated = entry(id = id, rating = 4)
        store.add(updated)
        assertEquals(listOf(updated), store.entries())
    }

    @Test
    fun instantNanosecondPrecisionRoundTripsExactly() = runTest {
        // Instant.now() on the JVM can carry sub-millisecond resolution;
        // the epoch-millis converter this replaced would have truncated
        // 123_456_789 nanos down to 123ms and lost it on read-back.
        val precise = Instant.ofEpochSecond(1_700_000_000L, 123_456_789L)
        val saved = entry(date = precise)
        store.add(saved)
        assertEquals(precise, store.entries().single().date)
    }

    @Test
    fun entriesReturnsNewestFirst() = runTest {
        val older = entry(date = Instant.ofEpochSecond(1_000))
        val newer = entry(date = Instant.ofEpochSecond(2_000))
        store.add(older)
        store.add(newer)
        assertEquals(listOf(newer, older), store.entries())
    }

    @Test
    fun entryReturnsTheMatchingSavedEntryById() = runTest {
        val saved = entry()
        store.add(saved)
        assertEquals(saved, store.entry(saved.id))
    }

    @Test
    fun entryReturnsNullForANonexistentId() = runTest {
        store.add(entry())
        assertNull(store.entry(UUID.randomUUID()))
    }

    @Test
    fun deleteRemovesTheEntry() = runTest {
        val saved = entry()
        store.add(saved)
        store.delete(saved.id)
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun deleteOfANonexistentIdIsANoOp() = runTest {
        val saved = entry()
        store.add(saved)
        store.delete(UUID.randomUUID())
        assertEquals(listOf(saved), store.entries())
    }

    @Test
    fun setRatingUpdatesRatingInPlace() = runTest {
        val saved = entry(rating = null)
        store.add(saved)
        store.setRating(5, saved.id)
        assertEquals(5, store.entries().single().rating)
    }

    @Test
    fun setRatingOnANonexistentIdIsANoOp() = runTest {
        store.setRating(5, UUID.randomUUID())
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun setNotesUpdatesNotesInPlace() = runTest {
        val saved = entry(notes = null)
        store.add(saved)
        store.setNotes("bloomed a bit long", saved.id)
        assertEquals("bloomed a bit long", store.entries().single().notes)
    }

    @Test
    fun setNotesToNullClearsExistingNotes() = runTest {
        val saved = entry(notes = "first note")
        store.add(saved)
        store.setNotes(null, saved.id)
        assertNull(store.entries().single().notes)
    }
}
