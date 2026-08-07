package com.jrlabapps.coffeegrams.core

/**
 * The physical category of a brew, which determines how its timeline and
 * water math behave.
 *
 * - [PULSE_POUR]: water is poured over grounds in stages and passes through
 *   (V60, Chemex). Yield ≈ water poured.
 * - [IMMERSION]: grounds sit submerged in water, then are separated
 *   (French Press, AeroPress, Cold Brew). Some water is retained by the
 *   grounds, so yield < water poured (a simplification documented in the
 *   calculator).
 * - [PRESSURE]: liquid is extracted under pressure (Espresso). The
 *   meaningful number is *shot yield weight vs. dose*, not water added — so
 *   this type is handled by a dedicated, deliberately shallow path.
 */
enum class BrewType {
    PULSE_POUR,
    IMMERSION,
    PRESSURE,
}
