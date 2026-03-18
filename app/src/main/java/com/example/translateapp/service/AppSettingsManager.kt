package com.example.translateapp.service

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppThemeMode(val storageValue: String, val nightMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromStorageValue(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

data class AppLanguageOption(
    val languageTag: String,
    val displayName: String
)

private const val PREF_THEME_MODE = "theme_mode"
private const val PREF_APP_LANGUAGE = "app_language"

object AppSettingsManager {
    fun getSavedThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return AppThemeMode.fromStorageValue(prefs.getString(PREF_THEME_MODE, AppThemeMode.SYSTEM.storageValue))
    }

    fun getSavedLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_APP_LANGUAGE, "") ?: ""
    }

    fun saveThemeMode(context: Context, themeMode: AppThemeMode) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_THEME_MODE, themeMode.storageValue).apply()
        AppCompatDelegate.setDefaultNightMode(themeMode.nightMode)
    }

    fun saveAppLanguage(context: Context, languageTag: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_APP_LANGUAGE, languageTag).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun applySavedConfiguration(context: Context) {
        val themeMode = getSavedThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(themeMode.nightMode)

        val languageTag = getSavedLanguageTag(context)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun supportedLanguages(): List<AppLanguageOption> {
        return listOf(
            AppLanguageOption("", "System default"),
            AppLanguageOption("en", "English"),
            AppLanguageOption("zh-CN", "Chinese (Simplified)"),
            AppLanguageOption("ja", "Japanese"),
            AppLanguageOption("ko", "Korean"),
            AppLanguageOption("ar", "Arabic"),
            AppLanguageOption("es", "Spanish"),
            AppLanguageOption("fr", "French"),
            AppLanguageOption("de", "German"),
            AppLanguageOption("pt-BR", "Portuguese (Brazil)"),
            AppLanguageOption("hi", "Hindi")
        )
    }
}
