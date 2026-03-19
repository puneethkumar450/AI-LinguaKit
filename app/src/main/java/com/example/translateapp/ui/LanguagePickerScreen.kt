package com.example.translateapp.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.translateapp.R
import com.example.translateapp.viewmodel.LanguageOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(
    title: String,
    selected: LanguageOption,
    options: List<LanguageOption>,
    downloadedCodes: Set<String> = emptySet(),
    blockedCode: String? = null,
    onSelect: (LanguageOption) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        var searchQuery by remember { mutableStateOf("") }
        val filteredOptions = remember(searchQuery, options) {
            if (searchQuery.isBlank()) options
            else options.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
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
                    item { SectionHeaderTitle("Downloaded Packs") }
                    items(downloaded) { lang ->
                        LanguageItem(
                            lang = lang,
                            selected = selected,
                            enabled = lang.code != blockedCode,
                            supportingText = "Ready to use",
                            onSelect = onSelect
                        )
                    }
                }

                if (others.isNotEmpty()) {
                    item { SectionHeaderTitle("Other Languages") }
                    items(others) { lang ->
                        LanguageItem(
                            lang = lang,
                            selected = selected,
                            enabled = false,
                            supportingText = "Download from Language Packs first",
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}
