package com.jrlabapps.coffeegrams.ui.espresso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.EspressoTarget
import com.jrlabapps.coffeegrams.core.ShotTimingState
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.EspressoShotViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The espresso-shot stopwatch. Timing-state colors (building/on
 * target/running long) are deliberately outside the brand palette —
 * semantic, not decorative — and every color use is paired with a caption
 * carrying the same meaning, never color alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EspressoShotScreen(target: EspressoTarget, modifier: Modifier = Modifier) {
    val application = currentApplication()
    val viewModel: EspressoShotViewModel = viewModel(
        factory = viewModelFactory { initializer { EspressoShotViewModel(target, application.clock) } },
    )

    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val hasStarted = isRunning || elapsedSeconds > 0

    var savedToLog by remember { mutableStateOf(false) }
    // isSaving flips synchronously on the first tap, before savedToLog has a
    // chance to update -- guards against a rapid double-tap enqueuing two
    // BrewLogEntry writes while the suspend Room write is in flight.
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val brewLogStore = application.brewLogStore

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(BrewMethod.ESPRESSO.displayName) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
        ) {
            MetricCard(doseGrams = target.doseGrams, targetYieldGrams = target.targetYieldGrams)

            val timingState = ShotTimingState.classify(elapsedSeconds, target.shotTimeRange)
            val (caption, tint) = when {
                !hasStarted -> stringResource(R.string.espresso_status_ready) to MaterialTheme.colorScheme.onSurfaceVariant
                timingState == ShotTimingState.TOO_EARLY -> stringResource(R.string.espresso_status_building) to AmberTiming
                timingState == ShotTimingState.ON_TARGET -> stringResource(R.string.espresso_status_on_target) to GreenTiming
                else -> stringResource(R.string.espresso_status_running_long) to RedTiming
            }
            val stopwatchDescription = stringResource(R.string.espresso_stopwatch_description, caption, elapsedSeconds)

            Column(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = stopwatchDescription },
            ) {
                Text(caption, style = MaterialTheme.typography.labelLarge, color = tint)
                Text(
                    stringResource(R.string.espresso_seconds_value, elapsedSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (hasStarted) tint else MaterialTheme.colorScheme.onSurface,
                )
            }

            Button(onClick = { viewModel.startOrStop() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isRunning) stringResource(R.string.espresso_stop) else stringResource(R.string.espresso_start))
            }

            if (hasStarted && !isRunning) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                    if (savedToLog) {
                        Text(stringResource(R.string.espresso_saved_to_log))
                    } else {
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    brewLogStore.add(
                                        BrewLogEntry(
                                            method = BrewMethod.ESPRESSO,
                                            doseGrams = target.doseGrams,
                                            waterGrams = target.targetYieldGrams,
                                            ratio = target.ratio,
                                            shotSeconds = elapsedSeconds,
                                        ),
                                    )
                                    savedToLog = true
                                }
                            },
                        ) { Text(stringResource(R.string.espresso_save_to_log)) }
                    }
                    TextButton(onClick = { viewModel.reset(); savedToLog = false; isSaving = false }) {
                        Text(stringResource(R.string.espresso_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(doseGrams: Double, targetYieldGrams: Double) {
    Surface(
        shape = RoundedCornerShape(Spacing.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.cardPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricColumn(stringResource(R.string.espresso_dose_label), formatGrams(doseGrams))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            MetricColumn(stringResource(R.string.espresso_yield_label), formatGrams(targetYieldGrams))
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value g", style = MaterialTheme.typography.titleLarge)
    }
}

private val AmberTiming = Color(0xFFB26A00)
private val GreenTiming = Color(0xFF2E7D32)
private val RedTiming = Color(0xFFC62828)

private fun formatGrams(value: Double): String =
    if (value == Math.floor(value)) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
