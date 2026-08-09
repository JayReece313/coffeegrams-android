package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Conformance-matched to iOS's `PurchaseControllerTests`. */
class PurchaseControllerTest {
    @Test
    fun `without Pro, only French Press is accessible`() {
        val controller = PurchaseController(ScriptedPurchases(purchased = false))
        assertTrue(controller.canAccess(BrewMethod.FRENCH_PRESS))
        assertFalse(controller.canAccess(BrewMethod.V60))
        assertFalse(controller.canAccess(BrewMethod.CHEMEX))
        assertFalse(controller.canAccess(BrewMethod.ESPRESSO))
    }

    @Test
    fun `Pro unlocks every method`() = runTest {
        val controller = PurchaseController(ScriptedPurchases(purchased = true))
        controller.restore() // pull entitlement from the provider
        for (method in BrewMethod.entries) {
            assertTrue(controller.canAccess(method))
        }
    }

    @Test
    fun `a successful purchase unlocks Pro`() = runTest {
        val controller = PurchaseController(ScriptedPurchases(purchased = false))
        assertFalse(controller.isPremiumUnlocked.value)

        val didPurchase = controller.purchase()
        assertTrue(didPurchase)
        assertTrue(controller.isPremiumUnlocked.value)
        assertTrue(controller.canAccess(BrewMethod.ESPRESSO))
    }

    @Test
    fun `a cancelled purchase leaves it locked`() = runTest {
        val fake = ScriptedPurchases(purchased = false, outcome = PurchaseOutcome.CANCELLED)
        val controller = PurchaseController(fake)

        val didPurchase = controller.purchase()
        assertFalse(didPurchase)
        assertFalse(controller.isPremiumUnlocked.value)
    }

    @Test
    fun `a failed purchase is handled gracefully`() = runTest {
        val fake = ScriptedPurchases(purchased = false, throwOnPurchase = true)
        val controller = PurchaseController(fake)

        val didPurchase = controller.purchase()
        assertFalse(didPurchase)
        assertFalse(controller.isPremiumUnlocked.value)
    }

    @Test
    fun `restore reflects an existing purchase`() = runTest {
        val controller = PurchaseController(ScriptedPurchases(purchased = true))
        controller.restore()
        assertTrue(controller.isPremiumUnlocked.value)
    }
}
