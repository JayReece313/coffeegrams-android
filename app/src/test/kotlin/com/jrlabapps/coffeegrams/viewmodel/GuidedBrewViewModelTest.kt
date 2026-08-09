package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewMethodProfile
import com.jrlabapps.coffeegrams.core.BrewTimelineBuilder
import com.jrlabapps.coffeegrams.platform.FakeAdvancingClock
import com.jrlabapps.coffeegrams.platform.RecordingHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Conformance-matched to iOS's `GuidedBrewViewModelTests` — same fake-clock
 * drive pattern (`clock.advance(x)` then one tick call), same expected
 * values, against a V60 pulse-pour timeline: bloom(45) + pour1(45) +
 * pour2(45) + drawdown(manual).
 *
 * Constructing the VM launches its internal tick loop via `viewModelScope`,
 * which resolves `Dispatchers.Main` immediately — hence the dispatcher
 * setup below. Tests never call `advanceUntilIdle()`/`advanceTimeBy()`, so
 * that loop's `delay()` never actually fires; every assertion here is driven
 * by the explicit, synchronous `tick()` calls instead, exactly like iOS's
 * `tickOnce()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuidedBrewViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun makeVM(clock: FakeAdvancingClock): GuidedBrewViewModel {
        val timeline = BrewTimelineBuilder.buildPulsePourTimeline(
            profile = BrewMethodProfile.v60,
            doseGrams = 18.0,
            ratio = 16.0,
        )
        return GuidedBrewViewModel(timeline, clock, RecordingHaptics())
    }

    /** Run the brew to its drawdown — 135s covers bloom + pour 1 + pour 2. */
    private fun runToDrawdown(vm: GuidedBrewViewModel, clock: FakeAdvancingClock) {
        vm.start()
        clock.advance(135.0)
        vm.tick()
    }

    @Test
    fun `starts idle showing the first step's duration`() {
        val vm = makeVM(FakeAdvancingClock())
        assertEquals(true, vm.isIdle)
        assertEquals(45, vm.remainingSeconds.value)
    }

    @Test
    fun `start begins running on the first step`() {
        val vm = makeVM(FakeAdvancingClock())
        vm.start()
        assertEquals(true, vm.isRunning)
        assertEquals(0, vm.currentStepIndex.value)
        assertEquals(45, vm.remainingSeconds.value)
    }

    @Test
    fun `advancing the clock counts the step down`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(20.0)
        vm.tick()
        assertEquals(25, vm.remainingSeconds.value)
        assertEquals(0, vm.currentStepIndex.value)
    }

    @Test
    fun `intermediate steps flow into the next one with no tap`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(45.0) // finishes the 45s bloom exactly
        vm.tick()
        assertEquals(1, vm.currentStepIndex.value) // straight on to pour 1
        assertEquals(true, vm.isRunning)
        assertEquals(45, vm.remainingSeconds.value)
        assertNull(vm.overrunSeconds.value) // no overrun on an intermediate step
        assertNull(vm.advanceTitle) // and nothing to tap

        clock.advance(45.0)
        vm.tick()
        assertEquals(2, vm.currentStepIndex.value) // and on to pour 2
        assertEquals(true, vm.isRunning)
    }

    @Test
    fun `the final step counts up and offers a single Done`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        runToDrawdown(vm, clock)

        assertEquals(true, vm.isAwaitingManualAdvance)
        assertEquals(true, vm.isOnFinalStep.value)
        assertEquals(0, vm.overrunSeconds.value) // just arrived
        assertEquals("Done", vm.advanceTitle)
        // Done already ends the brew, so no separate End Brew beside it.
        assertEquals(true, vm.stepActionEndsBrew)

        clock.advance(12.0)
        vm.tick()
        assertEquals(12, vm.overrunSeconds.value)
        assertEquals(true, vm.isAwaitingManualAdvance)

        vm.resolveCurrentStep() // user taps Done
        assertEquals(true, vm.isFinished)
        assertEquals(1.0, vm.fractionComplete.value)
        assertEquals(147, vm.actualSeconds) // 135 planned + 12 on the drawdown
    }

    @Test
    fun `the master clock keeps counting on the final step`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        runToDrawdown(vm, clock)
        assertEquals(135, vm.totalElapsedSeconds.value)

        clock.advance(20.0) // it keeps running while the bed drains
        vm.tick()
        assertEquals(155, vm.totalElapsedSeconds.value)
    }

    @Test
    fun `the master clock stops while paused`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(20.0)
        vm.tick()
        assertEquals(20, vm.totalElapsedSeconds.value)

        vm.pause()
        clock.advance(300.0)
        vm.tick()
        assertEquals(20, vm.totalElapsedSeconds.value)

        vm.resume()
        clock.advance(10.0)
        vm.tick()
        assertEquals(30, vm.totalElapsedSeconds.value)
    }

    @Test
    fun `planned vs actual, a brew ended on time reports no delta`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        runToDrawdown(vm, clock)
        vm.resolveCurrentStep() // finish the drawdown immediately

        assertEquals(true, vm.isFinished)
        assertEquals(135, vm.plannedSeconds)
        assertEquals(135, vm.actualSeconds)
    }

    @Test
    fun `planned vs actual, a long drawdown shows up as extra actual time`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        runToDrawdown(vm, clock)

        clock.advance(30.0) // the bed takes a while to drain
        vm.tick()
        vm.resolveCurrentStep()

        assertEquals(135, vm.plannedSeconds)
        assertEquals(165, vm.actualSeconds) // 30s over, honestly logged
    }

    @Test
    fun `pause freezes the countdown, resume continues`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(20.0)
        vm.tick()
        vm.pause()
        assertEquals(true, vm.isPaused)

        clock.advance(100.0) // ignored while paused
        vm.tick()
        assertEquals(25, vm.remainingSeconds.value)

        vm.resume()
        clock.advance(25.0) // finishes the bloom
        vm.tick()
        assertEquals(true, vm.isRunning)
        assertEquals(1, vm.currentStepIndex.value)
    }

    @Test
    fun `pausing the final step keeps its count-up on screen`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        runToDrawdown(vm, clock)

        clock.advance(12.0)
        vm.tick()
        assertEquals(12, vm.overrunSeconds.value)

        vm.pause()
        // The bug this guards: if overrunSeconds goes null here the screen falls
        // through to the countdown, and a manual step has no duration left to
        // count — so it would read "0:00" mid-drawdown.
        assertEquals(12, vm.overrunSeconds.value)
        assertEquals(true, vm.isShowingOverrun)

        clock.advance(60.0) // paused — the clock is frozen
        vm.tick()
        assertEquals(12, vm.overrunSeconds.value)
    }

    @Test
    fun `togglePause flips between pause and resume`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        vm.togglePause()
        assertEquals(true, vm.isPaused)
        vm.togglePause()
        assertEquals(true, vm.isRunning)
    }

    @Test
    fun `finish ends the brew mid-way and keeps the elapsed time`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(72.0)
        vm.tick()
        vm.finish()

        assertEquals(true, vm.isFinished)
        assertEquals(72, vm.actualSeconds)

        clock.advance(50.0) // the clock is stopped for good
        vm.tick()
        assertEquals(72, vm.actualSeconds)
    }

    @Test
    fun `reset returns to idle`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()
        clock.advance(50.0)
        vm.tick()
        vm.reset()
        assertEquals(true, vm.isIdle)
        assertEquals(0, vm.currentStepIndex.value)
    }
}
