package com.jrlabapps.coffeegrams.ui.methodpicker

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the Pro gate: an unlocked method navigates, a locked one opens
 * the paywall instead. `UnavailablePurchases` (the app's placeholder
 * adapter until M8) means every non-free method reads as locked here.
 */
@RunWith(AndroidJUnit4::class)
class MethodPickerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingAnUnlockedMethodNavigates() {
        var selected: BrewMethod? = null
        composeTestRule.setContent {
            MethodPickerScreen(onMethodSelected = { selected = it })
        }

        composeTestRule.onNodeWithContentDescription("French Press").performClick()

        assertEquals(BrewMethod.FRENCH_PRESS, selected)
    }

    @Test
    fun tappingALockedMethodOpensThePaywallInstead() {
        var selected: BrewMethod? = null
        composeTestRule.setContent {
            MethodPickerScreen(onMethodSelected = { selected = it })
        }

        composeTestRule.onNodeWithContentDescription("V60, Pro, locked").performClick()

        composeTestRule.onNodeWithText("CoffeeGrams Pro").assertExists()
        assertNull(selected)
    }

    @Test
    fun unlockProToolbarActionOpensThePaywall() {
        composeTestRule.setContent {
            MethodPickerScreen(onMethodSelected = {})
        }

        composeTestRule.onNodeWithText("Unlock Pro").performClick()

        composeTestRule.onNodeWithText("CoffeeGrams Pro").assertExists()
    }

    @Test
    fun brewLogToolbarActionNavigatesToTheLog() {
        var viewedLog = false
        composeTestRule.setContent {
            MethodPickerScreen(onMethodSelected = {}, onViewLog = { viewedLog = true })
        }

        composeTestRule.onNodeWithContentDescription("Brew log").performClick()

        assertTrue(viewedLog)
    }
}
