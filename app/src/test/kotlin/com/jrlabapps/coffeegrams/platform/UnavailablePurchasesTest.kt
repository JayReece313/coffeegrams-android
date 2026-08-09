package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class UnavailablePurchasesTest {
    @Test
    fun `reports not purchased`() = runTest {
        assertFalse(UnavailablePurchases().isPurchased())
    }

    @Test
    fun `has no price`() = runTest {
        assertNull(UnavailablePurchases().localizedPrice())
    }

    @Test
    fun `purchase always cancels`() = runTest {
        assertEquals(PurchaseOutcome.CANCELLED, UnavailablePurchases().purchase())
    }

    @Test
    fun `restore reports nothing to restore`() = runTest {
        assertFalse(UnavailablePurchases().restore())
    }

    @Test
    fun `entitlement updates is empty`() = runTest {
        assertEquals(emptyList(), UnavailablePurchases().entitlementUpdates().toList())
    }
}
