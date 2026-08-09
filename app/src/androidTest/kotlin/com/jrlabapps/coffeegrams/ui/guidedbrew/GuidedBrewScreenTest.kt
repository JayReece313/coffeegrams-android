package com.jrlabapps.coffeegrams.ui.guidedbrew

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.BrewMethodProfile
import com.jrlabapps.coffeegrams.core.BrewTimelineBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the interactions Compose's own test APIs can drive reliably
 * (button taps, text/content-description assertions). The predictive-back
 * leave-confirmation dialog was verified manually on-device instead —
 * simulating a real system back-press reliably from this test rule would
 * need an Espresso dependency this module doesn't otherwise need.
 */
@RunWith(AndroidJUnit4::class)
class GuidedBrewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun frenchPressTimeline() = BrewTimelineBuilder.timeline(
        method = BrewMethod.FRENCH_PRESS,
        doseGrams = 18.0,
        ratio = BrewMethodProfile.frenchPress.defaultRatio,
    )!!

    @Test
    fun startsIdleShowingFirstStepAndStartButton() {
        composeTestRule.setContent {
            GuidedBrewScreen(timeline = frenchPressTimeline(), doseGrams = 18.0, ratio = 15.0)
        }

        composeTestRule.onNodeWithText("READY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Timer").assertIsDisplayed()
    }

    @Test
    fun startingShowsRunningControls() {
        composeTestRule.setContent {
            GuidedBrewScreen(timeline = frenchPressTimeline(), doseGrams = 18.0, ratio = 15.0)
        }

        composeTestRule.onNodeWithText("Start Timer").performClick()

        composeTestRule.onNodeWithText("RUNNING").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithText("End Brew").assertIsDisplayed()
    }

    @Test
    fun pausingShowsPausedStatusAndResume() {
        composeTestRule.setContent {
            GuidedBrewScreen(timeline = frenchPressTimeline(), doseGrams = 18.0, ratio = 15.0)
        }

        composeTestRule.onNodeWithText("Start Timer").performClick()
        composeTestRule.onNodeWithText("Pause").performClick()

        composeTestRule.onNodeWithText("PAUSED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
    }

    @Test
    fun endBrewReachesDoneWithSaveAndBrewAgain() {
        composeTestRule.setContent {
            GuidedBrewScreen(timeline = frenchPressTimeline(), doseGrams = 18.0, ratio = 15.0)
        }

        composeTestRule.onNodeWithText("Start Timer").performClick()
        composeTestRule.onNodeWithText("End Brew").performClick()

        composeTestRule.onNodeWithText("DONE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save to Log").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brew Again").assertIsDisplayed()
    }
}
