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
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [UnconfinedTestDispatcher] as `Dispatchers.Main`, not [kotlinx.coroutines.test.StandardTestDispatcher]:
 * every `viewModelScope.launch` here runs against [InMemoryBrewLogStore], which never
 * suspends, so eager/synchronous execution is what lets assertions read the
 * post-launch state without a separate `advanceUntilIdle()` call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun entry(date: Instant = Instant.now()) = BrewLogEntry(
        method = BrewMethod.V60,
        doseGrams = 18.0,
        waterGrams = 288.0,
        ratio = 16.0,
        date = date,
    )

    @Test
    fun `loads entries newest first on init`() = runTest {
        val store = InMemoryBrewLogStore()
        val older = entry(date = Instant.ofEpochSecond(1_000))
        val newer = entry(date = Instant.ofEpochSecond(2_000))
        store.add(older)
        store.add(newer)

        val vm = LogViewModel(store)

        assertEquals(listOf(newer, older), vm.entries.value)
    }

    @Test
    fun `starts empty when the store has nothing saved`() = runTest {
        val vm = LogViewModel(InMemoryBrewLogStore())
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test
    fun `delete removes the entry and refreshes the list`() = runTest {
        val store = InMemoryBrewLogStore()
        val saved = entry()
        store.add(saved)
        val vm = LogViewModel(store)

        vm.delete(saved.id)

        assertTrue(vm.entries.value.isEmpty())
        assertTrue(store.entries().isEmpty())
    }
}
