package com.puneeth50.ailinguakit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.TranslationHistoryEntity
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import com.puneeth50.ailinguakit.viewmodel.HistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onBack: () -> Unit, onRestore: () -> Unit) {
    val allHistory by vm.allHistory.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val filter by vm.historyFilter.collectAsState()
    val searchQuery by vm.historySearchQuery.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    val displayList = remember(allHistory, favorites, filter, searchQuery) {
        val base = if (filter == HistoryFilter.FAVORITES) favorites else allHistory
        if (searchQuery.isBlank()) base
        else base.filter {
            it.sourceText.contains(searchQuery, ignoreCase = true) ||
                    it.translatedText.contains(searchQuery, ignoreCase = true) ||
                    it.sourceLangName.contains(searchQuery, ignoreCase = true) ||
                    it.targetLangName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = 16.dp)) {
            // ── Search bar ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setHistorySearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.setHistorySearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.history_search_clear_desc))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Gray.copy(0.15f),
                    unfocusedContainerColor = Color.Gray.copy(0.15f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Filter chips + clear button ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == HistoryFilter.ALL,
                        onClick = { vm.setHistoryFilter(HistoryFilter.ALL) },
                        label = { Text(stringResource(R.string.history_filter_all_template, allHistory.size)) },
                        leadingIcon = if (filter == HistoryFilter.ALL) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                    FilterChip(
                        selected = filter == HistoryFilter.FAVORITES,
                        onClick = { vm.setHistoryFilter(HistoryFilter.FAVORITES) },
                        label = { Text(stringResource(R.string.history_filter_starred_template, favorites.size)) },
                        leadingIcon = if (filter == HistoryFilter.FAVORITES) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
                if (allHistory.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.history_clear_all_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────────────────────
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (filter == HistoryFilter.FAVORITES) Icons.Default.StarBorder else Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) stringResource(R.string.history_empty_search_template, searchQuery)
                            else if (filter == HistoryFilter.FAVORITES) stringResource(R.string.history_empty_starred)
                            else stringResource(R.string.history_empty_all),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ── History list ─────────────────────────────────────────────────
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayList,
                        key = { _, item -> item.id }
                    ) { _, entry ->
                        val isExpanded = expandedId == entry.id
                        HistoryCard(
                            entry = entry,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedId = if (isExpanded) null else entry.id
                            },
                            onFavoriteToggle = { vm.toggleFavorite(entry) },
                            onDelete = { vm.deleteHistoryEntry(entry) },
                            onRestore = {
                                vm.restoreFromHistory(entry)
                                onRestore()
                            },
                            modifier = Modifier.animateItemPlacement(tween(200))
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // ── Clear confirmation dialog ────────────────────────────────────────────
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                title = { Text(stringResource(R.string.history_clear_dialog_title)) },
                text = { Text(stringResource(R.string.history_clear_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.clearHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.history_clear_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.history_clear_cancel)) }
                }
            )
        }
    }
}

@Composable
private fun HistoryCard(
    entry: TranslationHistoryEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(200))
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =  MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header row: language pair + timestamp + favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        entry.sourceLangName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        entry.targetLangName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dateFormatter.format(Date(entry.timestampMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (entry.isFavorite) stringResource(R.string.history_unstar_desc) else stringResource(R.string.history_star_desc),
                            tint = if (entry.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Source text (always visible, collapsed)
            Text(
                entry.sourceText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Divider + translation (only when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 2 },
                exit = fadeOut(tween(100))
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        entry.translatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.reuse), style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Collapse indicator
            if (!isExpanded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tap_to_see_translation),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@Composable
@Preview(showBackground = true)
fun HistoryScreenPreview() {
    Text("History screen preview is available through individual card previews.")
}

@Preview(showBackground = true)
@Composable
fun HistoryCardPreview_Collapsed() {

    fun getDummyHistory() = TranslationHistoryEntity(
        id = 1,
        sourceText = "Hello",
        translatedText = "Hola",
        sourceLangName = "English",
        targetLangName = "Spanish",
        isFavorite = false,
        sourceLangCode = "en",
        targetLangCode = "es")
    HistoryCard(
        entry = getDummyHistory(),
        isExpanded = false,
        onToggleExpand = {},
        onFavoriteToggle = {},
        onDelete = {},
        onRestore = {}
    )
}
