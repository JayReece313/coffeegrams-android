package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import com.jrlabapps.coffeegrams.core.Purchases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide entitlement state. One instance is meant to be constructed once
 * — in `CoffeeGramsApplication`, once M8 supplies a live [Purchases]
 * adapter — and shared across screens. That's why this is a plain class,
 * not an `androidx.lifecycle.ViewModel`: a real ViewModel is scoped to an
 * Activity/`NavBackStackEntry`, and this needs exactly one instance for the
 * whole app lifetime, matching iOS's single environment-injected instance.
 */
class PurchaseController(private val provider: Purchases) {

    private val _isPremiumUnlocked = MutableStateFlow(false)
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val _priceText = MutableStateFlow<String?>(null)
    val priceText: StateFlow<String?> = _priceText.asStateFlow()

    /** True while a purchase or restore is in flight (to disable buttons). */
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    /**
     * Gate for a brew method: the free method is always available;
     * everything else needs the Pro unlock. The free/paid split lives in
     * the domain ([BrewMethod.isFreeTier]), so this stays a one-liner.
     */
    fun canAccess(method: BrewMethod): Boolean = method.isFreeTier || _isPremiumUnlocked.value

    /**
     * Load current entitlement + price, then keep listening for
     * out-of-band changes. Call once from the app root's own long-lived
     * scope — this suspends for as long as [Purchases.entitlementUpdates]
     * stays open, exactly like iOS's `.task`-driven `for await` loop.
     */
    suspend fun start() {
        _isPremiumUnlocked.value = provider.isPurchased()
        _priceText.value = provider.localizedPrice()
        provider.entitlementUpdates().collect { unlocked -> _isPremiumUnlocked.value = unlocked }
    }

    /**
     * Returns true if the purchase completed. iOS models user-cancellation
     * as a *returned* outcome, not a thrown error, so anything the `catch`
     * sees here is a genuine provider failure — the `catch (Exception)`,
     * not `catch (Throwable)`, is deliberate too: cancellation is delivered
     * as an `Exception` subtype and must keep propagating, not be
     * swallowed into a normal `false` return.
     */
    suspend fun purchase(): Boolean {
        _isWorking.value = true
        try {
            val outcome = provider.purchase()
            if (outcome == PurchaseOutcome.PURCHASED) _isPremiumUnlocked.value = true
            return outcome == PurchaseOutcome.PURCHASED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            _isWorking.value = false
        }
    }

    suspend fun restore() {
        _isWorking.value = true
        try {
            _isPremiumUnlocked.value = provider.restore()
        } finally {
            _isWorking.value = false
        }
    }
}
