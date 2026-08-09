package com.jrlabapps.coffeegrams.data

import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [BrewLogEntity] is a plain Kotlin data class — no Room runtime needed to
 * exercise the mapping to/from [BrewLogEntry], so this runs as a fast JVM
 * unit test rather than an instrumented one.
 */
class BrewLogEntityMappingTest {

    @Test
    fun `entry to entity to entry round-trips exactly`() {
        val entry = BrewLogEntry(
            id = UUID.randomUUID(),
            date = Instant.ofEpochSecond(1_700_000_000),
            method = BrewMethod.CHEMEX,
            doseGrams = 20.0,
            waterGrams = 320.0,
            ratio = 16.0,
            shotSeconds = null,
            plannedSeconds = 465,
            actualSeconds = 502,
            rating = 4,
            notes = "a touch under-extracted",
        )

        assertEquals(entry, entry.toEntity().toEntry())
    }

    @Test
    fun `method is stored as its stable raw string`() {
        val entry = BrewLogEntry(method = BrewMethod.FRENCH_PRESS, doseGrams = 30.0, waterGrams = 450.0, ratio = 15.0)
        assertEquals("french_press", entry.toEntity().methodRawValue)
    }

    @Test
    fun `an unrecognized raw value falls back to V60, matching iOS`() {
        val entity = BrewLogEntity(
            id = UUID.randomUUID(),
            date = Instant.now(),
            methodRawValue = "some_future_method",
            doseGrams = 18.0,
            waterGrams = 288.0,
            ratio = 16.0,
            shotSeconds = null,
            plannedSeconds = null,
            actualSeconds = null,
            rating = null,
            notes = null,
        )
        assertEquals(BrewMethod.V60, entity.toEntry().method)
    }

    @Test
    fun `espresso shot yield is carried in waterGrams, matching the domain contract`() {
        val entry = BrewLogEntry(
            method = BrewMethod.ESPRESSO,
            doseGrams = 18.0,
            waterGrams = 36.0, // shot yield, not water — see BrewLogEntry's doc comment
            ratio = 2.0,
            shotSeconds = 27,
        )
        val entity = entry.toEntity()
        assertEquals(36.0, entity.waterGrams)
        assertEquals(27, entity.shotSeconds)
    }
}
