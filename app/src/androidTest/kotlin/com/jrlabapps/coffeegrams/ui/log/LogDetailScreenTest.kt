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

/** See [LogScreenTest]'s doc comment for why each test gets its own in-memory Room database. */
@RunWith(AndroidJUnit4::class)
class LogDetailScreenTest {
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
    fun rendersTheBrewsSummary() {
        val saved = entry()
        runBlocking { store.add(saved) }

        composeTestRule.setContent { LogDetailScreen(entryId = saved.id, onDeleted = {}, store = store) }

        composeTestRule.onNodeWithText("18 g").assertExists()
        composeTestRule.onNodeWithText("288 g").assertExists()
        composeTestRule.onNodeWithText("1:16").assertExists()
    }

    @Test
    fun tappingAStarPersistsTheRating() {
        val saved = entry()
        runBlocking { store.add(saved) }

        composeTestRule.setContent { LogDetailScreen(entryId = saved.id, onDeleted = {}, store = store) }

        composeTestRule.onNodeWithContentDescription("4 stars").performClick()
        composeTestRule.waitForIdle()

        assertEquals(4, runBlocking { store.entries() }.single().rating)
    }

    @Test
    fun deletingTheBrewRemovesItAndInvokesOnDeleted() {
        val saved = entry()
        runBlocking { store.add(saved) }

        var deleted = false
        composeTestRule.setContent { LogDetailScreen(entryId = saved.id, onDeleted = { deleted = true }, store = store) }

        composeTestRule.onNodeWithText("Delete Brew").performClick()
        composeTestRule.waitForIdle()

        assertTrue(deleted)
        assertTrue(runBlocking { store.entries() }.isEmpty())
    }
}
