package com.jrlabapps.coffeegrams

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/**
 * The Play Store screenshot harness — mirrors the iOS sibling's
 * `ScreenshotCaptureTests.swift`/`capture.sh` pattern exactly: the same
 * assertions run in every normal instrumented test pass (so a button-text
 * rename fails CI, the guard against the listing quietly going stale),
 * while the *shutter* is opt-in — only `Releases/screenshots/capture.sh`
 * sets `captureScreenshots=true`, so a plain `connectedAndroidTest` run
 * pays none of the screenshot cost.
 *
 * Drives the real app end-to-end ([createAndroidComposeRule] against
 * [MainActivity], not an isolated screen + test double like every other
 * Compose test in this repo) — a screenshot of a test harness isn't a
 * screenshot of what ships. [UiDevice.takeScreenshot] (not Compose's own
 * `captureToImage()`) is used specifically because it captures the whole
 * device, status bar included — `capture.sh` pins that to 9:41/full
 * battery via Android's "Demo Mode" broadcasts before running this.
 *
 * [GrantPermissionRule] pre-grants `POST_NOTIFICATIONS`, matching
 * [com.jrlabapps.coffeegrams.ui.guidedbrew.GuidedBrewScreenTest]'s own
 * reasoning: starting a guided brew here would otherwise launch the real
 * system permission dialog, which sits outside Compose's test tree and
 * would leave every assertion past that point failing with "no compose
 * hierarchies found."
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)).around(composeTestRule)
    } else {
        RuleChain.outerRule(composeTestRule)
    }

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Set by `capture.sh` via `adb shell am instrument -e captureScreenshots true ...`. */
    private val isCapturing: Boolean
        get() = InstrumentationRegistry.getArguments().getString("captureScreenshots") == "true"

    // MARK: 01 — Home

    @Test
    fun testCaptureHome() {
        composeTestRule.onNodeWithText("CoffeeGrams").assertExists()
        composeTestRule.onNodeWithContentDescription("French Press").assertExists()

        capture("01-home")
    }

    // MARK: 02 — Calculator

    @Test
    fun testCaptureCalculator() {
        composeTestRule.onNodeWithContentDescription("French Press").performClick()

        // Renamed once already (M11 on iOS); pinning the exact label here is
        // the guard against a future rename staling this screenshot silently.
        composeTestRule.onNodeWithText("Set Up Brew").assertExists()

        capture("02-calculator")
    }

    // MARK: 03 — Guided timer

    @Test
    fun testCaptureGuidedTimer() {
        composeTestRule.onNodeWithContentDescription("French Press").performClick()
        composeTestRule.onNodeWithText("Set Up Brew").performClick()
        composeTestRule.onNodeWithText("Start Timer").performClick()

        composeTestRule.onNodeWithText("Pause").assertExists()
        composeTestRule.onNodeWithText("End Brew").assertExists()

        // Only when shooting: let the clock run so TOTAL reads a non-zero
        // time — a frozen 0:00 would undersell the feature the shot exists
        // to show. waitUntil, not Thread.sleep(): a raw sleep on the test
        // thread doesn't synchronize with Compose's own idling loop, so the
        // ViewModel's real tick loop never actually got observed advancing
        // (confirmed empirically — TOTAL still read 0:00 after an 8s sleep).
        // waitUntil polls with real synchronization each iteration, which is
        // what actually lets the tick loop's genuine progress land. Not a
        // real-time wait in the normal (non-capturing) run, so the
        // assertions above stay fast.
        if (isCapturing) {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithText("TOTAL 0:00").fetchSemanticsNodes().isEmpty()
            }
        }

        capture("03-guided-timer")
    }

    // MARK: 04 — Paywall

    @Test
    fun testCapturePaywall() {
        composeTestRule.onNodeWithContentDescription("Espresso, Pro, locked").performClick()
        composeTestRule.onNodeWithText("CoffeeGrams Pro").assertExists()

        capture("04-paywall")
    }

    // MARK: 05 — Brew log

    @Test
    fun testCaptureBrewLog() {
        composeTestRule.onNodeWithContentDescription("French Press").performClick()
        composeTestRule.onNodeWithText("Set Up Brew").performClick()
        composeTestRule.onNodeWithText("Start Timer").performClick()

        // Bloom/fill/steep auto-advance; only the final (plunge) step holds
        // for a tap, surfacing as "Done" once reached. A plain for-loop, not
        // repeat(8) { ... return@repeat ... } -- return@repeat only skips
        // the current iteration, not the whole loop, so it kept "finding"
        // Done and re-checking on every remaining iteration instead of
        // actually stopping.
        for (i in 0 until 8) {
            if (composeTestRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()) break
            val skip = composeTestRule.onAllNodesWithText("Skip step").fetchSemanticsNodes()
            if (skip.isNotEmpty()) composeTestRule.onNodeWithText("Skip step").performClick()
        }
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.onNodeWithText("Save to Log").performClick()

        // UiDevice's back press operates outside Compose's own test
        // synchronization (unlike performClick()), so each one needs an
        // explicit waitForIdle() before the next assertion/action can see
        // the settled result.
        device.pressBack() // guided brew -> calculator
        composeTestRule.waitForIdle()
        device.pressBack() // calculator -> method picker
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Brew log").performClick()

        composeTestRule.onNodeWithText("Brew Log").assertExists()
        composeTestRule.onNodeWithText("No brews yet").assertDoesNotExist()

        capture("05-brew-log")
    }

    // MARK: Helpers

    /**
     * Full-device screenshot at native resolution, written to the app's
     * external files dir so `capture.sh` can `adb pull` it without needing
     * `run-as`. A no-op outside a capture run, so a normal suite pass
     * touches no files and takes no shots.
     */
    private fun capture(name: String) {
        if (!isCapturing) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        device.takeScreenshot(File(dir, "$name.png"))
    }
}
