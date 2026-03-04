package com.example.translateapp.viewmodel

import android.app.Application
import android.graphics.Bitmap
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
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.translateapp.service.LanguagePackRepository
import com.example.translateapp.service.ModelDownloadManager
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

    // ── Infrastructure ────────────────────────────────────────────────────────
    private val db         = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()
    private val packDao    = db.languagePackDao()
    private val downloader = ModelDownloadManager()
    val packRepo           = LanguagePackRepository(packDao, downloader, viewModelScope)

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

    private val _translateSourceLang = MutableStateFlow(SUPPORTED_LANGUAGES[0])
    val translateSourceLang: StateFlow<LanguageOption> = _translateSourceLang.asStateFlow()

    private val _translateTargetLang = MutableStateFlow(SUPPORTED_LANGUAGES[1])
    val translateTargetLang: StateFlow<LanguageOption> = _translateTargetLang.asStateFlow()

    private val _translateLoading   = MutableStateFlow(false)
    val translateLoading: StateFlow<Boolean> = _translateLoading.asStateFlow()

    private val _translateError     = MutableStateFlow<String?>(null)
    val translateError: StateFlow<String?> = _translateError.asStateFlow()

    /**
     * How the last translation result was produced — drives UI badges.
     *   NONE        = no result yet
     *   OFFLINE     = used on-device model (no network needed)
     *   ONLINE      = used network, no pack downloaded
     *   HISTORY     = served from local history cache (device was offline)
     */
    enum class TranslateSource { NONE, OFFLINE, ONLINE, HISTORY }

    private val _translateSource = MutableStateFlow(TranslateSource.NONE)
    val translateSource: StateFlow<TranslateSource> = _translateSource.asStateFlow()

    // ── TTS feature ───────────────────────────────────────────────────────────
    private val _ttsInput    = MutableStateFlow("")
    val ttsInput: StateFlow<String> = _ttsInput.asStateFlow()

    private val _ttsLanguage = MutableStateFlow(SUPPORTED_LANGUAGES[0])
    val ttsLanguage: StateFlow<LanguageOption> = _ttsLanguage.asStateFlow()

    // ── OCR feature ───────────────────────────────────────────────────────────
    private val _ocrText            = MutableStateFlow("")
    val ocrText: StateFlow<String>  = _ocrText.asStateFlow()

    private val _ocrLoading         = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    private val _ocrError           = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    private val _ocrSelectedBitmap  = MutableStateFlow<Bitmap?>(null)
    val ocrSelectedBitmap: StateFlow<Bitmap?> = _ocrSelectedBitmap.asStateFlow()

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
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("AppViewModel", "TTS initialized successfully")
                _ttsReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { 
                        Log.d("AppViewModel", "TTS started: $id")
                        _isSpeaking.value = true 
                    }
                    override fun onDone(id: String?)  { 
                        Log.d("AppViewModel", "TTS done: $id")
                        _isSpeaking.value = false 
                    }
                    override fun onError(id: String?) { 
                        Log.e("AppViewModel", "TTS error: $id")
                        _isSpeaking.value = false 
                    }
                })
            } else {
                Log.e("AppViewModel", "TTS initialization failed with status: $status")
            }
        }
    }

    // ── Translate actions ─────────────────────────────────────────────────────

    fun onTranslateInputChange(text: String) {
        _translateInput.value = text
        _translatedText.value = ""
        _translateError.value = null
        _translateSource.value = TranslateSource.NONE
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        _translateSourceLang.value = lang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        _translateTargetLang.value = lang
        _translatedText.value = ""
        _translateSource.value = TranslateSource.NONE
    }

    fun swapTranslateLanguages() {
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value
        _translateSourceLang.value = tgt
        _translateTargetLang.value = src
        val input  = _translateInput.value
        val output = _translatedText.value
        if (output.isNotBlank()) {
            _translateInput.value  = output
            _translatedText.value  = input
        }
    }

    fun translate() {
        val text = _translateInput.value.trim()
        if (text.isBlank()) return
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value

        viewModelScope.launch {
            _translateLoading.value = true
            _translateError.value   = null
            _translateSource.value  = TranslateSource.NONE

            try {
                val packsReady = packRepo.areBothPacksReady(src.mlKitCode, tgt.mlKitCode)
                val isOnline   = ConnectivityHelper.isOnline(getApplication())

                when {
                    // ── 1. Both packs downloaded → pure offline ───────────────
                    packsReady -> {
                        val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value  = result
                        _translateSource.value = TranslateSource.OFFLINE
                        saveHistory(text, result, src, tgt)
                    }

                    // ── 2. Packs missing but online → lightweight cloud translate
                    isOnline -> {
                        val result = downloader.translateOnline(text, src.mlKitCode, tgt.mlKitCode)
                        _translatedText.value  = result
                        _translateSource.value = TranslateSource.ONLINE
                        saveHistory(text, result, src, tgt)
                    }

                    // ── 3. Offline + no pack → check history cache ────────────
                    else -> {
                        val cached = historyDao.findInHistory(text, src.code, tgt.code)
                        if (cached != null) {
                            _translatedText.value  = cached.translatedText
                            _translateSource.value = TranslateSource.HISTORY
                        } else {
                            // Nothing we can do — tell the user clearly
                            _translateError.value = buildOfflineError(src, tgt)
                        }
                    }
                }
            } catch (e: Exception) {
                // If online translate threw a network error, still try history
                val isNetworkError = isNetworkException(e)
                if (isNetworkError) {
                    val src2 = _translateSourceLang.value
                    val tgt2 = _translateTargetLang.value
                    val text2 = _translateInput.value.trim()
                    val cached = historyDao.findInHistory(text2, src2.code, tgt2.code)
                    if (cached != null) {
                        _translatedText.value  = cached.translatedText
                        _translateSource.value = TranslateSource.HISTORY
                    } else {
                        _translateError.value = buildOfflineError(src2, tgt2)
                    }
                } else {
                    _translateError.value = "Translation failed: ${e.message}"
                }
                Log.e("AppViewModel", "translate error", e)
            } finally {
                _translateLoading.value = false
            }
        }
    }

    private fun isNetworkException(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("no internet") ||
               msg.contains("network") ||
               msg.contains("unable to resolve") ||
               msg.contains("failed to connect") ||
               msg.contains("timeout") ||
               e is java.net.UnknownHostException ||
               e is java.net.SocketTimeoutException ||
               e is java.io.IOException
    }

    private fun buildOfflineError(src: LanguageOption, tgt: LanguageOption): String {
        return "You're offline and \"${src.displayName} → ${tgt.displayName}\" " +
               "language pack isn't downloaded.\n\n" +
               "• Connect to the internet to translate, or\n" +
               "• Download the language pack in the Packs tab to work fully offline."
    }

    private suspend fun saveHistory(
        sourceText: String,
        translatedText: String,
        src: LanguageOption,
        tgt: LanguageOption
    ) {
        historyDao.insert(
            TranslationHistoryEntity(
                sourceText     = sourceText,
                translatedText = translatedText,
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
            _downloadingPacks.value = _downloadingPacks.value + mlKitCode
            try {
                packRepo.downloadPack(mlKitCode, requireWifi)
                val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
                _uiMessage.emit("${lang?.displayName ?: mlKitCode} ready for offline use!")
            } catch (e: Exception) {
                _uiMessage.emit("Download failed: ${e.message}")
            } finally {
                _downloadingPacks.value = _downloadingPacks.value - mlKitCode
            }
        }
    }

    fun deletePack(mlKitCode: String) {
        viewModelScope.launch {
            try {
                packRepo.deletePack(mlKitCode)
                val lang = SUPPORTED_LANGUAGES.find { it.mlKitCode == mlKitCode }
                _uiMessage.emit("${lang?.displayName ?: mlKitCode} model removed.")
            } catch (e: Exception) {
                _uiMessage.emit("Delete failed: ${e.message}")
            }
        }
    }

    fun refreshPackStatus() {
        viewModelScope.launch { packRepo.reconcileWithMlKit() }
    }

    // ── TTS actions ───────────────────────────────────────────────────────────

    fun onTtsInputChange(text: String) { _ttsInput.value = text }
    fun setTtsLanguage(lang: LanguageOption) { _ttsLanguage.value = lang }

    fun speakText(text: String, langCode: String) {
        Log.d("AppViewModel", "speakText: text='$text', langCode='$langCode', ttsReady=${_ttsReady.value}")
        if (!_ttsReady.value || text.isBlank()) return
        
        val locale = Locale.forLanguageTag(langCode)
        Log.d("AppViewModel", "Setting TTS language to $locale")
        
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("AppViewModel", "Language $langCode not supported or missing data, falling back to English")
            tts?.language = Locale.ENGLISH
        }
        
        tts?.stop()
        val speechResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
        Log.d("AppViewModel", "tts.speak result: $speechResult")
    }

    fun stopSpeaking() { 
        Log.d("AppViewModel", "stopSpeaking")
        tts?.stop()
        _isSpeaking.value = false 
    }

    // ── OCR actions ───────────────────────────────────────────────────────────

    fun setOcrBitmap(bitmap: Bitmap) {
        _ocrSelectedBitmap.value = bitmap
        _ocrText.value  = ""
        _ocrError.value = null
        recognizeText(bitmap)
    }

    fun setOcrTtsLanguage(lang: LanguageOption) { _ocrTtsLanguage.value = lang }

    private fun recognizeText(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrLoading.value = true
            _ocrError.value   = null
            try {
                _ocrText.value = runOcr(bitmap)
            } catch (e: Exception) {
                _ocrError.value = "Text recognition failed: ${e.message}"
                Log.e("AppViewModel", "ocr error", e)
            } finally {
                _ocrLoading.value = false
            }
        }
    }

    private suspend fun runOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result -> cont.resume(result.text); recognizer.close() }
            .addOnFailureListener { e -> cont.cancel(e); recognizer.close() }
    }

    fun clearOcr() { _ocrSelectedBitmap.value = null; _ocrText.value = ""; _ocrError.value = null }

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
        tts?.stop()
        tts?.shutdown()
    }
}
