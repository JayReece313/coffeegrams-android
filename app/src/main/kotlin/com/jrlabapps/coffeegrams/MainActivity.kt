package com.jrlabapps.coffeegrams

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jrlabapps.coffeegrams.ui.theme.CoffeeGramsTheme

/**
 * The single Activity hosting the whole Compose app.
 *
 * `enableEdgeToEdge()` is called before `setContent` because edge-to-edge is
 * enforced from API 35 — opting in explicitly means the insets behaviour is the
 * same on every supported API level rather than changing under us at 35.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CoffeeGramsTheme {
                CoffeeGramsApp()
            }
        }
    }
}

/**
 * Root composable. M7 replaces this placeholder with the navigation host and the
 * five real screens (method picker, calculator, guided brew, espresso, cold brew).
 */
@Composable
private fun CoffeeGramsApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Text(
            text = "CoffeeGrams",
            modifier = Modifier.padding(innerPadding),
        )
    }
}
