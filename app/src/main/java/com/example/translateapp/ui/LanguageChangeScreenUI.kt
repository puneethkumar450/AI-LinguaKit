package com.example.translateapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.translateapp.R
import com.example.translateapp.service.AppLanguageOption
import com.example.translateapp.service.AppSettingsManager
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.example.translateapp.viewmodel.AppViewModel

@Composable
fun LanguageChangeScreenUI(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val appLanguageTag by vm.appLanguageTag.collectAsState()

    LanguageChangeScreenUIContent(
        appLanguageTag = appLanguageTag,
        supportedLanguages = vm.supportedAppLanguages,
        onLanguageSelect = { vm.setAppLanguage(it) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageChangeScreenUIContent(
    appLanguageTag: String,
    supportedLanguages: List<AppLanguageOption>,
    onLanguageSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_change_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.app_language_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(supportedLanguages, key = { it.languageTag }) { option ->
                LanguageOptionItem(
                    option = option,
                    isSelected = appLanguageTag == option.languageTag,
                    onClick = { onLanguageSelect(option.languageTag) }
                )
            }
        }
    }
}

@Composable
fun LanguageOptionItem(
    option: AppLanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
                Text(option.displayName, color = MaterialTheme.colorScheme.onSecondary)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageChangeScreenUIPreview() {
    TranslateAppTheme(darkTheme = true) {
        LanguageChangeScreenUIContent(
            appLanguageTag = "en",
            supportedLanguages = AppSettingsManager.supportedLanguages(),
            onLanguageSelect = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageChangeScreenItemUIPreview() {
    TranslateAppTheme {
        LanguageOptionItem(
            option = AppLanguageOption("en", "English"),
            isSelected = true,
            onClick = {}
        )
    }
}
