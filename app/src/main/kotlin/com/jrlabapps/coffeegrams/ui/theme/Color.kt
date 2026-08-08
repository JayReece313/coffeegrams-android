package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The six brand tokens, light and dark, extracted from the iOS asset catalog.
 *
 * These hex values are the contract (see DESIGN.md) — they are not to be
 * re-derived, eyeballed, or "improved" independently on Android. Changing one
 * means updating DESIGN.md and confirming iOS still matches.
 */

// Cream — the 60% background/surface share.
val BackgroundLight = Color(0xFFF4EADB)
val BackgroundDark = Color(0xFF1C140E)
val SurfaceLight = Color(0xFFFBF4E9)
val SurfaceDark = Color(0xFF2A1F16)

// Espresso Brown — the 30% text/primary-UI share.
val TextPrimaryLight = Color(0xFF3A2A1E)
val TextPrimaryDark = Color(0xFFF1E7D8)

// Medium Roast Taupe — secondary text, a desaturated step of Espresso Brown,
// not a new hue.
val TextSecondaryLight = Color(0xFF6B5647)
val TextSecondaryDark = Color(0xFFB9A897)

// Caramel — the 10% accent/action share.
val AccentLight = Color(0xFFC6852E)
val AccentDark = Color(0xFFE0A34A)

// Deeper Gold — the running-timer numerals.
val TimerActiveLight = Color(0xFFA96B1E)
val TimerActiveDark = Color(0xFFE7B45C)
