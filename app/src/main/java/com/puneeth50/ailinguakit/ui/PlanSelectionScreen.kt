package com.puneeth50.ailinguakit.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val isPremium by vm.isPremium.collectAsState()
    val product by vm.premiumProduct.collectAsState()
    val billingReady by vm.billingReady.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_your_plan)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlanCard(
                title = stringResource(R.string.plan_free_title),
                subtitle = stringResource(R.string.plan_free_subtitle),
                points = listOf(
                    stringResource(R.string.plan_point_free_languages),
                    stringResource(R.string.plan_point_free_words),
                    stringResource(R.string.plan_point_free_ads),
                    stringResource(R.string.plan_point_free_manual)
                ),
                emphasized = !isPremium
            )

            PlanCard(
                title = stringResource(R.string.plan_premium_title),
                subtitle = product?.formattedPrice ?: stringResource(R.string.plan_premium_pricing_fallback),
                points = listOf(
                    stringResource(R.string.plan_point_premium_words),
                    stringResource(R.string.plan_point_premium_languages),
                    stringResource(R.string.plan_point_premium_ads),
                    stringResource(R.string.plan_point_premium_speech)
                ),
                emphasized = isPremium
            )

            if (isPremium) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(stringResource(R.string.premium_active))
                }
            } else {
                Button(
                    onClick = {
                        val activity = context as? Activity ?: return@Button
                        vm.launchPremiumPurchase(activity)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = billingReady
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        if (billingReady) stringResource(R.string.upgrade_now)
                        else stringResource(R.string.connecting_play_billing)
                    )
                }
            }

            Text(
                stringResource(R.string.billing_note),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = { vm.refreshPremiumState() }) {
                Text(stringResource(R.string.refresh_purchase_status))
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    points: List<String>,
    emphasized: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• $point")
                }
            }
        }
    }
}
