package com.puneeth50.ailinguakit.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.ui.theme.AccentGold
import com.puneeth50.ailinguakit.ui.theme.BrandBlue
import com.puneeth50.ailinguakit.viewmodel.AppViewModel

private enum class SelectedPlan {
    FREE,
    PREMIUM
}

@Composable
fun PlanSelectionScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val isPremium by vm.isPremium.collectAsState()
    val product by vm.premiumProduct.collectAsState()
    val billingReady by vm.billingReady.collectAsState()
    val context = LocalContext.current
    var selectedPlan by rememberSaveable(isPremium) {
        mutableStateOf(if (isPremium) SelectedPlan.PREMIUM else SelectedPlan.PREMIUM)
    }

    val backgroundTop = BrandBlue
    val backgroundMid = Color(0xFF3459D1)
    val backgroundBottom = Color(0xFF2946A7)
    val optionSurface = Color.White.copy(alpha = 0.10f)
    val optionBorder = Color.White.copy(alpha = 0.14f)
    val highlightBorder = AccentGold.copy(alpha = 0.85f)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(backgroundTop, backgroundMid, backgroundBottom)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 22.dp, vertical = 24.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    modifier = Modifier.size(138.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = stringResource(R.string.plan_hero_line_one),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.plan_hero_line_two),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.plan_hero_highlight),
                    color = AccentGold,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(5) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.plan_social_proof),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, optionBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        PremiumBenefitRow(stringResource(R.string.plan_benefit_unlimited_words))
                        PremiumBenefitRow(stringResource(R.string.plan_benefit_unlimited_languages))
                        PremiumBenefitRow(stringResource(R.string.plan_benefit_speech))
                        PremiumBenefitRow(stringResource(R.string.plan_benefit_no_ads))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = stringResource(R.string.choose_your_plan),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlanOptionCard(
                    title = stringResource(R.string.plan_premium_choice_title),
                    subtitle = product?.formattedPrice ?: stringResource(R.string.plan_premium_pricing_fallback),
                    trailing = stringResource(R.string.plan_premium_choice_trailing),
                    badge = stringResource(R.string.plan_most_popular),
                    selected = selectedPlan == SelectedPlan.PREMIUM || isPremium,
                    containerColor = optionSurface,
                    borderColor = if (selectedPlan == SelectedPlan.PREMIUM || isPremium) highlightBorder else optionBorder,
                    textColor = Color.White,
                    accentColor = AccentGold,
                    onClick = { selectedPlan = SelectedPlan.PREMIUM }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlanOptionCard(
                    title = stringResource(R.string.plan_free_choice_title),
                    subtitle = stringResource(R.string.plan_free_choice_subtitle),
                    trailing = stringResource(R.string.plan_free_choice_trailing),
                    badge = null,
                    selected = selectedPlan == SelectedPlan.FREE && !isPremium,
                    containerColor = optionSurface,
                    borderColor = if (selectedPlan == SelectedPlan.FREE && !isPremium) Color.White.copy(alpha = 0.44f) else optionBorder,
                    textColor = Color.White,
                    accentColor = Color.White,
                    onClick = { if (!isPremium) selectedPlan = SelectedPlan.FREE }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(34.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, optionBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedPlan == SelectedPlan.PREMIUM || isPremium) {
                                stringResource(R.string.plan_footer_premium)
                            } else {
                                stringResource(R.string.plan_footer_free)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isPremium) {
                                    onBack()
                                } else if (selectedPlan == SelectedPlan.PREMIUM) {
                                    val activity = context as? Activity ?: return@Button
                                    vm.launchPremiumPurchase(activity)
                                } else {
                                    onBack()
                                }
                            },
                            enabled = isPremium || selectedPlan == SelectedPlan.FREE || billingReady,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = BrandBlue
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = when {
                                    isPremium -> stringResource(R.string.premium_active)
                                    selectedPlan == SelectedPlan.PREMIUM && billingReady -> stringResource(R.string.plan_start_premium)
                                    selectedPlan == SelectedPlan.PREMIUM -> stringResource(R.string.connecting_play_billing)
                                    else -> stringResource(R.string.plan_continue_free)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.refresh_purchase_status),
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { vm.refreshPremiumState() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AccentGold.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    subtitle: String,
    trailing: String,
    badge: String?,
    selected: Boolean,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box {
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentGold)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = badge,
                    color = BrandBlue,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (badge != null) 20.dp else 0.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (selected) accentColor else Color.White.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = textColor.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = trailing,
                    color = textColor.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
