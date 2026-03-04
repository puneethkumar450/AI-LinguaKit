package com.example.translateapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePacksScreen(vm: AppViewModel) {
    val allPacks        by vm.allPacks.collectAsState()
    val downloading     by vm.downloadingPacks.collectAsState()
    var wifiOnly        by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf<LanguagePackEntity?>(null) }
    var showDownloadAll  by remember { mutableStateOf(false) }

    val downloadedCount  = allPacks.count { it.status == PackStatus.DOWNLOADED }
    val totalCount       = allPacks.size
    val storageUsedMb    = allPacks.filter { it.status == PackStatus.DOWNLOADED }
        .sumOf { it.approximateSizeKb } / 1024

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header summary card ───────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Offline Language Packs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$downloadedCount / $totalCount downloaded  •  ~${storageUsedMb} MB used",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { vm.refreshPackStatus() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) downloadedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )

                Spacer(Modifier.height(12.dp))

                // Wi-Fi toggle + download all
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (wifiOnly) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (wifiOnly) "Wi-Fi only" else "Any network",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = { wifiOnly = it },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (downloadedCount < totalCount) {
                        TextButton(
                            onClick = { showDownloadAll = true },
                            enabled = downloading.isEmpty()
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Download All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // ── Info banner ───────────────────────────────────────────────────────
        if (downloadedCount == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Each pack is ~30 MB. Once downloaded, translations work with no internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Pack list ─────────────────────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Downloaded section
            val downloadedPacks = allPacks.filter { it.status == PackStatus.DOWNLOADED }
            val otherPacks = allPacks.filter { it.status != PackStatus.DOWNLOADED }

            if (downloadedPacks.isNotEmpty()) {
                item {
                    SectionHeader("Downloaded (${downloadedPacks.size})", Icons.Default.CheckCircle, Color(0xFF16A34A))
                }
                items(downloadedPacks, key = { it.mlKitCode }) { pack ->
                    PackCard(
                        pack = pack,
                        isDownloading = downloading.contains(pack.mlKitCode),
                        onDownload = { vm.downloadPack(pack.mlKitCode, wifiOnly) },
                        onDelete = { showDeleteDialog = pack }
                    )
                }
            }

            if (otherPacks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("Not Downloaded (${otherPacks.size})", Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
                items(otherPacks, key = { it.mlKitCode }) { pack ->
                    PackCard(
                        pack = pack,
                        isDownloading = downloading.contains(pack.mlKitCode),
                        onDownload = { vm.downloadPack(pack.mlKitCode, wifiOnly) },
                        onDelete = { showDeleteDialog = pack }
                    )
                }
            }

            if (allPacks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    showDeleteDialog?.let { pack ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            title = { Text("Remove ${pack.displayName}?") },
            text = {
                Text("This frees ~${pack.approximateSizeKb / 1024} MB. You'll need internet to translate ${pack.displayName} again until re-downloaded.")
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.deletePack(pack.mlKitCode); showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    // ── Download All confirm ──────────────────────────────────────────────────
    if (showDownloadAll) {
        val pending = allPacks.filter { it.status != PackStatus.DOWNLOADED }
        AlertDialog(
            onDismissRequest = { showDownloadAll = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
            title = { Text("Download All Packs?") },
            text = {
                Text("Downloads ${pending.size} language model(s) — approximately ${pending.size * 30} MB total. ${if (wifiOnly) "Wi-Fi required." else "Any network will be used."}")
            },
            confirmButton = {
                TextButton(onClick = {
                    pending.forEach { vm.downloadPack(it.mlKitCode, wifiOnly) }
                    showDownloadAll = false
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadAll = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun PackCard(
    pack: LanguagePackEntity,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Spinning animation for in-progress indicator
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Status icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusBg(pack.status, isDownloading)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDownloading -> Icon(
                        Icons.Default.Sync,
                        contentDescription = "Downloading",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    pack.status == PackStatus.DOWNLOADED -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF16A34A)
                    )
                    pack.status == PackStatus.FAILED -> Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    else -> Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Not downloaded",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Language name + details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pack.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val subtitle = when {
                    isDownloading -> "Downloading…"
                    pack.status == PackStatus.DOWNLOADED ->
                        "Ready offline  •  downloaded ${dateFormatter.format(Date(pack.downloadedAtMs))}"
                    pack.status == PackStatus.FAILED ->
                        "Failed: ${pack.errorMessage ?: "unknown error"}"
                    else -> "~${pack.approximateSizeKb / 1024} MB"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        pack.status == PackStatus.FAILED -> MaterialTheme.colorScheme.error
                        pack.status == PackStatus.DOWNLOADED -> Color(0xFF16A34A)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }

            // Action button
            AnimatedContent(
                targetState = when {
                    isDownloading -> "loading"
                    pack.status == PackStatus.DOWNLOADED -> "delete"
                    else -> "download"
                },
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                label = "action"
            ) { state ->
                when (state) {
                    "loading" -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    "delete" -> IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove pack",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    else -> IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Downloading progress indicator (indeterminate bar under the row)
        if (isDownloading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun statusBg(status: PackStatus, isDownloading: Boolean): Color {
    return when {
        isDownloading -> MaterialTheme.colorScheme.primaryContainer
        status == PackStatus.DOWNLOADED -> Color(0xFFDCFCE7)
        status == PackStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}
