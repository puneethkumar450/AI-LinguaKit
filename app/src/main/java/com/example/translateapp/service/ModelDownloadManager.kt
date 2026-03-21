package com.example.translateapp.service

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ModelDownloadManager"

class ModelDownloadManager {

    private val remoteModelManager = RemoteModelManager.getInstance()
    private val translatorCache = ConcurrentHashMap<String, Translator>()
    private val modelReadyTasks = ConcurrentHashMap<String, Task<Void>>()

    // Warms up the translator by running a dummy translation
    // so the real translation is instant
    suspend fun warmUp(sourceLang: String, targetLang: String) {
        try {
            val translator = getTranslator(sourceLang, targetLang)
            ensureModelReady(translator, sourceLang, targetLang)
            translator.translate("hello") // lightweight dummy — just loads the model into memory
        } catch (e: Exception) {
            Log.w(TAG, "Warmup failed for $sourceLang->$targetLang, will retry on real translate", e)
        }
    }

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang-$targetLang"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    private fun ensureModelReady(translator: Translator, sourceLang: String, targetLang: String): Task<Void> {
        val key = "$sourceLang-$targetLang"
        return modelReadyTasks.getOrPut(key) {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
        }
    }

    /**
     * Optimized translation:
     * 1. Reuses Translator instances to avoid engine re-initialization.
     * 2. Caches the model-readiness Task to avoid redundant network checks.
     * 3. Executes immediately if the model is already available.
     */
    suspend fun translateOnline(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val translator = getTranslator(sourceLang, targetLang)
        val readyTask = ensureModelReady(translator, sourceLang, targetLang)

        return suspendCancellableCoroutine { cont ->
            if (readyTask.isSuccessful) {
                // Fast path: model is already ready
                translator.translate(text)
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            } else {
                // Wait for the model to be ready
                readyTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        translator.translate(text)
                            .addOnSuccessListener { result -> cont.resume(result) }
                            .addOnFailureListener { e -> cont.resumeWithException(e) }
                    } else {
                        // Remove from cache on failure so it can be retried
                        modelReadyTasks.remove("$sourceLang-$targetLang")
                        cont.resumeWithException(task.exception ?: Exception("Model download failed"))
                    }
                }
            }
        }
    }

    suspend fun translateOffline(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        val translator = getTranslator(sourceLang, targetLang)
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    fun close() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        modelReadyTasks.clear()
    }

    // ── Model Management ───────────────────────────────────────────────────────

    suspend fun isModelDownloaded(mlKitCode: String): Boolean =
        suspendCancellableCoroutine { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "isModelDownloaded failed for $mlKitCode", e)
                    cont.resume(false)
                }
        }

    suspend fun getDownloadedModels(): Set<String> =
        suspendCancellableCoroutine { cont ->
            remoteModelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    cont.resume(models.map { it.language }.toSet())
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "getDownloadedModels failed", e)
                    cont.resume(emptySet())
                }
        }

    suspend fun downloadModel(
        mlKitCode: String,
        requireWifi: Boolean = true,
        onProgress: ((Int) -> Unit)? = null
    ) {
        suspendCancellableCoroutine<Unit> { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            val conditions = if (requireWifi) {
                DownloadConditions.Builder().requireWifi().build()
            } else {
                DownloadConditions.Builder().build()
            }
            remoteModelManager.download(model, conditions)
                .addOnSuccessListener {
                    onProgress?.invoke(100)
                    cont.resume(Unit)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    suspend fun deleteModel(mlKitCode: String) {
        suspendCancellableCoroutine { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            remoteModelManager.deleteDownloadedModel(model)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }
}
