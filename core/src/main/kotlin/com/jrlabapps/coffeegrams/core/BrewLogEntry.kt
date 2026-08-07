package com.jrlabapps.coffeegrams.core

import java.time.Instant
import java.util.UUID

/**
 * One saved brew in the log.
 *
 * This is a **plain value type** living in `:core` so it can be created and
 * asserted in tests with no Android dependency. The app layer defines a
 * separate Room `@Entity` class and maps to/from this type — keeping the
 * persistence framework out of the pure core is the seam that keeps the
 * logic testable and portable.
 */
data class BrewLogEntry(
    val method: BrewMethod,
    val doseGrams: Double,
    /**
     * For espresso this stores the **shot yield** in grams (same field,
     * different meaning), per the spec. See [EspressoTarget] for the target.
     */
    val waterGrams: Double,
    val ratio: Double,
    val id: UUID = UUID.randomUUID(),
    val date: Instant = Instant.now(),
    /**
     * Espresso only — the measured shot time. `null` for every other
     * method. Kept separate from [actualSeconds]: a shot time is a quality
     * metric with its own target window, not a whole-brew duration.
     */
    val shotSeconds: Int? = null,
    /**
     * The brew's **planned** total, i.e. the sum of the guided timeline's
     * fixed step durations. Stored rather than recomputed so a saved brew
     * still reports the plan it was actually run against, even if a method
     * profile is retuned in a later release. `null` for brews with no
     * timeline (espresso, cold brew) and for entries saved before 1.1.
     */
    val plannedSeconds: Int? = null,
    /**
     * The brew's **actual** wall-clock total, from Start to finish —
     * including slow pours and however long the plunge took. Compared
     * against [plannedSeconds] this is what tells you your pours are
     * consistently running long. `null` for brews with no timeline and for
     * pre-1.1 entries.
     */
    val actualSeconds: Int? = null,
    /** Optional 1–5 rating. */
    val rating: Int? = null,
    val notes: String? = null,
) {
    /**
     * How far the brew ran over (positive) or under (negative) its plan, or
     * `null` when we don't have both numbers.
     */
    val timeDeltaSeconds: Int?
        get() {
            val planned = plannedSeconds ?: return null
            val actual = actualSeconds ?: return null
            return actual - planned
        }
}
