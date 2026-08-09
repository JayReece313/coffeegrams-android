package com.jrlabapps.coffeegrams

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jrlabapps.coffeegrams.ui.navigation.CoffeeGramsNavHost
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
                CoffeeGramsNavHost()
            }
        }
    }
}
