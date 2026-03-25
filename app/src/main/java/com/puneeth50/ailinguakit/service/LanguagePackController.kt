package com.puneeth50.ailinguakit.service

import android.app.Application
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.LanguagePackEntity
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.viewmodel.SUPPORTED_LANGUAGES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LanguagePackController(
    private val application: Application,
    private val packRepo: LanguagePackRepository,
    private val allPacks: StateFlow<List<LanguagePackEntity>>,
    private val isPremium: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val freeLanguageLimit: Int,
    private val onMessage: suspend (String) -> Unit,
    private val onPackStateChanged: () -> Unit
) {
    private val _downloadingPacks = MutableStateFlow<Set<String>>(emptySet())
    val downloadingPacks: StateFlow<Set<String>> = _downloadingPacks.asStateFlow()

    private val _packDownloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val packDownloadProgress: StateFlow<Map<String, Int>> = _packDownloadProgress.asStateFlow()

    private val packDownloadJobs = mutableMapOf<String, Job>()
    private val packProgressJobs = mutableMapOf<String, Job>()

    fun canDownloadPack(mlKitCode: String): Boolean {
        if (isPremium.value) return true
        val reservedCodes = allPacks.value
            .filter { it.status == PackStatus.DOWNLOADED }
            .map { it.mlKitCode }
            .toMutableSet()
        reservedCodes += _downloadingPacks.value
        return reservedCodes.contains(mlKitCode) || reservedCodes.size < freeLanguageLimit
    }

    fun downloadPack(mlKitCode: String, requireWifi: Boolean = true) {
        val job = scope.launch {
            if (_downloadingPacks.value.contains(mlKitCode)) {
                return@launch
            }

            if (!ConnectivityHelper.isOnline(application)) {
                onMessage(application.getString(R.string.no_internet_connection))
                return@launch
            }

            if (requireWifi && !ConnectivityHelper.isWifiConnected(application)) {
                onMessage(application.getString(R.string.wifi_required_for_language_pack))
                return@launch
            }

            if (!canDownloadPack(mlKitCode)) {
                onMessage(application.getString(R.string.free_plan_limit_delete_one_language))
                return@launch
            }

            _downloadingPacks.value += mlKitCode
            _packDownloadProgress.value = _packDownloadProgress.value + (mlKitCode to 1)
            startProgressSimulation(mlKitCode)
            try {
                packRepo.downloadPack(mlKitCode, requireWifi) { progress ->
                    updateProgress(mlKitCode, progress)
                }
                stopProgressSimulation(mlKitCode)
                updateProgress(mlKitCode, 100)
                val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
                onMessage(
                    application.getString(
                        R.string.language_ready_template,
                        lang?.displayName(application) ?: mlKitCode
                    )
                )
                onPackStateChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stopProgressSimulation(mlKitCode)
                val message = e.message?.takeIf { it.isNotBlank() }
                    ?: application.getString(R.string.please_check_connection_and_try_again)
                onMessage(application.getString(R.string.download_failed_template, message))
            } finally {
                _downloadingPacks.value = _downloadingPacks.value - mlKitCode
                _packDownloadProgress.value = _packDownloadProgress.value - mlKitCode
                packDownloadJobs.remove(mlKitCode)
            }
        }
        packDownloadJobs[mlKitCode] = job
    }

    fun cancelPackDownload(mlKitCode: String) {
        packDownloadJobs.remove(mlKitCode)?.cancel()
        stopProgressSimulation(mlKitCode)
        _downloadingPacks.value = _downloadingPacks.value - mlKitCode
        _packDownloadProgress.value = _packDownloadProgress.value - mlKitCode
        scope.launch {
            packRepo.markPackNotDownloaded(mlKitCode)
            onMessage(application.getString(R.string.download_cancelled))
        }
    }

    fun deletePack(mlKitCode: String) {
        scope.launch {
            packRepo.deletePack(mlKitCode)
            val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
            onMessage(
                application.getString(
                    R.string.language_removed_template,
                    lang?.displayName(application) ?: mlKitCode
                )
            )
            onPackStateChanged()
        }
    }

    fun refreshPackStatus() {
        scope.launch {
            packRepo.reconcileWithMlKit()
            onPackStateChanged()
        }
    }

    fun cancelAll() {
        packDownloadJobs.values.forEach { it.cancel() }
        packDownloadJobs.clear()
        packProgressJobs.values.forEach { it.cancel() }
        packProgressJobs.clear()
        _downloadingPacks.value = emptySet()
        _packDownloadProgress.value = emptyMap()
    }

    private fun startProgressSimulation(mlKitCode: String) {
        stopProgressSimulation(mlKitCode)
        packProgressJobs[mlKitCode] = scope.launch {
            while (isActive && _downloadingPacks.value.contains(mlKitCode)) {
                delay(350)
                val current = _packDownloadProgress.value[mlKitCode] ?: break
                if (current >= 92) continue
                val next = when {
                    current < 20 -> current + 6
                    current < 45 -> current + 4
                    current < 70 -> current + 3
                    else -> current + 1
                }.coerceAtMost(92)
                updateProgress(mlKitCode, next)
            }
        }
    }

    private fun stopProgressSimulation(mlKitCode: String) {
        packProgressJobs.remove(mlKitCode)?.cancel()
    }

    private fun updateProgress(mlKitCode: String, progress: Int) {
        val current = _packDownloadProgress.value[mlKitCode] ?: 0
        if (progress > current) {
            _packDownloadProgress.value =
                _packDownloadProgress.value + (mlKitCode to progress.coerceIn(0, 100))
        }
    }
}
