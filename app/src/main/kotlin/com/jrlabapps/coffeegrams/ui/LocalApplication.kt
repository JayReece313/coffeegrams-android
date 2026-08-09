package com.jrlabapps.coffeegrams.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.jrlabapps.coffeegrams.CoffeeGramsApplication

/**
 * The app-wide dependency graph, as constructed by [CoffeeGramsApplication].
 * No DI framework — every screen reaches its ViewModel's constructor args
 * (clock, haptics, brewLogStore, notificationScheduler, purchaseController)
 * through this one cast, rather than repeating it at each call site.
 */
@Composable
fun currentApplication(): CoffeeGramsApplication = LocalContext.current.applicationContext as CoffeeGramsApplication
