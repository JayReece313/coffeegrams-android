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
import com.jrlabapps.coffeegrams.ui.log.LogDetailScreen
import com.jrlabapps.coffeegrams.ui.log.LogScreen
import com.jrlabapps.coffeegrams.ui.methodpicker.MethodPickerScreen
import kotlinx.serialization.Serializable
import java.util.UUID

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

/** The brew history list — mirrors iOS's `LogView`, reached from the method picker's toolbar. */
@Serializable
object LogRoute

/** [entryId] is [UUID.toString], the same string-route pattern [CalculatorRoute] uses for [BrewMethod]. */
@Serializable
data class LogDetailRoute(val entryId: String)

@Composable
fun CoffeeGramsNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MethodPickerRoute) {
        composable<MethodPickerRoute> {
            MethodPickerScreen(
                onMethodSelected = { method -> navController.navigate(CalculatorRoute(method.rawValue)) },
                onViewLog = { navController.navigate(LogRoute) },
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
        composable<LogRoute> {
            LogScreen(
                onEntrySelected = { id -> navController.navigate(LogDetailRoute(id.toString())) },
            )
        }
        composable<LogDetailRoute> { backStackEntry: NavBackStackEntry ->
            val route: LogDetailRoute = backStackEntry.toRoute()
            LogDetailScreen(
                entryId = UUID.fromString(route.entryId),
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
