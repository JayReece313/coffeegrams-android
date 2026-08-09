package com.jrlabapps.coffeegrams.ui.calculator

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modeToggleSwitchesTheInputLabel() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60) }

        composeTestRule.onNodeWithText("Coffee dose (g)").assertExists()

        composeTestRule.onNodeWithText("Water → Dose").performClick()

        composeTestRule.onNodeWithText("Target Water (g)").assertExists()
    }

    @Test
    fun ratioSliderHasAContentDescription() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60) }

        composeTestRule.onNodeWithContentDescription("Brew ratio").assertExists()
    }

    @Test
    fun ctaIsDisabledAndLabelledPerMethod() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.ESPRESSO) }

        composeTestRule.onNodeWithText("Set Up Shot").assertIsNotEnabled()
    }

    @Test
    fun ctaLabelForColdBrewIsViewPlan() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.COLD_BREW) }

        composeTestRule.onNodeWithText("View Plan").assertIsNotEnabled()
    }

    @Test
    fun presetsShowOnlyForAeroPress() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.AEROPRESS) }

        composeTestRule.onNodeWithText("Hoffmann").assertExists()
    }

    @Test
    fun commaDecimalInputIsParsed() {
        // The decimal keyboard shows the device locale's own separator; a
        // comma must update state exactly like a period does.
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60) }

        composeTestRule.onNodeWithText("18").performTextReplacement("18,5")

        composeTestRule.onNodeWithText("296 g").assertExists() // 18.5 x 16
    }
}
