package com.example.translateapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationHistoryDao {

    @Query("SELECT * FROM translation_history ORDER BY timestampMs DESC LIMIT 200")
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestampMs DESC")
    fun getFavorites(): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE sourceText = :text AND sourceLangCode = :srcCode AND targetLangCode = :tgtCode LIMIT 1")
    suspend fun findInHistory(text: String, srcCode: String, tgtCode: String): TranslationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranslationHistoryEntity): Long

    @Query("UPDATE translation_history SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Delete
    suspend fun delete(entry: TranslationHistoryEntity)

    @Query("DELETE FROM translation_history WHERE isFavorite = 0")
    suspend fun clearNonFavorites()

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}
