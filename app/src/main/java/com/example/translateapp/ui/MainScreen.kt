package com.example.translateapp.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.translateapp.R
import com.example.translateapp.data.PackStatus
import com.example.translateapp.viewmodel.AdViewModel
import com.example.translateapp.viewmodel.AppViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val adVm: AdViewModel = viewModel()
    val isFirstRun by vm.isFirstRun.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val allPacks by vm.allPacks.collectAsState()
    val adUiState by adVm.adUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val activity = remember(context) { context.findActivity() }

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
            activity?.let { safeActivity ->
                adVm.showInterstitialIfAvailable(safeActivity)
            }
        }
    }

    LaunchedEffect(adUiState.isInterstitialReady, adUiState.isRewardedReady) {
        if (!adUiState.isInterstitialReady || !adUiState.isRewardedReady) {
            adVm.preloadAds()
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
                        selected = vm.translateSourceLang.collectAsState().value,
                        options = vm.getSelectableSourceLanguages(),
                        downloadedCodes = downloadedCodes,
                        blockedCode = vm.translateTargetLang.collectAsState().value.code,
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
                        selected = vm.translateTargetLang.collectAsState().value,
                        options = vm.getSelectableTargetLanguages(),
                        downloadedCodes = downloadedCodes,
                        blockedCode = vm.translateSourceLang.collectAsState().value.code,
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

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
