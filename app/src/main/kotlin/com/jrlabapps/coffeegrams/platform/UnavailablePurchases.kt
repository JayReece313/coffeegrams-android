package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import com.jrlabapps.coffeegrams.core.Purchases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A placeholder [Purchases] adapter used until M8 wires up the real Play
 * `BillingClient`. Always reports Pro as not purchased; [purchase] and
 * [restore] are no-ops that report failure, so the paywall's buy/restore
 * buttons render and are tappable (M7), but nothing can actually be bought
 * until M8 replaces this class — a straight swap of the adapter behind
 * [PurchaseController], not a change to the port or its caller.
 */
class UnavailablePurchases : Purchases {
    override suspend fun isPurchased(): Boolean = false

    override suspend fun localizedPrice(): String? = null

    override suspend fun purchase(): PurchaseOutcome = PurchaseOutcome.CANCELLED

    override suspend fun restore(): Boolean = false

    override fun entitlementUpdates(): Flow<Boolean> = emptyFlow()
}
