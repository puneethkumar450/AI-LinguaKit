package com.example.translateapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.translateapp.data.PackStatus
import com.example.translateapp.viewmodel.AppViewModel

sealed class AppTab(val label: String) {
    object Translate   : AppTab("Translate")
    object Speech      : AppTab("Text→Speech")
    object PhotoSpeech : AppTab("Photo→Speech")
    object History     : AppTab("History")
    object Packs       : AppTab("Packs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: AppViewModel = viewModel()) {
    var selectedTab  by remember { mutableStateOf<AppTab>(AppTab.Translate) }
    val historyItems by vm.allHistory.collectAsState()
    val allPacks     by vm.allPacks.collectAsState()
    val downloading  by vm.downloadingPacks.collectAsState()

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.uiMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    val notDownloadedCount = allPacks.count { it.status != PackStatus.DOWNLOADED }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("LinguaKit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    label = { Text("Translate") },
                    selected = selectedTab is AppTab.Translate,
                    onClick = { selectedTab = AppTab.Translate }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                    label = { Text("Speech") },
                    selected = selectedTab is AppTab.Speech,
                    onClick = { selectedTab = AppTab.Speech }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    label = { Text("Photo") },
                    selected = selectedTab is AppTab.PhotoSpeech,
                    onClick = { selectedTab = AppTab.PhotoSpeech }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (historyItems.isNotEmpty()) {
                                    Badge { Text(historyItems.size.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                        }
                    },
                    label = { Text("History") },
                    selected = selectedTab is AppTab.History,
                    onClick = { selectedTab = AppTab.History }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                // Badge shows how many packs still need downloading (or are actively DLing)
                                val activeOrPending = downloading.size + notDownloadedCount.coerceAtMost(99)
                                if (downloading.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                        Text(downloading.size.toString())
                                    }
                                } else if (notDownloadedCount > 0 && allPacks.isNotEmpty()) {
                                    Badge { Text(notDownloadedCount.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    },
                    label = { Text("Packs") },
                    selected = selectedTab is AppTab.Packs,
                    onClick = { selectedTab = AppTab.Packs }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    is AppTab.Translate   -> TranslateScreen(vm)
                    is AppTab.Speech      -> TextToSpeechScreen(vm)
                    is AppTab.PhotoSpeech -> PhotoToSpeechScreen(vm)
                    is AppTab.History     -> HistoryScreen(vm) { selectedTab = AppTab.Translate }
                    is AppTab.Packs       -> LanguagePacksScreen(vm)
                }
            }
        }
    }
}
