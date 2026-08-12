package com.jrlabapps.coffeegrams

import android.app.Application
import com.jrlabapps.coffeegrams.data.BrewLogDatabase
import com.jrlabapps.coffeegrams.data.RoomBrewLogStore
import com.jrlabapps.coffeegrams.platform.LiveHaptics
import com.jrlabapps.coffeegrams.platform.LiveMonotonicClock
import com.jrlabapps.coffeegrams.platform.LiveNotificationScheduler
import com.jrlabapps.coffeegrams.platform.LivePurchases
import com.jrlabapps.coffeegrams.viewmodel.PurchaseController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * This is where the manual dependency graph is assembled from M4 onward — Room
 * database, the platform adapters (clock, haptics, notifications) and the billing
 * client. Deliberately no DI framework: constructor injection by hand matches the
 * iOS app and keeps the dependency list at zero third-party SDKs.
 */
class CoffeeGramsApplication : Application() {
    /** Lives for the whole process — [PurchaseController.start] suspends forever, matching its lifetime. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val database by lazy { BrewLogDatabase.build(this) }

    val brewLogStore by lazy { RoomBrewLogStore(database.brewLogDao()) }

    val clock by lazy { LiveMonotonicClock() }
    val haptics by lazy { LiveHaptics(this) }
    val notificationScheduler by lazy { LiveNotificationScheduler(this) }

    /**
     * Exposed as the concrete type (not just [com.jrlabapps.coffeegrams.core.Purchases])
     * so [MainActivity] can reach [LivePurchases.attach]/[LivePurchases.detach] —
     * `launchBillingFlow` needs a foreground `Activity`, which the
     * provider-agnostic `PurchaseController` deliberately has no reason to
     * know about.
     */
    val livePurchases by lazy { LivePurchases(this) }

    /**
     * `PurchaseController` itself is not a `ViewModel`: it's meant for a
     * single instance shared across every screen, matching iOS's one
     * environment-injected instance.
     */
    val purchaseController by lazy { PurchaseController(livePurchases) }

    override fun onCreate() {
        super.onCreate()
        // Loads the initial entitlement/price, then keeps collecting
        // entitlementUpdates() for as long as the app runs — matches iOS's
        // single `.task { await purchases.start() }` call at the app root.
        applicationScope.launch { purchaseController.start() }
    }
}
