package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and shape scale, ported from the iOS app's own ad hoc screen
 * measurements — no such scale existed on Android before M7 (see
 * DESIGN.md's "Spacing & shape" section, added alongside this file).
 */
object Spacing {
    val screenPadding: Dp = 24.dp
    val cardPadding: Dp = 16.dp
    val cardCornerRadius: Dp = 16.dp
    val itemSpacing: Dp = 16.dp
    val smallSpacing: Dp = 8.dp
    val buttonVerticalPadding: Dp = 14.dp
}
