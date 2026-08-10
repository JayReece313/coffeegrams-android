package com.jrlabapps.coffeegrams.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.jrlabapps.coffeegrams.core.BrewLogStoring
import com.jrlabapps.coffeegrams.design.TimeFormat
import com.jrlabapps.coffeegrams.design.icon
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.LogViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

/**
 * History of saved brews, newest first. Mirrors iOS's `LogView`, with one
 * structural difference: iOS reads reactively from a SwiftData `@Query`,
 * while [LogViewModel] reloads explicitly (see its doc comment) since
 * [BrewLogStoring] is a one-shot `suspend` port here, not a `Flow`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onEntrySelected: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    store: BrewLogStoring = currentApplication().brewLogStore,
) {
    val viewModel: LogViewModel = viewModel(
        factory = viewModelFactory { initializer { LogViewModel(store) } },
    )
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.log_title)) }) },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            EmptyLog(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = Spacing.smallSpacing),
            ) {
                items(entries, key = { it.id.toString() }) { entry ->
                    LogRow(
                        entry = entry,
                        onClick = { onEntrySelected(entry.id) },
                        onDelete = { viewModel.delete(entry.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyLog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Coffee,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.log_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.itemSpacing),
        )
        Text(
            stringResource(R.string.log_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.smallSpacing),
        )
    }
}

/**
 * The delete [IconButton] is deliberately kept *outside* the clickable,
 * merged-semantics content [Row] below rather than nested inside it: a
 * `mergeDescendants` region swallows an inner actionable child into the
 * parent's single node, which would make the delete action unreachable by
 * TalkBack (the merged node only exposes the row's own click). Keeping it
 * as a sibling leaves it independently focusable.
 */
@Composable
private fun LogRow(entry: BrewLogEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val deleteHint = stringResource(R.string.log_delete_action)
    val description = rowDescription(entry)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = entry.method.displayName, onClick = onClick)
                .padding(horizontal = Spacing.screenPadding, vertical = Spacing.smallSpacing)
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = entry.method.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.itemSpacing),
            ) {
                Text(entry.method.displayName, style = MaterialTheme.typography.titleMedium)
                Text(subtitle(entry), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                timing(entry)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            entry.rating?.let { rating -> StarRating(rating = rating, size = 11.dp) }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = deleteHint)
        }
    }
}

@Composable
private fun rowDescription(entry: BrewLogEntry): String = "${entry.method.displayName}, ${subtitle(entry)}"

@Composable
private fun subtitle(entry: BrewLogEntry): String {
    val dose = Math.round(entry.doseGrams).toInt()
    val water = Math.round(entry.waterGrams).toInt()
    val ratio = Math.round(entry.ratio).toInt()
    val formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(entry.date.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.log_row_subtitle, dose, water, ratio, formattedDate)
}

/** "4:45 actual · 4:15 planned" — only for brews saved with a timeline (M7 onward); older/timerless brews omit the line. */
@Composable
private fun timing(entry: BrewLogEntry): String? {
    val actual = entry.actualSeconds ?: return null
    val planned = entry.plannedSeconds
        ?: return stringResource(R.string.log_row_timing_actual_only, TimeFormat.clock(actual))
    return stringResource(R.string.log_row_timing_actual_and_planned, TimeFormat.clock(actual), TimeFormat.clock(planned))
}
