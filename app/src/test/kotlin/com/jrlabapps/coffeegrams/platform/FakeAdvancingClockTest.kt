package com.jrlabapps.coffeegrams.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAdvancingClockTest {
    @Test
    fun `starts at the given time`() {
        val clock = FakeAdvancingClock(startingAt = 5.0)
        assertEquals(5.0, clock.now)
    }

    @Test
    fun `defaults to zero`() {
        assertEquals(0.0, FakeAdvancingClock().now)
    }

    @Test
    fun `advance moves now forward by the given delta`() {
        val clock = FakeAdvancingClock()
        clock.advance(2.5)
        clock.advance(1.5)
        assertEquals(4.0, clock.now)
    }
}
