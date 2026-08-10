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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * [isPurchased] and a fresh [purchase] both acknowledge defensively and
 * check the acknowledgement call's own result — a purchase only reads as
 * owned once acknowledgement has actually succeeded, never on the
 * optimistic assumption that it did; [restore] delegates straight to
 * [isPurchased] for the same reason.
 *
 * [isPurchased], [localizedPrice], and [restore] never throw — matching the
 * [Purchases] port's contract, where [purchase] is the only method
 * documented to throw [PurchaseUnavailableException]. A failed
 * `BillingClient` connection (no Play Store, no network, service
 * disconnected) is a normal, expected condition here, not exceptional: it
 * degrades to "not purchased" / no price rather than propagating, which
 * matters most for [isPurchased] since [PurchaseController.start] calls it
 * from `CoffeeGramsApplication.onCreate` — an uncaught exception there
 * would crash the app on launch.
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
    private val connectionMutex = Mutex()

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach() {
        this.activity = null
    }

    /**
     * Closes the `BillingClient` connection. Not wired to any lifecycle
     * callback — [LivePurchases] is an application-scoped singleton living
     * for the process lifetime, and `Application.onTerminate` is never
     * called in production, so there is no reliable hook to call this
     * automatically. Exists for completeness (and tests) rather than an
     * active shutdown path.
     */
    fun close() {
        billingClient.endConnection()
    }

    /**
     * Connects if needed and reports whether the client is ready — never
     * throws. [connectionMutex] serializes concurrent callers so two
     * suspended calls can't each start their own `startConnection()`.
     */
    private suspend fun ensureConnected(): Boolean {
        if (connected) return true
        connectionMutex.withLock {
            if (connected) return@withLock
            val billingResult = suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(billingResult) { _, _, _ -> }
                    }

                    override fun onBillingServiceDisconnected() {
                        connected = false
                    }
                })
            }
            connected = billingResult.responseCode == BillingResponseCode.OK
        }
        return connected
    }

    private suspend fun proPurchase(): Purchase? {
        if (!ensureConnected()) return null
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        return billingClient.queryPurchasesAsync(params).purchasesList.firstOrNull { PRO_PRODUCT_ID in it.products }
    }

    /** Returns whether [purchase] is acknowledged once this returns — checking the ack call's own result, not assuming it succeeded. */
    private suspend fun acknowledgeIfNeeded(purchase: Purchase): Boolean {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        if (purchase.isAcknowledged) return true
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        return billingClient.acknowledgePurchase(params).responseCode == BillingResponseCode.OK
    }

    override suspend fun isPurchased(): Boolean {
        val purchase = proPurchase() ?: return false
        return acknowledgeIfNeeded(purchase)
    }

    override suspend fun localizedPrice(): String? {
        if (!ensureConnected()) return null
        val result = billingClient.queryProductDetails(proProductDetailsParams())
        return result.productDetailsList
            ?.firstOrNull { it.productId == PRO_PRODUCT_ID }
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }

    override suspend fun purchase(): PurchaseOutcome {
        if (!ensureConnected()) throw PurchaseUnavailableException()
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
     *
     * A `PURCHASED` classification only resolves as [PurchaseOutcome.PURCHASED]
     * once [acknowledgeIfNeeded] actually confirms acknowledgement — reporting
     * success on the optimistic assumption it worked is exactly how a
     * purchase goes unacknowledged and gets auto-refunded (see the class
     * doc comment). An acknowledgement failure surfaces as
     * [PurchaseUnavailableException]; the purchase itself isn't lost —
     * Play still has it, and the next [isPurchased]/[restore] call retries
     * the acknowledgement.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val continuation = pendingPurchase
        pendingPurchase = null

        val purchase = purchases?.firstOrNull { PRO_PRODUCT_ID in it.products }
        when (val classified = classifyPurchaseResponse(billingResult.responseCode, purchase?.purchaseState)) {
            is BillingOutcome.Result -> if (classified.outcome == PurchaseOutcome.PURCHASED) {
                scope.launch {
                    val acknowledged = (purchase ?: proPurchase())?.let { acknowledgeIfNeeded(it) } ?: false
                    if (acknowledged) {
                        entitlementUpdates.tryEmit(true)
                        continuation?.resume(PurchaseOutcome.PURCHASED) { _, _, _ -> }
                    } else {
                        continuation?.resumeWithException(PurchaseUnavailableException())
                    }
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
