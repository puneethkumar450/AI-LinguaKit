package com.puneeth50.ailinguakit.service

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.LanguagePackDao
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.data.TranslationHistoryDao
import com.puneeth50.ailinguakit.data.TranslationHistoryEntity
import com.puneeth50.ailinguakit.viewmodel.AUTO_DETECT_OPTION
import com.puneeth50.ailinguakit.viewmodel.LanguageOption
import com.puneeth50.ailinguakit.viewmodel.SUPPORTED_LANGUAGES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslationController(
    private val application: Application,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val historyDao: TranslationHistoryDao,
    private val packDao: LanguagePackDao,
    private val downloader: ModelDownloadManager,
    private val isPremium: StateFlow<Boolean>,
    initialSource: LanguageOption,
    initialTarget: LanguageOption,
    private val sanitizeSource: (LanguageOption) -> LanguageOption,
    private val sanitizeTarget: (LanguageOption) -> LanguageOption,
    private val resolveSourceLanguage: suspend (String) -> LanguageOption,
    private val ensureFreeWordLimit: suspend (String, LanguageOption, LanguageOption) -> Unit,
    private val ensureLanguagePairAllowed: suspend (LanguageOption, LanguageOption) -> Unit,
    private val markWordsUsedIfNeeded: (String, LanguageOption, LanguageOption) -> Unit,
    private val registerTranslationAction: suspend () -> Unit
) {
    companion object {
        private const val PREF_SOURCE_LANG_CODE = "source_lang_code"
        private const val PREF_TARGET_LANG_CODE = "target_lang_code"
    }

    enum class TranslateSource { NONE, OFFLINE, ONLINE, HISTORY }

    private val _translateInput = MutableStateFlow("")
    val translateInput: StateFlow<String> = _translateInput.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _translateSourceLang = MutableStateFlow(initialSource)
    val translateSourceLang: StateFlow<LanguageOption> = _translateSourceLang.asStateFlow()

    private val _translateTargetLang = MutableStateFlow(initialTarget)
    val translateTargetLang: StateFlow<LanguageOption> = _translateTargetLang.asStateFlow()

    private val _translateLoading = MutableStateFlow(false)
    val translateLoading: StateFlow<Boolean> = _translateLoading.asStateFlow()

    private val _translateError = MutableStateFlow<String?>(null)
    val translateError: StateFlow<String?> = _translateError.asStateFlow()

    private val _translateSource = MutableStateFlow(TranslateSource.NONE)
    val translateSource: StateFlow<TranslateSource> = _translateSource.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<LanguageOption?>(null)
    val detectedLanguage: StateFlow<LanguageOption?> = _detectedLanguage.asStateFlow()

    private var translateJob: Job? = null

    fun onTranslateInputChange(text: String) {
        _translateInput.value = text
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        val safeLang = sanitizeSource(lang)
        if (safeLang.code == _translateTargetLang.value.code) return
        _translateSourceLang.value = safeLang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
        prefs.edit { putString(PREF_SOURCE_LANG_CODE, safeLang.code) }

        scope.launch {
            val tgt = _translateTargetLang.value
            if (safeLang != AUTO_DETECT_OPTION && canWarmUp(safeLang.mlKitCode, tgt.mlKitCode)) {
                downloader.warmUp(safeLang.mlKitCode, tgt.mlKitCode)
            }
        }
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        val safeLang = sanitizeTarget(lang)
        if (safeLang.code == _translateSourceLang.value.code) return
        _translateTargetLang.value = safeLang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        prefs.edit { putString(PREF_TARGET_LANG_CODE, safeLang.code) }

        scope.launch {
            val src = _translateSourceLang.value
            val srcCode = if (src == AUTO_DETECT_OPTION) com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH else src.mlKitCode
            if (canWarmUp(srcCode, safeLang.mlKitCode)) {
                downloader.warmUp(srcCode, safeLang.mlKitCode)
            }
        }
    }

    fun setTranslateLanguages(source: LanguageOption, target: LanguageOption) {
        val safeSource = sanitizeSource(source)
        val safeTarget = sanitizeTarget(target)
        if (safeSource.code == safeTarget.code) return

        _translateSourceLang.value = safeSource
        _translateTargetLang.value = safeTarget
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        _translateError.value = null
        _detectedLanguage.value = null

        prefs.edit {
            putString(PREF_SOURCE_LANG_CODE, safeSource.code)
            putString(PREF_TARGET_LANG_CODE, safeTarget.code)
        }

        scope.launch {
            val srcCode = if (safeSource == AUTO_DETECT_OPTION) {
                com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH
            } else {
                safeSource.mlKitCode
            }
            if (canWarmUp(srcCode, safeTarget.mlKitCode)) {
                downloader.warmUp(srcCode, safeTarget.mlKitCode)
            }
        }
    }

    fun swapTranslateLanguages() {
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value
        if (src == AUTO_DETECT_OPTION) return

        val input = _translateInput.value
        val output = _translatedText.value

        _translateSourceLang.value = sanitizeSource(tgt)
        _translateTargetLang.value = sanitizeTarget(src)
        _translateSource.value = TranslateSource.NONE
        _translateError.value = null
        _detectedLanguage.value = null
        _translateInput.value = output
        _translatedText.value = input

        prefs.edit {
            putString(PREF_SOURCE_LANG_CODE, _translateSourceLang.value.code)
            putString(PREF_TARGET_LANG_CODE, _translateTargetLang.value.code)
        }

        scope.launch {
            val swappedSource = _translateSourceLang.value
            val swappedTarget = _translateTargetLang.value
            if (canWarmUp(swappedSource.mlKitCode, swappedTarget.mlKitCode)) {
                downloader.warmUp(swappedSource.mlKitCode, swappedTarget.mlKitCode)
            }
        }
    }

    fun clearTranslation() {
        _translateInput.value = ""
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
    }

    fun clearTranslateError() {
        _translateError.value = null
    }

    fun restoreFromHistory(entry: TranslationHistoryEntity, fallbackSource: LanguageOption, fallbackTarget: LanguageOption) {
        setTranslateLanguages(
            SUPPORTED_LANGUAGES.find { it.code == entry.sourceLangCode } ?: fallbackSource,
            SUPPORTED_LANGUAGES.find { it.code == entry.targetLangCode } ?: fallbackTarget
        )
        _translateInput.value = entry.sourceText
        _translatedText.value = entry.translatedText
        _translateSource.value = TranslateSource.HISTORY
    }

    fun coerceSelectedLanguages(sourceFallback: LanguageOption, targetFallback: LanguageOption) {
        if (_translateSourceLang.value.code != sanitizeSource(_translateSourceLang.value).code) {
            _translateSourceLang.value = sourceFallback
        }
        val safeTarget = sanitizeTarget(_translateTargetLang.value)
        if (_translateTargetLang.value.code != safeTarget.code || safeTarget.code == _translateSourceLang.value.code) {
            _translateTargetLang.value = targetFallback
        }
        prefs.edit {
            putString(PREF_SOURCE_LANG_CODE, _translateSourceLang.value.code)
                .putString(PREF_TARGET_LANG_CODE, _translateTargetLang.value.code)
        }
    }

    fun translate(buildOfflineError: (LanguageOption, LanguageOption) -> String) {
        val text = _translateInput.value.trim()
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }

        translateJob?.cancel()
        translateJob = scope.launch {
            _translateLoading.value = true
            _translateError.value = null
            _translateSource.value = TranslateSource.NONE

            try {
                val src = resolveSourceLanguage(text).also { _detectedLanguage.value = it.takeIf { detected -> _translateSourceLang.value == AUTO_DETECT_OPTION } }
                val tgt = sanitizeTarget(_translateTargetLang.value)
                val cached = historyDao.findInHistory(text, src.code, tgt.code)

                if (cached != null) {
                    _translatedText.value = cached.translatedText
                    _translateSource.value = TranslateSource.HISTORY
                    historyDao.touch(cached.id)
                    registerTranslationAction()
                    return@launch
                }

                if (!isPremium.value) {
                    ensureLanguagePairAllowed(src, tgt)
                    ensureFreeWordLimit(text, src, tgt)
                }

                val packsReady = areBothPacksReady(src.mlKitCode, tgt.mlKitCode)
                val isOnline = ConnectivityHelper.isOnline(application)

                when {
                    packsReady -> {
                        val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                        onTranslationCompleted(text, result, src, tgt, TranslateSource.OFFLINE)
                    }
                    isOnline -> {
                        val result = downloader.translateOnline(text, src.mlKitCode, tgt.mlKitCode)
                        onTranslationCompleted(text, result, src, tgt, TranslateSource.ONLINE)
                    }
                    else -> {
                        _translateError.value = buildOfflineError(src, tgt)
                    }
                }
            } catch (e: Exception) {
                _translateError.value = e.message ?: application.getString(R.string.translation_failed)
            } finally {
                _translateLoading.value = false
            }
        }
    }

    private suspend fun onTranslationCompleted(
        text: String,
        result: String,
        src: LanguageOption,
        tgt: LanguageOption,
        source: TranslateSource
    ) {
        _translatedText.value = result
        _translateSource.value = source
        saveHistoryIfNeeded(text, result, src, tgt)
        markWordsUsedIfNeeded(text, src, tgt)
        registerTranslationAction()
    }

    private suspend fun saveHistoryIfNeeded(
        text: String,
        result: String,
        src: LanguageOption,
        tgt: LanguageOption,
    ) {
        val existing = historyDao.findExactHistoryEntry(text, result, src.code, tgt.code)
        if (existing != null) {
            historyDao.touch(existing.id)
            return
        }

        historyDao.insert(
            TranslationHistoryEntity(
                sourceText = text,
                translatedText = result,
                sourceLangCode = src.code,
                sourceLangName = src.displayName(application),
                targetLangCode = tgt.code,
                targetLangName = tgt.displayName(application)
            )
        )
    }

    private suspend fun areBothPacksReady(sourceMlKitCode: String, targetMlKitCode: String): Boolean {
        val src = packDao.getByCode(sourceMlKitCode)
        val tgt = packDao.getByCode(targetMlKitCode)
        return src?.status == PackStatus.DOWNLOADED && tgt?.status == PackStatus.DOWNLOADED
    }

    private suspend fun canWarmUp(sourceMlKitCode: String, targetMlKitCode: String): Boolean {
        val srcReady = packDao.getByCode(sourceMlKitCode)?.status == PackStatus.DOWNLOADED
        val tgtReady = packDao.getByCode(targetMlKitCode)?.status == PackStatus.DOWNLOADED
        return isPremium.value || (srcReady && tgtReady)
    }
}
