package com.jrlabapps.coffeegrams.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import com.jrlabapps.coffeegrams.viewmodel.BrewPresets
import com.jrlabapps.coffeegrams.viewmodel.CalculatorViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    method: BrewMethod,
    onStartBrew: (doseGrams: Double, ratio: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CalculatorViewModel = viewModel(
        factory = viewModelFactory { initializer { CalculatorViewModel(method = method) } },
    )

    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val doseGrams by viewModel.doseGrams.collectAsStateWithLifecycle()
    val targetYieldGrams by viewModel.targetYieldGrams.collectAsStateWithLifecycle()
    val ratio by viewModel.ratio.collectAsStateWithLifecycle()

    var inputText by remember(mode) {
        mutableStateOf(formatGrams(if (mode == CalculatorViewModel.Mode.DOSE_FIRST) doseGrams else targetYieldGrams))
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(method.displayName) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
        ) {
            ResultHeadline(label = viewModel.resultLabel, grams = viewModel.resultGrams)

            val presets = BrewPresets.presets(method)
            if (presets.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = {
                                viewModel.applyPreset(doseGrams = preset.doseGrams, ratio = preset.ratio)
                                inputText = formatGrams(preset.doseGrams)
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CalculatorViewModel.Mode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { viewModel.setMode(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalculatorViewModel.Mode.entries.size),
                    ) {
                        Text(entry.title)
                    }
                }
            }

            val inputLabel = if (mode == CalculatorViewModel.Mode.DOSE_FIRST) {
                stringResource(R.string.calculator_input_label_dose)
            } else {
                stringResource(R.string.calculator_input_label_target, viewModel.waterOrYieldLabel)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { text ->
                    inputText = text
                    // The decimal keyboard shows the device locale's own separator
                    // (e.g. ',' on many non-English locales); normalize before
                    // parsing so typing it doesn't silently fail to update state.
                    text.replace(',', '.').toDoubleOrNull()?.let { value ->
                        if (mode == CalculatorViewModel.Mode.DOSE_FIRST) {
                            viewModel.setDoseGrams(value)
                        } else {
                            viewModel.setTargetYieldGrams(value)
                        }
                    }
                },
                label = { Text(inputLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    stringResource(R.string.calculator_ratio_label, viewModel.ratioLabel),
                    style = MaterialTheme.typography.labelLarge,
                )
                val range = viewModel.ratioRange
                val step = viewModel.ratioStep
                val steps = ((range.endInclusive - range.start) / step).roundToInt() - 1
                val ratioDescription = stringResource(R.string.calculator_ratio_content_description)
                val ratioLabel = viewModel.ratioLabel
                Slider(
                    value = ratio.toFloat(),
                    onValueChange = { viewModel.setRatio(it.toDouble()) },
                    valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
                    steps = steps.coerceAtLeast(0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = ratioDescription
                            stateDescription = ratioLabel
                        },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatGrams(range.start), style = MaterialTheme.typography.labelSmall)
                    Text(formatGrams(range.endInclusive), style = MaterialTheme.typography.labelSmall)
                }
            }

            val ctaLabel = when (method) {
                BrewMethod.ESPRESSO -> stringResource(R.string.calculator_start_espresso)
                BrewMethod.COLD_BREW -> stringResource(R.string.calculator_start_cold_brew)
                else -> stringResource(R.string.calculator_start_pour_over)
            }
            Button(
                onClick = { onStartBrew(viewModel.effectiveDoseGrams, ratio) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ctaLabel)
            }
        }
    }
}

@Composable
private fun ResultHeadline(label: String, grams: Double) {
    val description = stringResource(R.string.calculator_result_description, label, grams.roundToInt())
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text("${grams.roundToInt()} g", style = MaterialTheme.typography.displayLarge)
    }
}

private fun formatGrams(value: Double): String =
    if (value == Math.floor(value)) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
