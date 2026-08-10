package com.jrlabapps.coffeegrams.ui.methodpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrlabapps.coffeegrams.R
import com.jrlabapps.coffeegrams.core.BrewMethod
import com.jrlabapps.coffeegrams.design.icon
import com.jrlabapps.coffeegrams.design.subtitle
import com.jrlabapps.coffeegrams.ui.currentApplication
import com.jrlabapps.coffeegrams.ui.paywall.PaywallScreen
import com.jrlabapps.coffeegrams.ui.theme.Spacing

/**
 * The app's navigation root: every [BrewMethod], with a Pro gate on the
 * methods [BrewMethod.isFreeTier] doesn't cover. No dedicated ViewModel —
 * the only dynamic state is [com.jrlabapps.coffeegrams.viewmodel.PurchaseController],
 * read directly, matching the iOS screen this mirrors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodPickerScreen(onMethodSelected: (BrewMethod) -> Unit, onViewLog: () -> Unit = {}, modifier: Modifier = Modifier) {
    val purchases = currentApplication().purchaseController
    val isPremiumUnlocked by purchases.isPremiumUnlocked.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    val viewLogDescription = stringResource(R.string.method_picker_view_log)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (!isPremiumUnlocked) {
                        TextButton(onClick = { showPaywall = true }) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null)
                            Spacer(Modifier.width(Spacing.smallSpacing))
                            Text(stringResource(R.string.method_picker_unlock_pro))
                        }
                    }
                    IconButton(onClick = onViewLog) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = viewLogDescription)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.smallSpacing),
        ) {
            items(BrewMethod.entries) { method ->
                // isPremiumUnlocked is read above so this composable recomposes
                // when it changes; canAccess itself re-derives from the same
                // PurchaseController rather than duplicating its logic here.
                val accessible = purchases.canAccess(method)
                MethodRow(
                    method = method,
                    accessible = accessible,
                    onClick = { if (accessible) onMethodSelected(method) else showPaywall = true },
                )
            }
        }
    }

    if (showPaywall) {
        ModalBottomSheet(onDismissRequest = { showPaywall = false }) {
            PaywallScreen(onDismiss = { showPaywall = false })
        }
    }
}

@Composable
private fun MethodRow(method: BrewMethod, accessible: Boolean, onClick: () -> Unit) {
    val hint = if (accessible) {
        stringResource(R.string.method_picker_unlocked_hint, method.displayName)
    } else {
        stringResource(R.string.method_picker_locked_hint)
    }
    val description = if (accessible) {
        method.displayName
    } else {
        stringResource(R.string.method_picker_locked_description, method.displayName)
    }

    Surface(
        shape = RoundedCornerShape(Spacing.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = hint, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.cardPadding)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = method.icon, contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(Spacing.itemSpacing))
            Column(Modifier.weight(1f)) {
                Text(method.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    method.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!accessible) {
                Spacer(Modifier.width(Spacing.smallSpacing))
                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.method_picker_pro_badge)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
        }
    }
}
