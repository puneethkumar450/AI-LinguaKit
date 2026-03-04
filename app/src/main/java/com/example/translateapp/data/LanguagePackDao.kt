package com.example.translateapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguagePackDao {

    /** Observe all packs — used to drive the Language Packs management screen. */
    @Query("SELECT * FROM language_packs ORDER BY displayName ASC")
    fun observeAll(): Flow<List<LanguagePackEntity>>

    /** Get a single pack by its ML Kit language code. */
    @Query("SELECT * FROM language_packs WHERE mlKitCode = :code LIMIT 1")
    suspend fun getByCode(code: String): LanguagePackEntity?

    /** Observe a single pack reactively (used to show inline status in TranslateScreen). */
    @Query("SELECT * FROM language_packs WHERE mlKitCode = :code LIMIT 1")
    fun observeByCode(code: String): Flow<LanguagePackEntity?>

    /** Observe only the packs that are fully downloaded and ready. */
    @Query("SELECT * FROM language_packs WHERE status = 'DOWNLOADED' ORDER BY displayName ASC")
    fun observeDownloaded(): Flow<List<LanguagePackEntity>>

    /** Insert-or-replace a pack record. */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(pack: LanguagePackEntity)

    /** Upsert a list of pack seeds in one transaction (used on first launch). */
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAllIfAbsent(packs: List<LanguagePackEntity>)

    /** Update just the status and optional error message. */
    @Query("""
        UPDATE language_packs
        SET status = :status,
            errorMessage = :error,
            downloadedAtMs = CASE WHEN :status = 'DOWNLOADED' THEN :nowMs ELSE downloadedAtMs END
        WHERE mlKitCode = :code
    """)
    suspend fun updateStatus(
        code: String,
        status: PackStatus,
        error: String? = null,
        nowMs: Long = System.currentTimeMillis()
    )

    /** Mark a previously-downloaded model as NOT_DOWNLOADED after deletion. */
    @Query("UPDATE language_packs SET status = 'NOT_DOWNLOADED', downloadedAtMs = 0 WHERE mlKitCode = :code")
    suspend fun markDeleted(code: String)
}