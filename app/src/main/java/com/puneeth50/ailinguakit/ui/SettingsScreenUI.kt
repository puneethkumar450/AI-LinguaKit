package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.service.AppThemeMode
import com.puneeth50.ailinguakit.service.PremiumPlanOption
import com.puneeth50.ailinguakit.service.PremiumPlanType
import com.puneeth50.ailinguakit.ui.theme.AccentGold
import com.puneeth50.ailinguakit.ui.theme.BrandBlue
import com.puneeth50.ailinguakit.ui.theme.highlightCardBrush
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenUI(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenLanguageChange: () -> Unit,
    onOpenLanguagePacks: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenPlanSelection: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val themeMode by vm.themeMode.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val availablePremiumPlans by vm.availablePremiumPlans.collectAsState()
    val activePremiumPlanType by vm.activePremiumPlanType.collectAsState()
    val rewardWallet by vm.rewardWallet.collectAsState()
    val rewardsSubtitle = rememberRewardSettingsSubtitle(
        isPremium = isPremium,
        bonusWordsRemaining = rewardWallet.bonusWordsRemaining,
        extraSlotCredits = rewardWallet.extraSlotCredits,
        speechUnlockUntilMs = rewardWallet.speechUnlockUntilMs,
        adFreeUntilMs = rewardWallet.adFreeUntilMs
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentPlanCard(
                    isPremium = isPremium,
                    availablePlans = availablePremiumPlans,
                    activePlanType = activePremiumPlanType,
                    onClick = onOpenPlanSelection
                )
            }

            item {
                Card(shape = RoundedCornerShape(18.dp),
                   /* colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer)*/
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.theme_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemeOptionRow(
                            label = stringResource(R.string.theme_mode_system),
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { vm.setThemeMode(AppThemeMode.SYSTEM) }
                        )
                        ThemeOptionRow(
                            label = stringResource(R.string.theme_mode_light),
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { vm.setThemeMode(AppThemeMode.LIGHT) }
                        )
                        ThemeOptionRow(
                            label = stringResource(R.string.theme_mode_dark),
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { vm.setThemeMode(AppThemeMode.DARK) }
                        )
                    }
                }
            }

            item {
                SettingsActionCard(
                    title = stringResource(R.string.app_language_title),
                    subtitle = stringResource(R.string.app_language_subtitle),
                    onClick = onOpenLanguageChange
                )
            }

            item {
                SettingsActionCard(
                    title = stringResource(R.string.language_packs_title),
                    subtitle = stringResource(R.string.language_packs_subtitle),
                    onClick = onOpenLanguagePacks
                )
            }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.rewards_title),
                    subtitle = rewardsSubtitle,
                    onClick = onOpenRewards
                )
            }

            item {
                SettingsActionCard(
                    title = stringResource(R.string.privacy_policy_title),
                    subtitle = stringResource(R.string.privacy_policy_subtitle, stringResource(R.string.app_name)),
                    onClick = onOpenPrivacyPolicy
                )
            }
        }
    }
}

@Composable
private fun CurrentPlanCard(
    isPremium: Boolean,
    availablePlans: List<PremiumPlanOption>,
    activePlanType: PremiumPlanType?,
    onClick: () -> Unit
) {
    val backgroundBrush = highlightCardBrush()

    val activePlan = activePlanType?.let { selectedType ->
        availablePlans.firstOrNull { it.type == selectedType }
    }

    val title = if (isPremium) {
        currentPremiumTitle(activePlanType)
    } else {
        stringResource(R.string.settings_plan_free_title)
    }
    val subtitle = if (isPremium) {
        currentPremiumSubtitle(activePlanType, activePlan)
    } else {
        stringResource(R.string.settings_plan_free_subtitle)
    }
    val body = if (isPremium) {
        currentPremiumBody(activePlanType)
    } else {
        stringResource(R.string.settings_plan_free_body)
    }
    val buttonLabel = if (isPremium) {
        stringResource(R.string.settings_plan_view_benefits)
    } else {
        stringResource(R.string.settings_plan_upgrade_cta)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanChip(
                            text = if (isPremium) {
                                stringResource(R.string.settings_plan_chip_premium)
                            } else {
                                stringResource(R.string.settings_plan_chip_free)
                            },
                            background = if (isPremium) AccentGold else Color.White.copy(alpha = 0.18f),
                            content = if (isPremium) BrandBlue else Color.White
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }

                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isPremium) {
                            Color.White.copy(alpha = 0.12f)
                        } else {
                            AccentGold.copy(alpha = 0.18f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPremium) AccentGold else AccentGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                if (!isPremium) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePlans
                            .sortedBy { it.type.settingsOrder() }
                            .forEach { plan ->
                                PlanMiniPill(
                                    text = when (plan.type) {
                                        PremiumPlanType.MONTHLY -> stringResource(
                                            R.string.settings_plan_chip_monthly_template,
                                            plan.formattedPrice
                                        )
                                        PremiumPlanType.YEARLY -> stringResource(
                                            R.string.settings_plan_chip_yearly_template,
                                            plan.formattedPrice
                                        )
                                        PremiumPlanType.LIFETIME -> stringResource(
                                            R.string.settings_plan_chip_lifetime_template,
                                            plan.formattedPrice
                                        )
                                    }
                                )
                            }
                    }
                }

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = if (isPremium) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandBlue
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = BrandBlue
                        )
                    }
                ) {
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanChip(
    text: String,
    background: Color,
    content: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlanMiniPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun currentPremiumTitle(activePlanType: PremiumPlanType?): String {
    return when {
        activePlanType == PremiumPlanType.LIFETIME -> stringResource(R.string.settings_plan_lifetime_title)
        activePlanType == PremiumPlanType.YEARLY -> stringResource(R.string.settings_plan_yearly_title)
        activePlanType == PremiumPlanType.MONTHLY -> stringResource(R.string.settings_plan_monthly_title)
        else -> stringResource(R.string.settings_plan_premium_title)
    }
}

@Composable
private fun currentPremiumSubtitle(
    activePlanType: PremiumPlanType?,
    activePlan: PremiumPlanOption?
): String {
    val price = activePlan?.formattedPrice ?: stringResource(R.string.see_play_store)
    return when {
        activePlanType == PremiumPlanType.LIFETIME -> stringResource(R.string.settings_plan_lifetime_subtitle)
        activePlanType == PremiumPlanType.YEARLY -> stringResource(R.string.settings_plan_yearly_subtitle, price)
        activePlanType == PremiumPlanType.MONTHLY -> stringResource(R.string.settings_plan_monthly_subtitle, price)
        activePlan != null -> stringResource(R.string.settings_plan_premium_subtitle_generic, price)
        else -> stringResource(R.string.premium_active)
    }
}

@Composable
private fun currentPremiumBody(activePlanType: PremiumPlanType?): String {
    return when {
        activePlanType == PremiumPlanType.LIFETIME -> stringResource(R.string.settings_plan_lifetime_body)
        activePlanType == PremiumPlanType.YEARLY -> stringResource(R.string.settings_plan_subscription_body_yearly)
        activePlanType == PremiumPlanType.MONTHLY -> stringResource(R.string.settings_plan_subscription_body_monthly)
        else -> stringResource(R.string.settings_plan_subscription_body_generic)
    }
}

private fun PremiumPlanType.settingsOrder(): Int = when (this) {
    PremiumPlanType.YEARLY -> 0
    PremiumPlanType.MONTHLY -> 1
    PremiumPlanType.LIFETIME -> 2
}

@Composable
private fun rememberRewardSettingsSubtitle(
    isPremium: Boolean,
    bonusWordsRemaining: Int,
    extraSlotCredits: Int,
    speechUnlockUntilMs: Long,
    adFreeUntilMs: Long
): String {
    if (isPremium) {
        return stringResource(R.string.rewards_status_premium)
    }

    val nowMs = System.currentTimeMillis()
    return when {
        bonusWordsRemaining > 0 -> stringResource(R.string.rewards_settings_words_template, bonusWordsRemaining)
        extraSlotCredits > 0 -> stringResource(R.string.rewards_settings_slots_template, extraSlotCredits)
        speechUnlockUntilMs > nowMs -> {
            val minutes = ceil((speechUnlockUntilMs - nowMs) / 60_000.0).toInt().coerceAtLeast(1)
            stringResource(R.string.rewards_settings_speech_template, minutes)
        }
        adFreeUntilMs > nowMs -> {
            val minutes = ceil((adFreeUntilMs - nowMs) / 60_000.0).toInt().coerceAtLeast(1)
            stringResource(R.string.rewards_settings_ad_free_template, minutes)
        }
        else -> stringResource(R.string.rewards_settings_subtitle)
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        /*colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor  = MaterialTheme.colorScheme.onPrimaryContainer
        ),*/
        modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
