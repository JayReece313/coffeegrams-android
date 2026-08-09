package com.jrlabapps.coffeegrams.viewmodel

import androidx.lifecycle.ViewModel
import com.jrlabapps.coffeegrams.core.BrewCalculator
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.core.BrewMethodProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Drives the calculator screen: owns the values the user manipulates
 * (method, direction, dose, target yield, ratio) and *derives* the results
 * by calling the already-tested [BrewCalculator]. Keeping this out of the
 * screen is what makes the numbers testable independent of Compose.
 */
class CalculatorViewModel(
    method: BrewMethod = BrewMethod.V60,
    doseGrams: Double = 18.0,
    targetYieldGrams: Double = 300.0,
) : ViewModel() {

    /** Which way the user is calculating. */
    enum class Mode {
        /** Enter coffee grams, get water (or espresso yield). */
        DOSE_FIRST,

        /** Enter the desired output, get the coffee dose. */
        YIELD_FIRST,
        ;

        val title: String
            get() = when (this) {
                DOSE_FIRST -> "Dose → Water"
                YIELD_FIRST -> "Water → Dose"
            }
    }

    // MARK: Inputs (bound to controls in the screen)

    private val _method = MutableStateFlow(method)

    /** The chosen brew method — change it through [selectMethod], which also fixes up the ratio. */
    val method: StateFlow<BrewMethod> = _method.asStateFlow()

    private val _mode = MutableStateFlow(Mode.DOSE_FIRST)
    val mode: StateFlow<Mode> = _mode.asStateFlow()
    fun setMode(mode: Mode) {
        _mode.value = mode
    }

    private val _doseGrams = MutableStateFlow(doseGrams)
    val doseGrams: StateFlow<Double> = _doseGrams.asStateFlow()
    fun setDoseGrams(value: Double) {
        _doseGrams.value = value
    }

    private val _targetYieldGrams = MutableStateFlow(targetYieldGrams)
    val targetYieldGrams: StateFlow<Double> = _targetYieldGrams.asStateFlow()
    fun setTargetYieldGrams(value: Double) {
        _targetYieldGrams.value = value
    }

    private val _ratio = MutableStateFlow(BrewMethodProfile.profile(method).defaultRatio)
    val ratio: StateFlow<Double> = _ratio.asStateFlow()
    fun setRatio(value: Double) {
        _ratio.value = value
    }

    // MARK: Method selection

    /**
     * Switch method and reset the ratio to that method's default. Each
     * method has a different valid ratio range (e.g. espresso 1:1–1:3 vs
     * V60 1:15–1:17), so carrying the old ratio over could land out of range.
     */
    fun selectMethod(newMethod: BrewMethod) {
        _method.value = newMethod
        _ratio.value = profile.defaultRatio
    }

    /**
     * Apply a named preset (e.g. an AeroPress recipe): set the dose and
     * ratio at once, clamping the ratio into the method's valid range, and
     * go dose-first so the result shows the water immediately.
     */
    fun applyPreset(doseGrams: Double, ratio: Double) {
        _doseGrams.value = doseGrams
        _ratio.value = BrewCalculator.clampRatio(ratio, _method.value)
        _mode.value = Mode.DOSE_FIRST
    }

    // MARK: Derived configuration

    val profile: BrewMethodProfile get() = BrewMethodProfile.profile(_method.value)
    val ratioRange get() = profile.ratioRange

    /** Slider granularity — espresso is dialled in finer steps than brewed ratios. */
    val ratioStep: Double get() = if (_method.value == BrewMethod.ESPRESSO) 0.25 else 0.5

    /** "1:16"-style label. Whole numbers show no decimal; fractional show one. */
    val ratioLabel: String
        get() {
            val r = _ratio.value
            val value = if (r == Math.floor(r)) {
                String.format(Locale.US, "%.0f", r)
            } else {
                String.format(Locale.US, "%.1f", r)
            }
            return "1:$value"
        }

    /** Espresso produces *yield* under pressure; the others add *water*. */
    val waterOrYieldLabel: String get() = if (_method.value == BrewMethod.ESPRESSO) "Yield" else "Water"

    // MARK: Results (delegated to the tested core calculator)

    val computedWaterGrams: Double get() = BrewCalculator.waterGrams(_doseGrams.value, _ratio.value)
    val computedDoseGrams: Double
        get() = BrewCalculator.doseGrams(_targetYieldGrams.value, _ratio.value, _method.value)

    /** The headline number, given the current direction. */
    val resultGrams: Double
        get() = when (_mode.value) {
            Mode.DOSE_FIRST -> computedWaterGrams
            Mode.YIELD_FIRST -> computedDoseGrams
        }

    /**
     * The coffee dose implied by the current inputs — the *entered* dose in
     * dose-first mode, or the *computed* dose in yield-first mode. This is
     * what a guided brew and the log should use, so a yield-first setup
     * brews/logs the right dose.
     */
    val effectiveDoseGrams: Double
        get() = when (_mode.value) {
            Mode.DOSE_FIRST -> _doseGrams.value
            Mode.YIELD_FIRST -> computedDoseGrams
        }

    /** Label for [resultGrams]. */
    val resultLabel: String
        get() = when (_mode.value) {
            Mode.DOSE_FIRST -> waterOrYieldLabel
            Mode.YIELD_FIRST -> "Coffee"
        }
}
