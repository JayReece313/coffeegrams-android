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

    /**
     * [LivePurchases] needs a foreground `Activity` to launch the Play
     * billing flow. Attaching only while this Activity is started (not for
     * the whole process lifetime) means a destroyed instance — e.g. across
     * a configuration change — is never held onto.
     */
    override fun onStart() {
        super.onStart()
        (application as CoffeeGramsApplication).livePurchases.attach(this)
    }

    override fun onStop() {
        (application as CoffeeGramsApplication).livePurchases.detach()
        super.onStop()
    }
}
