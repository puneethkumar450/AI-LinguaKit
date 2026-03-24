package com.example.translateapp.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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

    DisposableEffect(Unit) {
        onDispose {
            vm.stopSpeaking()
        }
    }

    MainTranslateView(
        input = input,
        loading = loading,
        error = error,
        translatedText = translatedText,
        sourceLanguage = if (sourceLang.code == "auto") {
            detectedLang?.let { context.getString(R.string.auto_detect_with_language,
                it.displayName(context)) }
                ?: context.getString(R.string.auto_detect)
        } else {
            sourceLang.displayName(context)
        },
        targetLanguage = targetLang.displayName(context),
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
        onSourceLangClick = {
            vm.stopSpeaking()
            onNavigateToSourcePicker()
        },
        onTargetLangClick = {
            vm.stopSpeaking()
            onNavigateToTargetPicker()
        },
        onHistoryClick = {
            vm.stopSpeaking()
            onNavigateToHistory()
        },
        onSettingsClick = {
            vm.stopSpeaking()
            onNavigateToSettings()
        },
        onSpeak = { vm.speakText(translatedText, targetLang.code) },
        onStopSpeaking = vm::stopSpeaking,
        onClearInput = vm::clearTranslation,
        canChangeLanguages = true,
        showBannerAd = !isPremium
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val minimumLines = when(input.isEmpty()) {
        true -> 30
        else -> estimatedVisualLines
    }
    val isKeyboardVisible = WindowInsets.isImeVisible

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { if(input.isEmpty()) Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onHistoryClick) {
                        BadgedBox(badge = {
                            if (historyCount > 0) {
                                Badge { Text(historyCount.coerceAtMost(99).toString()) }
                            }
                        }) {
                            Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.history_title))
                        }
                    }
                },
                actions = {
                    Row {
                        if (input.isNotBlank()) {
                            IconButton(
                                onClick = onClearInput
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Log.d("ModernTranslate", "Keyboard visible: $isKeyboardVisible")
            if (isKeyboardVisible) {
                KeyboardLanguageBar(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    canChangeLanguages = canChangeLanguages,
                    onSourceLangClick = onSourceLangClick,
                    onTargetLangClick = onTargetLangClick,
                    onLanguageSwap = onLanguageSwap
                )
            }
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
                    .padding(horizontal = 4.dp)
            ) {
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
                            style = TextStyle(color = Color.Gray, fontSize = 26.sp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colorScheme.primary
                    ),
                    minLines = minimumLines
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
                        TextButton(onClick = onSpeak,
                            colors = ButtonDefaults.buttonColors(
                            contentColor = colorScheme.primary,
                            containerColor = colorScheme.primary.copy(0.1f))) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.speak))
                        }
                    }

                    if (isSpeaking) {
                        TextButton(onClick = onStopSpeaking,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = colorScheme.primary,
                                containerColor = colorScheme.primary.copy(0.1f))){
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stop))
                        }
                    }
                    //Spacer(modifier = Modifier.height(12.dp))
                    //Text("Offline Translation", color = Color.Gray, fontSize = 14.sp)
                }
            }

            if (!isKeyboardVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.Gray.copy(0.2f)
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
                                    contentDescription = stringResource(R.string.swap),
                                    tint = colorScheme.primary,
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
                                    Text(stringResource(R.string.translate))
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
                                    Text(if (isRecording) stringResource(R.string.listening) else stringResource(R.string.speak))
                                }
                            }
                        } else {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                onClick = onPrimaryAction,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.translate))
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
        Text(stringResource(R.string.translating))
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
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = {
            Text(
                text = lang.displayName(context),
                fontWeight = if (lang.code == selected.code || enabled) FontWeight.SemiBold else FontWeight.Normal,
                color = if((lang.code == selected.code) && enabled){
                    colors.primary
                }else{
                    if (enabled) colors.onSecondary else colors.onSecondary.copy(alpha = 0.8f)
                }
            )
        },
        supportingContent = {
            Text(
                fontSize = 13.sp,
                text = supportingText,
                color = if (enabled) colors.onSecondary else colors.onSecondary.copy(alpha = 0.8f)
            )
        },
        trailingContent = {
            when {
                lang.code == selected.code -> Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected), tint = Color(0xFF1F6FEB))
                !enabled -> Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked), tint = Color.Gray)
            }
        },
        colors = ListItemDefaults.colors(containerColor =
            if(lang.code == selected.code) colors.primary.copy(alpha = 0.05f)
            else Color.Transparent),
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
    val colors = colorScheme
    Surface(
        color = colors.surface,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (!enabled) BorderStroke(1.dp, Color.LightGray.copy(0.3f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = colors.onPrimaryContainer.copy(alpha = if (enabled) 1f else 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun KeyboardLanguageBar(
    sourceLanguage: String,
    targetLanguage: String,
    canChangeLanguages: Boolean,
    onSourceLangClick: () -> Unit,
    onTargetLangClick: () -> Unit,
    onLanguageSwap: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 0.dp, vertical = 0.dp),
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyboardLanguageButton(
                text = sourceLanguage,
                onClick = onSourceLangClick,
                modifier = Modifier.weight(1f),
                enabled = canChangeLanguages
            )
            Surface(
                onClick = onLanguageSwap,
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.swap),
                        tint = colorScheme.primary
                    )
                }
            }
            KeyboardLanguageButton(
                text = targetLanguage,
                onClick = onTargetLangClick,
                modifier = Modifier.weight(1f),
                enabled = canChangeLanguages
            )
        }
    }
}

@Composable
private fun KeyboardLanguageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    val colors = colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(18.dp),
        border = if (!enabled) BorderStroke(1.dp, Color.LightGray.copy(0.3f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = colors.onPrimaryContainer.copy(alpha = if (enabled) 1f else 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
