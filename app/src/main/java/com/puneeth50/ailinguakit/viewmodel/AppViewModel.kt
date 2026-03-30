package com.puneeth50.ailinguakit.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.LanguagePackDao
import com.puneeth50.ailinguakit.data.LanguagePackEntity
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.data.TranslationHistoryDao
import com.puneeth50.ailinguakit.data.TranslationHistoryEntity
import com.puneeth50.ailinguakit.service.AppLanguageOption
import com.puneeth50.ailinguakit.service.AppSettingsManager
import com.puneeth50.ailinguakit.service.AppThemeMode
import com.puneeth50.ailinguakit.service.BillingRepository
import com.puneeth50.ailinguakit.service.LanguagePackController
import com.puneeth50.ailinguakit.service.LanguagePackRepository
import com.puneeth50.ailinguakit.service.ModelDownloadManager
import com.puneeth50.ailinguakit.service.PremiumPlanOption
import com.puneeth50.ailinguakit.service.PremiumPlanType
import com.puneeth50.ailinguakit.service.RewardType
import com.puneeth50.ailinguakit.service.RewardsRepository
import com.puneeth50.ailinguakit.service.SpeechController
import com.puneeth50.ailinguakit.service.TranslationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class LanguageOption(
    val code: String,
    @StringRes val displayNameResId: Int,
    val mlKitCode: String
) {
    fun displayName(context: Context): String = context.getString(displayNameResId)
}

val AUTO_DETECT_OPTION = LanguageOption("auto", R.string.auto_detect, "auto")

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", R.string.lang_english, TranslateLanguage.ENGLISH),
    LanguageOption("es", R.string.lang_spanish, TranslateLanguage.SPANISH),
    LanguageOption("fr", R.string.lang_french, TranslateLanguage.FRENCH),
    LanguageOption("de", R.string.lang_german, TranslateLanguage.GERMAN),
    LanguageOption("it", R.string.lang_italian, TranslateLanguage.ITALIAN),
    LanguageOption("pt", R.string.lang_portuguese, TranslateLanguage.PORTUGUESE),
    LanguageOption("zh", R.string.lang_chinese, TranslateLanguage.CHINESE),
    LanguageOption("ja", R.string.lang_japanese, TranslateLanguage.JAPANESE),
    LanguageOption("ko", R.string.lang_korean, TranslateLanguage.KOREAN),
    LanguageOption("ar", R.string.lang_arabic, TranslateLanguage.ARABIC),
    LanguageOption("ru", R.string.lang_russian, TranslateLanguage.RUSSIAN),
    LanguageOption("hi", R.string.lang_hindi, TranslateLanguage.HINDI),
    //LanguageOption("ta", R.string.lang_tamil, TranslateLanguage.TAMIL),
    //LanguageOption("te", R.string.lang_telugu, TranslateLanguage.TELUGU),
    //LanguageOption("bn", R.string.lang_bengali, TranslateLanguage.BENGALI),
    //LanguageOption("mr", R.string.lang_marathi, TranslateLanguage.MARATHI),
    //LanguageOption("kn", R.string.lang_kannada, TranslateLanguage.KANNADA),
    LanguageOption("vi", R.string.lang_vietnamese, TranslateLanguage.VIETNAMESE),
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
private const val PREF_FREE_LANGUAGE_CODES_ORDERED = "free_language_codes_ordered"
private const val FREE_WORD_LIMIT = 3_000
private const val FREE_LANGUAGE_LIMIT = 3
private const val AD_FREQUENCY = 5
private val DEFAULT_FREE_LANGUAGE = SUPPORTED_LANGUAGES.first { it.code == "en" }

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val prefs: SharedPreferences,
    private val historyDao: TranslationHistoryDao,
    private val packDao: LanguagePackDao,
    private val downloader: ModelDownloadManager,
    private val billingRepo: BillingRepository,
    private val rewardsRepo: RewardsRepository,
    private val packRepoFactory: LanguagePackRepository.Factory
) : AndroidViewModel(application) {

    val packRepo = packRepoFactory.create(viewModelScope)

    private val _isFirstRun = MutableStateFlow(prefs.getBoolean(PREF_IS_FIRST_RUN, true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingRepo.isPremium
    val availablePremiumPlans: StateFlow<List<PremiumPlanOption>> = billingRepo.availablePremiumPlans
    val activePremiumPlanType: StateFlow<PremiumPlanType?> = billingRepo.activePremiumPlanType
    val billingReady = billingRepo.billingReady
    private val _themeMode = MutableStateFlow(AppSettingsManager.getSavedThemeMode(application))
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()
    private val _appLanguageTag = MutableStateFlow(AppSettingsManager.getSavedLanguageTag(application))
    val appLanguageTag: StateFlow<String> = _appLanguageTag.asStateFlow()
    val supportedAppLanguages: List<AppLanguageOption> = AppSettingsManager.supportedLanguages(application)

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessage = _uiMessage.asSharedFlow()

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val showInterstitialAd = _showInterstitialAd.asSharedFlow()
    val rewardRequests = rewardsRepo.rewardRequests
    val rewardWallet = rewardsRepo.wallet
    private val _showWordRewardPrompt = MutableStateFlow(false)
    val showWordRewardPrompt: StateFlow<Boolean> = _showWordRewardPrompt.asStateFlow()

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

    private var pendingRewardedTranslateRetry = false
    private var pendingRewardedPackDownload: Pair<String, Boolean>? = null

    private val speechController = SpeechController(application, ::handleSpeechResult)
    val ttsReady: StateFlow<Boolean> = speechController.ttsReady
    val isSpeaking: StateFlow<Boolean> = speechController.isSpeaking

    val isRecording: StateFlow<Boolean> = speechController.isRecording

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
    private val packController = LanguagePackController(
        application = application,
        packRepo = packRepo,
        allPacks = allPacks,
        isPremium = isPremium,
        scope = viewModelScope,
        freeLanguageLimit = FREE_LANGUAGE_LIMIT,
        extraSlotCredits = rewardWallet,
        onExtraSlotConsumed = rewardsRepo::consumeExtraSlotCredit,
        onMessage = { message -> _uiMessage.emit(message) },
        onPackStateChanged = ::reconcileFreeLanguagesWithCurrentState
    )
    val downloadingPacks: StateFlow<Set<String>> = packController.downloadingPacks
    val packDownloadProgress: StateFlow<Map<String, Int>> = packController.packDownloadProgress
    private val translationController = TranslationController(
        application = application,
        prefs = prefs,
        scope = viewModelScope,
        historyDao = historyDao,
        packDao = packDao,
        downloader = downloader,
        isPremium = isPremium,
        initialSource = loadInitialSourceLanguage(),
        initialTarget = loadInitialTargetLanguage(),
        sanitizeSource = ::sanitizeSourceLanguage,
        sanitizeTarget = ::sanitizeTargetLanguage,
        resolveSourceLanguage = ::resolveSourceLanguage,
        ensureFreeWordLimit = ::ensureFreeWordLimit,
        ensureLanguagePairAllowed = ::ensureFreePlanAllowsLanguagePair,
        markWordsUsedIfNeeded = ::markWordsUsedIfNeeded,
        registerTranslationAction = ::registerTranslationAction
    )
    val translateInput: StateFlow<String> = translationController.translateInput
    val translatedText: StateFlow<String> = translationController.translatedText
    val translateSourceLang: StateFlow<LanguageOption> = translationController.translateSourceLang
    val translateTargetLang: StateFlow<LanguageOption> = translationController.translateTargetLang
    val translateLoading: StateFlow<Boolean> = translationController.translateLoading
    val translateError: StateFlow<String?> = translationController.translateError
    val translateSource: StateFlow<TranslationController.TranslateSource> = translationController.translateSource
    val detectedLanguage: StateFlow<LanguageOption?> = translationController.detectedLanguage

    init {
        packRepo.initAsync()
        reconcileFreeLanguagesWithCurrentState()

        viewModelScope.launch {
            billingRepo.isPremium.collectLatest { premium ->
                if (premium) {
                    translationController.clearTranslateError()
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
                .putString(PREF_SOURCE_LANG_CODE, source.code)
                .putString(PREF_TARGET_LANG_CODE, target.code)
        }

        _isFirstRun.value = false
        translationController.setTranslateLanguages(source, target)

        viewModelScope.launch {
            _uiMessage.emit(appString(R.string.downloading_selected_language_packs))
            selected.forEach { lang ->
                if (packDao.getByCode(lang.mlKitCode)?.status != PackStatus.DOWNLOADED) {
                    runCatching { packRepo.downloadPack(lang.mlKitCode, requireWifi = false) }
                }
            }
            reconcileFreeLanguagesWithCurrentState()
        }
    }

    fun observePackStatus(mlKitCode: String): Flow<LanguagePackEntity?> = packRepo.observePack(mlKitCode)

    fun isFreePlan(): Boolean = !isPremium.value

    fun canOpenLanguagePicker(): Boolean = isPremium.value

    fun isFreeLanguage(lang: LanguageOption): Boolean {
        return currentAllowedFreeLanguages().any { it.code == lang.code }
    }

    fun canUseSpeechReward(): Boolean = !isPremium.value && !rewardsRepo.isSpeechUnlockedNow()

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

    fun launchPremiumPurchase(activity: Activity, planType: PremiumPlanType): Boolean {
        return billingRepo.launchPurchase(activity, planType)
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

    fun bonusWordsRemaining(): Int = rewardsRepo.bonusWordsRemaining()

    fun availableDownloadSlots(): Int = currentFreeLanguageLimit()

    fun shouldShowAds(): Boolean = !isPremium.value && !rewardsRepo.isAdFreeNow()

    fun shouldShowBannerAd(): Boolean = !isPremium.value && !rewardsRepo.isAdFreeNow()

    fun canDownloadPack(mlKitCode: String): Boolean {
        return packController.canDownloadPack(mlKitCode)
    }

    fun setLiveCameraMode(enabled: Boolean) {
        _isLiveCameraMode.value = enabled
    }

    fun onTranslateInputChange(text: String) {
        _showWordRewardPrompt.value = false
        translationController.onTranslateInputChange(text)
    }

    fun setTranslateSourceLang(lang: LanguageOption) {
        translationController.setTranslateSourceLang(lang)
    }

    fun setTranslateTargetLang(lang: LanguageOption) {
        translationController.setTranslateTargetLang(lang)
    }

    fun swapTranslateLanguages() {
        translationController.swapTranslateLanguages()
    }

    fun translate() {
        _showWordRewardPrompt.value = false
        translationController.translate(::buildOfflineError)
    }

    fun translateFromPrimaryAction() {
        translate()
    }

    fun startRecording() {
        if (!isPremium.value && !rewardsRepo.isSpeechUnlockedNow()) {
            viewModelScope.launch {
                _uiMessage.emit(appString(R.string.speech_unlock_reward_message))
            }
            return
        }
        val currentSource = translateSourceLang.value
        speechController.startRecording(
            languageCode = currentSource.takeIf { it != AUTO_DETECT_OPTION }?.code
        )
    }

    fun stopRecording() {
        speechController.stopRecording()
    }

    fun clearTranslation() {
        _showWordRewardPrompt.value = false
        translationController.clearTranslation()
        stopSpeaking()
    }

    fun downloadPack(mlKitCode: String, requireWifi: Boolean = true) {
        packController.downloadPack(mlKitCode, requireWifi)
    }

    fun requestPackSlotRewardAndDownload(mlKitCode: String, requireWifi: Boolean) {
        if (rewardsRepo.hasUnusedExtraSlotCredit()) {
            downloadPack(mlKitCode, requireWifi)
            return
        }
        pendingRewardedPackDownload = mlKitCode to requireWifi
        rewardsRepo.requestReward(RewardType.EXTRA_SLOT)
    }

    fun requestWordRewardAndRetry() {
        if (rewardsRepo.hasUnusedBonusWords()) {
            viewModelScope.launch {
                _uiMessage.emit(appString(R.string.reward_use_existing_words_first))
            }
            return
        }
        pendingRewardedTranslateRetry = true
        _showWordRewardPrompt.value = false
        rewardsRepo.requestReward(RewardType.EXTRA_WORDS)
    }

    fun requestSpeechRewardAndRecord() {
        rewardsRepo.requestReward(RewardType.SPEECH_SESSION)
    }

    fun requestReward(type: RewardType) {
        rewardsRepo.requestReward(type)
    }

    fun dismissWordRewardPrompt() {
        _showWordRewardPrompt.value = false
    }

    fun onRewardAdCompleted(type: RewardType) {
        rewardsRepo.grantReward(type)
        when (type) {
            RewardType.EXTRA_WORDS -> {
                viewModelScope.launch {
                    _uiMessage.emit(appString(R.string.reward_words_earned_template, rewardsRepo.bonusWordsRemaining()))
                }
                if (pendingRewardedTranslateRetry) {
                    pendingRewardedTranslateRetry = false
                    translate()
                }
            }
            RewardType.EXTRA_SLOT -> {
                viewModelScope.launch {
                    _uiMessage.emit(appString(R.string.reward_slot_earned))
                }
                pendingRewardedPackDownload?.let { (code, wifiOnly) ->
                    pendingRewardedPackDownload = null
                    downloadPack(code, wifiOnly)
                }
            }
            RewardType.SPEECH_SESSION -> {
                viewModelScope.launch {
                    _uiMessage.emit(appString(R.string.reward_speech_earned))
                }
            }
            RewardType.AD_FREE -> {
                viewModelScope.launch {
                    _uiMessage.emit(appString(R.string.reward_ad_free_earned))
                }
            }
        }
    }

    fun onRewardAdFailed() {
        clearPendingRewardActions()
        viewModelScope.launch {
            _uiMessage.emit(appString(R.string.reward_ad_not_ready))
        }
    }

    fun onRewardAdDismissed() {
        clearPendingRewardActions()
    }

    fun cancelPackDownload(mlKitCode: String) {
        packController.cancelPackDownload(mlKitCode)
    }

    fun deletePack(mlKitCode: String) {
        packController.deletePack(mlKitCode)
    }

    fun refreshPackStatus() {
        packController.refreshPackStatus()
    }

    fun onTtsInputChange(text: String) {}

    fun speakText(text: String, langCode: String) {
        speechController.speakText(text, langCode)
    }

    fun stopSpeaking() {
        speechController.stopSpeaking()
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
        val target = translateTargetLang.value
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
        translationController.restoreFromHistory(
            entry = entry,
            fallbackSource = _freePlanLanguages.value.first(),
            fallbackTarget = _freePlanLanguages.value.last()
        )
    }

    override fun onCleared() {
        super.onCleared()
        packController.cancelAll()
        speechController.release()
        billingRepo.endConnection()
    }

    private fun handleSpeechResult(text: String) {
        translationController.onTranslateInputChange(text)
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
        return if (translateSourceLang.value == AUTO_DETECT_OPTION) {
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
            if (!isPremium.value && !isFreeLanguage(safeDetected)) {
                _freePlanLanguages.value.firstOrNull() ?: safeDetected
            } else {
                safeDetected
            }
        } else {
            sanitizeSourceLanguage(translateSourceLang.value)
        }
    }

    private fun markWordsUsedIfNeeded(text: String, src: LanguageOption, tgt: LanguageOption) {
        if (isPremium.value) return
        val key = translationUsageKey(text, src, tgt)
        val counted = prefs.getStringSet(PREF_COUNTED_TRANSLATION_KEYS, emptySet()).orEmpty().toMutableSet()
        if (counted.contains(key)) return

        val words = countWords(text)
        val freeRemaining = (FREE_WORD_LIMIT - _freeWordsUsed.value).coerceAtLeast(0)
        val freePortion = words.coerceAtMost(freeRemaining)
        val rewardPortion = (words - freePortion).coerceAtLeast(0)

        if (rewardPortion > 0) {
            rewardsRepo.consumeBonusWords(rewardPortion)
        }

        counted.add(key)
        val newTotal = (_freeWordsUsed.value + freePortion).coerceAtMost(FREE_WORD_LIMIT)
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

        val wordsNeeded = countWords(text)
        val freeRemaining = (FREE_WORD_LIMIT - _freeWordsUsed.value).coerceAtLeast(0)
        val rewardRemaining = rewardsRepo.bonusWordsRemaining()
        if (freeRemaining + rewardRemaining < wordsNeeded) {
            if (rewardRemaining > 0) {
                throw IllegalStateException(appString(R.string.reward_use_existing_words_first))
            }
            _showWordRewardPrompt.value = true
            throw IllegalStateException(appString(R.string.free_plan_limit_upgrade_unlimited))
        }
    }

    private suspend fun ensureFreePlanAllowsLanguagePair(src: LanguageOption, tgt: LanguageOption) {
        val allowed = _freePlanLanguages.value.map { it.code }.toSet()
        if (src.code !in allowed || tgt.code !in allowed) {
            throw IllegalStateException(appString(R.string.free_plan_supports_only_selected_languages))
        }
    }

    private suspend fun registerTranslationAction() {
        val nextValue = _translateActions.value + 1
        prefs.edit { putInt(PREF_TRANSLATE_ACTIONS, nextValue) }
        _translateActions.value = nextValue
        if (!isPremium.value && !rewardsRepo.isAdFreeNow() && nextValue % AD_FREQUENCY == 0) {
            _showInterstitialAd.emit(Unit)
        }
    }

    private fun buildOfflineError(src: LanguageOption, tgt: LanguageOption): String {
        return appString(
            R.string.offline_packs_not_ready_template,
            src.displayName(getApplication()),
            tgt.displayName(getApplication())
        )
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
        val languageLimit = currentFreeLanguageLimit()
        val chosen = when {
            downloaded.size >= languageLimit -> downloaded.take(languageLimit)
            persisted.isNotEmpty() -> persisted.take(languageLimit)
            else -> listOf(DEFAULT_FREE_LANGUAGE) + SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code }.take(languageLimit - 1)
        }

        persistFreeLanguages(chosen, languageLimit)
        if (chosen.isNotEmpty()) {
            translationController.coerceSelectedLanguages(
                sourceFallback = chosen.first(),
                targetFallback = chosen.getOrElse(1) { chosen.first() }
            )
        }
    }

    private fun persistFreeLanguages(languages: List<LanguageOption>, limit: Int = FREE_LANGUAGE_LIMIT) {
        val codes = languages.distinctBy { it.code }.take(limit).map { it.code }
        prefs.edit {
            putStringSet(PREF_FREE_LANGUAGE_CODES, codes.toSet())
            putString(PREF_FREE_LANGUAGE_CODES_ORDERED, codes.joinToString(","))
        }
        _freePlanLanguages.value = codes.mapNotNull { code ->
            SUPPORTED_LANGUAGES.find { it.code == code }
        }.ifEmpty {
            listOf(DEFAULT_FREE_LANGUAGE) + SUPPORTED_LANGUAGES.filter { it.code != DEFAULT_FREE_LANGUAGE.code }.take(limit - 1)
        }
    }

    private fun loadPersistedFreeLanguages(): List<LanguageOption> {
        val orderedCodes = prefs.getString(PREF_FREE_LANGUAGE_CODES_ORDERED, null)
            ?.split(",")
            ?.map(String::trim)
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val codes = if (orderedCodes.isNotEmpty()) {
            orderedCodes
        } else {
            prefs.getStringSet(PREF_FREE_LANGUAGE_CODES, emptySet()).orEmpty().toList()
        }
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
            currentAllowedFreeLanguages().find { it.code == lang.code } ?: DEFAULT_FREE_LANGUAGE
        }
    }

    private fun sanitizeTargetLanguage(lang: LanguageOption): LanguageOption {
        return if (isPremium.value) {
            lang
        } else {
            currentAllowedFreeLanguages().find { it.code == lang.code }
                ?: currentAllowedFreeLanguages().firstOrNull { it.code != translateSourceLang.value.code }
                ?: DEFAULT_FREE_LANGUAGE
        }
    }

    private fun currentAllowedFreeLanguages(): List<LanguageOption> {
        val downloaded = allPacks.value
            .filter { it.status == PackStatus.DOWNLOADED }
            .mapNotNull { pack -> SUPPORTED_LANGUAGES.find { it.mlKitCode == pack.mlKitCode } }
            .distinctBy { it.code }
            .take(currentFreeLanguageLimit())
        return downloaded.ifEmpty { _freePlanLanguages.value }
    }

    private fun currentFreeLanguageLimit(): Int {
        val downloadedCount = allPacks.value.count { it.status == PackStatus.DOWNLOADED }
        return maxOf(FREE_LANGUAGE_LIMIT + rewardsRepo.extraSlotCredits(), downloadedCount)
    }

    private fun clearPendingRewardActions() {
        pendingRewardedTranslateRetry = false
        pendingRewardedPackDownload = null
    }

    private fun appString(@StringRes resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }
}
