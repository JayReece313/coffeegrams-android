package com.jrlabapps.coffeegrams.platform

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [classifyPurchaseResponse] only — the pure `Int` -> [BillingOutcome]
 * mapping [LivePurchases] uses to interpret a `BillingClient` callback. The
 * rest of [LivePurchases] talks to a real `BillingClient` connection and is
 * verified by `testing.md`'s physical-device checklist instead; this project's
 * unit tests don't set `isReturnDefaultValues`, so anything that actually
 * constructs a [Purchase] (its constructor parses JSON) isn't safe to exercise
 * here — only the plain `Int` constants referenced below are.
 */
class LivePurchasesTest {
    @Test
    fun `user cancellation maps to CANCELLED`() {
        val result = classifyPurchaseResponse(BillingResponseCode.USER_CANCELED, purchaseState = null)
        assertEquals(BillingOutcome.Result(PurchaseOutcome.CANCELLED), result)
    }

    @Test
    fun `already-owned maps to PURCHASED regardless of purchase state`() {
        val result = classifyPurchaseResponse(BillingResponseCode.ITEM_ALREADY_OWNED, purchaseState = null)
        assertEquals(BillingOutcome.Result(PurchaseOutcome.PURCHASED), result)
    }

    @Test
    fun `OK with a purchased state maps to PURCHASED`() {
        val result = classifyPurchaseResponse(BillingResponseCode.OK, Purchase.PurchaseState.PURCHASED)
        assertEquals(BillingOutcome.Result(PurchaseOutcome.PURCHASED), result)
    }

    @Test
    fun `OK with a pending state maps to PENDING`() {
        val result = classifyPurchaseResponse(BillingResponseCode.OK, Purchase.PurchaseState.PENDING)
        assertEquals(BillingOutcome.Result(PurchaseOutcome.PENDING), result)
    }

    @Test
    fun `OK with no matching purchase is unavailable`() {
        val result = classifyPurchaseResponse(BillingResponseCode.OK, purchaseState = null)
        assertEquals(BillingOutcome.Unavailable, result)
    }

    @Test
    fun `a real store failure is unavailable`() {
        val result = classifyPurchaseResponse(BillingResponseCode.BILLING_UNAVAILABLE, purchaseState = null)
        assertEquals(BillingOutcome.Unavailable, result)
    }
}
