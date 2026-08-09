package com.jrlabapps.coffeegrams.viewmodel

import com.jrlabapps.coffeegrams.core.BrewTimelineBuilder
import com.jrlabapps.coffeegrams.core.ShotTimingState
import com.jrlabapps.coffeegrams.platform.FakeAdvancingClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Conformance-matched to iOS's `EspressoShotViewModelTests`. */
@OptIn(ExperimentalCoroutinesApi::class)
class EspressoShotViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun makeVM(clock: FakeAdvancingClock): EspressoShotViewModel {
        val target = BrewTimelineBuilder.buildEspressoTarget(doseGrams = 18.0, ratio = 2.0)
        return EspressoShotViewModel(target, clock)
    }

    @Test
    fun `target reflects an 18g, 1 to 2 shot`() {
        val vm = makeVM(FakeAdvancingClock())
        assertEquals(36.0, vm.target.targetYieldGrams)
        assertEquals(25..30, vm.target.shotTimeRange)
        assertFalse(vm.hasStarted)
    }

    @Test
    fun `elapsed tracks the clock while running`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()
        clock.advance(27.0)
        vm.tick()
        assertEquals(27, vm.elapsedSeconds.value)
        assertTrue(vm.isRunning.value)
        assertTrue(vm.hasStarted)
    }

    @Test
    fun `timing state is amber early, green in-window, red late`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()

        clock.advance(20.0)
        vm.tick()
        assertEquals(ShotTimingState.TOO_EARLY, vm.timingState)

        clock.advance(7.0) // 27s -- inside 25..30
        vm.tick()
        assertEquals(ShotTimingState.ON_TARGET, vm.timingState)

        clock.advance(8.0) // 35s -- past the window
        vm.tick()
        assertEquals(ShotTimingState.TOO_LATE, vm.timingState)
    }

    @Test
    fun `stop halts the clock, reset clears it`() {
        val clock = FakeAdvancingClock()
        val vm = makeVM(clock)
        vm.start()
        clock.advance(28.0)
        vm.tick()
        vm.stop()
        assertFalse(vm.isRunning.value)
        assertTrue(vm.hasStarted) // still shows the 28s result
        assertEquals(28, vm.elapsedSeconds.value)

        clock.advance(50.0) // ignored -- stopped
        vm.tick()
        assertEquals(28, vm.elapsedSeconds.value)

        vm.reset()
        assertEquals(0, vm.elapsedSeconds.value)
        assertFalse(vm.hasStarted)
    }
}
