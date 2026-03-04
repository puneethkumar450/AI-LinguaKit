package com.example.translateapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.translateapp.viewmodel.AppViewModel

@Composable
fun TextToSpeechScreen(vm: AppViewModel) {
    val input by vm.ttsInput.collectAsState()
    val language by vm.ttsLanguage.collectAsState()
    val ttsReady by vm.ttsReady.collectAsState()
    val isSpeaking by vm.isSpeaking.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Text to Speech",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Type any text, choose a language, and hear it spoken aloud.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Language picker
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Language: ", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            LanguageDropdown(
                selected = language,
                onSelect = { vm.setTtsLanguage(it) }
            )
        }

        // Input
        OutlinedTextField(
            value = input,
            onValueChange = { vm.onTtsInputChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            placeholder = { Text("Enter text to speak…") },
            label = { Text("Text") },
            maxLines = 10,
            shape = RoundedCornerShape(12.dp)
        )

        // Speak / Stop button
        if (!isSpeaking) {
            Button(
                onClick = { vm.speakText(input, language.code) },
                enabled = input.isNotBlank() && ttsReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (!ttsReady) "Initializing TTS…" else "Speak",
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            OutlinedButton(
                onClick = { vm.stopSpeaking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop Speaking", fontWeight = FontWeight.SemiBold)
            }
        }

        // Status indicator
        if (isSpeaking) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Speaking in ${language.displayName}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (!ttsReady) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Initializing text-to-speech engine…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
