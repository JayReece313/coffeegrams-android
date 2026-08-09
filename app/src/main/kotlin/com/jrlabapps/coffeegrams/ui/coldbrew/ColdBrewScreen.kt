package com.jrlabapps.coffeegrams.ui.coldbrew

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.ColdBrewViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The cold-brew screen. No live timer (spec) — starting a steep schedules a
 * local reminder and saves to the log immediately, matching iOS's
 * `Task { await vm.startSteep(); saveToLog() }` sequencing exactly.
 *
 * The permission choreography [ColdBrewViewModel.startSteep]'s own KDoc
 * requires: check [ColdBrewViewModel.canScheduleReminders] first (a state
 * check only — Android can't prompt from the ViewModel), and only if that's
 * false does this screen show the real system permission dialog via
 * [rememberLauncherForActivityResult], then pass the result in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColdBrewScreen(doseGrams: Double, ratio: Double, modifier: Modifier = Modifier) {
    val application = currentApplication()
    val viewModel: ColdBrewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ColdBrewViewModel(doseGrams, ratio, application.notificationScheduler) }
        },
    )

    val steepHours by viewModel.steepHours.collectAsStateWithLifecycle()
    val reminderState by viewModel.reminderState.collectAsStateWithLifecycle()
    // isStarting flips synchronously on the first tap, before reminderState
    // has a chance to update -- guards the CTA against a rapid double-tap
    // enqueuing two BrewLogEntry writes while the suspend work is in flight.
    var isStarting by remember { mutableStateOf(false) }
    val started = isStarting || reminderState !is ColdBrewViewModel.ReminderState.Idle

    val scope = rememberCoroutineScope()
    val brewLogStore = application.brewLogStore

    val onPermissionResolved: suspend (Boolean) -> Unit = { granted ->
        viewModel.startSteep(granted)
        brewLogStore.add(
            BrewLogEntry(
                method = BrewMethod.COLD_BREW,
                doseGrams = doseGrams,
                waterGrams = viewModel.waterGrams,
                ratio = ratio,
            ),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Don't trust the raw grant result: POST_NOTIFICATIONS can already be
        // granted while the user has notifications disabled for the app in
        // Settings, in which case the launcher fires without a dialog and
        // reports `true` even though nothing will actually be delivered.
        // Re-check the effective, OS-reported capability instead.
        scope.launch { onPermissionResolved(viewModel.canScheduleReminders()) }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(BrewMethod.COLD_BREW.displayName) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
        ) {
            MetricCard(doseGrams = doseGrams, waterGrams = viewModel.waterGrams, ratio = ratio)

            Column {
                Text(stringResource(R.string.cold_brew_steep_time_caption), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.cold_brew_steep_hours_value, steepHours.roundToInt()),
                    style = MaterialTheme.typography.displaySmall,
                )
                Slider(
                    value = steepHours.toFloat(),
                    onValueChange = { viewModel.setSteepHours(it.toDouble()) },
                    valueRange = 12f..24f,
                    steps = 11,
                    enabled = !started,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("12h", style = MaterialTheme.typography.labelSmall)
                    Text("24h", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(stringResource(R.string.cold_brew_instructions), style = MaterialTheme.typography.bodyMedium)

            if (started) {
                val message = when (val state = reminderState) {
                    is ColdBrewViewModel.ReminderState.Scheduled -> {
                        stringResource(R.string.cold_brew_reminder_scheduled, state.hours)
                    }
                    ColdBrewViewModel.ReminderState.Denied -> stringResource(R.string.cold_brew_reminder_denied)
                    ColdBrewViewModel.ReminderState.Idle -> stringResource(R.string.cold_brew_saved_to_log)
                }
                Text(message, style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = {
                        isStarting = true
                        if (viewModel.canScheduleReminders()) {
                            scope.launch { onPermissionResolved(true) }
                        } else {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cold_brew_start_steep))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(doseGrams: Double, waterGrams: Double, ratio: Double) {
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
            MetricColumn(stringResource(R.string.cold_brew_coffee_label), "${formatGrams(doseGrams)} g")
            MetricColumn(stringResource(R.string.cold_brew_water_label), "${formatGrams(waterGrams)} g")
            MetricColumn(stringResource(R.string.cold_brew_ratio_label_short), "1:${formatGrams(ratio)}")
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

private fun formatGrams(value: Double): String =
    if (value == Math.floor(value)) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
