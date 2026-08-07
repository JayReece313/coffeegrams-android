package com.jrlabapps.coffeegrams.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Medication
import androidx.compose.ui.graphics.vector.ImageVector
import com.jrlabapps.coffeegrams.core.BrewMethod

/**
 * App-layer presentation details for the domain's [BrewMethod]. These live in
 * `:app` (not `:core`) to keep the domain model free of UI concerns — mirrors
 * the iOS app's `BrewMethod+Presentation.swift`.
 *
 * Per DESIGN.md these are **placeholders**, standing in for the custom vector
 * method icons (a real Chemex / V60 silhouette, etc.) that M11 draws. Material
 * Symbols are Android's equivalent of iOS's placeholder SF Symbols: crisp at
 * any size, tintable with the palette, and — unlike hand-drawn art — already
 * professionally designed, which matters for a placeholder that ships to
 * users before the real set exists.
 */
val BrewMethod.icon: ImageVector
    get() = when (this) {
        BrewMethod.V60 -> Icons.Filled.ChangeHistory // cone dripper
        BrewMethod.CHEMEX -> Icons.Filled.HourglassBottom // hourglass silhouette
        BrewMethod.FRENCH_PRESS -> Icons.Filled.LocalCafe // carafe
        BrewMethod.AEROPRESS -> Icons.Filled.Medication // plunger tube
        BrewMethod.COLD_BREW -> Icons.Filled.AcUnit // served cold
        BrewMethod.ESPRESSO -> Icons.Filled.Coffee
    }
