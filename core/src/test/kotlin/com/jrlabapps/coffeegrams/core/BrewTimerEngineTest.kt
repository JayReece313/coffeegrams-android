package com.jrlabapps.coffeegrams.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M4 gate: the guided-brew timer state machine. Driven by exact time
 * deltas, so a full brew is verified instantly and deterministically — no
 * real waiting, no flakiness.
 *
 * Ported case-for-case from the iOS BrewTimerEngineTests.swift conformance
 * suite (20 cases).
 */
class BrewTimerEngineTest {

    /** Collects events so their order can be asserted. */
    private class Recorder {
        val events = mutableListOf<BrewTimerEvent>()
        fun attach(engine: BrewTimerEngine) {
            engine.onEvent = { events.add(it) }
        }
    }

    private fun v60(): BrewTimeline =
        // bloom(45) + pour1(45) + pour2(45) + drawdown(manual)
        BrewTimelineBuilder.buildPulsePourTimeline(BrewMethodProfile.v60, doseGrams = 18.0, ratio = 16.0)

    private fun frenchPress(): BrewTimeline =
        // bloom(30) + fill(15) + steep(240) + plunge(manual)
        BrewTimelineBuilder.buildFrenchPressTimeline(doseGrams = 30.0, ratio = 15.0)

    /**
     * A timeline whose **last** step is timed rather than manual, to
     * exercise the final-step hold on its own terms. (No shipping method
     * ends this way today — all four end on a plunge or drawdown.)
     */
    private fun timedFinalStep(): BrewTimeline = BrewTimeline(
        method = BrewMethod.V60,
        steps = listOf(BrewStep.Bloom(targetGrams = 40.0, durationSeconds = 45), BrewStep.Steep(durationSeconds = 60)),
        totalWaterGrams = 288.0,
    )

    // MARK: Start

    @Test
    fun `start emits started plus first step and enters running`() {
        val engine = BrewTimerEngine(v60())
        val rec = Recorder(); rec.attach(engine)
        engine.start()

        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
        assertEquals(0, engine.currentStepIndex)
        assertEquals(
            listOf(BrewTimerEvent.Started, BrewTimerEvent.StepBegan(0, v60().steps[0])),
            rec.events,
        )
        assertEquals(45.0, engine.remainingInStep)
    }

    @Test
    fun `advance before start does nothing`() {
        val engine = BrewTimerEngine(v60())
        engine.advance(100.0)
        assertEquals(BrewTimerPhase.IDLE, engine.phase)
        assertEquals(0.0, engine.totalElapsed)
    }

    // MARK: Stepping through fixed-duration steps

    @Test
    fun `partial advance accumulates within a step`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(20.0)
        assertEquals(20.0, engine.elapsedInStep)
        assertEquals(25.0, engine.remainingInStep)
        assertEquals(0, engine.currentStepIndex)
    }

    @Test
    fun `intermediate steps flow into the next one with no tap`() {
        val engine = BrewTimerEngine(v60())
        val rec = Recorder(); rec.attach(engine)
        engine.start()

        engine.advance(45.0) // the bloom's full duration
        assertEquals(1, engine.currentStepIndex) // straight on to pour 1
        assertEquals(0.0, engine.elapsedInStep)
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
        assertEquals(
            listOf(
                BrewTimerEvent.StepCompleted(0, v60().steps[0]),
                BrewTimerEvent.StepBegan(1, v60().steps[1]),
            ),
            rec.events.takeLast(2),
        )
        // An intermediate step never announces an overrun; it simply ends.
        assertFalse(rec.events.contains(BrewTimerEvent.ReachedTarget(0, v60().steps[0])))

        engine.advance(45.0)
        assertEquals(2, engine.currentStepIndex) // and on to pour 2
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
    }

    @Test
    fun `a single coarse tick crosses every timed step and stops on the last`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(10_000.0) // way past every timed step
        // bloom45 + pour45 + pour45 = 135 of plan, then the drawdown holds.
        assertEquals(135.0, engine.totalElapsed)
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        assertEquals(BrewStep.Drawdown(untilDripsStop = true), engine.currentStep)
        assertTrue(engine.isOnFinalStep)
        // The leftover is still real time, spent on that final step — every
        // second of it past the plan.
        assertEquals(10_000.0, engine.totalWallElapsed)
        assertEquals(engine.totalWallElapsed - engine.totalElapsed, engine.overrunInStep)
    }

    // MARK: The final step holds

    @Test
    fun `a timed final step holds at its target and counts up`() {
        val engine = BrewTimerEngine(timedFinalStep())
        val rec = Recorder(); rec.attach(engine)
        engine.start()

        engine.advance(45.0) // bloom auto-advances into the final steep
        assertEquals(1, engine.currentStepIndex)
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)

        engine.advance(60.0) // the final step reaches its target
        assertEquals(BrewTimerPhase.OVERRUNNING, engine.phase)
        assertEquals(1, engine.currentStepIndex) // does NOT complete on its own
        assertEquals(0.0, engine.remainingInStep)
        assertEquals(0.0, engine.overrunInStep)
        assertEquals(BrewTimerEvent.ReachedTarget(1, BrewStep.Steep(durationSeconds = 60)), rec.events.last())

        engine.advance(7.0)
        assertEquals(7.0, engine.overrunInStep)
        // Overrun is real time but not *plan* progress.
        assertEquals(105.0, engine.totalElapsed)
        assertEquals(112.0, engine.totalWallElapsed)

        engine.advanceStep() // user ends the brew
        assertEquals(BrewTimerPhase.COMPLETED, engine.phase)
        assertNull(engine.overrunInStep)
    }

    @Test
    fun `a manual final step counts every second as over-plan time`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(135.0) // straight through to the drawdown
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        assertEquals(0.0, engine.overrunInStep) // just arrived, nothing over yet

        engine.advance(12.0)
        // A manual step has no target, so all 12s are past the plan — and
        // because everything before it ran exactly to time, this is also
        // precisely how far the whole brew is over.
        assertEquals(12.0, engine.overrunInStep)
        assertEquals(12.0, engine.totalWallElapsed - engine.totalElapsed)
    }

    // MARK: Manual steps

    @Test
    fun `manual step holds the plan but the master clock keeps running`() {
        val engine = BrewTimerEngine(v60())
        val rec = Recorder(); rec.attach(engine)
        engine.start()
        engine.advance(135.0) // bloom + pour 1 + pour 2, no taps needed

        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        assertEquals(BrewStep.Drawdown(untilDripsStop = true), engine.currentStep)
        assertEquals(135.0, engine.totalElapsed) // bloom45 + pour45 + pour45

        engine.advance(999.0) // the plan does not move…
        assertEquals(135.0, engine.totalElapsed)
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        assertEquals(135.0 + 999.0, engine.totalWallElapsed) // …but the brew does

        engine.advanceStep() // user taps "done"
        assertEquals(BrewTimerPhase.COMPLETED, engine.phase)
        assertTrue(engine.isFinished)
        assertEquals(1.0, engine.fractionComplete)
        assertEquals(BrewTimerEvent.Completed, rec.events.last())
    }

    @Test
    fun `advanceStep skips the remainder of a timed step`() {
        val engine = BrewTimerEngine(frenchPress())
        engine.start()
        engine.advance(10.0) // 10s into the 30s bloom
        engine.advanceStep() // skip the rest of the bloom
        assertEquals(1, engine.currentStepIndex) // now on the fill pour
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
    }

    // MARK: Full brews

    @Test
    fun `French Press runs to completion and holds at plunge`() {
        val engine = BrewTimerEngine(frenchPress())
        val rec = Recorder(); rec.attach(engine)
        engine.start()

        engine.advance(285.0) // bloom30 + fill15 + steep240, all automatic
        assertEquals(BrewStep.Plunge, engine.currentStep)
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        assertEquals(285.0, engine.totalElapsed)

        engine.advanceStep()
        assertEquals(BrewTimerPhase.COMPLETED, engine.phase)
        assertEquals(BrewTimerEvent.Completed, rec.events.last())
        // Sanity: exactly one completed event, and it is last.
        assertEquals(1, rec.events.count { it == BrewTimerEvent.Completed })
    }

    // MARK: Pause / resume

    @Test
    fun `pause freezes time resume continues where it left off`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(20.0)
        engine.pause()
        assertEquals(BrewTimerPhase.PAUSED, engine.phase)
        engine.advance(100.0) // ignored while paused
        assertEquals(20.0, engine.elapsedInStep)
        assertEquals(20.0, engine.totalWallElapsed) // the master clock stops too

        engine.resume()
        engine.advance(25.0) // finishes the 45s bloom
        assertEquals(1, engine.currentStepIndex)
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
        assertEquals(45.0, engine.totalWallElapsed)
    }

    @Test
    fun `pausing an overrun resumes back into the overrun, not the next step`() {
        val engine = BrewTimerEngine(timedFinalStep())
        engine.start()
        engine.advance(110.0) // bloom45 + steep60 + 5s over on the final step
        assertEquals(BrewTimerPhase.OVERRUNNING, engine.phase)

        engine.pause()
        engine.advance(100.0) // ignored
        // The reading freezes rather than disappearing: a null here would
        // send the view to the countdown, which reads "0:00" on a step with
        // no duration left to count.
        assertEquals(5.0, engine.overrunInStep)
        engine.resume()

        assertEquals(BrewTimerPhase.OVERRUNNING, engine.phase)
        assertEquals(5.0, engine.overrunInStep)
        assertEquals(1, engine.currentStepIndex)
    }

    @Test
    fun `a paused manual final step still reports its count-up`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(135.0) // through to the drawdown
        engine.advance(12.0)
        assertEquals(12.0, engine.overrunInStep)

        engine.pause()
        // Same trap as above, and worse here: a manual step has no
        // duration at all, so losing this value leaves the view nothing
        // but "0:00".
        assertEquals(12.0, engine.overrunInStep)
        assertNull(engine.remainingInStep)

        engine.advance(100.0) // paused — clock frozen
        assertEquals(12.0, engine.overrunInStep)

        engine.resume()
        engine.advance(3.0)
        assertEquals(15.0, engine.overrunInStep)
    }

    @Test
    fun `a manual hold can be paused, stopping the master clock`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(135.0)
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)

        engine.pause()
        engine.advance(500.0)
        assertEquals(135.0, engine.totalWallElapsed)

        engine.resume()
        assertEquals(BrewTimerPhase.AWAITING_MANUAL_ADVANCE, engine.phase)
        engine.advance(10.0)
        assertEquals(145.0, engine.totalWallElapsed)
    }

    // MARK: Finish (the "Done" button)

    @Test
    fun `finish ends the brew mid-way and keeps the elapsed time`() {
        val engine = BrewTimerEngine(v60())
        val rec = Recorder(); rec.attach(engine)
        engine.start()
        engine.advance(60.0) // mid-bloom-overrun

        engine.finish()
        assertEquals(BrewTimerPhase.COMPLETED, engine.phase)
        assertTrue(engine.isFinished)
        assertEquals(60.0, engine.totalWallElapsed) // the honest brew duration
        assertEquals(BrewTimerEvent.Completed, rec.events.last())
        assertEquals(1, rec.events.count { it == BrewTimerEvent.Completed })

        engine.advance(100.0) // the clock is stopped for good
        assertEquals(60.0, engine.totalWallElapsed)
    }

    @Test
    fun `finish is a no-op before the brew starts and after it ends`() {
        val engine = BrewTimerEngine(v60())
        val rec = Recorder(); rec.attach(engine)

        engine.finish() // never started
        assertEquals(BrewTimerPhase.IDLE, engine.phase)
        assertTrue(rec.events.isEmpty())

        engine.start()
        engine.finish()
        engine.finish() // second tap must not emit a second completion
        assertEquals(1, rec.events.count { it == BrewTimerEvent.Completed })
    }

    // MARK: Reset

    @Test
    fun `reset returns to idle so the timeline can be rerun`() {
        val engine = BrewTimerEngine(v60())
        engine.start()
        engine.advance(90.0)
        engine.reset()
        assertEquals(BrewTimerPhase.IDLE, engine.phase)
        assertEquals(0, engine.currentStepIndex)
        assertEquals(0.0, engine.totalElapsed)
        assertEquals(0.0, engine.totalWallElapsed)

        engine.start() // rerunnable
        assertEquals(BrewTimerPhase.RUNNING, engine.phase)
    }

    // MARK: Final-step detection

    @Test
    fun `only the timeline's last step counts as final`() {
        val engine = BrewTimerEngine(v60()) // 4 steps
        engine.start()
        assertFalse(engine.isOnFinalStep)

        engine.advance(90.0) // through bloom and pour 1
        assertEquals(2, engine.currentStepIndex)
        assertFalse(engine.isOnFinalStep)

        engine.advance(45.0) // on to the drawdown
        assertEquals(3, engine.currentStepIndex)
        assertTrue(engine.isOnFinalStep)
    }

    // MARK: Progress + edge cases

    @Test
    fun `fractionComplete tracks counted time over fixed duration`() {
        val engine = BrewTimerEngine(v60()) // total fixed = 135
        engine.start()
        engine.advance(45.0)
        assertTrue(kotlin.math.abs(engine.fractionComplete - (45.0 / 135.0)) < 1e-9)
    }

    @Test
    fun `an empty timeline completes immediately on start`() {
        val engine = BrewTimerEngine(BrewTimeline(method = BrewMethod.V60, steps = emptyList(), totalWaterGrams = 0.0))
        val rec = Recorder(); rec.attach(engine)
        engine.start()
        assertEquals(BrewTimerPhase.COMPLETED, engine.phase)
        assertEquals(listOf<BrewTimerEvent>(BrewTimerEvent.Completed), rec.events)
    }
}
