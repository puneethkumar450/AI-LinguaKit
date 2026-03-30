package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.ui.theme.AccentGold
import com.puneeth50.ailinguakit.ui.theme.AccentGoldDark
import com.puneeth50.ailinguakit.ui.theme.BrandBlue
import com.puneeth50.ailinguakit.ui.theme.highlightCardBrush
import com.puneeth50.ailinguakit.viewmodel.RewardCardUi
import com.puneeth50.ailinguakit.viewmodel.RewardsViewModel
import kotlinx.coroutines.delay
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreenUI(
    onBack: () -> Unit,
    onOpenPlanSelection: () -> Unit,
    vm: RewardsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rewards_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPlanSelection) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = stringResource(R.string.rewards_open_premium)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GradientRewardsCard(
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        stringResource(R.string.rewards_header_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        stringResource(
                            if (state.isPremium) R.string.rewards_header_premium_body else R.string.rewards_header_free_body
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RewardsSummaryChip(
                            title = stringResource(R.string.rewards_words_left),
                            value = if (state.isPremium) stringResource(R.string.rewards_unlimited) else state.remainingWords.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        RewardsSummaryChip(
                            title = stringResource(R.string.rewards_language_slots),
                            value = if (state.isPremium) {
                                stringResource(R.string.rewards_unlimited)
                            } else {
                                "${state.downloadedLanguages}/${maxOf(vm.maxFreeLanguages() + state.extraSlotCredits, state.downloadedLanguages)}"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    RewardsStatusPill(text = stringResource(state.activeRewardLabelRes))
                }
            }

            item {
                Text(
                    stringResource(R.string.rewards_wallet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                RewardsWalletCard(
                    state = state,
                    nowMs = rememberCurrentTimeMillis()
                )
            }

            item {
                Text(
                    stringResource(R.string.rewards_available_now),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = state.rewardCards,
                key = { it.type.name }
            ) { reward ->
                RewardFeatureCard(
                    reward = reward,
                    onAction = { vm.requestReward(reward.type) }
                )
            }
        }
    }
}

@Composable
private fun RewardsWalletCard(
    state: com.puneeth50.ailinguakit.viewmodel.RewardsUiState,
    nowMs: Long
) {
    GradientRewardsCard {
        RewardBalanceRow(
            icon = Icons.Default.Bolt,
            title = stringResource(R.string.reward_extra_words_title),
            value = if (state.isPremium) {
                stringResource(R.string.rewards_unlimited)
            } else {
                stringResource(R.string.rewards_bonus_words_template, state.bonusWordsRemaining)
            }
        )
        RewardBalanceRow(
            icon = Icons.Default.Language,
            title = stringResource(R.string.reward_extra_slot_title),
            value = if (state.isPremium) {
                stringResource(R.string.rewards_unlimited)
            } else {
                stringResource(R.string.rewards_extra_slots_template, state.extraSlotCredits)
            }
        )
        RewardBalanceRow(
            icon = Icons.Default.GraphicEq,
            title = stringResource(R.string.reward_speech_title),
            value = if (state.isPremium) {
                stringResource(R.string.rewards_included_in_plan)
            } else {
                rewardExpiryText(
                    nowMs = nowMs,
                    untilMs = state.speechUnlockUntilMs,
                    activeLabel = R.string.rewards_time_left_template,
                    inactiveLabel = R.string.rewards_not_active
                )
            }
        )
        RewardBalanceRow(
            icon = Icons.Default.Campaign,
            title = stringResource(R.string.reward_ad_free_title),
            value = if (state.isPremium) {
                stringResource(R.string.rewards_included_in_plan)
            } else {
                rewardExpiryText(
                    nowMs = nowMs,
                    untilMs = state.adFreeUntilMs,
                    activeLabel = R.string.rewards_time_left_template,
                    inactiveLabel = R.string.rewards_not_active
                )
            }
        )
    }
}

@Composable
private fun RewardsSummaryChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun RewardsStatusPill(
    text: String,
    containerColor: Color = Color.White.copy(alpha = 0.14f),
    textColor: Color = Color.White,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun RewardFeatureCard(
    reward: RewardCardUi,
    onAction: () -> Unit
) {
    GradientRewardsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(reward.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            RewardsStatusPill(
                text = stringResource(reward.badgeRes),
                containerColor = if (reward.badgeRes == R.string.rewards_active_badge) {
                    activeRewardBadgeContainerColor()
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
                textColor = if (reward.badgeRes == R.string.rewards_active_badge) {
                    activeRewardBadgeTextColor()
                } else {
                    Color.White
                },
                fontWeight = if (reward.badgeRes == R.string.rewards_active_badge) {
                    FontWeight.Bold
                } else {
                    FontWeight.SemiBold
                }
            )
        }
        Text(
            text = stringResource(reward.subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
        Button(
            onClick = onAction,
            enabled = reward.enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = BrandBlue,
                disabledContainerColor = Color.White.copy(alpha = 0.18f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(stringResource(reward.actionLabelRes))
        }
    }
}

@Composable
private fun RewardBalanceRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Box(
                modifier = Modifier.padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentGold)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun GradientRewardsCard(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(highlightCardBrush())
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun rememberCurrentTimeMillis(): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun rewardExpiryText(
    nowMs: Long,
    untilMs: Long,
    activeLabel: Int,
    inactiveLabel: Int
): String {
    if (untilMs <= nowMs) {
        return stringResource(inactiveLabel)
    }
    val minutesLeft = ceil((untilMs - nowMs) / 60_000.0).toInt().coerceAtLeast(1)
    return stringResource(activeLabel, minutesLeft)
}

@Composable
private fun activeRewardBadgeContainerColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        AccentGold.copy(alpha = 0.18f)
    } else {
        AccentGoldDark.copy(alpha = 0.2f)
    }
}

@Composable
private fun activeRewardBadgeTextColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        AccentGoldDark
    } else {
        AccentGold
    }
}
