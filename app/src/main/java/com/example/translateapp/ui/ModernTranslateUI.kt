package com.example.translateapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.R
import com.example.translateapp.viewmodel.AppViewModel
import com.example.translateapp.viewmodel.LanguageOption
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ModernTranslateScreen(
    vm: AppViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSourcePicker: () -> Unit,
    onNavigateToTargetPicker: () -> Unit
) {
    val input by vm.translateInput.collectAsState()
    val translatedText by vm.translatedText.collectAsState()
    val sourceLang by vm.translateSourceLang.collectAsState()
    val targetLang by vm.translateTargetLang.collectAsState()
    val detectedLang by vm.detectedLanguage.collectAsState()
    val loading by vm.translateLoading.collectAsState()
    val error by vm.translateError.collectAsState()
    val isRecording by vm.isRecording.collectAsState()
    val isSpeaking by vm.isSpeaking.collectAsState()
    val historyItems by vm.allHistory.collectAsState()
    val isPremium by vm.isPremium.collectAsState()

    val recordAudioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    MainTranslateView(
        input = input,
        loading = loading,
        error = error,
        translatedText = translatedText,
        sourceLanguage = if (sourceLang.code == "auto") {
            detectedLang?.let { "Auto (${it.displayName})" } ?: "Auto Detect"
        } else {
            sourceLang.displayName
        },
        targetLanguage = targetLang.displayName,
        isRecording = isRecording,
        isSpeaking = isSpeaking,
        historyCount = historyItems.size,
        isPremium = isPremium,
        onInputChange = vm::onTranslateInputChange,
        onPrimaryAction = {
            if (isPremium) {
                if (recordAudioPermissionState.status.isGranted) {
                    if (isRecording) vm.stopRecording() else vm.startRecording()
                } else {
                    recordAudioPermissionState.launchPermissionRequest()
                }
            } else {
                vm.translateFromPrimaryAction()
            }
        },
        onSecondaryTranslate = { vm.translateFromPrimaryAction() },
        onLanguageSwap = { vm.swapTranslateLanguages() },
        onSourceLangClick = onNavigateToSourcePicker,
        onTargetLangClick = onNavigateToTargetPicker,
        onHistoryClick = onNavigateToHistory,
        onSettingsClick = onNavigateToSettings,
        onSpeak = { vm.speakText(translatedText, targetLang.code) },
        onStopSpeaking = vm::stopSpeaking,
        onClearInput = vm::clearTranslation,
        canChangeLanguages = true,
        showBannerAd = !isPremium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTranslateView(
    input: String,
    loading: Boolean,
    error: String?,
    translatedText: String,
    sourceLanguage: String,
    targetLanguage: String,
    isRecording: Boolean,
    isSpeaking: Boolean,
    historyCount: Int,
    isPremium: Boolean,
    onInputChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryTranslate: () -> Unit,
    onLanguageSwap: () -> Unit,
    onSourceLangClick: () -> Unit,
    onTargetLangClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClearInput: () -> Unit,
    canChangeLanguages: Boolean,
    showBannerAd: Boolean
) {
    val estimatedVisualLines = remember(input) {
        input.lineSequence().sumOf { line ->
            val wrappedSegments = (line.length / 32).coerceAtLeast(0)
            wrappedSegments + 1
        }.coerceAtLeast(1)
    }
    val inputFontSize = when(estimatedVisualLines) {
        in 1..3 -> 24.sp
        in 4..6 -> 18.sp
        else -> 16.sp
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LinguaKit", color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onHistoryClick) {
                        BadgedBox(badge = {
                            if (historyCount > 0) {
                                Badge { Text(historyCount.coerceAtMost(99).toString()) }
                            }
                        }) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color.Black)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = Color.Black)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                if (input.isNotBlank()) {
                    IconButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onClearInput
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = inputFontSize, fontWeight = FontWeight.Normal),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSecondaryTranslate() }),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.enter_text_placeholder),
                            style = TextStyle(color = Color.Black.copy(alpha = 0.3f), fontSize = inputFontSize)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    )
                )

                if (loading) {
                    LinearLoadingRow()
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (translatedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = translatedText,
                        style = TextStyle(
                            color = Color(0xFF1F6FEB),
                            fontSize = inputFontSize,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if(!isSpeaking){
                        TextButton(onClick = onSpeak) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Speak result")
                        }
                    }
                    if (isSpeaking) {
                        TextButton(onClick = onStopSpeaking) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Offline Translation", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFFF7F8FC)
            ) {
                Column(
                    modifier = Modifier.padding(top = 18.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LanguageButton(
                            text = sourceLanguage,
                            onClick = onSourceLangClick,
                            modifier = Modifier.weight(1f),
                            enabled = canChangeLanguages
                        )
                        IconButton(onClick = onLanguageSwap, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Swap",
                                tint = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        LanguageButton(
                            text = targetLanguage,
                            onClick = onTargetLangClick,
                            modifier = Modifier.weight(1f),
                            enabled = canChangeLanguages
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isPremium) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onSecondaryTranslate,
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Translate")
                            }
                            Button(
                                onClick = onPrimaryAction,
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Lock else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isRecording) "Listening" else "Speak")
                            }
                        }
                    } else {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Translate")
                        }
                    }

                    if (showBannerAd) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AdBannerCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun LinearLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Translating...")
    }
}

@Composable
fun SectionHeaderTitle(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun LanguageItem(
    lang: LanguageOption,
    selected: LanguageOption,
    enabled: Boolean,
    supportingText: String,
    onSelect: (LanguageOption) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = lang.displayName,
                fontWeight = if (lang.code == selected.code) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) Color.Black else Color.Gray
            )
        },
        supportingContent = {
            Text(
                supportingText,
                color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.8f)
            )
        },
        trailingContent = {
            when {
                lang.code == selected.code -> Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF1F6FEB))
                !enabled -> Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (enabled) Modifier.clickable { onSelect(lang) } else Modifier
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Black.copy(alpha = 0.05f))
}

@Composable
private fun LanguageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (!enabled) BorderStroke(1.dp, Color(0xFFD8DCE8)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.Black.copy(alpha = if (enabled) 1f else 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
