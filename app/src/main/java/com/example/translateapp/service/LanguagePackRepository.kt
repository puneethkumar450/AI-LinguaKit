package com.example.translateapp.service

import android.content.Context
import android.util.Log
import com.example.translateapp.data.LanguagePackDao
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "LangPackRepo"

/**
 * Coordinates between [LanguagePackDao] (persistent state) and
 * [ModelDownloadManager] (ML Kit on-device models).
 *
 * Responsibilities:
 *  - Seed Room with all supported languages on first launch
 *  - Reconcile Room status with actual on-device model presence at startup
 *  - Expose reactive [Flow]s for the UI
 *  - Provide download / delete operations that update both ML Kit and Room
 */
class LanguagePackRepository @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LanguagePackDao,
    private val downloader: ModelDownloadManager,
    @Assisted private val scope: CoroutineScope
) {

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): LanguagePackRepository
    }

    // ── Public flows ───────────────────────────────────────────────────────────

    val allPacks: StateFlow<List<LanguagePackEntity>> = dao.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val downloadedPacks: StateFlow<List<LanguagePackEntity>> = dao.observeDownloaded()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun observePack(mlKitCode: String): Flow<LanguagePackEntity?> =
        dao.observeByCode(mlKitCode)

    // ── Initialisation ─────────────────────────────────────────────────────────

    /**
     * Call once from the ViewModel init block.
     * Seeds missing rows and reconciles Room status with actual on-device models.
     */
    fun initAsync() {
        scope.launch {
            // 1. Seed rows for every supported language (IGNORE if already present)
            val seeds = SUPPORTED_LANGUAGES.map { lang ->
                LanguagePackEntity(
                    mlKitCode = lang.mlKitCode,
                    displayName = lang.displayName(context)
                )
            }
            dao.insertAllIfAbsent(seeds)

            // 2. Reconcile: query ML Kit for what is actually downloaded
            reconcileWithMlKit()
        }
    }

    /**
     * Asks ML Kit which models are genuinely present on-device and syncs Room
     * to match. This corrects stale DOWNLOADING rows left by a previous crash,
     * and marks freshly-present models (e.g. from a previous install) as DOWNLOADED.
     */
    suspend fun reconcileWithMlKit() {
        try {
            val downloaded = downloader.getDownloadedModels()
            val all = dao.observeAll().let {
                // Read current snapshot from Room directly rather than through Flow
                val packs = mutableListOf<LanguagePackEntity>()
                SUPPORTED_LANGUAGES.forEach { lang ->
                    val row = dao.getByCode(lang.mlKitCode)
                    if (row != null) packs.add(row)
                }
                packs
            }
            all.forEach { pack ->
                val isPresent = downloaded.contains(pack.mlKitCode)
                when {
                    isPresent && pack.status != PackStatus.DOWNLOADED ->
                        dao.updateStatus(pack.mlKitCode, PackStatus.DOWNLOADED)
                    !isPresent && pack.status == PackStatus.DOWNLOADED ->
                        dao.markDeleted(pack.mlKitCode)
                    pack.status == PackStatus.DOWNLOADING ->
                        // crashed mid-download; reset to NOT_DOWNLOADED
                        dao.updateStatus(pack.mlKitCode, PackStatus.NOT_DOWNLOADED)
                    else -> { /* already consistent */ }
                }
            }
            Log.d(TAG, "Reconcile complete. Downloaded: ${downloaded.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Reconcile failed", e)
        }
    }

    // ── Download ───────────────────────────────────────────────────────────────

    /**
     * Downloads the ML Kit model for [mlKitCode].
     * Updates Room status to DOWNLOADING → DOWNLOADED or FAILED.
     *
     * @param requireWifi Restrict download to Wi-Fi only.
     */
    suspend fun downloadPack(
        mlKitCode: String,
        requireWifi: Boolean = true,
        onProgress: ((Int) -> Unit)? = null
    ) {
        dao.updateStatus(mlKitCode, PackStatus.DOWNLOADING)
        onProgress?.invoke(3)
        try {
            downloader.downloadModel(mlKitCode, requireWifi, onProgress = onProgress)
            dao.updateStatus(mlKitCode, PackStatus.DOWNLOADED)
            onProgress?.invoke(100)
            Log.d(TAG, "Pack downloaded and saved: $mlKitCode")
        } catch (e: CancellationException) {
            dao.markDeleted(mlKitCode)
            Log.d(TAG, "Pack download cancelled: $mlKitCode")
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            dao.updateStatus(mlKitCode, PackStatus.FAILED, error = msg)
            Log.e(TAG, "Pack download failed: $mlKitCode – $msg", e)
            throw e   // re-throw so ViewModel can show a snackbar
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    /**
     * Removes the on-device ML Kit model and marks the Row as NOT_DOWNLOADED.
     */
    suspend fun deletePack(mlKitCode: String) {
        try {
            downloader.deleteModel(mlKitCode)
            dao.markDeleted(mlKitCode)
            Log.d(TAG, "Pack deleted: $mlKitCode")
        } catch (e: Exception) {
            Log.e(TAG, "Pack delete failed: $mlKitCode", e)
            throw e
        }
    }

    suspend fun markPackNotDownloaded(mlKitCode: String) {
        dao.markDeleted(mlKitCode)
    }

    // ── Check ──────────────────────────────────────────────────────────────────

    /**
     * Returns true if both the source and target language models are present
     * on-device and translation can proceed entirely offline.
     */
    suspend fun areBothPacksReady(sourceMlKitCode: String, targetMlKitCode: String): Boolean {
        val src = dao.getByCode(sourceMlKitCode)
        val tgt = dao.getByCode(targetMlKitCode)
        return src?.status == PackStatus.DOWNLOADED && tgt?.status == PackStatus.DOWNLOADED
    }
}
