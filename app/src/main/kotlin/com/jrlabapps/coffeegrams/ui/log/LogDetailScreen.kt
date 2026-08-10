package com.jrlabapps.coffeegrams.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewLogStoring
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.design.TimeFormat
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.LogDetailViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * View, rate, annotate, or delete a single saved brew. Mirrors iOS's
 * `LogDetailView`: the rating persists the instant it changes, while notes
 * are captured in a local draft and only written back to the store when the
 * screen is left ([DisposableEffect]) or the app is backgrounded
 * ([LifecycleEventEffect] on `ON_STOP`) — not on every keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    entryId: UUID,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    store: BrewLogStoring = currentApplication().brewLogStore,
) {
    val viewModel: LogDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { LogDetailViewModel(entryId, store) } },
    )
    val entry by viewModel.entry.collectAsStateWithLifecycle()

    // null until the first load resolves; once set, later entry changes
    // (e.g. a rating tap re-emitting the StateFlow) must not clobber
    // whatever the user has already typed into the notes field.
    var notesDraft by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        if (notesDraft == null) entry?.let { notesDraft = it.notes ?: "" }
    }

    DisposableEffect(Unit) {
        onDispose { notesDraft?.let { viewModel.saveNotes(it) } }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        notesDraft?.let { viewModel.saveNotes(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(entry?.method?.displayName.orEmpty()) }) },
    ) { innerPadding ->
        val currentEntry = entry
        if (currentEntry == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LogDetailContent(
                entry = currentEntry,
                notes = notesDraft ?: "",
                onNotesChange = { notesDraft = it },
                onRatingChange = viewModel::setRating,
                onDelete = { viewModel.delete(onDeleted) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LogDetailContent(
    entry: BrewLogEntry,
    notes: String,
    onNotesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
            SummaryRow(stringResource(R.string.log_detail_method_label), entry.method.displayName)
            SummaryRow(stringResource(R.string.log_detail_dose_label), stringResource(R.string.log_detail_grams_value, Math.round(entry.doseGrams).toInt()))
            SummaryRow(
                if (entry.method == BrewMethod.ESPRESSO) stringResource(R.string.log_detail_yield_label) else stringResource(R.string.log_detail_water_label),
                stringResource(R.string.log_detail_grams_value, Math.round(entry.waterGrams).toInt()),
            )
            SummaryRow(stringResource(R.string.log_detail_ratio_label), stringResource(R.string.log_detail_ratio_value, Math.round(entry.ratio).toInt()))
            entry.shotSeconds?.let {
                SummaryRow(stringResource(R.string.log_detail_shot_time_label), stringResource(R.string.log_detail_shot_seconds_value, it))
            }
            entry.plannedSeconds?.let {
                SummaryRow(stringResource(R.string.log_detail_planned_time_label), TimeFormat.clock(it))
            }
            entry.actualSeconds?.let {
                SummaryRow(stringResource(R.string.log_detail_actual_time_label), TimeFormat.clock(it), detail = deltaText(entry))
            }
            SummaryRow(stringResource(R.string.log_detail_date_label), formattedDate(entry))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
            SectionHeader(stringResource(R.string.log_detail_rating_header))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                StarRating(rating = entry.rating ?: 0, onRatingChange = onRatingChange, size = 28.dp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
            SectionHeader(stringResource(R.string.log_detail_notes_header))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.log_detail_notes_placeholder)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.log_detail_delete_brew))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, detail: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.End) {
            Text(value)
            detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "+0:12 over plan" / "−0:12 under plan" — `null` when there's no delta to show. */
@Composable
private fun deltaText(entry: BrewLogEntry): String? {
    val delta = entry.timeDeltaSeconds
    if (delta == null || delta == 0) return null
    val magnitude = TimeFormat.clock(abs(delta))
    return if (delta > 0) {
        stringResource(R.string.log_detail_delta_over, magnitude)
    } else {
        stringResource(R.string.log_detail_delta_under, magnitude)
    }
}

@Composable
private fun formattedDate(entry: BrewLogEntry): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(entry.date.atZone(ZoneId.systemDefault()))
