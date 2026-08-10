package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.data.InMemoryBrewLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** See [LogViewModelTest]'s doc comment for why `Dispatchers.Main` is [UnconfinedTestDispatcher] here. */
@OptIn(ExperimentalCoroutinesApi::class)
class LogDetailViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun entry(rating: Int? = null, notes: String? = null) = BrewLogEntry(
        method = BrewMethod.V60,
        doseGrams = 18.0,
        waterGrams = 288.0,
        ratio = 16.0,
        rating = rating,
        notes = notes,
    )

    @Test
    fun `loads the matching entry by id on init`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)

        val vm = LogDetailViewModel(saved.id, store)

        assertEquals(saved, vm.entry.value)
    }

    @Test
    fun `setRating updates local state immediately and persists through the store`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        val vm = LogDetailViewModel(saved.id, store)

        vm.setRating(4)

        assertEquals(4, vm.entry.value?.rating)
        assertEquals(4, store.entries().single().rating)
    }

    @Test
    fun `setRating with the current value clears it back to unrated`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry(rating = 3)
        store.add(saved)
        val vm = LogDetailViewModel(saved.id, store)

        vm.setRating(0)

        assertNull(vm.entry.value?.rating)
        assertNull(store.entries().single().rating)
    }

    @Test
    fun `saveNotes trims and persists, empty becomes null`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        val vm = LogDetailViewModel(saved.id, store)

        vm.saveNotes("  bloomed a bit long  ")
        assertEquals("bloomed a bit long", store.entries().single().notes)

        vm.saveNotes("   ")
        assertNull(store.entries().single().notes)
    }

    @Test
    fun `delete removes the entry and invokes the callback`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        val vm = LogDetailViewModel(saved.id, store)

        var deleted = false
        vm.delete { deleted = true }

        assertTrue(store.entries().isEmpty())
        assertTrue(deleted)
    }
}
