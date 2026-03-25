package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.service.TranslationController.TranslateSource
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import com.puneeth50.ailinguakit.viewmodel.LanguageOption
import com.puneeth50.ailinguakit.viewmodel.SUPPORTED_LANGUAGES

@Composable
fun TranslateScreen(vm: AppViewModel) {
    val input       by vm.translateInput.collectAsState()
    val output      by vm.translatedText.collectAsState()
    val sourceLang  by vm.translateSourceLang.collectAsState()
    val targetLang  by vm.translateTargetLang.collectAsState()
    val loading     by vm.translateLoading.collectAsState()
    val error       by vm.translateError.collectAsState()
    val translateSource by vm.translateSource.collectAsState()
    val isSpeaking by vm.isSpeaking.collectAsState()

    // Observe pack status for selected languages
    val srcPack by vm.observePackStatus(sourceLang.mlKitCode).collectAsState(initial = null)
    val tgtPack by vm.observePackStatus(targetLang.mlKitCode).collectAsState(initial = null)
    val srcReady = srcPack?.status == PackStatus.DOWNLOADED
    val tgtReady = tgtPack?.status == PackStatus.DOWNLOADED

    TranslateScreenContent(
        input = input,
        output = output,
        sourceLang = sourceLang,
        targetLang = targetLang,
        loading = loading,
        error = error,
        translateSource = translateSource,
        isSpeaking = isSpeaking,
        srcReady = srcReady,
        tgtReady = tgtReady,
        onInputChange = { vm.onTranslateInputChange(it) },
        onSourceLangSelect = { vm.setTranslateSourceLang(it) },
        onTargetLangSelect = { vm.setTranslateTargetLang(it) },
        onSwapLanguages = { vm.swapTranslateLanguages() },
        onTranslate = { vm.translate() },
        onSpeak = { text, lang -> vm.speakText(text, lang) },
        onStopSpeaking = { vm.stopSpeaking() }
    )
}

@Composable
private fun TranslateScreenContent(
    input: String,
    output: String,
    sourceLang: LanguageOption,
    targetLang: LanguageOption,
    loading: Boolean,
    error: String?,
    translateSource: TranslateSource,
    isSpeaking: Boolean,
    srcReady: Boolean,
    tgtReady: Boolean,
    onInputChange: (String) -> Unit,
    onSourceLangSelect: (LanguageOption) -> Unit,
    onTargetLangSelect: (LanguageOption) -> Unit,
    onSwapLanguages: () -> Unit,
    onTranslate: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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
                onSelect = onSourceLangSelect
            )
            IconButton(onClick = onSwapLanguages) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = stringResource(R.string.swap_languages),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            LanguageDropdownWithStatus(
                selected = targetLang,
                isDownloaded = tgtReady,
                onSelect = onTargetLangSelect
            )
        }

        // ── Input ─────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text(stringResource(R.string.enter_text_to_translate)) },
            label = { Text(stringResource(R.string.source_text)) },
            maxLines = 8,
            shape = RoundedCornerShape(12.dp)
        )

        // ── Translate button ──────────────────────────────────────────────────
        Button(
            onClick = onTranslate,
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
                Text(stringResource(R.string.translating_ellipsis))
            } else {
                Icon(
                    if (bothReady) Icons.Default.OfflinePin else Icons.Default.Language,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (bothReady) stringResource(R.string.translate_offline) else stringResource(R.string.translate),
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
                                stringResource(R.string.translation_with_language, targetLang.displayName(context)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            // Source badge — varies by how result was produced
                            TranslateSourceBadge(translateSource)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isSpeaking) onStopSpeaking()
                                    else onSpeak(output, targetLang.code)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(R.string.speak),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(output)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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

/** Small pill badge showing how the last translation was produced. */
@Composable
private fun TranslateSourceBadge(source: TranslateSource) {
    if (source == TranslateSource.NONE) return
    val (icon, label, bgColor, textColor) = when (source) {
        TranslateSource.OFFLINE -> Quad(
            Icons.Default.OfflinePin, stringResource(R.string.offline),
            Color(0xFFDCFCE7), Color(0xFF15803D)
        )
        TranslateSource.ONLINE -> Quad(
            Icons.Default.Language, stringResource(R.string.online),
            Color(0xFFDBEAFE), Color(0xFF1D4ED8)
        )
        TranslateSource.HISTORY -> Quad(
            Icons.Default.History, stringResource(R.string.from_cache),
            Color(0xFFFEF9C3), Color(0xFF854D0E)
        )
        else -> return
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null,
                modifier = Modifier.size(10.dp), tint = textColor)
            Text(label, fontSize = 9.sp, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Banner shown below the top bar to communicate translation mode. */
@Composable
private fun OfflineReadinessBanner(
    srcReady: Boolean,
    tgtReady: Boolean,
    srcLang: LanguageOption,
    tgtLang: LanguageOption
) {
    val context = LocalContext.current
    val bothReady = srcReady && tgtReady
    if (bothReady) {
        // Fully offline — best case
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
                    stringResource(R.string.language_packs_ready_works_without_internet),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF15803D)
                )
            }
        }
    } else {
        // Packs missing — online translate will be used, or history fallback if offline
        val missing = buildList {
            if (!srcReady) add(srcLang.displayName(context))
            if (!tgtReady) add(tgtLang.displayName(context))
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
                    stringResource(
                        if (missing.size > 1) R.string.models_not_downloaded_template else R.string.model_not_downloaded_template,
                        missing.joinToString(stringResource(R.string.and_separator))
                    ),
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
    val context = LocalContext.current
    val allLanguages = SUPPORTED_LANGUAGES

    Box {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected.displayName(context),
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
                        text = { Text(lang.displayName(context)) },
                        onClick = { onSelect(lang); expanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslateScreenPreview() {
    TranslateAppTheme {
        TranslateScreenContent(
            input = "Hello, how are you?",
            output = "Bonjour, comment allez-vous ?",
            sourceLang = SUPPORTED_LANGUAGES[0],
            targetLang = SUPPORTED_LANGUAGES[2],
            loading = false,
            error = null,
            translateSource = TranslateSource.ONLINE,
            isSpeaking = false,
            srcReady = true,
            tgtReady = true,
            onInputChange = {},
            onSourceLangSelect = {},
            onTargetLangSelect = {},
            onSwapLanguages = {},
            onTranslate = {},
            onSpeak = { _, _ -> },
            onStopSpeaking = {}
        )
    }
}
