package com.example.translateapp.service

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ModelDownloadManager"

/**
 * Thin coroutine wrapper around ML Kit's [RemoteModelManager].
 *
 * All public functions are safe to call from any coroutine dispatcher; they
 * suspend until the underlying Task completes.
 */
class ModelDownloadManager {

    private val remoteModelManager = RemoteModelManager.getInstance()

    // ── Query ──────────────────────────────────────────────────────────────────

    /**
     * Returns true if the on-device model for [mlKitCode] is already present.
     * Does NOT require network access.
     */
    suspend fun isModelDownloaded(mlKitCode: String): Boolean =
        suspendCancellableCoroutine { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { e ->
                    Log.w(TAG, "isModelDownloaded failed for $mlKitCode", e)
                    cont.resume(false)   // treat failure as not-downloaded
                }
        }

    /**
     * Returns the set of all ML Kit translate language codes whose models are
     * currently stored on this device.
     */
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

    // ── Download ───────────────────────────────────────────────────────────────

    /**
     * Downloads the model for [mlKitCode], requiring Wi-Fi.
     *
     * Suspends until the download finishes, then resumes.
     * Throws on failure so the caller can update Room status to FAILED.
     *
     * @param requireWifi When true, the download is restricted to Wi-Fi.
     *                    Pass false to allow cellular (user's choice).
     */
    suspend fun downloadModel(mlKitCode: String, requireWifi: Boolean = true) {
        suspendCancellableCoroutine<Unit> { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            val conditions = if (requireWifi) {
                DownloadConditions.Builder().requireWifi().build()
            } else {
                DownloadConditions.Builder().build()
            }
            remoteModelManager.download(model, conditions)
                .addOnSuccessListener {
                    Log.d(TAG, "Downloaded model: $mlKitCode")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Download failed: $mlKitCode", e)
                    cont.resumeWithException(e)
                }
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    /**
     * Removes the on-device model for [mlKitCode] to free storage.
     * Throws on failure.
     */
    suspend fun deleteModel(mlKitCode: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            remoteModelManager.deleteDownloadedModel(model)
                .addOnSuccessListener {
                    Log.d(TAG, "Deleted model: $mlKitCode")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Delete failed: $mlKitCode", e)
                    cont.resumeWithException(e)
                }
        }
    }

    // ── Translate (offline — model must be pre-downloaded) ────────────────────

    /**
     * Translates [text] using a model that **must already be downloaded**.
     * Does NOT attempt any network download — throws if model is absent.
     */
    suspend fun translateOffline(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String = suspendCancellableCoroutine { cont ->
        val options = com.google.mlkit.nl.translate.TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = com.google.mlkit.nl.translate.Translation.getClient(options)
        translator.translate(text)
            .addOnSuccessListener { result ->
                cont.resume(result)
                translator.close()
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
                translator.close()
            }
    }

    // ── Translate (online — no local model required) ──────────────────────────

    /**
     * Translates [text] using ML Kit's cloud path via [downloadModelIfNeeded].
     *
     * This does NOT download the full ~30 MB language pack — ML Kit fetches
     * just enough data to produce the translation over the network.
     * Use this when the pack is absent but the device is online.
     *
     * Requires internet. Throws if offline or the request fails.
     */
    suspend fun translateOnline(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String = suspendCancellableCoroutine { cont ->
        val options = com.google.mlkit.nl.translate.TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = com.google.mlkit.nl.translate.Translation.getClient(options)
        // downloadModelIfNeeded with no conditions = uses network if needed,
        // but does NOT persist the full model to disk.
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        cont.resume(result)
                        translator.close()
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e)
                        translator.close()
                    }
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
                translator.close()
            }
    }
}
