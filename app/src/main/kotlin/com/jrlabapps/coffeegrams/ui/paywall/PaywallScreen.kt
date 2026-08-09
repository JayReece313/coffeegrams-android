package com.jrlabapps.coffeegrams.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Paywall content, presented as a `ModalBottomSheet` by
 * [com.jrlabapps.coffeegrams.ui.methodpicker.MethodPickerScreen] — matches
 * the iOS app's sheet, not a pushed nav destination. Auto-dismisses the
 * moment [com.jrlabapps.coffeegrams.viewmodel.PurchaseController.isPremiumUnlocked]
 * flips true, from any source (a successful purchase here, or restore).
 */
@Composable
fun PaywallScreen(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val purchases = currentApplication().purchaseController
    val isPremiumUnlocked by purchases.isPremiumUnlocked.collectAsStateWithLifecycle()
    val priceText by purchases.priceText.collectAsStateWithLifecycle()
    val isWorking by purchases.isWorking.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isPremiumUnlocked) {
        if (isPremiumUnlocked) onDismiss()
    }

    Column(
        modifier = modifier.padding(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing),
    ) {
        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(44.dp))
        Text(stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.paywall_subtitle), style = MaterialTheme.typography.bodyMedium)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing)) {
            listOf(
                R.string.paywall_benefit_all_methods,
                R.string.paywall_benefit_guided_timers,
                R.string.paywall_benefit_full_log,
                R.string.paywall_benefit_one_time,
            ).forEach { benefit ->
                Text("• " + stringResource(benefit), style = MaterialTheme.typography.bodyMedium)
            }
        }

        val price = priceText
        val buyLabel = when {
            isWorking -> stringResource(R.string.paywall_working)
            price != null -> stringResource(R.string.paywall_buy_button_with_price, price)
            else -> stringResource(R.string.paywall_buy_button)
        }
        Button(
            onClick = { scope.launch { if (purchases.purchase()) onDismiss() } },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(buyLabel)
        }

        TextButton(
            onClick = { scope.launch { purchases.restore() } },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.paywall_restore_button))
        }

        Text(
            stringResource(R.string.paywall_footnote),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
