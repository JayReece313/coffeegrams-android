package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// M3 has more color roles than the brand has tokens (see DESIGN.md), so only
// the roles DESIGN.md's mapping table names are overridden here — every other
// role (secondary, error, outline, scrim, ...) is left at Material3's own
// default baseline rather than inventing brand-adjacent colors the palette
// doesn't define.
private val LightColors = lightColorScheme(
    background = BackgroundLight,
    surface = BackgroundLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    primary = AccentLight,
    onPrimary = BackgroundLight,
    tertiary = TimerActiveLight,
)

private val DarkColors = darkColorScheme(
    background = BackgroundDark,
    surface = BackgroundDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    primary = AccentDark,
    onPrimary = BackgroundDark,
    tertiary = TimerActiveDark,
)

/**
 * CoffeeGrams' Material 3 theme. No Dynamic Color: the brand identity must
 * match the iOS build exactly, so the palette is fixed rather than derived
 * from the user's wallpaper — a deliberate exception to the usual Android
 * default (see DESIGN.md).
 */
@Composable
fun CoffeeGramsTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = CoffeeGramsTypography,
        content = content,
    )
}
