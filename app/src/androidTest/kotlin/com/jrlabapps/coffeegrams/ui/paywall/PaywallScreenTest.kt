package com.jrlabapps.coffeegrams.ui.paywall

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.platform.UnavailablePurchases
import com.jrlabapps.coffeegrams.viewmodel.PurchaseController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Injects a fresh [PurchaseController] over [UnavailablePurchases] explicitly
 * — rather than relying on `CoffeeGramsApplication`'s own default, which is
 * the real Play Billing adapter from M8 onward — so the buy button never
 * shows a price and tapping it never dismisses the sheet, deterministically,
 * with no live billing connection involved.
 */
@RunWith(AndroidJUnit4::class)
class PaywallScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun unavailablePurchases() = PurchaseController(UnavailablePurchases())

    @Test
    fun rendersAllFourBenefits() {
        composeTestRule.setContent { PaywallScreen(onDismiss = {}, purchases = unavailablePurchases()) }

        composeTestRule.onNodeWithText("• All 6 brew methods").assertExists()
        composeTestRule.onNodeWithText("• Guided step-by-step timers").assertExists()
        composeTestRule.onNodeWithText("• Full brew log history").assertExists()
        composeTestRule.onNodeWithText("• One-time purchase, no subscription").assertExists()
    }

    @Test
    fun buyButtonNeverFabricatesAPrice() {
        composeTestRule.setContent { PaywallScreen(onDismiss = {}, purchases = unavailablePurchases()) }

        composeTestRule.onNodeWithText("Unlock Everything").assertExists()
    }

    @Test
    fun restoreDoesNotDismissWithNothingToRestore() {
        var dismissed = false
        composeTestRule.setContent { PaywallScreen(onDismiss = { dismissed = true }, purchases = unavailablePurchases()) }

        composeTestRule.onNodeWithText("Restore Purchase").performClick()
        composeTestRule.waitForIdle()

        assert(!dismissed)
    }
}
