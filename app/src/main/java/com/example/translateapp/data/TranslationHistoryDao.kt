package com.example.translateapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationHistoryDao {

    @Query("SELECT * FROM translation_history ORDER BY timestampMs DESC LIMIT 200")
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestampMs DESC")
    fun getFavorites(): Flow<List<TranslationHistoryEntity>>

    @Query("""
        SELECT * FROM translation_history
        WHERE LOWER(TRIM(sourceText)) = LOWER(TRIM(:sourceText))
          AND sourceLangCode = :sourceLangCode
          AND targetLangCode = :targetLangCode
        ORDER BY timestampMs DESC
        LIMIT 1
    """)
    suspend fun findInHistory(
        sourceText: String,
        sourceLangCode: String,
        targetLangCode: String
    ): TranslationHistoryEntity?

    @Query("""
        SELECT * FROM translation_history
        WHERE LOWER(TRIM(sourceText)) = LOWER(TRIM(:sourceText))
          AND LOWER(TRIM(translatedText)) = LOWER(TRIM(:translatedText))
          AND sourceLangCode = :sourceLangCode
          AND targetLangCode = :targetLangCode
        ORDER BY timestampMs DESC
        LIMIT 1
    """)
    suspend fun findExactHistoryEntry(
        sourceText: String,
        translatedText: String,
        sourceLangCode: String,
        targetLangCode: String
    ): TranslationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranslationHistoryEntity): Long

    @Query("UPDATE translation_history SET timestampMs = :timestampMs WHERE id = :id")
    suspend fun touch(id: Long, timestampMs: Long = System.currentTimeMillis())

    @Query("UPDATE translation_history SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Delete
    suspend fun delete(entry: TranslationHistoryEntity)

    @Query("DELETE FROM translation_history WHERE isFavorite = 0")
    suspend fun clearNonFavorites()

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}
