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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.R
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.example.translateapp.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LanguagePacksScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val allPacks by vm.allPacks.collectAsState()
    val downloading by vm.downloadingPacks.collectAsState()
    val downloadProgress by vm.packDownloadProgress.collectAsState()
    val isPremium by vm.isPremium.collectAsState()

    LanguagePacksContent(
        allPacks = allPacks,
        downloading = downloading,
        downloadProgress = downloadProgress,
        isPremium = isPremium,
        onBack = onBack,
        onRefresh = { vm.refreshPackStatus() },
        onDownload = { code, wifi -> vm.downloadPack(code, wifi) },
        onCancelDownload = { code -> vm.cancelPackDownload(code) },
        onDelete = { code -> vm.deletePack(code) },
        canDownload = { code -> vm.canDownloadPack(code) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePacksContent(
    allPacks: List<LanguagePackEntity>,
    downloading: Set<String>,
    downloadProgress: Map<String, Int>,
    isPremium: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (String, Boolean) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    canDownload: (String) -> Boolean
) {
    var wifiOnly by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LanguagePackEntity?>(null) }
    var showUpgradeDialog by remember { mutableStateOf<LanguagePackEntity?>(null) }

    val downloadedCount = allPacks.count { it.status == PackStatus.DOWNLOADED }
    val totalCount = allPacks.size
    val storageUsedMb = allPacks
        .filter { it.status == PackStatus.DOWNLOADED }
        .sumOf { it.approximateSizeKb } / 1024

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language_packs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(0.1f))
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$downloadedCount / $totalCount downloaded • ~${storageUsedMb} MB used",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) downloadedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
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
                                modifier = Modifier.size(16.dp)
                            )
                            Text(if (wifiOnly) "Wi-Fi only" else "Any network")
                            Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                        }

                        if (!isPremium) {
                            Text(
                                "Free plan: 3 languages max",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (!isPremium) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //Icon(Icons.Default.Info, contentDescription = null)
                    Text("Delete a downloaded language to free a slot, then download a new one.",
                        fontSize = 12.sp)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            progressPercent = downloadProgress[pack.mlKitCode] ?: 0,
                            onDownload = {},
                            onCancelDownload = { onCancelDownload(pack.mlKitCode) },
                            onDelete = { showDeleteDialog = pack }
                        )
                    }
                }

                if (otherPacks.isNotEmpty()) {
                    item {
                        SectionHeader("Not Downloaded (${otherPacks.size})", Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    items(otherPacks, key = { it.mlKitCode }) { pack ->
                        PackCard(
                            pack = pack,
                            isDownloading = downloading.contains(pack.mlKitCode),
                            progressPercent = downloadProgress[pack.mlKitCode] ?: 0,
                            onDownload = {
                                if (canDownload(pack.mlKitCode)) {
                                    onDownload(pack.mlKitCode, wifiOnly)
                                } else {
                                    showUpgradeDialog = pack
                                }
                            },
                            onCancelDownload = { onCancelDownload(pack.mlKitCode) },
                            onDelete = {}
                        )
                    }
                }
            }
        }

        showDeleteDialog?.let { pack ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                //icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                title = { Text("Remove ${pack.displayName}?") },
                text = { Text("This frees one free-plan slot so you can download another language.") },
                confirmButton = {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            containerColor =  MaterialTheme.colorScheme.errorContainer.copy(0.2f) ,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            onDelete(pack.mlKitCode)
                            showDeleteDialog = null
                        }
                    ) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }

        showUpgradeDialog?.let { pack ->
            AlertDialog(
                onDismissRequest = { showUpgradeDialog = null },
                //icon = { Icon(Icons.Default.Star, contentDescription = null) },
                title = { Text("Free plan limit reached") },
                text = {
                    Text("You are using the free plan. Delete one downloaded language to download ${pack.displayName}.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUpgradeDialog = null
                        }
                    ) { Text("OK") }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun PackCard(
    pack: LanguagePackEntity,
    isDownloading: Boolean,
    progressPercent: Int,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                    pack.status == PackStatus.DOWNLOADED -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                    pack.status == PackStatus.FAILED -> Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    else -> Icon(Icons.Default.CloudOff, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(pack.displayName, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondary)
                val subtitle = when {
                    isDownloading -> "Downloading... $progressPercent%"
                    pack.status == PackStatus.DOWNLOADED -> "Ready offline • downloaded ${dateFormatter.format(Date(pack.downloadedAtMs))}"
                    pack.status == PackStatus.FAILED -> "Failed: ${pack.errorMessage ?: "unknown error"}"
                    else -> "~${pack.approximateSizeKb / 1024} MB"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pack.status == PackStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (progressPercent.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                }
            }

            AnimatedContent(
                targetState = when {
                    isDownloading -> "loading"
                    pack.status == PackStatus.DOWNLOADED -> "delete"
                    else -> "download"
                },
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                label = "pack_action"
            ) { state ->
                when (state) {
                    "loading" -> IconButton(onClick = onCancelDownload) {
                        Icon(Icons.Default.Stop,
                            contentDescription = "Cancel download", tint = MaterialTheme.colorScheme.error)
                    }
                    "delete" -> IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error)
                    }
                    else -> IconButton(onClick = onDownload) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Download")
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularPackIndicator() {
    LinearProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White,
        modifier = Modifier.width(40.dp).height(4.dp)
    )
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

@Preview(showBackground = true)
@Composable
fun LanguagePacksScreenPreview() {
    val samplePacks = listOf(
        LanguagePackEntity("en", "English", PackStatus.DOWNLOADED, System.currentTimeMillis()),
        LanguagePackEntity("es", "Spanish", PackStatus.DOWNLOADED, System.currentTimeMillis()),
        LanguagePackEntity("fr", "French", PackStatus.NOT_DOWNLOADED),
        LanguagePackEntity("de", "German", PackStatus.DOWNLOADING),
        LanguagePackEntity("it", "Italian", PackStatus.FAILED, errorMessage = "Connection timeout")
    )

    TranslateAppTheme(darkTheme = true) {
        LanguagePacksContent(
            allPacks = samplePacks,
            downloading = setOf("de"),
            downloadProgress = mapOf("de" to 62),
            isPremium = false,
            onBack = {},
            onRefresh = {},
            onDownload = { _, _ -> },
            onCancelDownload = { _ -> },
            onDelete = { _ -> },
            canDownload = { true }
        )
    }
}
