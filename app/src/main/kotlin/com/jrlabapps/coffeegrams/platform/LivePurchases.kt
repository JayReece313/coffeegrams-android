package com.jrlabapps.coffeegrams.platform

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.jrlabapps.coffeegrams.core.PurchaseOutcome
import com.jrlabapps.coffeegrams.core.PurchaseUnavailableException
import com.jrlabapps.coffeegrams.core.Purchases
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

private const val PRO_PRODUCT_ID = "com.jrlabapps.coffeegrams.pro"

/** What [classifyPurchaseResponse] decided about a `BillingClient` callback. */
internal sealed interface BillingOutcome {
    data class Result(val outcome: PurchaseOutcome) : BillingOutcome
    data object Unavailable : BillingOutcome
}

/**
 * Maps a raw `BillingResponseCode` (+ the matching purchase's state, when there is
 * one) to what [Purchases.purchase] promises its caller. Pulled out as a pure
 * function of plain `Int`s — not a live [Purchase] — specifically so it's
 * unit-testable on the JVM: this project's unit tests don't set
 * `isReturnDefaultValues`, so touching an Android-adjacent class like [Purchase]
 * (its constructor parses JSON) directly in `:app:testDebugUnitTest` isn't safe,
 * but `BillingResponseCode`/`Purchase.PurchaseState` are plain `Int` constants.
 */
internal fun classifyPurchaseResponse(responseCode: Int, purchaseState: Int?): BillingOutcome = when (responseCode) {
    BillingResponseCode.USER_CANCELED -> BillingOutcome.Result(PurchaseOutcome.CANCELLED)
    BillingResponseCode.ITEM_ALREADY_OWNED -> BillingOutcome.Result(PurchaseOutcome.PURCHASED)
    BillingResponseCode.OK -> when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> BillingOutcome.Result(PurchaseOutcome.PURCHASED)
        Purchase.PurchaseState.PENDING -> BillingOutcome.Result(PurchaseOutcome.PENDING)
        else -> BillingOutcome.Unavailable
    }
    else -> BillingOutcome.Unavailable
}

/**
 * The live [Purchases] adapter, backed by Play's `BillingClient`.
 *
 * Needs a foreground [Activity] to launch the billing flow — unlike iOS's
 * StoreKit call, `launchBillingFlow` takes one directly, and the [Purchases]
 * port (ported 1:1 from iOS in M6) doesn't carry one. This app is
 * single-`Activity`, so [attach]/[detach] (wired from `MainActivity`'s
 * `onStart`/`onStop`) is the same shape Play's own `BillingClient` samples
 * use, not a new pattern; the field is cleared on [detach] so a destroyed
 * `Activity` is never held past a configuration change.
 *
 * Acknowledgement is mandatory on Play — unlike StoreKit's `finish()`, an
 * unacknowledged purchase is auto-refunded within days (see `testing.md`).
 * [isPurchased] and a fresh [purchase] both acknowledge defensively, so a
 * killed process or a missed callback can never leave a purchase to expire
 * unacknowledged; [restore] delegates straight to [isPurchased] for the
 * same reason.
 */
class LivePurchases(context: Context) : Purchases, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var activity: Activity? = null

    private val entitlementUpdates = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /**
     * The one purchase attempt the paywall allows in flight at a time — its
     * buy button disables while
     * [PurchaseController.isWorking][com.jrlabapps.coffeegrams.viewmodel.PurchaseController.isWorking]
     * is true — so a single field is enough to correlate [purchase]'s
     * suspend call with the shared [onPurchasesUpdated] callback.
     */
    private var pendingPurchase: CancellableContinuation<PurchaseOutcome>? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var connected = false

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach() {
        this.activity = null
    }

    private suspend fun ensureConnected() {
        if (connected) return
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connected = billingResult.responseCode == BillingResponseCode.OK
                    if (continuation.isActive) continuation.resume(Unit) { _, _, _ -> }
                }

                override fun onBillingServiceDisconnected() {
                    connected = false
                }
            })
        }
    }

    private suspend fun proPurchase(): Purchase? {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        return billingClient.queryPurchasesAsync(params).purchasesList.firstOrNull { PRO_PRODUCT_ID in it.products }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params)
        }
    }

    override suspend fun isPurchased(): Boolean {
        val purchase = proPurchase() ?: return false
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        acknowledgeIfNeeded(purchase)
        return true
    }

    override suspend fun localizedPrice(): String? {
        ensureConnected()
        val result = billingClient.queryProductDetails(proProductDetailsParams())
        return result.productDetailsList
            ?.firstOrNull { it.productId == PRO_PRODUCT_ID }
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }

    override suspend fun purchase(): PurchaseOutcome {
        ensureConnected()
        val launchingActivity = activity ?: throw PurchaseUnavailableException()
        val productDetails = billingClient.queryProductDetails(proProductDetailsParams())
            .productDetailsList
            ?.firstOrNull { it.productId == PRO_PRODUCT_ID }
            ?: throw PurchaseUnavailableException()

        val offerParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(offerParams))
            .build()

        return suspendCancellableCoroutine { continuation ->
            pendingPurchase = continuation
            continuation.invokeOnCancellation { pendingPurchase = null }
            val launchResult = billingClient.launchBillingFlow(launchingActivity, flowParams)
            if (launchResult.responseCode != BillingResponseCode.OK) {
                pendingPurchase = null
                continuation.resumeWithException(PurchaseUnavailableException())
            }
        }
    }

    override suspend fun restore(): Boolean = isPurchased()

    override fun entitlementUpdates(): Flow<Boolean> = entitlementUpdates.asSharedFlow()

    /**
     * Fires for both [purchase]'s own billing-flow launch and any later,
     * out-of-band resolution of a purchase that started `PENDING` — the
     * latter arrives with no [pendingPurchase] waiting, which is why
     * [entitlementUpdates] is pushed unconditionally on a purchased
     * outcome rather than only alongside a continuation resume.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val continuation = pendingPurchase
        pendingPurchase = null

        val purchase = purchases?.firstOrNull { PRO_PRODUCT_ID in it.products }
        when (val classified = classifyPurchaseResponse(billingResult.responseCode, purchase?.purchaseState)) {
            is BillingOutcome.Result -> if (classified.outcome == PurchaseOutcome.PURCHASED) {
                scope.launch {
                    (purchase ?: proPurchase())?.let { acknowledgeIfNeeded(it) }
                    entitlementUpdates.tryEmit(true)
                    continuation?.resume(classified.outcome) { _, _, _ -> }
                }
            } else {
                continuation?.resume(classified.outcome) { _, _, _ -> }
            }
            BillingOutcome.Unavailable -> continuation?.resumeWithException(PurchaseUnavailableException())
        }
    }

    private fun proProductDetailsParams(): QueryProductDetailsParams {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(ProductType.INAPP)
            .build()
        return QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
    }
}
