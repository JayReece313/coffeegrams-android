package com.jrlabapps.coffeegrams.platform

import com.jrlabapps.coffeegrams.core.MonotonicClock

/** A [MonotonicClock] test double whose reading only moves when [advance] is called. */
class FakeAdvancingClock(startingAt: Double = 0.0) : MonotonicClock {
    override var now: Double = startingAt
        private set

    fun advance(seconds: Double) {
        now += seconds
    }
}
