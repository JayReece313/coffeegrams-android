package com.jrlabapps.coffeegrams.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.ui.calculator.CalculatorScreen
import com.jrlabapps.coffeegrams.ui.guidedbrew.BrewSessionScreen
import com.jrlabapps.coffeegrams.ui.methodpicker.MethodPickerScreen
import kotlinx.serialization.Serializable

/** The method picker — the app's navigation root. */
@Serializable
object MethodPickerRoute

/**
 * [method] is [BrewMethod.rawValue], not the enum itself — keeps `:core`
 * free of a `kotlinx-serialization` dependency it would otherwise only need
 * for `:app`'s routing concern. Converted back via [BrewMethod.fromRawValue].
 */
@Serializable
data class CalculatorRoute(val method: String)

/** Routes to [BrewSessionScreen], which branches by method — mirrors iOS's `BrewSessionView`. */
@Serializable
data class BrewSessionRoute(val method: String, val doseGrams: Double, val ratio: Double)

@Composable
fun CoffeeGramsNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MethodPickerRoute) {
        composable<MethodPickerRoute> {
            MethodPickerScreen(
                onMethodSelected = { method -> navController.navigate(CalculatorRoute(method.rawValue)) },
            )
        }
        composable<CalculatorRoute> { backStackEntry: NavBackStackEntry ->
            val route: CalculatorRoute = backStackEntry.toRoute()
            val method = BrewMethod.fromRawValue(route.method)
            CalculatorScreen(
                method = method,
                onStartBrew = { doseGrams, ratio ->
                    navController.navigate(BrewSessionRoute(method.rawValue, doseGrams, ratio))
                },
            )
        }
        composable<BrewSessionRoute> { backStackEntry: NavBackStackEntry ->
            val route: BrewSessionRoute = backStackEntry.toRoute()
            BrewSessionScreen(
                method = BrewMethod.fromRawValue(route.method),
                doseGrams = route.doseGrams,
                ratio = route.ratio,
            )
        }
    }
}
