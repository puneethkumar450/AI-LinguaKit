package com.example.translateapp.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.translateapp.viewmodel.AppViewModel

@Composable
fun MainScreen(vm: AppViewModel = viewModel()) {
    val isLiveCameraMode by vm.isLiveCameraMode.collectAsState()
    val navController = rememberNavController()

    if (isLiveCameraMode) {
        LiveTranslationScreen(vm = vm, onDismiss = { vm.setLiveCameraMode(false) })
    } else {
        NavHost(navController = navController, startDestination = "translate") {
            composable("translate") {
                ModernTranslateScreen(
                    vm = vm,
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToPacks = { navController.navigate("packs") },
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
            composable("source_picker") {
                LanguagePickerScreen(
                    title = "Translate from",
                    selected = vm.translateSourceLang.collectAsState().value,
                    options = listOf(com.example.translateapp.viewmodel.AUTO_DETECT_OPTION) + com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES,
                    onSelect = { 
                        vm.setTranslateSourceLang(it)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("target_picker") {
                LanguagePickerScreen(
                    title = "Translate to",
                    selected = vm.translateTargetLang.collectAsState().value,
                    options = com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES,
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