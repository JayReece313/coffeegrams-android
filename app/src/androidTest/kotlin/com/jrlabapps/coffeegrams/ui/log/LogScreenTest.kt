package com.jrlabapps.coffeegrams.ui.log

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.data.BrewLogDatabase
import com.jrlabapps.coffeegrams.data.RoomBrewLogStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Each test builds its own in-memory Room database — same isolation
 * approach as `RoomBrewLogStoreTest` — rather than reading through
 * `currentApplication().brewLogStore`, which would hit the real on-device
 * database and leak state between test runs. `LogScreen`'s `store` param
 * exists for exactly this seam.
 */
@RunWith(AndroidJUnit4::class)
class LogScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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

    private fun entry(rating: Int? = null) = BrewLogEntry(
        method = BrewMethod.V60,
        doseGrams = 18.0,
        waterGrams = 288.0,
        ratio = 16.0,
        rating = rating,
    )

    @Test
    fun emptyLogShowsTheEmptyState() {
        composeTestRule.setContent { LogScreen(onEntrySelected = {}, store = store) }

        composeTestRule.onNodeWithText("No brews yet").assertExists()
    }

    @Test
    fun tappingARowNavigatesWithItsId() {
        val saved = entry()
        runBlocking { store.add(saved) }

        var selected: UUID? = null
        composeTestRule.setContent { LogScreen(onEntrySelected = { selected = it }, store = store) }

        composeTestRule.onNodeWithContentDescription("V60", substring = true).performClick()

        assertEquals(saved.id, selected)
    }

    @Test
    fun deletingARowRemovesItFromTheStore() {
        val saved = entry()
        runBlocking { store.add(saved) }

        composeTestRule.setContent { LogScreen(onEntrySelected = {}, store = store) }
        composeTestRule.onNodeWithContentDescription("V60", substring = true).assertExists()

        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No brews yet").assertExists()
        assertTrue(runBlocking { store.entries() }.isEmpty())
    }
}
