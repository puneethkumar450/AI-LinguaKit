package com.puneeth50.ailinguakit.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(vm: AppViewModel = hiltViewModel()) {
    val isFirstRun by vm.isFirstRun.collectAsState()
    val allPacks by vm.allPacks.collectAsState()
    val sourceLang by vm.translateSourceLang.collectAsState()
    val targetLang by vm.translateTargetLang.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val downloadedCodes = remember(allPacks) {
        allPacks.filter { it.status == PackStatus.DOWNLOADED }
            .map { it.mlKitCode }
            .toSet()
    }

    LaunchedEffect(Unit) {
        vm.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        vm.showInterstitialAd.collectLatest {
            navController.navigate("advertisement")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isFirstRun) {
            OneTimeLanguagePicker(
                onLanguagesSelected = { source, target ->
                    vm.completeFirstRun(source, target)
                }
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = "translate"
            ) {
                composable("translate") {
                    ModernTranslateScreen(
                        vm = vm,
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToSourcePicker = { navController.navigate("source_picker") },
                        onNavigateToTargetPicker = { navController.navigate("target_picker") }
                    )
                }
                composable("history") {
                    HistoryScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onRestore = { navController.popBackStack() }
                    )
                }
                composable("packs") {
                    LanguagePacksScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("plan_selection") {
                    PlanSelectionScreen(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreenUI(
                        vm = vm,
                        onBack = { navController.popBackStack() },
                        onOpenLanguageChange = { navController.navigate("language_change") },
                        onOpenLanguagePacks = { navController.navigate("packs") }
                    )
                }
                composable("language_change") {
                    LanguageChangeScreenUI(
                        vm = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("advertisement") {
                    AdvertisementScreenUI(
                        onFinished = { navController.popBackStack() }
                    )
                }
                composable("source_picker") {
                    LanguagePickerScreen(
                        titleRes = R.string.translate_from,
                        selected = sourceLang,
                        options = vm.getSelectableSourceLanguages(),
                        downloadedCodes = downloadedCodes,
                        blockedCode = targetLang.code,
                        onSelect = {
                            vm.setTranslateSourceLang(it)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("target_picker") {
                    LanguagePickerScreen(
                        titleRes = R.string.translate_to,
                        selected = targetLang,
                        options = vm.getSelectableTargetLanguages(),
                        downloadedCodes = downloadedCodes,
                        blockedCode = sourceLang.code,
                        onSelect = {
                            vm.setTranslateTargetLang(it)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
