package com.example.translateapp.viewmodel

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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.translateapp.data.AppDatabase
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.data.TranslationHistoryEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

// ── Language definitions ──────────────────────────────────────────────────────

data class LanguageOption(val code: String, val displayName: String, val mlKitCode: String)

val AUTO_DETECT_OPTION = LanguageOption("auto", "Auto Detect", "auto")

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English",    TranslateLanguage.ENGLISH),
    LanguageOption("es", "Spanish",    TranslateLanguage.SPANISH),
    LanguageOption("fr", "French",     TranslateLanguage.FRENCH),
    LanguageOption("de", "German",     TranslateLanguage.GERMAN),
    LanguageOption("it", "Italian",    TranslateLanguage.ITALIAN),
    LanguageOption("pt", "Portuguese", TranslateLanguage.PORTUGUESE),
    LanguageOption("zh", "Chinese",    TranslateLanguage.CHINESE),
    LanguageOption("ja", "Japanese",   TranslateLanguage.JAPANESE),
    LanguageOption("ko", "Korean",     TranslateLanguage.KOREAN),
    LanguageOption("ar", "Arabic",     TranslateLanguage.ARABIC),
    LanguageOption("ru", "Russian",    TranslateLanguage.RUSSIAN),
    LanguageOption("hi", "Hindi",      TranslateLanguage.HINDI),
    LanguageOption("ta", "Tamil",      TranslateLanguage.TAMIL),
    LanguageOption("te", "Telugu",     TranslateLanguage.TELUGU),
    LanguageOption("bn", "Bengali",    TranslateLanguage.BENGALI),
    LanguageOption("mr", "Marathi",    TranslateLanguage.MARATHI),
    LanguageOption("kn", "Kannada",    TranslateLanguage.KANNADA),
    LanguageOption("vi", "Vietnamese", TranslateLanguage.VIETNAMESE),
)

enum class HistoryFilter { ALL, FAVORITES }

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // ── Infrastructure ────────────────────────────────────────────────────────
    private val db         = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()
    private val packDao    = db.languagePackDao()
    private val downloader = ModelDownloadManager()
    val packRepo           = LanguagePackRepository(packDao, downloader, viewModelScope)

    // ── First Run Logic ───────────────────────────────────────────────────────
    private val _isFirstRun = MutableStateFlow(prefs.getBoolean("is_first_run", true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    fun completeFirstRun(source: LanguageOption, target: LanguageOption) {
        prefs.edit().apply {
            putBoolean("is_first_run", false)
            putString("source_lang_code", source.code)
            putString("target_lang_code", target.code)
            apply()
        }
        _isFirstRun.value = false
        _translateSourceLang.value = source
        _translateTargetLang.value = target

        viewModelScope.launch {
            downloader.warmUp(source.mlKitCode, target.mlKitCode)
        }
    }

    // ── Plan Management ───────────────────────────────────────────────────────
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun togglePremium() {
        _isPremium.value = !_isPremium.value
    }

    // ── One-shot UI messages (Snackbar) ───────────────────────────────────────
    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val uiMessage = _uiMessage.asSharedFlow()

    // ── History flows ─────────────────────────────────────────────────────────
    val allHistory: StateFlow<List<TranslationHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<TranslationHistoryEntity>> = historyDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // ── TTS ───────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private val _ttsReady   = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // ── Translate feature ─────────────────────────────────────────────────────
    private val _translateInput     = MutableStateFlow("")
    val translateInput: StateFlow<String> = _translateInput.asStateFlow()

    private val _translatedText     = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _translateSourceLang = MutableStateFlow(
        SUPPORTED_LANGUAGES.find { it.code == prefs.getString("source_lang_code", "en") } ?: SUPPORTED_LANGUAGES[0]
    )
    val translateSourceLang: StateFlow<LanguageOption> = _translateSourceLang.asStateFlow()

    private val _translateTargetLang = MutableStateFlow(
        SUPPORTED_LANGUAGES.find { it.code == prefs.getString("target_lang_code", "es") } ?: SUPPORTED_LANGUAGES[1]
    )
    val translateTargetLang: StateFlow<LanguageOption> = _translateTargetLang.asStateFlow()

    private val _translateLoading   = MutableStateFlow(false)
    val translateLoading: StateFlow<Boolean> = _translateLoading.asStateFlow()

    private val _translateError     = MutableStateFlow<String?>(null)
    val translateError: StateFlow<String?> = _translateError.asStateFlow()

    enum class TranslateSource { NONE, OFFLINE, ONLINE, HISTORY }

    private val _translateSource = MutableStateFlow(TranslateSource.NONE)
    val translateSource: StateFlow<TranslateSource> = _translateSource.asStateFlow()

    // ── TTS feature ───────────────────────────────────────────────────────────
    private val _ttsInput    = MutableStateFlow("")
    val ttsInput: StateFlow<String> = _ttsInput.asStateFlow()

    private val _ttsLanguage = MutableStateFlow(SUPPORTED_LANGUAGES[0])
    val ttsLanguage: StateFlow<LanguageOption> = _ttsLanguage.asStateFlow()

    // ── Speech to Speech feature ──────────────────────────────────────────────
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<LanguageOption?>(null)
    val detectedLanguage: StateFlow<LanguageOption?> = _detectedLanguage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    // ── OCR feature ───────────────────────────────────────────────────────────
    private val _isLiveCameraMode   = MutableStateFlow(false)
    val isLiveCameraMode: StateFlow<Boolean> = _isLiveCameraMode.asStateFlow()

    fun setLiveCameraMode(enabled: Boolean) {
        _isLiveCameraMode.value = enabled
    }

    private val _ocrText            = MutableStateFlow("")
    val ocrText: StateFlow<String>  = _ocrText.asStateFlow()

    private val _ocrTranslatedText  = MutableStateFlow("")
    val ocrTranslatedText: StateFlow<String> = _ocrTranslatedText.asStateFlow()

    private val _ocrLoading         = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    private val _ocrError           = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    private val _ocrTtsLanguage     = MutableStateFlow(SUPPORTED_LANGUAGES[0])
    val ocrTtsLanguage: StateFlow<LanguageOption> = _ocrTtsLanguage.asStateFlow()

    // ── Language pack management ──────────────────────────────────────────────
    val allPacks: StateFlow<List<LanguagePackEntity>> = packRepo.allPacks

    private val _downloadingPacks = MutableStateFlow<Set<String>>(emptySet())
    val downloadingPacks: StateFlow<Set<String>> = _downloadingPacks.asStateFlow()

    fun observePackStatus(mlKitCode: String): Flow<LanguagePackEntity?> =
        packRepo.observePack(mlKitCode)

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        packRepo.initAsync()
        //initTts(application)
       // initSpeechRecognizer(application)
        
        // Warm up only if already downloaded or if premium
        /*viewModelScope.launch {
            val enReady = packDao.getByCode(TranslateLanguage.ENGLISH)?.status == PackStatus.DOWNLOADED
            val esReady = packDao.getByCode(SUPPORTED_LANGUAGES[1].mlKitCode)?.status == PackStatus.DOWNLOADED
            if (_isPremium.value || (enReady && esReady)) {
                downloader.warmUp(TranslateLanguage.ENGLISH, SUPPORTED_LANGUAGES[1].mlKitCode)
            }
        }*/
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _ttsReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { _isSpeaking.value = true }
                    override fun onDone(id: String?)  { _isSpeaking.value = false }
                    override fun onError(id: String?) { _isSpeaking.value = false }
                })
            }
        }
    }

    private fun initSpeechRecognizer(application: Application) {
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { _isRecording.value = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { _isRecording.value = false }
                    override fun onError(error: Int) {
                        _isRecording.value = false
                        Log.e("AppViewModel", "Speech recognition error: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            onTtsInputChange(text)
                            handleSpeechResult(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) { }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    // ── Speech actions ────────────────────────────────────────────────────────

    fun startRecording() {
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

    private suspend fun identifyPossibleLanguagesSuspend(text: String): List<IdentifiedLanguage> = suspendCancellableCoroutine { cont ->
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

    // ── Translate actions ─────────────────────────────────────────────────────

    fun onTranslateInputChange(text: String) {
        _translateInput.value = text
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        _translateSourceLang.value = lang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        _detectedLanguage.value = null
        
        prefs.edit().putString("source_lang_code", lang.code).apply()

        if (lang != AUTO_DETECT_OPTION) {
            viewModelScope.launch {
                val srcReady = packDao.getByCode(lang.mlKitCode)?.status == PackStatus.DOWNLOADED
                val tgtReady = packDao.getByCode(_translateTargetLang.value.mlKitCode)?.status == PackStatus.DOWNLOADED
                if (_isPremium.value || (srcReady && tgtReady)) {
                    downloader.warmUp(lang.mlKitCode, _translateTargetLang.value.mlKitCode)
                }
            }
        }
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        _translateTargetLang.value = lang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
        
        prefs.edit().putString("target_lang_code", lang.code).apply()

        viewModelScope.launch {
            val src = _translateSourceLang.value
            val srcCode = if (src == AUTO_DETECT_OPTION) TranslateLanguage.ENGLISH else src.mlKitCode
            val srcReady = packDao.getByCode(srcCode)?.status == PackStatus.DOWNLOADED
            val tgtReady = packDao.getByCode(lang.mlKitCode)?.status == PackStatus.DOWNLOADED
            
            if (_isPremium.value || (srcReady && tgtReady)) {
                downloader.warmUp(srcCode, lang.mlKitCode)
            }
        }
    }

    fun swapTranslateLanguages() {
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value
        if (src == AUTO_DETECT_OPTION) return // Cannot swap if source is auto-detect
        
        setTranslateSourceLang(tgt)
        setTranslateTargetLang(src)

        val input  = _translateInput.value
        val output = _translatedText.value
        if (output.isNotBlank()) {
            _translateInput.value  = output
            _translatedText.value  = input
        }
    }

    private var translateJob: Job? = null
    fun translate() {
        val text = _translateInput.value.trim()
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            _translateLoading.value = true
            _translateError.value   = null
            _translateSource.value  = TranslateSource.NONE

            try {
                val src = if (_translateSourceLang.value == AUTO_DETECT_OPTION) {
                    val detectedCode = identifyLanguageSuspend(text)
                    var detected = if (detectedCode != "und") {
                        SUPPORTED_LANGUAGES.find { 
                            detectedCode == it.code || detectedCode.startsWith(it.code + "-") 
                        }
                    } else null
                    
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

                    _detectedLanguage.value = detected
                    detected ?: SUPPORTED_LANGUAGES[0]
                } else {
                    _translateSourceLang.value
                }

                val tgt = _translateTargetLang.value

                val packsReady = packRepo.areBothPacksReady(src.mlKitCode, tgt.mlKitCode)
                val isOnline   = ConnectivityHelper.isOnline(getApplication())

                when {
                    packsReady -> {
                        val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value  = result
                        _translateSource.value = TranslateSource.OFFLINE
                        saveHistory(text, result, src, tgt)
                    }
                    isOnline -> {
                        // Plan limit check: if premium, no limit.
                        // If free, must have room for BOTH models if they aren't already downloaded.
                        if (!_isPremium.value) {
                            val downloaded = allPacks.value.filter { it.status == PackStatus.DOWNLOADED }.map { it.mlKitCode }.toSet()
                            val needed = mutableListOf<String>()
                            if (!downloaded.contains(src.mlKitCode)) needed.add(src.mlKitCode)
                            if (!downloaded.contains(tgt.mlKitCode)) needed.add(tgt.mlKitCode)
                            
                            if (downloaded.size + needed.size > 2) {
                                _translateError.value = "Free plan limit: Max 2 offline models. Delete a pack to use ${src.displayName} or ${tgt.displayName}."
                                return@launch
                            }
                        }

                        val result = downloader.translateOnline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value  = result
                        _translateSource.value = TranslateSource.ONLINE
                        saveHistory(text, result, src, tgt)
                    }
                    else -> {
                        val cached = historyDao.findInHistory(text, src.code, tgt.code)
                        if (cached != null) {
                            _translatedText.value  = cached.translatedText
                            _translateSource.value = TranslateSource.HISTORY
                        } else {
                            _translateError.value = buildOfflineError(src, tgt)
                        }
                    }
                }
            } catch (e: Exception) {
                _translateError.value = "Translation failed: ${e.message}"
            } finally {
                _translateLoading.value = false
            }
        }
    }

    private fun buildOfflineError(src: LanguageOption, tgt: LanguageOption): String {
        return "You're offline and \"${src.displayName} → ${tgt.displayName}\" pack isn't downloaded."
    }

    private suspend fun saveHistory(text: String, result: String, src: LanguageOption, tgt: LanguageOption) {
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

    // ── Language pack actions ─────────────────────────────────────────────────

    fun downloadPack(mlKitCode: String, requireWifi: Boolean = true) {
        viewModelScope.launch {
            val downloadedCount = allPacks.value.count { it.status == PackStatus.DOWNLOADED }
            if (!_isPremium.value && downloadedCount >= 2) {
                _uiMessage.emit("Free plan limit: Max 2 offline models.")
                return@launch
            }

            _downloadingPacks.value = _downloadingPacks.value + mlKitCode
            try {
                packRepo.downloadPack(mlKitCode, requireWifi)
                val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
                _uiMessage.emit("${lang?.displayName ?: mlKitCode} ready!")
            } catch (e: Exception) {
                _uiMessage.emit("Download failed: ${e.message}")
            } finally {
                _downloadingPacks.value = _downloadingPacks.value - mlKitCode
            }
        }
    }

    fun deletePack(mlKitCode: String) {
        viewModelScope.launch {
            packRepo.deletePack(mlKitCode)
            val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
            _uiMessage.emit("${lang?.displayName ?: mlKitCode} removed.")
        }
    }

    fun refreshPackStatus() {
        viewModelScope.launch { packRepo.reconcileWithMlKit() }
    }

    // ── TTS actions ───────────────────────────────────────────────────────────

    fun onTtsInputChange(text: String) { _ttsInput.value = text }
    fun setTtsLanguage(lang: LanguageOption) { _ttsLanguage.value = lang }

    fun speakText(text: String, langCode: String) {
        if (!_ttsReady.value || text.isBlank()) return
        val result = tts?.setLanguage(Locale.forLanguageTag(langCode))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() { tts?.stop(); _isSpeaking.value = false }

    // ── OCR actions ───────────────────────────────────────────────────────────

    fun setOcrBitmap(bitmap: Bitmap) {
        recognizeText(bitmap)
    }

    fun setOcrTtsLanguage(lang: LanguageOption) { _ocrTtsLanguage.value = lang }

    private fun recognizeText(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrLoading.value = true
            try {
                val extracted = runOcr(bitmap)
                _ocrText.value = extracted
                
                // Automatically translate the extracted text
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
            // Using auto-detect logic or assuming English as base
            val result = downloader.translateOnline(text, TranslateLanguage.ENGLISH, target.mlKitCode)
            _ocrTranslatedText.value = result
        } catch (e: Exception) {
            Log.e("AppViewModel", "OCR Translation failed", e)
        }
    }

    private suspend fun runOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result -> cont.resume(result.text); recognizer.close() }
            .addOnFailureListener { e -> cont.cancel(e); recognizer.close() }
    }

    fun clearOcr() { 
        _ocrText.value = ""
        _ocrTranslatedText.value = ""
    }

    // ── History actions ───────────────────────────────────────────────────────

    fun setHistoryFilter(filter: HistoryFilter) { _historyFilter.value = filter }
    fun setHistorySearchQuery(q: String) { _historySearchQuery.value = q }

    fun toggleFavorite(entry: TranslationHistoryEntity) {
        viewModelScope.launch { historyDao.setFavorite(entry.id, !entry.isFavorite) }
    }

    fun deleteHistoryEntry(entry: TranslationHistoryEntity) {
        viewModelScope.launch { historyDao.delete(entry) }
    }

    fun clearHistory() { viewModelScope.launch { historyDao.clearNonFavorites() } }

    fun restoreFromHistory(entry: TranslationHistoryEntity) {
        _translateInput.value      = entry.sourceText
        _translatedText.value      = entry.translatedText
        _translateSourceLang.value = SUPPORTED_LANGUAGES.find { it.code == entry.sourceLangCode } ?: SUPPORTED_LANGUAGES[0]
        _translateTargetLang.value = SUPPORTED_LANGUAGES.find { it.code == entry.targetLangCode } ?: SUPPORTED_LANGUAGES[1]
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
