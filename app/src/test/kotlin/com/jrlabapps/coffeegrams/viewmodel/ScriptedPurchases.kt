package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import com.jrlabapps.coffeegrams.core.PurchaseUnavailableException
import com.jrlabapps.coffeegrams.core.Purchases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** A [Purchases] test double scripted with a fixed outcome — no Play Billing. */
class ScriptedPurchases(
    var purchased: Boolean = false,
    var price: String? = "$4.99",
    var outcome: PurchaseOutcome = PurchaseOutcome.PURCHASED,
    var throwOnPurchase: Boolean = false,
) : Purchases {
    override suspend fun isPurchased(): Boolean = purchased

    override suspend fun localizedPrice(): String? = price

    override suspend fun purchase(): PurchaseOutcome {
        if (throwOnPurchase) throw PurchaseUnavailableException()
        if (outcome == PurchaseOutcome.PURCHASED) purchased = true
        return outcome
    }

    override suspend fun restore(): Boolean = purchased

    override fun entitlementUpdates(): Flow<Boolean> = emptyFlow()
}
