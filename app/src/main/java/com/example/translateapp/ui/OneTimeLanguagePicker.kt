package com.example.translateapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.viewmodel.LanguageOption
import com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTimeLanguagePicker(
    onLanguagesSelected: (LanguageOption, LanguageOption) -> Unit
) {
    var selectedLanguages by remember { mutableStateOf(listOf<LanguageOption>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Choose 2 Languages",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        )
                    }
                },
                actions = {
                    if (selectedLanguages.size == 2) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63))
                                .clickable { 
                                    onLanguagesSelected(selectedLanguages[0], selectedLanguages[1]) 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Show count if not yet 2
                        Text(
                            text = "${selectedLanguages.size}/2",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(SUPPORTED_LANGUAGES) { language ->
                LanguageGridItem(
                    language = language,
                    isSelected = selectedLanguages.any { it.code == language.code },
                    onClick = {
                        if (selectedLanguages.any { it.code == language.code }) {
                            selectedLanguages = selectedLanguages.filter { it.code != language.code }
                        } else if (selectedLanguages.size < 2) {
                            selectedLanguages = selectedLanguages + language
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageGridItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFE91E63) else Color(0xFFF5F5F5)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getFlagEmoji(language.code),
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getFlagEmoji(countryCode: String): String {
    return when (countryCode.lowercase()) {
        "en" -> "🇺🇸"
        "uk" -> "🇬🇧"
        "fr" -> "🇫🇷"
        "de" -> "🇩🇪"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "pt" -> "🇵🇹"
        "es" -> "🇪🇸"
        "ar" -> "🇸🇦"
        "zh" -> "🇨🇳"
        "it" -> "🇮🇹"
        "ru" -> "🇷🇺"
        "hi" -> "🇮🇳"
        "ta" -> "🇮🇳"
        "te" -> "🇮🇳"
        "bn" -> "🇮🇳"
        "mr" -> "🇮🇳"
        "kn" -> "🇮🇳"
        "vi" -> "🇻🇳"
        else -> "🌐"
    }
}
