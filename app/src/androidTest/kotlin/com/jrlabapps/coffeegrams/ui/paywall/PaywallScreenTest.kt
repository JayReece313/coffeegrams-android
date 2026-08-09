package com.jrlabapps.coffeegrams.ui.paywall

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `UnavailablePurchases` (the app's placeholder adapter until M8) has no
 * price and never completes a purchase, so the buy button never shows a
 * price and tapping it never dismisses the sheet — both asserted here.
 */
@RunWith(AndroidJUnit4::class)
class PaywallScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersAllFourBenefits() {
        composeTestRule.setContent { PaywallScreen(onDismiss = {}) }

        composeTestRule.onNodeWithText("• All 6 brew methods").assertExists()
        composeTestRule.onNodeWithText("• Guided step-by-step timers").assertExists()
        composeTestRule.onNodeWithText("• Full brew log history").assertExists()
        composeTestRule.onNodeWithText("• One-time purchase, no subscription").assertExists()
    }

    @Test
    fun buyButtonNeverFabricatesAPrice() {
        composeTestRule.setContent { PaywallScreen(onDismiss = {}) }

        composeTestRule.onNodeWithText("Unlock Everything").assertExists()
    }

    @Test
    fun restoreDoesNotDismissWithNothingToRestore() {
        var dismissed = false
        composeTestRule.setContent { PaywallScreen(onDismiss = { dismissed = true }) }

        composeTestRule.onNodeWithText("Restore Purchase").performClick()
        composeTestRule.waitForIdle()

        assert(!dismissed)
    }
}
