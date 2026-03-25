package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.viewmodel.LanguageOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(
    titleRes: Int,
    selected: LanguageOption,
    options: List<LanguageOption>,
    downloadedCodes: Set<String> = emptySet(),
    blockedCode: String? = null,
    onSelect: (LanguageOption) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        var searchQuery by remember { mutableStateOf("") }
        val localizedOptions = remember(options, context) {
            options.associateWith { it.displayName(context) }
        }
        val filteredOptions = remember(searchQuery, options, localizedOptions) {
            if (searchQuery.isBlank()) options
            else options.filter { localizedOptions[it].orEmpty().contains(searchQuery, ignoreCase = true) }
        }

        val (downloaded, others) = remember(filteredOptions, downloadedCodes) {
            filteredOptions.partition { downloadedCodes.contains(it.mlKitCode) || it.mlKitCode == "auto" }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_languages), color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Gray.copy(0.15f),
                    unfocusedContainerColor = Color.Gray.copy(0.15f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (downloaded.isNotEmpty()) {
                    item { SectionHeaderTitle(stringResource(R.string.downloaded_packs)) }
                    items(downloaded) { lang ->
                        LanguageItem(
                            lang = lang,
                            selected = selected,
                            enabled = lang.code != blockedCode,
                            supportingText = stringResource(R.string.ready_to_use),
                            onSelect = onSelect
                        )
                    }
                }

                if (others.isNotEmpty()) {
                    item { SectionHeaderTitle(stringResource(R.string.other_languages)) }
                    items(others) { lang ->
                        LanguageItem(
                            lang = lang,
                            selected = selected,
                            enabled = false,
                            supportingText = stringResource(R.string.download_from_language_packs_first),
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}
