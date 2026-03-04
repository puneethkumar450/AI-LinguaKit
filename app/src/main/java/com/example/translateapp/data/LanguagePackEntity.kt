package com.example.translateapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the local download state of each ML Kit translation language model.
 *
 * [mlKitCode]       — the language tag used by ML Kit (e.g. "en", "fr")
 * [displayName]     — human-readable name shown in the UI
 * [status]          — current lifecycle state of the on-device model
 * [downloadedAtMs]  — epoch ms when the model finished downloading (0 = never)
 * [approximateSizeKb] — rough model size for display (~30 MB each for ML Kit translate)
 */
@Entity(tableName = "language_packs")
data class LanguagePackEntity(
    @PrimaryKey val mlKitCode: String,
    val displayName: String,
    val status: PackStatus = PackStatus.NOT_DOWNLOADED,
    val downloadedAtMs: Long = 0L,
    val approximateSizeKb: Int = 30_720,   // ~30 MB default
    val errorMessage: String? = null
)

enum class PackStatus {
    NOT_DOWNLOADED,   // model absent from device
    DOWNLOADING,      // in progress (transient – not persisted across restarts)
    DOWNLOADED,       // model present and ready offline
    FAILED            // last attempt failed
}
