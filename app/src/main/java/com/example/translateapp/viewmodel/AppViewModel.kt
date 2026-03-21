package com.example.translateapp.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.translateapp.data.AppDatabase
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.data.TranslationHistoryEntity
import com.example.translateapp.service.AppLanguageOption
import com.example.translateapp.service.AppSettingsManager
import com.example.translateapp.service.AppThemeMode
import com.example.translateapp.service.BillingRepository
import com.example.translateapp.service.ConnectivityHelper
import com.example.translateapp.service.LanguagePackRepository
import com.example.translateapp.service.ModelDownloadManager
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LanguageOption(val code: String, val displayName: String, val mlKitCode: String)

val AUTO_DETECT_OPTION = LanguageOption("auto", "Auto Detect", "auto")

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English", TranslateLanguage.ENGLISH),
    LanguageOption("es", "Spanish", TranslateLanguage.SPANISH),
    LanguageOption("fr", "French", TranslateLanguage.FRENCH),
    LanguageOption("de", "German", TranslateLanguage.GERMAN),
    LanguageOption("it", "Italian", TranslateLanguage.ITALIAN),
    LanguageOption("pt", "Portuguese", TranslateLanguage.PORTUGUESE),
    LanguageOption("zh", "Chinese", TranslateLanguage.CHINESE),
    LanguageOption("ja", "Japanese", TranslateLanguage.JAPANESE),
    LanguageOption("ko", "Korean", TranslateLanguage.KOREAN),
    LanguageOption("ar", "Arabic", TranslateLanguage.ARABIC),
    LanguageOption("ru", "Russian", TranslateLanguage.RUSSIAN),
    LanguageOption("hi", "Hindi", TranslateLanguage.HINDI),
    LanguageOption("ta", "Tamil", TranslateLanguage.TAMIL),
    LanguageOption("te", "Telugu", TranslateLanguage.TELUGU),
    LanguageOption("bn", "Bengali", TranslateLanguage.BENGALI),
    LanguageOption("mr", "Marathi", TranslateLanguage.MARATHI),
    LanguageOption("kn", "Kannada", TranslateLanguage.KANNADA),
    LanguageOption("vi", "Vietnamese", TranslateLanguage.VIETNAMESE),
)

enum class HistoryFilter { ALL, FAVORITES }

private const val TAG = "AppViewModel"
private const val PREF_IS_FIRST_RUN = "is_first_run"
private const val PREF_SOURCE_LANG_CODE = "source_lang_code"
private const val PREF_TARGET_LANG_CODE = "target_lang_code"
private const val PREF_FREE_WORDS_USED = "free_words_used"
private const val PREF_TRANSLATE_ACTIONS = "translate_actions"
private const val PREF_COUNTED_TRANSLATION_KEYS = "counted_translation_keys"
private const val PREF_FREE_LANGUAGE_CODES = "free_language_codes"
private const val FREE_WORD_LIMIT = 5_000
private const val FREE_LANGUAGE_LIMIT = 3
private const val AD_FREQUENCY = 3
private val DEFAULT_FREE_LANGUAGE = SUPPORTED_LANGUAGES.first { it.code == "en" }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val db = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()
    private val packDao = db.languagePackDao()
    private val downloader = ModelDownloadManager()
    val packRepo = LanguagePackRepository(packDao, downloader, viewModelScope)
    private val billingRepo = BillingRepository(application, prefs)

    private val _isFirstRun = MutableStateFlow(prefs.getBoolean(PREF_IS_FIRST_RUN, true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingRepo.isPremium
    val premiumProduct = billingRepo.premiumProduct
    val billingReady = billingRepo.billingReady
    private val _themeMode = MutableStateFlow(AppSettingsManager.getSavedThemeMode(application))
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()
    private val _appLanguageTag = MutableStateFlow(AppSettingsManager.getSavedLanguageTag(application))
    val appLanguageTag: StateFlow<String> = _appLanguageTag.asStateFlow()
    val supportedAppLanguages: List<AppLanguageOption> = AppSettingsManager.supportedLanguages()

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessage = _uiMessage.asSharedFlow()

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val showInterstitialAd = _showInterstitialAd.asSharedFlow()

    private val _freeWordsUsed = MutableStateFlow(prefs.getInt(PREF_FREE_WORDS_USED, 0))
    val freeWordsUsed: StateFlow<Int> = _freeWordsUsed.asStateFlow()

    private val _translateActions = MutableStateFlow(prefs.getInt(PREF_TRANSLATE_ACTIONS, 0))
    val translateActions: StateFlow<Int> = _translateActions.asStateFlow()

    private val _freePlanLanguages = MutableStateFlow(loadInitialFreeLanguages())
    val freePlanLanguages: StateFlow<List<LanguageOption>> = _freePlanLanguages.asStateFlow()

    val allHistory: StateFlow<List<TranslationHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<TranslationHistoryEntity>> = historyDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _translateInput = MutableStateFlow("")
    val translateInput: StateFlow<String> = _translateInput.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _translateSourceLang = MutableStateFlow(loadInitialSourceLanguage())
    val translateSourceLang: StateFlow<LanguageOption> = _translateSourceLang.asStateFlow()

    private val _translateTargetLang = MutableStateFlow(loadInitialTargetLanguage())
    val translateTargetLang: StateFlow<LanguageOption> = _translateTargetLang.asStateFlow()

    private val _translateLoading = MutableStateFlow(false)
    val translateLoading: StateFlow<Boolean> = _translateLoading.asStateFlow()

    private val _translateError = MutableStateFlow<String?>(null)
    val translateError: StateFlow<String?> = _translateError.asStateFlow()

    enum class TranslateSource { NONE, OFFLINE, ONLINE, HISTORY }

    private val _translateSource = MutableStateFlow(TranslateSource.NONE)
    val translateSource: StateFlow<TranslateSource> = _translateSource.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<LanguageOption?>(null)
    val detectedLanguage: StateFlow<LanguageOption?> = _detectedLanguage.asStateFlow()

    private val _isLiveCameraMode = MutableStateFlow(false)
    val isLiveCameraMode: StateFlow<Boolean> = _isLiveCameraMode.asStateFlow()

    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()

    private val _ocrTranslatedText = MutableStateFlow("")
    val ocrTranslatedText: StateFlow<String> = _ocrTranslatedText.asStateFlow()

    private val _ocrLoading = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    val allPacks: StateFlow<List<LanguagePackEntity>> = packRepo.allPacks

    private val _downloadingPacks = MutableStateFlow<Set<String>>(emptySet())
    val downloadingPacks: StateFlow<Set<String>> = _downloadingPacks.asStateFlow()
    private val _packDownloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val packDownloadProgress: StateFlow<Map<String, Int>> = _packDownloadProgress.asStateFlow()
    private val packDownloadJobs = mutableMapOf<String, Job>()
    private val packProgressJobs = mutableMapOf<String, Job>()

    private var speechRecognizer: SpeechRecognizer? = null
    private var translateJob: Job? = null

    init {
        packRepo.initAsync()
        initTts(application)
        initSpeechRecognizer(application)
        reconcileFreeLanguagesWithCurrentState()

        viewModelScope.launch {
            billingRepo.isPremium.collectLatest { premium ->
                if (premium) {
                    _translateError.value = null
                } else {
                    reconcileFreeLanguagesWithCurrentState()
                }
            }
        }

        viewModelScope.launch {
            billingRepo.billingMessage.collectLatest { message ->
                if (!message.isNullOrBlank()) {
                    _uiMessage.emit(message)
                    billingRepo.clearBillingMessage()
                }
            }
        }
    }

    fun completeFirstRun(source: LanguageOption, target: LanguageOption) {
        val selected = listOf(DEFAULT_FREE_LANGUAGE, source, target)
            .distinctBy { it.code }
            .take(FREE_LANGUAGE_LIMIT)
        persistFreeLanguages(selected)
        prefs.edit {
            putBoolean(PREF_IS_FIRST_RUN, false)
                .putString(PREF_SOURCE_LANG_CODE, DEFAULT_FREE_LANGUAGE.code)
                .putString(PREF_TARGET_LANG_CODE, source.code)
        }

        _isFirstRun.value = false
        _translateSourceLang.value = DEFAULT_FREE_LANGUAGE
        _translateTargetLang.value = source

        viewModelScope.launch {
            _uiMessage.emit("Downloading your selected language packs.")
            selected.forEach { lang ->
                if (packDao.getByCode(lang.mlKitCode)?.status != PackStatus.DOWNLOADED) {
                    runCatching { packRepo.downloadPack(lang.mlKitCode, requireWifi = false) }
                }
            }
            downloader.warmUp(source.mlKitCode, target.mlKitCode)
            reconcileFreeLanguagesWithCurrentState()
        }
    }

    fun observePackStatus(mlKitCode: String): Flow<LanguagePackEntity?> = packRepo.observePack(mlKitCode)

    fun isFreePlan(): Boolean = !isPremium.value

    fun canOpenLanguagePicker(): Boolean = isPremium.value

    fun isFreeLanguage(lang: LanguageOption): Boolean {
        return _freePlanLanguages.value.any { it.code == lang.code }
    }

    fun getSelectableSourceLanguages(): List<LanguageOption> {
        return if (isPremium.value) {
            listOf(AUTO_DETECT_OPTION) + SUPPORTED_LANGUAGES
        } else {
            SUPPORTED_LANGUAGES
        }
    }

    fun getSelectableTargetLanguages(): List<LanguageOption> {
        return if (isPremium.value) {
            SUPPORTED_LANGUAGES
        } else {
            SUPPORTED_LANGUAGES
        }
    }

    fun launchPremiumPurchase(activity: Activity): Boolean {
        return billingRepo.launchPurchase(activity)
    }

    fun refreshPremiumState() {
        billingRepo.refreshPurchases()
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        AppSettingsManager.saveThemeMode(getApplication(), themeMode)
        _themeMode.value = themeMode
    }

    fun setAppLanguage(languageTag: String) {
        AppSettingsManager.saveAppLanguage(getApplication(), languageTag)
        _appLanguageTag.value = languageTag
    }

    fun remainingFreeWords(): Int = (FREE_WORD_LIMIT - _freeWordsUsed.value).coerceAtLeast(0)

    fun shouldShowAds(): Boolean = !isPremium.value

    fun shouldShowBannerAd(): Boolean = !isPremium.value

    fun canDownloadPack(mlKitCode: String): Boolean {
        if (isPremium.value) return true
        val reservedCodes = allPacks.value
            .filter { it.status == PackStatus.DOWNLOADED }
            .map { it.mlKitCode }
            .toMutableSet()
        reservedCodes += _downloadingPacks.value
        return reservedCodes.contains(mlKitCode) || reservedCodes.size < FREE_LANGUAGE_LIMIT
    }

    fun setLiveCameraMode(enabled: Boolean) {
        _isLiveCameraMode.value = enabled
    }

    fun onTranslateInputChange(text: String) {
        _translateInput.value = text
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        val safeLang = sanitizeSourceLanguage(lang)
        if (safeLang.code == _translateTargetLang.value.code) return
        _translateSourceLang.value = safeLang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
        prefs.edit { putString(PREF_SOURCE_LANG_CODE, safeLang.code) }

        viewModelScope.launch {
            val tgt = _translateTargetLang.value
            if (safeLang != AUTO_DETECT_OPTION && canWarmUp(safeLang.mlKitCode, tgt.mlKitCode)) {
                downloader.warmUp(safeLang.mlKitCode, tgt.mlKitCode)
            }
        }
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        val safeLang = sanitizeTargetLanguage(lang)
        if (safeLang.code == _translateSourceLang.value.code) return
        _translateTargetLang.value = safeLang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        prefs.edit { putString(PREF_TARGET_LANG_CODE, safeLang.code) }

        viewModelScope.launch {
            val src = _translateSourceLang.value
            val srcCode = if (src == AUTO_DETECT_OPTION) TranslateLanguage.ENGLISH else src.mlKitCode
            if (canWarmUp(srcCode, safeLang.mlKitCode)) {
                downloader.warmUp(srcCode, safeLang.mlKitCode)
            }
        }
    }

    fun swapTranslateLanguages() {
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value
        if (src == AUTO_DETECT_OPTION) return

        val input = _translateInput.value
        val output = _translatedText.value

        _translateSourceLang.value = sanitizeSourceLanguage(tgt)
        _translateTargetLang.value = sanitizeTargetLanguage(src)
        _translateSource.value = TranslateSource.NONE
        _translateError.value = null
        _detectedLanguage.value = null
        _translateInput.value = output
        _translatedText.value = input

        prefs.edit {
            putString(PREF_SOURCE_LANG_CODE, _translateSourceLang.value.code)
            putString(PREF_TARGET_LANG_CODE, _translateTargetLang.value.code)
        }

        viewModelScope.launch {
            val swappedSource = _translateSourceLang.value
            val swappedTarget = _translateTargetLang.value
            if (canWarmUp(swappedSource.mlKitCode, swappedTarget.mlKitCode)) {
                downloader.warmUp(swappedSource.mlKitCode, swappedTarget.mlKitCode)
            }
        }
    }

    fun translate() {
        val rawText = _translateInput.value
        val text = rawText.trim()
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            _translateLoading.value = true
            _translateError.value = null
            _translateSource.value = TranslateSource.NONE

            try {
                val src = resolveSourceLanguage(text)
                val tgt = sanitizeTargetLanguage(_translateTargetLang.value)
                val cached = historyDao.findInHistory(text, src.code, tgt.code)

                if (cached != null) {
                    _translatedText.value = cached.translatedText
                    _translateSource.value = TranslateSource.HISTORY
                    historyDao.touch(cached.id)
                    registerTranslationAction()
                    return@launch
                }

                if (!isPremium.value) {
                    ensureFreePlanAllowsLanguagePair(src, tgt)
                    ensureFreeWordLimit(text, src, tgt)
                }

                val packsReady = packRepo.areBothPacksReady(src.mlKitCode, tgt.mlKitCode)
                val isOnline = ConnectivityHelper.isOnline(getApplication())

                when {
                    packsReady -> {
                        val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value = result
                        _translateSource.value = TranslateSource.OFFLINE
                        saveHistoryIfNeeded(text, result, src, tgt)
                        markWordsUsedIfNeeded(text, src, tgt)
                        registerTranslationAction()
                    }
                    isOnline -> {
                        val result = downloader.translateOnline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value = result
                        _translateSource.value = TranslateSource.ONLINE
                        saveHistoryIfNeeded(text, result, src, tgt)
                        markWordsUsedIfNeeded(text, src, tgt)
                        registerTranslationAction()
                    }
                    else -> {
                        _translateError.value = buildOfflineError(src, tgt)
                    }
                }
            } catch (e: Exception) {
                _translateError.value = e.message ?: "Translation failed."
            } finally {
                _translateLoading.value = false
            }
        }
    }

    fun translateFromPrimaryAction() {
        translate()
    }

    fun startRecording() {
        if (!isPremium.value) {
            viewModelScope.launch {
                _uiMessage.emit("Speech input is available in Premium.")
            }
            return
        }
        val currentSource = _translateSourceLang.value
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (currentSource != AUTO_DETECT_OPTION) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentSource.code)
            } else {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            }
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        _isRecording.value = false
    }

    fun clearTranslation() {
        _translateInput.value = ""
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
        stopSpeaking()
    }

    fun downloadPack(mlKitCode: String, requireWifi: Boolean = true) {
        val job = viewModelScope.launch {
            if (_downloadingPacks.value.contains(mlKitCode)) {
                return@launch
            }

            val app = getApplication<Application>()
            if (!ConnectivityHelper.isOnline(app)) {
                _uiMessage.emit("No internet connection. Connect and try again.")
                return@launch
            }

            if (requireWifi && !ConnectivityHelper.isWifiConnected(app)) {
                _uiMessage.emit("Wi-Fi is required to download this language pack.")
                return@launch
            }

            if (!canDownloadPack(mlKitCode)) {
                _uiMessage.emit("Free plan limit reached. Delete one downloaded language before downloading another.")
                return@launch
            }

            _downloadingPacks.value += mlKitCode
            _packDownloadProgress.value = _packDownloadProgress.value + (mlKitCode to 1)
            startPackProgressSimulation(mlKitCode)
            try {
                packRepo.downloadPack(mlKitCode, requireWifi) { progress ->
                    updatePackProgress(mlKitCode, progress)
                }
                stopPackProgressSimulation(mlKitCode)
                updatePackProgress(mlKitCode, 100)
                val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
                _uiMessage.emit("${lang?.displayName ?: mlKitCode} ready!")
                reconcileFreeLanguagesWithCurrentState()
            } catch (e: Exception) {
                stopPackProgressSimulation(mlKitCode)
                val message = e.message?.takeIf { it.isNotBlank() } ?: "Please check your connection and try again."
                _uiMessage.emit("Download failed: $message")
            } finally {
                _downloadingPacks.value = _downloadingPacks.value - mlKitCode
                _packDownloadProgress.value = _packDownloadProgress.value - mlKitCode
                packDownloadJobs.remove(mlKitCode)
            }
        }
        packDownloadJobs[mlKitCode] = job
    }

    fun cancelPackDownload(mlKitCode: String) {
        packDownloadJobs.remove(mlKitCode)?.cancel()
        stopPackProgressSimulation(mlKitCode)
        _downloadingPacks.value = _downloadingPacks.value - mlKitCode
        _packDownloadProgress.value = _packDownloadProgress.value - mlKitCode
        viewModelScope.launch {
            packRepo.markPackNotDownloaded(mlKitCode)
            _uiMessage.emit("Download cancelled.")
        }
    }

    fun deletePack(mlKitCode: String) {
        viewModelScope.launch {
            packRepo.deletePack(mlKitCode)
            val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
            _uiMessage.emit("${lang?.displayName ?: mlKitCode} removed.")
            reconcileFreeLanguagesWithCurrentState()
        }
    }

    fun refreshPackStatus() {
        viewModelScope.launch {
            packRepo.reconcileWithMlKit()
            reconcileFreeLanguagesWithCurrentState()
        }
    }

    private fun startPackProgressSimulation(mlKitCode: String) {
        stopPackProgressSimulation(mlKitCode)
        packProgressJobs[mlKitCode] = viewModelScope.launch {
            while (isActive && _downloadingPacks.value.contains(mlKitCode)) {
                delay(350)
                val current = _packDownloadProgress.value[mlKitCode] ?: break
                if (current >= 92) continue
                val next = when {
                    current < 20 -> current + 6
                    current < 45 -> current + 4
                    current < 70 -> current + 3
                    else -> current + 1
                }.coerceAtMost(92)
                updatePackProgress(mlKitCode, next)
            }
        }
    }

    private fun stopPackProgressSimulation(mlKitCode: String) {
        packProgressJobs.remove(mlKitCode)?.cancel()
    }

    private fun updatePackProgress(mlKitCode: String, progress: Int) {
        val current = _packDownloadProgress.value[mlKitCode] ?: 0
        if (progress > current) {
            _packDownloadProgress.value = _packDownloadProgress.value + (mlKitCode to progress.coerceIn(0, 100))
        }
    }

    fun onTtsInputChange(text: String) {}

    fun speakText(text: String, langCode: String) {
        if (!_ttsReady.value || text.isBlank()) return
        val result = tts?.setLanguage(Locale.forLanguageTag(langCode))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setOcrBitmap(bitmap: Bitmap) {
        recognizeText(bitmap)
    }

    private fun recognizeText(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrLoading.value = true
            try {
                val extracted = runOcr(bitmap)
                _ocrText.value = extracted
                if (extracted.isNotBlank()) {
                    translateOcrText(extracted)
                }
            } finally {
                _ocrLoading.value = false
            }
        }
    }

    private suspend fun translateOcrText(text: String) {
        val target = _translateTargetLang.value
        try {
            val result = downloader.translateOnline(text, TranslateLanguage.ENGLISH, target.mlKitCode)
            _ocrTranslatedText.value = result
        } catch (e: Exception) {
            Log.e(TAG, "OCR Translation failed", e)
        }
    }

    private suspend fun runOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.text)
                recognizer.close()
            }
            .addOnFailureListener { e ->
                cont.cancel(e)
                recognizer.close()
            }
    }

    fun clearOcr() {
        _ocrText.value = ""
        _ocrTranslatedText.value = ""
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun setHistorySearchQuery(q: String) {
        _historySearchQuery.value = q
    }

    fun toggleFavorite(entry: TranslationHistoryEntity) {
        viewModelScope.launch { historyDao.setFavorite(entry.id, !entry.isFavorite) }
    }

    fun deleteHistoryEntry(entry: TranslationHistoryEntity) {
        viewModelScope.launch { historyDao.delete(entry) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearNonFavorites() }
    }

    fun restoreFromHistory(entry: TranslationHistoryEntity) {
        setTranslateSourceLang(SUPPORTED_LANGUAGES.find { it.code == entry.sourceLangCode } ?: _freePlanLanguages.value.first())
        setTranslateTargetLang(SUPPORTED_LANGUAGES.find { it.code == entry.targetLangCode } ?: _freePlanLanguages.value.last())
        _translateInput.value = entry.sourceText
        _translatedText.value = entry.translatedText
        _translateSource.value = TranslateSource.HISTORY
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        speechRecognizer?.destroy()
        billingRepo.endConnection()
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _ttsReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    private fun initSpeechRecognizer(application: Application) {
        if (!SpeechRecognizer.isRecognitionAvailable(application)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isRecording.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isRecording.value = false
                }

                override fun onError(error: Int) {
                    _isRecording.value = false
                    Log.e(TAG, "Speech recognition error: $error")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handleSpeechResult(matches.first())
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleSpeechResult(text: String) {
        _translateInput.value = text
        if (_translateSourceLang.value != AUTO_DETECT_OPTION) {
            _detectedLanguage.value = null
        }
        translate()
    }

    private suspend fun identifyLanguageSuspend(text: String): String = suspendCancellableCoroutine { cont ->
        val options = LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.15f)
            .build()
        val languageIdentifier = LanguageIdentification.getClient(options)
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener {
                cont.resume(it)
                languageIdentifier.close()
            }
            .addOnFailureListener {
                cont.resume("und")
                languageIdentifier.close()
            }
    }

    private suspend fun identifyPossibleLanguagesSuspend(text: String): List<IdentifiedLanguage> =
        suspendCancellableCoroutine { cont ->
            val options = LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.01f)
                .build()
            val languageIdentifier = LanguageIdentification.getClient(options)
            languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener {
                    cont.resume(it)
                    languageIdentifier.close()
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                    languageIdentifier.close()
                }
        }

    private suspend fun resolveSourceLanguage(text: String): LanguageOption {
        return if (_translateSourceLang.value == AUTO_DETECT_OPTION) {
            val detectedCode = identifyLanguageSuspend(text)
            var detected = if (detectedCode != "und") {
                SUPPORTED_LANGUAGES.find {
                    detectedCode == it.code || detectedCode.startsWith(it.code + "-")
                }
            } else {
                null
            }

            if (detected == null) {
                val possible = identifyPossibleLanguagesSuspend(text)
                for (p in possible) {
                    val found = SUPPORTED_LANGUAGES.find {
                        p.languageTag == it.code || p.languageTag.startsWith(it.code + "-")
                    }
                    if (found != null) {
                        detected = found
                        break
                    }
                }
            }

            val safeDetected = detected ?: _freePlanLanguages.value.firstOrNull() ?: SUPPORTED_LANGUAGES.first()
            _detectedLanguage.value = safeDetected
            if (!isPremium.value && !isFreeLanguage(safeDetected)) {
                _freePlanLanguages.value.firstOrNull() ?: safeDetected
            } else {
                safeDetected
            }
        } else {
            sanitizeSourceLanguage(_translateSourceLang.value)
        }
    }

    private suspend fun canWarmUp(sourceMlKitCode: String, targetMlKitCode: String): Boolean {
        val srcReady = packDao.getByCode(sourceMlKitCode)?.status == PackStatus.DOWNLOADED
        val tgtReady = packDao.getByCode(targetMlKitCode)?.status == PackStatus.DOWNLOADED
        return isPremium.value || (srcReady && tgtReady)
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
                sourceLangName = src.displayName,
                targetLangCode = tgt.code,
                targetLangName = tgt.displayName
            )
        )
    }

    private fun markWordsUsedIfNeeded(text: String, src: LanguageOption, tgt: LanguageOption) {
        if (isPremium.value) return
        val key = translationUsageKey(text, src, tgt)
        val counted = prefs.getStringSet(PREF_COUNTED_TRANSLATION_KEYS, emptySet()).orEmpty().toMutableSet()
        if (counted.contains(key)) return

        counted.add(key)
        val newTotal = _freeWordsUsed.value + countWords(text)
        prefs.edit()
            .putInt(PREF_FREE_WORDS_USED, newTotal)
            .putStringSet(PREF_COUNTED_TRANSLATION_KEYS, counted)
            .apply()
        _freeWordsUsed.value = newTotal
    }

    private suspend fun ensureFreeWordLimit(text: String, src: LanguageOption, tgt: LanguageOption) {
        val key = translationUsageKey(text, src, tgt)
        val counted = prefs.getStringSet(PREF_COUNTED_TRANSLATION_KEYS, emptySet()).orEmpty()
        if (counted.contains(key)) return

        val prospectiveTotal = _freeWordsUsed.value + countWords(text)
        if (prospectiveTotal > FREE_WORD_LIMIT) {
            throw IllegalStateException("Free plan limit reached. Upgrade for unlimited translations.")
        }
    }

    private suspend fun ensureFreePlanAllowsLanguagePair(src: LanguageOption, tgt: LanguageOption) {
        val allowed = _freePlanLanguages.value.map { it.code }.toSet()
        if (src.code !in allowed || tgt.code !in allowed) {
            throw IllegalStateException("Free plan supports only your two selected languages.")
        }
    }

    private suspend fun registerTranslationAction() {
        val nextValue = _translateActions.value + 1
        prefs.edit { putInt(PREF_TRANSLATE_ACTIONS, nextValue) }
        _translateActions.value = nextValue
        if (!isPremium.value && nextValue % AD_FREQUENCY == 0) {
            _showInterstitialAd.emit(Unit)
        }
    }

    private fun buildOfflineError(src: LanguageOption, tgt: LanguageOption): String {
        return "You're offline and the ${src.displayName} to ${tgt.displayName} packs are not ready."
    }

    private fun countWords(text: String): Int {
        return Regex("\\S+").findAll(text.trim()).count()
    }

    private fun translationUsageKey(text: String, src: LanguageOption, tgt: LanguageOption): String {
        return "${src.code}|${tgt.code}|${text.trim().lowercase(Locale.getDefault())}"
    }

    private fun reconcileFreeLanguagesWithCurrentState() {
        if (isPremium.value) return

        val downloaded = allPacks.value
            .filter { it.status == PackStatus.DOWNLOADED }
            .mapNotNull { pack -> SUPPORTED_LANGUAGES.find { it.mlKitCode == pack.mlKitCode } }
            .distinctBy { it.code }

        val persisted = loadPersistedFreeLanguages()
        val chosen = when {
            downloaded.size >= FREE_LANGUAGE_LIMIT -> downloaded.take(FREE_LANGUAGE_LIMIT)
            persisted.isNotEmpty() -> persisted.take(FREE_LANGUAGE_LIMIT)
            else -> listOf(DEFAULT_FREE_LANGUAGE) + SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code }.take(FREE_LANGUAGE_LIMIT - 1)
        }

        persistFreeLanguages(chosen)
        if (chosen.isNotEmpty()) {
            if (!isFreeLanguage(_translateSourceLang.value)) {
                _translateSourceLang.value = chosen.first()
            }
            if (!isFreeLanguage(_translateTargetLang.value) || _translateTargetLang.value == _translateSourceLang.value) {
                _translateTargetLang.value = chosen.getOrElse(1) { chosen.first() }
            }
            prefs.edit {
                putString(PREF_SOURCE_LANG_CODE, _translateSourceLang.value.code)
                    .putString(PREF_TARGET_LANG_CODE, _translateTargetLang.value.code)
            }
        }
    }

    private fun persistFreeLanguages(languages: List<LanguageOption>) {
        val codes = languages.distinctBy { it.code }.take(FREE_LANGUAGE_LIMIT).map { it.code }
            .toSet()
        prefs.edit { putStringSet(PREF_FREE_LANGUAGE_CODES, codes)}
        _freePlanLanguages.value = codes.mapNotNull { code ->
            SUPPORTED_LANGUAGES.find { it.code == code }
        }.ifEmpty {
            listOf(DEFAULT_FREE_LANGUAGE) + SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code }.take(FREE_LANGUAGE_LIMIT - 1)
        }
    }

    private fun loadPersistedFreeLanguages(): List<LanguageOption> {
        val codes = prefs.getStringSet(PREF_FREE_LANGUAGE_CODES, emptySet()).orEmpty().toList()
        return codes.mapNotNull { code -> SUPPORTED_LANGUAGES.find { it.code == code } }
    }

    private fun loadInitialFreeLanguages(): List<LanguageOption> {
        val persisted = loadPersistedFreeLanguages()
        if (persisted.isNotEmpty()) return persisted
        val source = prefs.getString(PREF_SOURCE_LANG_CODE, SUPPORTED_LANGUAGES.first().code)
        val target = prefs.getString(PREF_TARGET_LANG_CODE, SUPPORTED_LANGUAGES.getOrNull(1)?.code ?: SUPPORTED_LANGUAGES.first().code)
        return listOfNotNull(
            DEFAULT_FREE_LANGUAGE,
            SUPPORTED_LANGUAGES.find { it.code == target }
        ).distinctBy { it.code }
            .plus(SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code && it.code != target })
            .take(FREE_LANGUAGE_LIMIT)
            .ifEmpty {
                listOf(DEFAULT_FREE_LANGUAGE) + SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code }.take(FREE_LANGUAGE_LIMIT - 1)
            }
    }

    private fun loadInitialSourceLanguage(): LanguageOption {
        val persisted = prefs.getString(PREF_SOURCE_LANG_CODE, null)
        val preferred = SUPPORTED_LANGUAGES.find { it.code == persisted }
        return if (!isPremium.value && preferred != null && !loadInitialFreeLanguages().any { it.code == preferred.code }) {
            DEFAULT_FREE_LANGUAGE
        } else {
            preferred ?: DEFAULT_FREE_LANGUAGE
        }
    }

    private fun loadInitialTargetLanguage(): LanguageOption {
        val persisted = prefs.getString(PREF_TARGET_LANG_CODE, null)
        val preferred = SUPPORTED_LANGUAGES.find { it.code == persisted }
        return if (!isPremium.value && preferred != null && !loadInitialFreeLanguages().any { it.code == preferred.code }) {
            loadInitialFreeLanguages().getOrElse(1) { DEFAULT_FREE_LANGUAGE }
        } else {
            preferred ?: loadInitialFreeLanguages().getOrElse(1) { DEFAULT_FREE_LANGUAGE }
        }
    }

    private fun sanitizeSourceLanguage(lang: LanguageOption): LanguageOption {
        return if (isPremium.value) {
            lang
        } else {
            _freePlanLanguages.value.find { it.code == lang.code } ?: DEFAULT_FREE_LANGUAGE
        }
    }

    private fun sanitizeTargetLanguage(lang: LanguageOption): LanguageOption {
        return if (isPremium.value) {
            lang
        } else {
            _freePlanLanguages.value.find { it.code == lang.code }
                ?: _freePlanLanguages.value.firstOrNull { it.code != _translateSourceLang.value.code }
                ?: DEFAULT_FREE_LANGUAGE
        }
    }
}
