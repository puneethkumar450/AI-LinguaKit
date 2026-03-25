package com.puneeth50.ailinguakit.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import com.puneeth50.ailinguakit.viewmodel.LanguageOption
import com.puneeth50.ailinguakit.viewmodel.SUPPORTED_LANGUAGES

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeechToSpeechScreen(vm: AppViewModel) {
    val input by vm.translateInput.collectAsState()
    val translatedText by vm.translatedText.collectAsState()
    val sourceLanguage by vm.translateSourceLang.collectAsState()
    val targetLanguage by vm.translateTargetLang.collectAsState()
    val detectedLanguage by vm.detectedLanguage.collectAsState()
    val ttsReady by vm.ttsReady.collectAsState()
    val isSpeaking by vm.isSpeaking.collectAsState()
    val isRecording by vm.isRecording.collectAsState()
    val isLoading by vm.translateLoading.collectAsState()

    val recordAudioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    // Clear translation when navigating away from this screen
    DisposableEffect(Unit) {
        onDispose {
            vm.clearTranslation()
        }
    }

    SpeechToSpeechContent(
        input = input,
        translatedText = translatedText,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        detectedLanguage = detectedLanguage,
        ttsReady = ttsReady,
        isSpeaking = isSpeaking,
        isRecording = isRecording,
        isLoading = isLoading,
        onSourceLanguageSelect = { vm.setTranslateSourceLang(it) },
        onTargetLanguageSelect = { vm.setTranslateTargetLang(it) },
        onSwapLanguages = { vm.swapTranslateLanguages() },
        onToggleRecording = {
            if (recordAudioPermissionState.status.isGranted) {
                if (isRecording) vm.stopRecording() else vm.startRecording()
            } else {
                recordAudioPermissionState.launchPermissionRequest()
            }
        },
        onSpeakTranslated = {
            if (translatedText.isNotBlank()) {
                vm.speakText(translatedText, targetLanguage.code)
            }
        },
        onStopSpeaking = { vm.stopSpeaking() },
        onClear = { vm.clearTranslation() }
    )

    // Auto-speak when translation completes
    LaunchedEffect(translatedText) {
        if (translatedText.isNotBlank() && ttsReady) {
            vm.speakText(translatedText, targetLanguage.code)
        }
    }
}

@Composable
fun SpeechToSpeechContent(
    input: String,
    translatedText: String,
    sourceLanguage: LanguageOption,
    targetLanguage: LanguageOption,
    detectedLanguage: LanguageOption?,
    ttsReady: Boolean,
    isSpeaking: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    onSourceLanguageSelect: (LanguageOption) -> Unit,
    onTargetLanguageSelect: (LanguageOption) -> Unit,
    onSwapLanguages: () -> Unit,
    onToggleRecording: () -> Unit,
    onSpeakTranslated: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.speech_to_speech),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Language Selection Row with Swap
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            /*Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("From", style = MaterialTheme.typography.labelSmall)
                    LanguageDropdown(selected = sourceLanguage, onSelect = onSourceLanguageSelect)
                }
                
                IconButton(onClick = onSwapLanguages) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = MaterialTheme.colorScheme.primary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("To", style = MaterialTheme.typography.labelSmall)
                    LanguageDropdown(selected = targetLanguage, onSelect = onTargetLanguageSelect)
                }
            }*/

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.translate_to_label), style = MaterialTheme.typography.bodyLarge)
                    /*Text(
                        targetLanguage.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )*/
                }
                Spacer(Modifier.width(15.dp))
                LanguageDropdown(
                    selected = targetLanguage,
                    onSelect = onTargetLanguageSelect
                )
            }
        }

        // Recording Section
        Box(
            modifier = Modifier
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                PulseAnimation()
            }
            
            LargeFloatingActionButton(
                onClick = onToggleRecording,
                shape = CircleShape,
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(90.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) stringResource(R.string.stop_recording) else stringResource(R.string.start_recording),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            text = if (isRecording) stringResource(R.string.listening_ellipsis) else stringResource(R.string.tap_to_speak),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        // Results Section
        if (input.isNotBlank() || isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Original Speech
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                detectedLanguage?.let { stringResource(R.string.recognized_with_language, it.displayName(context)) }
                                    ?: stringResource(R.string.recognized),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(input, style = MaterialTheme.typography.bodyLarge)
                    }

                    Divider()

                    // Translation
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(R.string.translation_with_language, targetLanguage.displayName(context)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (translatedText.isNotBlank()) {
                                IconButton(onClick = onSpeakTranslated, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = stringResource(R.string.speak),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        
                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        } else {
                            Text(
                                translatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            if (input.isNotBlank() || translatedText.isNotBlank()) {
                TextButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.clear), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (isSpeaking) {
            Button(
                onClick = onStopSpeaking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.stop_audio))
            }
        }

        if (!ttsReady) {
            Text(
                stringResource(R.string.initializing_speech_services),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha), CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
private fun SpeechToSpeechScreenPreview() {
    TranslateAppTheme {
        SpeechToSpeechContent(
            input = "How do I get to the airport?",
            translatedText = "¿Cómo llego al aeropuerto?",
            sourceLanguage = SUPPORTED_LANGUAGES[0],
            targetLanguage = SUPPORTED_LANGUAGES[1],
            detectedLanguage = SUPPORTED_LANGUAGES[0],
            ttsReady = true,
            isSpeaking = false,
            isRecording = false,
            isLoading = false,
            onSourceLanguageSelect = {},
            onTargetLanguageSelect = {},
            onSwapLanguages = {},
            onToggleRecording = {},
            onSpeakTranslated = {},
            onStopSpeaking = {},
            onClear = {}
        )
    }
}
