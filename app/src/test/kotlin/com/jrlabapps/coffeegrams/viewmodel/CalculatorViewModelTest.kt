package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethod
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Conformance-matched to iOS's `CalculatorViewModelTests` (same inputs, same
 * expected outputs). `presetsPerMethod` is ported separately in
 * [BrewPresetTest] since it exercises [BrewPresets], not this VM.
 */
class CalculatorViewModelTest {
    @Test
    fun `defaults to V60 at its default ratio, dose-first`() {
        val vm = CalculatorViewModel()
        assertEquals(BrewMethod.V60, vm.method.value)
        assertEquals(16.0, vm.ratio.value)
        assertEquals(CalculatorViewModel.Mode.DOSE_FIRST, vm.mode.value)
    }

    @Test
    fun `dose-first result is water = dose times ratio`() {
        val vm = CalculatorViewModel(method = BrewMethod.V60, doseGrams = 18.0)
        assertEquals(288.0, vm.resultGrams) // 18 x 16
        assertEquals("Water", vm.resultLabel)
    }

    @Test
    fun `selecting a method resets the ratio to that method's default`() {
        val vm = CalculatorViewModel()
        vm.selectMethod(BrewMethod.FRENCH_PRESS)
        assertEquals(BrewMethod.FRENCH_PRESS, vm.method.value)
        assertEquals(15.0, vm.ratio.value)

        vm.selectMethod(BrewMethod.ESPRESSO)
        assertEquals(2.0, vm.ratio.value)
        assertEquals(1.0..3.0, vm.ratioRange)
    }

    @Test
    fun `ratio label formats as 1 N`() {
        val vm = CalculatorViewModel(method = BrewMethod.V60)
        assertEquals("1:16", vm.ratioLabel)

        vm.selectMethod(BrewMethod.ESPRESSO)
        assertEquals("1:2", vm.ratioLabel)

        vm.setRatio(2.5)
        assertEquals("1:2.5", vm.ratioLabel)
    }

    @Test
    fun `espresso output is labelled Yield, not Water`() {
        val vm = CalculatorViewModel(method = BrewMethod.ESPRESSO, doseGrams = 18.0)
        assertEquals("Yield", vm.waterOrYieldLabel)
        assertEquals(36.0, vm.resultGrams) // 18 x 2
    }

    @Test
    fun `yield-first result is dose = yield over ratio`() {
        val vm = CalculatorViewModel(method = BrewMethod.V60, targetYieldGrams = 320.0)
        vm.setMode(CalculatorViewModel.Mode.YIELD_FIRST)
        assertEquals(20.0, vm.resultGrams) // 320 / 16
        assertEquals("Coffee", vm.resultLabel)
    }

    @Test
    fun `ratio step is finer for espresso than for brewed methods`() {
        val vm = CalculatorViewModel(method = BrewMethod.V60)
        assertEquals(0.5, vm.ratioStep)
        vm.selectMethod(BrewMethod.ESPRESSO)
        assertEquals(0.25, vm.ratioStep)
    }

    @Test
    fun `applying a preset sets dose plus clamped ratio and goes dose-first`() {
        val vm = CalculatorViewModel(method = BrewMethod.AEROPRESS)
        vm.applyPreset(doseGrams = 11.0, ratio = 18.0)
        assertEquals(11.0, vm.doseGrams.value)
        assertEquals(18.0, vm.ratio.value)
        assertEquals(CalculatorViewModel.Mode.DOSE_FIRST, vm.mode.value)

        // A preset ratio outside the method range is clamped (AeroPress 12-18).
        vm.applyPreset(doseGrams = 11.0, ratio = 25.0)
        assertEquals(18.0, vm.ratio.value)
    }

    @Test
    fun `effective dose follows the mode (entered dose vs computed dose)`() {
        val vm = CalculatorViewModel(method = BrewMethod.V60, doseGrams = 18.0, targetYieldGrams = 320.0)
        assertEquals(18.0, vm.effectiveDoseGrams) // dose-first: the entered dose
        vm.setMode(CalculatorViewModel.Mode.YIELD_FIRST)
        assertEquals(20.0, vm.effectiveDoseGrams) // yield-first: 320 / 16 = 20
    }
}
