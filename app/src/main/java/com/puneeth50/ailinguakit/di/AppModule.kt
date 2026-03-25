package com.puneeth50.ailinguakit.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.puneeth50.ailinguakit.data.AppDatabase
import com.puneeth50.ailinguakit.data.LanguagePackDao
import com.puneeth50.ailinguakit.data.PackStatusConverter
import com.puneeth50.ailinguakit.data.TranslationHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "linguakit.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addTypeConverter(PackStatusConverter())
            .build()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): TranslationHistoryDao = database.historyDao()

    @Provides
    fun provideLanguagePackDao(database: AppDatabase): LanguagePackDao = database.languagePackDao()
}
