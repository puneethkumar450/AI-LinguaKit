package com.example.translateapp.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.translateapp.data.AppDatabase
import com.example.translateapp.data.LanguagePackEntity
import com.example.translateapp.data.PackStatus
import com.example.translateapp.data.TranslationHistoryEntity
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

    private val _wasOffline = MutableStateFlow(false)
    val wasOffline: StateFlow<Boolean> = _wasOffline.asStateFlow()

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
                _ttsReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { _isSpeaking.value = true }
                    override fun onDone(id: String?)  { _isSpeaking.value = false }
                    override fun onError(id: String?) { _isSpeaking.value = false }
                })
            }
        }
    }

    // ── Translate actions ─────────────────────────────────────────────────────

    fun onTranslateInputChange(text: String) {
        _translateInput.value = text
        _translatedText.value = ""
        _translateError.value = null
        _wasOffline.value = false
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        _translateSourceLang.value = lang
        _translatedText.value = ""
        _wasOffline.value = false
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        _translateTargetLang.value = lang
        _translatedText.value = ""
        _wasOffline.value = false
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

    private fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun translate() {
        val text = _translateInput.value.trim()
        if (text.isBlank()) return
        val src = _translateSourceLang.value
        val tgt = _translateTargetLang.value

        viewModelScope.launch {
            _translateLoading.value = true
            _translateError.value   = null
            _wasOffline.value       = false
            try {
                val bothReady = packRepo.areBothPacksReady(src.mlKitCode, tgt.mlKitCode)

                if (bothReady) {
                    val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                    _translatedText.value = result
                    _wasOffline.value     = true
                    saveHistory(text, result, src, tgt)
                } else {
                    // Not both ready. Check internet.
                    if (isOnline()) {
                        // Online mode - ML Kit's Translation.getClient() with downloaded models = false 
                        // can handle online translation if we allow it, but ML Kit Translate is 
                        // strictly on-device. If models aren't there, we MUST download.
                        // However, ML Kit's Translator.downloadModelIfNeeded() is the standard way.
                        // If user wants "Online Translate without full download", ML Kit doesn't 
                        // support a "thin" online API for Translation. It's offline-first.
                        
                        // IF we want to strictly follow "dont download whole pack for one word", 
                        // ML Kit might not be the right tool for that specific requirement 
                        // as it requires the model.
                        
                        // BUT, if we interpret "online translate" as "download if needed" 
                        // we can use downloader.translateOffline but let it download.
                        // To satisfy "dont download whole pack for one word", we'd need a different API.
                        
                        // Assuming we stay with ML Kit: 
                        // If offline: check history.
                        if (!isOnline()) {
                             val historyMatch = historyDao.findInHistory(text, src.code, tgt.code)
                             if (historyMatch != null) {
                                 _translatedText.value = historyMatch.translatedText
                                 _wasOffline.value = true
                                 _uiMessage.emit("Restored from history (Offline)")
                             } else {
                                 _translateError.value = "Cannot translate. Connect to internet or download the package to work offline."
                             }
                        } else {
                            // Online - we can download.
                            _uiMessage.emit("Downloading language models…")
                            downloadMissingPacks(src.mlKitCode, tgt.mlKitCode)
                            val result = downloader.translateOffline(text, src.mlKitCode, tgt.mlKitCode)
                            _translatedText.value = result
                            saveHistory(text, result, src, tgt)
                        }
                    } else {
                        // Offline and packs not ready.
                        val historyMatch = historyDao.findInHistory(text, src.code, tgt.code)
                        if (historyMatch != null) {
                            _translatedText.value = historyMatch.translatedText
                            _wasOffline.value = true
                            _uiMessage.emit("Restored from history (Offline)")
                        } else {
                            _translateError.value = "Cannot translate. Connect to internet or download the package to work offline."
                        }
                    }
                }
            } catch (e: Exception) {
                _translateError.value = buildErrorMessage(e)
                Log.e("AppViewModel", "translate error", e)
            } finally {
                _translateLoading.value = false
            }
        }
    }

    private suspend fun downloadMissingPacks(srcCode: String, tgtCode: String) {
        val srcPack = packDao.getByCode(srcCode)
        val tgtPack = packDao.getByCode(tgtCode)
        if (srcPack?.status != PackStatus.DOWNLOADED) packRepo.downloadPack(srcCode, requireWifi = false)
        if (tgtPack?.status != PackStatus.DOWNLOADED) packRepo.downloadPack(tgtCode, requireWifi = false)
    }

    private fun buildErrorMessage(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("no internet") || msg.contains("network") || msg.contains("unable to resolve") ->
                "No internet. Download language packs first in the Packs tab."
            msg.contains("model") || msg.contains("download") ->
                "Model unavailable. Go to Language Packs to download it."
            else -> "Translation failed: ${e.message}"
        }
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
        if (!_ttsReady.value || text.isBlank()) return
        val locale = Locale.forLanguageTag(langCode)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() { tts?.stop(); _isSpeaking.value = false }

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
