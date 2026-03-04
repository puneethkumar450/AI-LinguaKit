package com.example.translateapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TranslationHistoryEntity::class, LanguagePackEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PackStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): TranslationHistoryDao
    abstract fun languagePackDao(): LanguagePackDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Adds the language_packs table introduced in v2. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `language_packs` (
                        `mlKitCode`           TEXT    NOT NULL,
                        `displayName`         TEXT    NOT NULL,
                        `status`              TEXT    NOT NULL DEFAULT 'NOT_DOWNLOADED',
                        `downloadedAtMs`      INTEGER NOT NULL DEFAULT 0,
                        `approximateSizeKb`   INTEGER NOT NULL DEFAULT 30720,
                        `errorMessage`        TEXT,
                        PRIMARY KEY(`mlKitCode`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linguakit.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addTypeConverter(PackStatusConverter())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
