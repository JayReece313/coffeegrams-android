package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Placeholder theme. M3 replaces these schemes with the real CoffeeGrams palette
 * (Cream / Espresso Brown / Caramel, 60-30-10) mapped onto Material 3 roles.
 *
 * Note there is deliberately no Dynamic Color support: CoffeeGrams is a branded
 * app whose identity must match the iOS build, so the palette is fixed rather
 * than derived from the user's wallpaper.
 */
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun CoffeeGramsTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
