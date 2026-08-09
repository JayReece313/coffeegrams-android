package com.jrlabapps.coffeegrams.ui.coldbrew

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Exercises the full "Start Steep" happy path. [GrantPermissionRule]
 * pre-grants `POST_NOTIFICATIONS` for this test process so tapping the
 * button never launches the real system permission dialog — that dialog is
 * outside Compose's test tree and not something this test rule can drive.
 * (API 33+ only — the permission doesn't exist below that.)
 */
@RunWith(AndroidJUnit4::class)
class ColdBrewScreenTest {
    private val composeTestRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)).around(composeTestRule)
    } else {
        RuleChain.outerRule(composeTestRule)
    }

    @Test
    fun startsShowingMetricsAndStartSteepButton() {
        composeTestRule.setContent { ColdBrewScreen(doseGrams = 100.0, ratio = 5.0) }

        composeTestRule.onNodeWithText("100 g").assertIsDisplayed() // coffee
        composeTestRule.onNodeWithText("500 g").assertIsDisplayed() // water = 100 x 5
        composeTestRule.onNodeWithText("1:5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Steep").assertIsDisplayed()
    }

    @Test
    fun startingSteepShowsAConfirmation() {
        composeTestRule.setContent { ColdBrewScreen(doseGrams = 100.0, ratio = 5.0) }

        composeTestRule.onNodeWithText("Start Steep").performClick()
        // startSteep() does real suspend work (WorkManager scheduling), so
        // wait for it to actually finish rather than assuming one idle pass
        // covers it.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Saved · reminder set for 16h").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Saved · reminder set for 16h").assertIsDisplayed()
    }
}
