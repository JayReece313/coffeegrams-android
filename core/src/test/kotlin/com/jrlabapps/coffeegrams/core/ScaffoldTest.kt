package com.jrlabapps.coffeegrams.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the headless test harness is wired up before any real logic exists —
 * `./gradlew :core:test` must run and report from the CLI with no emulator.
 *
 * M2 replaces this with the 49 cases ported from the iOS Swift Testing suite,
 * which are the conformance spec for the whole port.
 */
class ScaffoldTest {
    @Test
    fun `core module targets parity with iOS 1_1`() {
        assertEquals("iOS 1.1", CoffeeGramsCore.PARITY_TARGET)
    }
}
