package com.jrlabapps.coffeegrams

import android.app.Application
import com.jrlabapps.coffeegrams.data.BrewLogDatabase
import com.jrlabapps.coffeegrams.data.RoomBrewLogStore
import com.jrlabapps.coffeegrams.platform.LiveHaptics
import com.jrlabapps.coffeegrams.platform.LiveMonotonicClock
import com.jrlabapps.coffeegrams.platform.LiveNotificationScheduler
import com.jrlabapps.coffeegrams.platform.UnavailablePurchases
import com.jrlabapps.coffeegrams.viewmodel.PurchaseController

/**
 * Application entry point.
 *
 * This is where the manual dependency graph is assembled from M4 onward — Room
 * database, the platform adapters (clock, haptics, notifications) and the billing
 * client. Deliberately no DI framework: constructor injection by hand matches the
 * iOS app and keeps the dependency list at zero third-party SDKs.
 */
class CoffeeGramsApplication : Application() {
    private val database by lazy { BrewLogDatabase.build(this) }

    val brewLogStore by lazy { RoomBrewLogStore(database.brewLogDao()) }

    val clock by lazy { LiveMonotonicClock() }
    val haptics by lazy { LiveHaptics(this) }
    val notificationScheduler by lazy { LiveNotificationScheduler(this) }

    /**
     * Backed by [UnavailablePurchases] until M8 wires up the real Play
     * `BillingClient` adapter — Pro reads as locked everywhere until then.
     * `PurchaseController` itself is not a `ViewModel`: it's meant for a
     * single instance shared across every screen, matching iOS's one
     * environment-injected instance.
     */
    val purchaseController by lazy { PurchaseController(UnavailablePurchases()) }
}
