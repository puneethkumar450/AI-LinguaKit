package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.service.AppThemeMode
import com.puneeth50.ailinguakit.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenUI(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenLanguageChange: () -> Unit,
    onOpenLanguagePacks: () -> Unit,
    onOpenRewards: () -> Unit
) {
    val themeMode by vm.themeMode.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val freeWordsUsed by vm.freeWordsUsed.collectAsState()
    val allPacks by vm.allPacks.collectAsState()
    val remainingWords = vm.remainingFreeWords()
    val downloadedCount = allPacks.count { it.status.name == "DOWNLOADED" }

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
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    subtitle = stringResource(R.string.rewards_settings_subtitle),
                    onClick = onOpenRewards
                )
            }
            item {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors().copy(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.plan_usage_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isPremium) {
                            Text(stringResource(R.string.premium_active))
                        } else {
                            Text(stringResource(R.string.words_left_template, remainingWords))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.downloaded_languages_template, downloadedCount))
                        }
                    }
                }
            }
        }
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
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor  = MaterialTheme.colorScheme.onPrimaryContainer
        ),
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
