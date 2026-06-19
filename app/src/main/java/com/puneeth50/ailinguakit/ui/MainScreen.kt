package com.puneeth50.ailinguakit.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.puneeth50.ailinguakit.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.viewmodel.AppViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun MainScreen(vm: AppViewModel = hiltViewModel()) {
    val isFirstRun by vm.isFirstRun.collectAsState()
    val allPacks by vm.allPacks.collectAsState()
    val sourceLang by vm.translateSourceLang.collectAsState()
    val targetLang by vm.translateTargetLang.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    var showOnboarding by rememberSaveable(isFirstRun) { mutableStateOf(false) }

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
        RewardedAdHost(vm = vm)
        if (isFirstRun) {
            if (showOnboarding) {
                OnboardingFlow(
                    onFinished = { showOnboarding = false }
                )
            } else {
                OneTimeLanguagePicker(
                    onLanguagesSelected = { source, target ->
                        vm.completeFirstRun(source, target)
                    }
                )
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = "translate"
            ) {
                composable("translate") {
                    ModernTranslateScreen(
                        vm = vm,
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToRewards = { navController.navigate("rewards") },
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
                        onOpenLanguagePacks = { navController.navigate("packs") },
                        onOpenRewards = { navController.navigate("rewards") },
                        onOpenPlanSelection = { navController.navigate("plan_selection") }
                    )
                }
                composable("rewards") {
                    RewardsScreenUI(
                        onBack = { navController.popBackStack() },
                        onOpenPlanSelection = { navController.navigate("plan_selection") }
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

@Composable
private fun RewardedAdHost(vm: AppViewModel) {
    val context = LocalContext.current

    LaunchedEffect(context) {
        vm.rewardRequests.collectLatest { rewardType ->
            val activity = context.findActivity()
            if (activity == null) {
                vm.onRewardAdFailed()
                return@collectLatest
            }

            val rewardedAd = loadRewardedAd(context)
            if (rewardedAd == null) {
                vm.onRewardAdFailed()
                return@collectLatest
            }

            val earned = showRewardedAd(activity, rewardedAd)
            if (earned) {
                vm.onRewardAdCompleted(rewardType)
            } else {
                vm.onRewardAdDismissed()
            }
        }
    }
}

private suspend fun loadRewardedAd(context: Context): RewardedAd? = suspendCancellableCoroutine { cont ->
    RewardedAd.load(
        context,
        BuildConfig.ADMOB_REWARDED_UNIT_ID,
        AdRequest.Builder().build(),
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                if (cont.isActive) {
                    cont.resume(rewardedAd)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        }
    )
}

private suspend fun showRewardedAd(activity: Activity, rewardedAd: RewardedAd): Boolean =
    suspendCancellableCoroutine { cont ->
        var earnedReward = false
        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (cont.isActive) {
                    cont.resume(earnedReward)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                if (cont.isActive) {
                    cont.resume(false)
                }
            }
        }
        rewardedAd.show(activity) {
            earnedReward = true
        }
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
