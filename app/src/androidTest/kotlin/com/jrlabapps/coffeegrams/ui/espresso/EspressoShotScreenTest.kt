package com.jrlabapps.coffeegrams.ui.espresso

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewTimelineBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EspressoShotScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun target() = BrewTimelineBuilder.buildEspressoTarget(doseGrams = 18.0, ratio = 2.0)

    @Test
    fun startsReadyShowingTargetMetrics() {
        composeTestRule.setContent { EspressoShotScreen(target = target()) }

        composeTestRule.onNodeWithText("READY").assertIsDisplayed()
        composeTestRule.onNodeWithText("18 g").assertIsDisplayed() // dose
        composeTestRule.onNodeWithText("36 g").assertIsDisplayed() // target yield
    }

    @Test
    fun startingShowsBuildingAndStopButton() {
        composeTestRule.setContent { EspressoShotScreen(target = target()) }

        composeTestRule.onNodeWithText("Start Shot").performClick()

        composeTestRule.onNodeWithText("BUILDING").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun stoppingOffersSaveAndReset() {
        composeTestRule.setContent { EspressoShotScreen(target = target()) }

        composeTestRule.onNodeWithText("Start Shot").performClick()
        // hasStarted requires elapsedSeconds > 0 once stopped (matches iOS's
        // identical definition) -- wait for the 100ms tick loop to actually
        // produce a whole second rather than sleeping a fixed wall-clock
        // duration, which can flake on a slow/loaded device.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("0s").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Stop").performClick()

        composeTestRule.onNodeWithText("Save to Log").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun savingShowsSavedToLog() {
        composeTestRule.setContent { EspressoShotScreen(target = target()) }

        composeTestRule.onNodeWithText("Start Shot").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("0s").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Stop").performClick()
        composeTestRule.onNodeWithText("Save to Log").performClick()
        // add() is a real suspend Room write -- wait for it rather than
        // assuming one idle pass covers it.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Saved to log").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Saved to log").assertIsDisplayed()
    }
}
