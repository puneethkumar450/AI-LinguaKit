package com.example.translateapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translateapp.R
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.example.translateapp.viewmodel.AUTO_DETECT_OPTION
import com.example.translateapp.viewmodel.AppViewModel
import com.example.translateapp.viewmodel.LanguageOption
import com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES
import kotlinx.coroutines.delay

private enum class SelectionState {
    MAIN, SOURCE, TARGET
}

@Composable
fun ModernTranslateScreen(
    vm: AppViewModel,
    onConversationClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    var currentState by remember { mutableStateOf(SelectionState.MAIN) }

    val input by vm.translateInput.collectAsState()
    val translatedText by vm.translatedText.collectAsState()
    val sourceLang by vm.translateSourceLang.collectAsState()
    val targetLang by vm.translateTargetLang.collectAsState()
    val loading by vm.translateLoading.collectAsState()
    val isRecording by vm.isRecording.collectAsState()

    // Debounced automatic translation - Defaulting to ML Kit Online Mode behavior via ViewModel
    LaunchedEffect(input, sourceLang, targetLang) {
        if (input.isNotBlank()) {
            delay(600) // Debounce typing
            vm.translate()
        }
    }

    AnimatedContent(
        targetState = currentState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { state ->
        when (state) {
            SelectionState.MAIN -> {
                ModernTranslateScreenContent(
                    input = input,
                    loading = loading,
                    onInputChange = { vm.onTranslateInputChange(it) },
                    translatedText = translatedText,
                    sourceLanguage = sourceLang.displayName,
                    targetLanguage = targetLang.displayName,
                    onConversationClick = onConversationClick,
                    onMicClick = { 
                        if (isRecording) vm.stopRecording() else vm.startRecording()
                    },
                    isRecording = isRecording,
                    onCameraClick = onCameraClick,
                    onLanguageSwap = {
                        if (sourceLang != AUTO_DETECT_OPTION) {
                            vm.swapTranslateLanguages()
                        }
                    },
                    onSourceLangClick = { currentState = SelectionState.SOURCE },
                    onTargetLangClick = { currentState = SelectionState.TARGET }
                )
            }
            SelectionState.SOURCE -> {
                LanguagePickerScreen(
                    title = "Translate from",
                    selected = sourceLang,
                    options = listOf(AUTO_DETECT_OPTION) + SUPPORTED_LANGUAGES,
                    onSelect = {
                        vm.setTranslateSourceLang(it)
                        currentState = SelectionState.MAIN
                    },
                    onBack = { currentState = SelectionState.MAIN }
                )
            }
            SelectionState.TARGET -> {
                LanguagePickerScreen(
                    title = "Translate to",
                    selected = targetLang,
                    options = SUPPORTED_LANGUAGES,
                    onSelect = {
                        vm.setTranslateTargetLang(it)
                        currentState = SelectionState.MAIN
                    },
                    onBack = { currentState = SelectionState.MAIN }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerScreen(
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140F0F)
                )
            )
        }
    ) { padding ->
        var searchQuery by remember { mutableStateOf("") }
        val filteredOptions = remember(searchQuery) {
            if (searchQuery.isBlank()) options
            else options.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFF2B8B5)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.clickable { onSelect(lang) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernTranslateScreenContent(
    input: String,
    loading: Boolean,
    onInputChange: (String) -> Unit,
    translatedText: String,
    sourceLanguage: String,
    targetLanguage: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onConversationClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onLanguageSwap: () -> Unit = {},
    onSourceLangClick: () -> Unit = {},
    onTargetLangClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF140F0F)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top area: Input & Result
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
                // Input Field
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.enter_text_placeholder),
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = Color(0xFFF2B8B5),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                if (translatedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = translatedText,
                        style = TextStyle(
                            color = Color(0xFFF2B8B5),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = Color(0xFF1E1818)
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Language selection row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LanguageButton(
                            text = sourceLanguage,
                            onClick = onSourceLangClick,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onLanguageSwap,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Languages",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        LanguageButton(
                            text = targetLanguage,
                            onClick = onTargetLangClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Main actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionItem(
                            icon = Icons.Default.InterpreterMode,
                            label = stringResource(id = R.string.conversation_label),
                            onClick = onConversationClick
                        )

                        MicButton(onClick = onMicClick, isRecording = isRecording)

                        ActionItem(
                            icon = Icons.Default.CameraAlt,
                            label = stringResource(id = R.string.camera_label),
                            onClick = onCameraClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF140F0F)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF2B2121), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MicButton(
    onClick: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .background(if (isRecording) Color.Red else Color(0xFFF2B8B5), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = "Microphone",
            tint = if (isRecording) Color.White else Color(0xFF601410),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Preview
@Composable
private fun ModernTranslateScreenPreview() {
    TranslateAppTheme {
        ModernTranslateScreenContent(
            input = "Hello",
            loading = false,
            onInputChange = {},
            translatedText = "Ciao",
            sourceLanguage = "English",
            targetLanguage = "Italian",
            isRecording = false
        )
    }
}
