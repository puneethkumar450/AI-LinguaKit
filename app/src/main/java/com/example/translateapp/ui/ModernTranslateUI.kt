package com.example.translateapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.R
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.data.TranslationHistoryEntity
import com.example.translateapp.viewmodel.AUTO_DETECT_OPTION
import com.example.translateapp.viewmodel.AppViewModel
import com.example.translateapp.viewmodel.LanguageOption
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

private enum class AppScreenState {
    MAIN
}



@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ModernTranslateScreen(vm: AppViewModel,
                          onNavigateToHistory: () -> Unit,
                          onNavigateToPacks: () -> Unit,
                          onNavigateToSourcePicker: () -> Unit,
                          onNavigateToTargetPicker: () -> Unit) {
    var screenState by remember { mutableStateOf(AppScreenState.MAIN) }

    val input by vm.translateInput.collectAsState()
    val translatedText by vm.translatedText.collectAsState()
    val sourceLang by vm.translateSourceLang.collectAsState()
    val targetLang by vm.translateTargetLang.collectAsState()
    val detectedLang by vm.detectedLanguage.collectAsState()
    val loading by vm.translateLoading.collectAsState()
    val error by vm.translateError.collectAsState()
    val isRecording by vm.isRecording.collectAsState()

    val historyItems by vm.allHistory.collectAsState()
    val allPacks by vm.allPacks.collectAsState()
    val downloading by vm.downloadingPacks.collectAsState()
    val notDownloadedCount by remember(allPacks) {
        mutableStateOf(allPacks.count { it.status != PackStatus.DOWNLOADED })
    }

    val recordAudioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    // Automatic translation logic
    LaunchedEffect(input, sourceLang, targetLang) {
        if (input.isNotBlank()) {
            delay(600)
            vm.translate()
        }
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { state ->
        when (state) {
            AppScreenState.MAIN -> {
                MainTranslateView(
                    input = input,
                    loading = loading,
                    error = error,
                    translatedText = translatedText,
                    sourceLanguage = if (sourceLang == AUTO_DETECT_OPTION) {
                        if (detectedLang != null) "Auto (${detectedLang?.displayName})" else "Auto Detect"
                    } else sourceLang.displayName,
                    targetLanguage = targetLang.displayName,
                    isRecording = isRecording,
                    historyCount = historyItems.size,
                    packsNotification = notDownloadedCount + downloading.size,
                    onInputChange = { vm.onTranslateInputChange(it) },
                    onMicClick = {
                        if (recordAudioPermissionState.status.isGranted) {
                            if (isRecording) vm.stopRecording() else vm.startRecording()
                        } else {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    },
                    onLanguageSwap = { if (sourceLang != AUTO_DETECT_OPTION) vm.swapTranslateLanguages() },
                    onSourceLangClick = { onNavigateToSourcePicker() },
                    onTargetLangClick = { onNavigateToTargetPicker() },
                    onHistoryClick = { onNavigateToHistory() },
                    onPacksClick = { onNavigateToPacks() }
                )
            }
        }
    }
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
    historyCount: Int,
    packsNotification: Int,
    onInputChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onLanguageSwap: () -> Unit,
    onSourceLangClick: () -> Unit,
    onTargetLangClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPacksClick: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF140F0F),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LinguaKit", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onHistoryClick) {
                        BadgedBox(badge = { if (historyCount > 0) Badge { Text(historyCount.coerceAtMost(99).toString()) } }) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPacksClick) {
                        BadgedBox(badge = { if (packsNotification > 0) Badge { Text(packsNotification.coerceAtMost(99).toString()) } }) {
                            Icon(Icons.Default.Download, contentDescription = "Language Packs", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top area: Input & Result
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Normal),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.enter_text_placeholder),
                            style = TextStyle(color = Color.White.copy(alpha = 0.3f), fontSize = 32.sp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        color = Color(0xFFF2B8B5),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (translatedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = translatedText,
                        style = TextStyle(color = Color(0xFFF2B8B5), fontSize = 28.sp, fontWeight = FontWeight.Light),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom section
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = Color(0xFF1E1818)
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.width(32.dp).height(4.dp).background(Color.White.copy(alpha = 0.15f), CircleShape))
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LanguageButton(text = sourceLanguage, onClick = onSourceLangClick, modifier = Modifier.weight(1f))
                        IconButton(onClick = onLanguageSwap, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                        }
                        LanguageButton(text = targetLanguage, onClick = onTargetLangClick, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    MicButton(onClick = onMicClick, isRecording = isRecording)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerScreen(
    title: String,
    selected: LanguageOption,
    options: List<LanguageOption>,
    onSelect: (LanguageOption) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF140F0F),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF140F0F))
            )
        }
    ) { padding ->
        var searchQuery by remember { mutableStateOf("") }
        val filteredOptions = remember(searchQuery) {
            if (searchQuery.isBlank()) options
            else options.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search languages...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1818),
                    unfocusedContainerColor = Color(0xFF1E1818),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredOptions) { lang ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = lang.displayName,
                                fontWeight = if (lang.code == selected.code) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        trailingContent = {
                            if (lang.code == selected.code) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFFF2B8B5))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(lang) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(64.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFF140F0F)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MicButton(onClick: () -> Unit,
                      isRecording: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            PulseAnimation()
        }

        LargeFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier.size(90.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}