package com.puneeth50.ailinguakit.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.LanguagePackDao
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.service.BillingRepository
import com.puneeth50.ailinguakit.service.RewardType
import com.puneeth50.ailinguakit.service.RewardsRepository
import com.puneeth50.ailinguakit.service.RewardsWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val PREF_FREE_WORDS_USED = "free_words_used"
private const val REWARDS_FREE_WORD_LIMIT = 3_000
private const val REWARDS_FREE_LANGUAGE_LIMIT = 3

data class RewardCardUi(
    val type: RewardType,
    val titleRes: Int,
    val subtitleRes: Int,
    val badgeRes: Int,
    val enabled: Boolean,
    val actionLabelRes: Int
)

data class RewardsUiState(
    val isPremium: Boolean = false,
    val remainingWords: Int = REWARDS_FREE_WORD_LIMIT,
    val downloadedLanguages: Int = 0,
    val bonusWordsRemaining: Int = 0,
    val extraSlotCredits: Int = 0,
    val speechUnlockUntilMs: Long = 0L,
    val adFreeUntilMs: Long = 0L,
    val activeRewardLabelRes: Int = R.string.rewards_status_none,
    val rewardCards: List<RewardCardUi> = emptyList()
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    packDao: LanguagePackDao,
    billingRepo: BillingRepository,
    private val rewardsRepo: RewardsRepository
) : ViewModel() {

    private val freeWordsUsed = MutableStateFlow(prefs.getInt(PREF_FREE_WORDS_USED, 0))

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_FREE_WORDS_USED) {
            freeWordsUsed.value = prefs.getInt(PREF_FREE_WORDS_USED, 0)
        }
    }

    private val downloadedCount = packDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<RewardsUiState> = combine(
        billingRepo.isPremium,
        freeWordsUsed,
        downloadedCount,
        rewardsRepo.wallet
    ) { isPremium, wordsUsed, packs, wallet ->
        val downloadedLanguages = packs.count { it.status == PackStatus.DOWNLOADED }
        RewardsUiState(
            isPremium = isPremium,
            remainingWords = (REWARDS_FREE_WORD_LIMIT - wordsUsed).coerceAtLeast(0) + wallet.bonusWordsRemaining,
            downloadedLanguages = downloadedLanguages,
            bonusWordsRemaining = wallet.bonusWordsRemaining,
            extraSlotCredits = wallet.extraSlotCredits,
            speechUnlockUntilMs = wallet.speechUnlockUntilMs,
            adFreeUntilMs = wallet.adFreeUntilMs,
            activeRewardLabelRes = when {
                isPremium -> R.string.rewards_status_premium
                wallet.bonusWordsRemaining > 0 || wallet.extraSlotCredits > 0 ||
                    wallet.speechUnlockUntilMs > System.currentTimeMillis() ||
                    wallet.adFreeUntilMs > System.currentTimeMillis() -> R.string.rewards_status_active
                else -> R.string.rewards_status_none
            },
            rewardCards = buildRewardCards(isPremium, wallet, downloadedLanguages)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsUiState())

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }

    fun maxFreeLanguages(): Int = REWARDS_FREE_LANGUAGE_LIMIT

    fun requestReward(type: RewardType) {
        if (type == RewardType.EXTRA_WORDS && rewardsRepo.hasUnusedBonusWords()) return
        if (type == RewardType.EXTRA_SLOT && rewardsRepo.hasUnusedExtraSlotCredit()) return
        rewardsRepo.requestReward(type)
    }

    private fun buildRewardCards(
        isPremium: Boolean,
        wallet: RewardsWallet,
        downloadedLanguages: Int
    ): List<RewardCardUi> {
        val speechActive = wallet.speechUnlockUntilMs > System.currentTimeMillis()
        val adFreeActive = wallet.adFreeUntilMs > System.currentTimeMillis()
        val bonusWordsReady = wallet.bonusWordsRemaining > 0
        val extraSlotReady = wallet.extraSlotCredits > 0
        return listOf(
            RewardCardUi(
                type = RewardType.EXTRA_WORDS,
                titleRes = R.string.reward_extra_words_title,
                subtitleRes = R.string.reward_extra_words_subtitle,
                badgeRes = when {
                    isPremium -> R.string.rewards_included_badge
                    bonusWordsReady -> R.string.rewards_active_badge
                    else -> R.string.rewards_available_badge
                },
                enabled = !isPremium && !bonusWordsReady,
                actionLabelRes = when {
                    isPremium -> R.string.rewards_included_in_plan
                    bonusWordsReady -> R.string.rewards_use_your_bonus_words
                    else -> R.string.rewards_watch_to_earn
                }
            ),
            RewardCardUi(
                type = RewardType.EXTRA_SLOT,
                titleRes = R.string.reward_extra_slot_title,
                subtitleRes = R.string.reward_extra_slot_subtitle,
                badgeRes = when {
                    isPremium -> R.string.rewards_included_badge
                    extraSlotReady -> R.string.rewards_active_badge
                    else -> R.string.rewards_available_badge
                },
                enabled = !isPremium && !extraSlotReady && downloadedLanguages >= REWARDS_FREE_LANGUAGE_LIMIT,
                actionLabelRes = when {
                    isPremium -> R.string.rewards_included_in_plan
                    extraSlotReady -> R.string.rewards_use_in_language_packs
                    downloadedLanguages < REWARDS_FREE_LANGUAGE_LIMIT -> R.string.rewards_fill_free_slots_first
                    else -> R.string.rewards_watch_to_unlock
                }
            ),
            RewardCardUi(
                type = RewardType.SPEECH_SESSION,
                titleRes = R.string.reward_speech_title,
                subtitleRes = R.string.reward_speech_subtitle,
                badgeRes = when {
                    isPremium -> R.string.rewards_included_badge
                    speechActive -> R.string.rewards_active_badge
                    else -> R.string.rewards_available_badge
                },
                enabled = !isPremium && !speechActive,
                actionLabelRes = when {
                    isPremium -> R.string.rewards_included_in_plan
                    speechActive -> R.string.rewards_active_now
                    else -> R.string.rewards_watch_to_unlock
                }
            ),
            RewardCardUi(
                type = RewardType.AD_FREE,
                titleRes = R.string.reward_ad_free_title,
                subtitleRes = R.string.reward_ad_free_subtitle,
                badgeRes = when {
                    isPremium -> R.string.rewards_included_badge
                    adFreeActive -> R.string.rewards_active_badge
                    else -> R.string.rewards_available_badge
                },
                enabled = !isPremium && !adFreeActive,
                actionLabelRes = when {
                    isPremium -> R.string.rewards_included_in_plan
                    adFreeActive -> R.string.rewards_active_now
                    else -> R.string.rewards_watch_to_unlock
                }
            )
        )
    }
}
