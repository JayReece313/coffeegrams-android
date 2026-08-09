package com.jrlabapps.coffeegrams.core

import kotlinx.coroutines.flow.Flow

/** What happened when [Purchases.purchase] was attempted. */
enum class PurchaseOutcome { PURCHASED, CANCELLED, PENDING }

/** Thrown by [Purchases.purchase] when the store can't complete the purchase (e.g. product unavailable). */
class PurchaseUnavailableException : Exception()

/**
 * The Pro-unlock purchase the app needs. A port — an abstraction `:core`
 * owns but does not implement — so `:app` can back it with Play's
 * `BillingClient` and tests can back it with a scripted double, mirroring
 * the iOS app's `PurchaseProviding` protocol.
 *
 * The port lands now (M6, alongside its first caller, `PurchaseController`)
 * even though the live adapter doesn't land until M8 — the same split M2's
 * `MonotonicClock` went through before M5 gave it a live adapter. `M8` also
 * needs the physical-device milestone gate that the port itself does not.
 */
interface Purchases {
    suspend fun isPurchased(): Boolean
    suspend fun localizedPrice(): String?
    suspend fun purchase(): PurchaseOutcome
    suspend fun restore(): Boolean

    /** Entitlement changes as they happen (e.g. a family-shared purchase granted elsewhere). */
    fun entitlementUpdates(): Flow<Boolean>
}
