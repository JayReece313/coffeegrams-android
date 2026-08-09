package com.jrlabapps.coffeegrams.ui.calculator

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jrlabapps.coffeegrams.core.BrewMethod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modeToggleSwitchesTheInputLabel() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithText("Coffee dose (g)").assertExists()

        composeTestRule.onNodeWithText("Water → Dose").performClick()

        composeTestRule.onNodeWithText("Target Water (g)").assertExists()
    }

    @Test
    fun ratioSliderHasAContentDescription() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithContentDescription("Brew ratio").assertExists()
    }

    @Test
    fun ctaIsEnabledAndLabelledPerMethod() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.ESPRESSO, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithText("Set Up Shot").assertIsEnabled()
    }

    @Test
    fun ctaLabelForColdBrewIsViewPlan() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.COLD_BREW, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithText("View Plan").assertIsEnabled()
    }

    @Test
    fun ctaPassesEffectiveDoseAndRatio() {
        var startedDose: Double? = null
        var startedRatio: Double? = null
        composeTestRule.setContent {
            CalculatorScreen(
                method = BrewMethod.V60,
                onStartBrew = { dose, ratio -> startedDose = dose; startedRatio = ratio },
            )
        }

        composeTestRule.onNodeWithText("Set Up Brew").performClick()

        assertEquals(18.0, startedDose)
        assertEquals(16.0, startedRatio)
    }

    @Test
    fun presetsShowOnlyForAeroPress() {
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.AEROPRESS, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithText("Hoffmann").assertExists()
    }

    @Test
    fun commaDecimalInputIsParsed() {
        // The decimal keyboard shows the device locale's own separator; a
        // comma must update state exactly like a period does.
        composeTestRule.setContent { CalculatorScreen(method = BrewMethod.V60, onStartBrew = { _, _ -> }) }

        composeTestRule.onNodeWithText("18").performTextReplacement("18,5")

        composeTestRule.onNodeWithText("296 g").assertExists() // 18.5 x 16
    }
}
