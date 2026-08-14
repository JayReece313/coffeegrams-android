package com.jrlabapps.coffeegrams.ui.guidedbrew

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewStep
import com.jrlabapps.coffeegrams.core.BrewTimeline
import com.jrlabapps.coffeegrams.core.BrewTimerPhase
import com.jrlabapps.coffeegrams.design.TimeFormat
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.GuidedBrewViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The guided-brew timer screen: a big timer numeral, the step list, and
 * live controls. The tick loop lives entirely in [GuidedBrewViewModel] —
 * this screen only observes its `StateFlow`s, never drives ticking itself.
 *
 * [phase] is threaded explicitly into [TimerBlock]/[Controls] (rather than
 * having them call `viewModel.isRunning` etc. with nothing forcing a real
 * Compose read) because some transitions — e.g. pausing mid-step — change
 * only `phase` and leave every other collected `StateFlow` unchanged, so a
 * composable that never reads `phase` itself would miss them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedBrewScreen(timeline: BrewTimeline, doseGrams: Double, ratio: Double, modifier: Modifier = Modifier) {
    val application = currentApplication()
    val viewModel: GuidedBrewViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GuidedBrewViewModel(
                    timeline,
                    application.clock,
                    application.haptics,
                    application.brewSessionNotifier,
                    application.notificationScheduler,
                )
            }
        },
    )
    // Doesn't gate start() on the result — see GuidedBrewViewModel's doc
    // comment for why the timer itself doesn't need to wait on this.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val fractionComplete by viewModel.fractionComplete.collectAsStateWithLifecycle()
    val overrunSeconds by viewModel.overrunSeconds.collectAsStateWithLifecycle()
    val totalElapsedSeconds by viewModel.totalElapsedSeconds.collectAsStateWithLifecycle()

    var savedToLog by remember { mutableStateOf(false) }
    // isSaving flips synchronously on the first tap, before savedToLog has a
    // chance to update -- guards against a rapid double-tap enqueuing two
    // BrewLogEntry writes while the suspend Room write is in flight.
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val brewLogStore = application.brewLogStore

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var confirmedLeave by remember { mutableStateOf(false) }
    val hasStarted = phase != BrewTimerPhase.IDLE && phase != BrewTimerPhase.COMPLETED

    BackHandler(enabled = hasStarted && !confirmedLeave) {
        showLeaveConfirmation = true
    }
    LaunchedEffect(confirmedLeave) {
        if (confirmedLeave) backDispatcher?.onBackPressed()
    }

    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = { Text(stringResource(R.string.guided_brew_leave_title)) },
            text = { Text(stringResource(R.string.guided_brew_leave_message)) },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirmation = false; confirmedLeave = true }) {
                    Text(stringResource(R.string.guided_brew_leave_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text(stringResource(R.string.guided_brew_leave_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(timeline.method.displayName) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
        ) {
            LinearProgressIndicator(progress = { fractionComplete.toFloat() }, modifier = Modifier.fillMaxWidth())

            TimerBlock(
                viewModel = viewModel,
                phase = phase,
                remainingSeconds = remainingSeconds,
                overrunSeconds = overrunSeconds,
                totalElapsedSeconds = totalElapsedSeconds,
                hasStarted = hasStarted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                timeline.steps.forEachIndexed { index, step ->
                    val state = when {
                        phase == BrewTimerPhase.COMPLETED || index < currentStepIndex -> StepRowState.DONE
                        index == currentStepIndex && phase != BrewTimerPhase.IDLE -> StepRowState.CURRENT
                        else -> StepRowState.UPCOMING
                    }
                    StepRow(step = step, state = state)
                }
            }

            Controls(
                viewModel = viewModel,
                phase = phase,
                savedToLog = savedToLog,
                isSaving = isSaving,
                onStart = {
                    if (!viewModel.canShowSessionNotification()) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.start()
                },
                onSave = {
                    isSaving = true
                    scope.launch {
                        brewLogStore.add(
                            BrewLogEntry(
                                method = timeline.method,
                                doseGrams = doseGrams,
                                waterGrams = timeline.totalWaterGrams,
                                ratio = ratio,
                                plannedSeconds = viewModel.plannedSeconds,
                                actualSeconds = viewModel.actualSeconds,
                            ),
                        )
                        savedToLog = true
                    }
                },
                onBrewAgain = {
                    viewModel.reset()
                    savedToLog = false
                    isSaving = false
                },
            )
        }
    }
}

@Composable
private fun TimerBlock(
    viewModel: GuidedBrewViewModel,
    phase: BrewTimerPhase,
    remainingSeconds: Int,
    overrunSeconds: Int?,
    totalElapsedSeconds: Int,
    hasStarted: Boolean,
) {
    val isPaused = phase == BrewTimerPhase.PAUSED
    val isRunning = phase == BrewTimerPhase.RUNNING
    val isAwaitingManualAdvance = phase == BrewTimerPhase.AWAITING_MANUAL_ADVANCE
    val isFinished = phase == BrewTimerPhase.COMPLETED

    val statusCaption = when {
        isFinished -> stringResource(R.string.guided_brew_status_done)
        isPaused -> stringResource(R.string.guided_brew_status_paused)
        overrunSeconds != null && isAwaitingManualAdvance -> stringResource(R.string.guided_brew_status_tap_done)
        overrunSeconds != null -> stringResource(R.string.guided_brew_status_over_target)
        isRunning -> stringResource(R.string.guided_brew_status_running)
        else -> stringResource(R.string.guided_brew_status_ready)
    }

    val timerActiveColor = MaterialTheme.colorScheme.tertiary
    val idleColor = MaterialTheme.colorScheme.onSurface
    val (timerText, timerColor) = when {
        overrunSeconds != null -> "+${TimeFormat.clock(overrunSeconds)}" to (if (isPaused) idleColor else timerActiveColor)
        isAwaitingManualAdvance -> stringResource(R.string.guided_brew_your_move) to idleColor
        else -> TimeFormat.clock(remainingSeconds) to (if (isRunning) timerActiveColor else idleColor)
    }

    val instructionText = if (isFinished) {
        stringResource(R.string.guided_brew_complete_message)
    } else {
        viewModel.currentStep?.title
    }

    val spokenTotal = TimeFormat.spoken(totalElapsedSeconds)
    val totalDescription = stringResource(R.string.guided_brew_total_elapsed_description, spokenTotal)

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(statusCaption, timerText, instructionText).joinToString(", ")
        },
    ) {
        Text(statusCaption, style = MaterialTheme.typography.labelLarge)
        Text(timerText, style = MaterialTheme.typography.displayLarge, color = timerColor)
        instructionText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (hasStarted || isFinished) {
            Text(
                stringResource(R.string.guided_brew_total_elapsed, TimeFormat.clock(totalElapsedSeconds)),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.semantics { contentDescription = totalDescription },
            )
        }
    }
}

private enum class StepRowState { DONE, CURRENT, UPCOMING }

@Composable
private fun StepRow(step: BrewStep, state: StepRowState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val icon = when (state) {
            StepRowState.DONE -> Icons.Filled.CheckCircle
            StepRowState.CURRENT -> Icons.Filled.Circle
            StepRowState.UPCOMING -> Icons.Outlined.Circle
        }
        val tint = when (state) {
            StepRowState.DONE -> MaterialTheme.colorScheme.primary
            StepRowState.CURRENT -> MaterialTheme.colorScheme.tertiary
            StepRowState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(Spacing.smallSpacing))
        Text(
            step.title,
            style = if (state == StepRowState.CURRENT) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier.weight(1f),
        )
        stepDetailText(step)?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun Controls(
    viewModel: GuidedBrewViewModel,
    phase: BrewTimerPhase,
    savedToLog: Boolean,
    isSaving: Boolean,
    onStart: () -> Unit,
    onSave: () -> Unit,
    onBrewAgain: () -> Unit,
) {
    val isPaused = phase == BrewTimerPhase.PAUSED
    val isRunning = phase == BrewTimerPhase.RUNNING

    when (phase) {
        BrewTimerPhase.COMPLETED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                if (savedToLog) {
                    Text(stringResource(R.string.guided_brew_saved_to_log))
                } else {
                    Button(onClick = onSave, enabled = !isSaving) { Text(stringResource(R.string.guided_brew_save_to_log)) }
                }
                OutlinedButton(onClick = onBrewAgain) { Text(stringResource(R.string.guided_brew_brew_again)) }
            }
        }
        BrewTimerPhase.IDLE -> {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.guided_brew_start_timer))
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                viewModel.advanceTitle?.let { title ->
                    Button(onClick = { viewModel.resolveCurrentStep() }, modifier = Modifier.fillMaxWidth()) {
                        Text(title)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                    OutlinedButton(onClick = { viewModel.togglePause() }) {
                        Text(
                            if (isPaused) stringResource(R.string.guided_brew_resume) else stringResource(R.string.guided_brew_pause),
                        )
                    }
                    if (!viewModel.stepActionEndsBrew) {
                        OutlinedButton(onClick = { viewModel.finish() }) {
                            Text(stringResource(R.string.guided_brew_end_brew))
                        }
                    }
                    if (isRunning) {
                        TextButton(onClick = { viewModel.advanceStep() }) {
                            Text(stringResource(R.string.guided_brew_skip_step))
                        }
                    }
                }
            }
        }
    }
}

private fun stepDetailText(step: BrewStep): String? = when (step) {
    is BrewStep.Bloom -> "${formatGrams(step.targetGrams)} g"
    is BrewStep.Pour -> "${formatGrams(step.targetCumulativeGrams)} g"
    else -> step.duration?.let { TimeFormat.clock(it) }
}

private fun formatGrams(value: Double): String =
    if (value == Math.floor(value)) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
