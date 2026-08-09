package com.jrlabapps.coffeegrams.ui.guidedbrew

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.BrewTimelineBuilder
import com.jrlabapps.coffeegrams.ui.coldbrew.ColdBrewScreen
import com.jrlabapps.coffeegrams.ui.espresso.EspressoShotScreen

/**
 * Routes to the right brewing screen for [method] — mirrors iOS's
 * `BrewSessionView`, which fans one "Start Brewing" destination out into
 * three different screen types depending on the method's physical category.
 */
@Composable
fun BrewSessionScreen(method: BrewMethod, doseGrams: Double, ratio: Double, modifier: Modifier = Modifier) {
    when (method) {
        BrewMethod.ESPRESSO -> {
            val target = BrewTimelineBuilder.buildEspressoTarget(doseGrams, ratio)
            EspressoShotScreen(target = target, modifier = modifier)
        }
        BrewMethod.COLD_BREW -> {
            ColdBrewScreen(doseGrams = doseGrams, ratio = ratio, modifier = modifier)
        }
        else -> {
            val timeline = BrewTimelineBuilder.timeline(method, doseGrams, ratio)
            if (timeline != null) {
                GuidedBrewScreen(timeline = timeline, doseGrams = doseGrams, ratio = ratio, modifier = modifier)
            }
        }
    }
}
