package com.puneeth50.ailinguakit.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puneeth50.ailinguakit.R
import com.puneeth50.ailinguakit.data.LanguagePackDao
import com.puneeth50.ailinguakit.data.PackStatus
import com.puneeth50.ailinguakit.service.BillingRepository
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
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val badgeRes: Int,
    val enabled: Boolean,
    val comingSoon: Boolean
)

data class RewardsUiState(
    val isPremium: Boolean = false,
    val remainingWords: Int = REWARDS_FREE_WORD_LIMIT,
    val downloadedLanguages: Int = 0,
    val activeRewardLabelRes: Int = R.string.rewards_status_none,
    val rewardCards: List<RewardCardUi> = emptyList()
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    packDao: LanguagePackDao,
    billingRepo: BillingRepository
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
        downloadedCount
    ) { isPremium, wordsUsed, packs ->
        val downloadedLanguages = packs.count { it.status == PackStatus.DOWNLOADED }
        RewardsUiState(
            isPremium = isPremium,
            remainingWords = (REWARDS_FREE_WORD_LIMIT - wordsUsed).coerceAtLeast(0),
            downloadedLanguages = downloadedLanguages,
            activeRewardLabelRes = if (isPremium) R.string.rewards_status_premium else R.string.rewards_status_none,
            rewardCards = buildRewardCards(isPremium)
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

    private fun buildRewardCards(isPremium: Boolean): List<RewardCardUi> {
        return listOf(
            RewardCardUi(
                id = "extra_words",
                titleRes = R.string.reward_extra_words_title,
                subtitleRes = R.string.reward_extra_words_subtitle,
                badgeRes = if (isPremium) R.string.rewards_included_badge else R.string.rewards_recommended_badge,
                enabled = !isPremium,
                comingSoon = !isPremium
            ),
            RewardCardUi(
                id = "extra_slot",
                titleRes = R.string.reward_extra_slot_title,
                subtitleRes = R.string.reward_extra_slot_subtitle,
                badgeRes = if (isPremium) R.string.rewards_included_badge else R.string.rewards_coming_soon_badge,
                enabled = false,
                comingSoon = true
            ),
            RewardCardUi(
                id = "speech_unlock",
                titleRes = R.string.reward_speech_title,
                subtitleRes = R.string.reward_speech_subtitle,
                badgeRes = if (isPremium) R.string.rewards_included_badge else R.string.rewards_coming_soon_badge,
                enabled = false,
                comingSoon = true
            ),
            RewardCardUi(
                id = "ad_free",
                titleRes = R.string.reward_ad_free_title,
                subtitleRes = R.string.reward_ad_free_subtitle,
                badgeRes = if (isPremium) R.string.rewards_included_badge else R.string.rewards_coming_soon_badge,
                enabled = false,
                comingSoon = true
            )
        )
    }
}
