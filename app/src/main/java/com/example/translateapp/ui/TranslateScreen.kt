package com.example.translateapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.data.PackStatus
import com.example.translateapp.viewmodel.AppViewModel
import com.example.translateapp.viewmodel.LanguageOption

@Composable
fun TranslateScreen(vm: AppViewModel) {
    val input       by vm.translateInput.collectAsState()
    val output      by vm.translatedText.collectAsState()
    val sourceLang  by vm.translateSourceLang.collectAsState()
    val targetLang  by vm.translateTargetLang.collectAsState()
    val loading     by vm.translateLoading.collectAsState()
    val error       by vm.translateError.collectAsState()
    val wasOffline  by vm.wasOffline.collectAsState()
    val clipboard   = LocalClipboardManager.current

    // Observe pack status for selected languages
    val srcPack by vm.observePackStatus(sourceLang.mlKitCode).collectAsState(initial = null)
    val tgtPack by vm.observePackStatus(targetLang.mlKitCode).collectAsState(initial = null)
    val srcReady = srcPack?.status == PackStatus.DOWNLOADED
    val tgtReady = tgtPack?.status == PackStatus.DOWNLOADED
    val bothReady = srcReady && tgtReady

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Offline readiness banner ──────────────────────────────────────────
        OfflineReadinessBanner(srcReady = srcReady, tgtReady = tgtReady, srcLang = sourceLang, tgtLang = targetLang)

        // ── Language row ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageDropdownWithStatus(
                selected = sourceLang,
                isDownloaded = srcReady,
                onSelect = { vm.setTranslateSourceLang(it) }
            )
            IconButton(onClick = { vm.swapTranslateLanguages() }) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Swap languages",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            LanguageDropdownWithStatus(
                selected = targetLang,
                isDownloaded = tgtReady,
                onSelect = { vm.setTranslateTargetLang(it) }
            )
        }

        // ── Input ─────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = input,
            onValueChange = { vm.onTranslateInputChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text("Enter text to translate…") },
            label = { Text("Source text") },
            maxLines = 8,
            shape = RoundedCornerShape(12.dp)
        )

        // ── Translate button ──────────────────────────────────────────────────
        Button(
            onClick = { vm.translate() },
            enabled = input.isNotBlank() && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (bothReady)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(if (bothReady) "Translating…" else "Downloading models & translating…")
            } else {
                Icon(
                    if (bothReady) Icons.Default.OfflinePin else Icons.Default.CloudDownload,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (bothReady) "Translate (offline)" else "Translate (download & save)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Output ────────────────────────────────────────────────────────────
        if (output.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Translation (${targetLang.displayName})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            // Offline badge on result
                            if (wasOffline) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF16A34A).copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OfflinePin,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = Color(0xFF16A34A)
                                        )
                                        Text(
                                            "offline",
                                            fontSize = 9.sp,
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(output)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            output,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/** Banner shown below the top bar when one or both pack(s) are missing. */
@Composable
private fun OfflineReadinessBanner(
    srcReady: Boolean,
    tgtReady: Boolean,
    srcLang: LanguageOption,
    tgtLang: LanguageOption
) {
    val bothReady = srcReady && tgtReady
    if (bothReady) {
        // Show a subtle "fully offline" confirmation
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null,
                    tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                Text(
                    "Both language models are on-device — translates without internet",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF15803D)
                )
            }
        }
    } else {
        val missing = buildList {
            if (!srcReady) add(srcLang.displayName)
            if (!tgtReady) add(tgtLang.displayName)
        }
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text(
                    "${missing.joinToString(" & ")} model${if (missing.size > 1) "s" else ""} not downloaded — tap Translate to auto-download, or go to Language Packs.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/** Language dropdown that shows a small offline/online dot indicator. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdownWithStatus(
    selected: LanguageOption,
    isDownloaded: Boolean,
    onSelect: (LanguageOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allLanguages = com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES

    Box {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected.displayName,
                onValueChange = {},
                readOnly = true,
                leadingIcon = {
                    Icon(
                        if (isDownloaded) Icons.Default.OfflinePin else Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isDownloaded) Color(0xFF16A34A)
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(152.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allLanguages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = { onSelect(lang); expanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
