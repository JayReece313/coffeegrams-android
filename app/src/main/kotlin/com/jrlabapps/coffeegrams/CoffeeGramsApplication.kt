package com.jrlabapps.coffeegrams

import android.app.Application
import com.jrlabapps.coffeegrams.data.BrewLogDatabase
import com.jrlabapps.coffeegrams.data.RoomBrewLogStore

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

    /** No ViewModel consumes this yet (M6) — constructed here so it's ready when one does. */
    val brewLogStore by lazy { RoomBrewLogStore(database.brewLogDao()) }
}
