package com.puneeth50.ailinguakit.service

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RewardType {
    EXTRA_WORDS,
    EXTRA_SLOT,
    SPEECH_SESSION,
    AD_FREE
}

data class RewardsWallet(
    val bonusWordsRemaining: Int = 0,
    val extraSlotCredits: Int = 0,
    val speechUnlockUntilMs: Long = 0L,
    val adFreeUntilMs: Long = 0L
)

private const val PREF_REWARD_BONUS_WORDS = "reward_bonus_words"
private const val PREF_REWARD_EXTRA_SLOT_CREDITS = "reward_extra_slot_credits"
private const val PREF_REWARD_SPEECH_UNLOCK_UNTIL = "reward_speech_unlock_until"
private const val PREF_REWARD_AD_FREE_UNTIL = "reward_ad_free_until"

const val REWARD_EXTRA_WORDS_AMOUNT = 500
const val REWARD_SPEECH_UNLOCK_MINUTES = 15
const val REWARD_AD_FREE_MINUTES = 30

@Singleton
class RewardsRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val _wallet = MutableStateFlow(loadWallet())
    val wallet: StateFlow<RewardsWallet> = _wallet.asStateFlow()

    private val _rewardRequests = MutableSharedFlow<RewardType>(extraBufferCapacity = 4)
    val rewardRequests: SharedFlow<RewardType> = _rewardRequests.asSharedFlow()

    fun requestReward(type: RewardType) {
        _rewardRequests.tryEmit(type)
    }

    fun grantReward(type: RewardType) {
        when (type) {
            RewardType.EXTRA_WORDS -> updateWallet {
                if (bonusWordsRemaining > 0) this else copy(bonusWordsRemaining = REWARD_EXTRA_WORDS_AMOUNT)
            }
            RewardType.EXTRA_SLOT -> updateWallet {
                if (extraSlotCredits > 0) this else copy(extraSlotCredits = 1)
            }
            RewardType.SPEECH_SESSION -> updateWallet {
                copy(
                    speechUnlockUntilMs = System.currentTimeMillis() +
                        REWARD_SPEECH_UNLOCK_MINUTES * 60_000L
                )
            }
            RewardType.AD_FREE -> updateWallet {
                copy(
                    adFreeUntilMs = System.currentTimeMillis() +
                        REWARD_AD_FREE_MINUTES * 60_000L
                )
            }
        }
    }

    fun consumeBonusWords(wordCount: Int) {
        if (wordCount <= 0) return
        updateWallet {
            copy(bonusWordsRemaining = (bonusWordsRemaining - wordCount).coerceAtLeast(0))
        }
    }

    fun consumeExtraSlotCredit() {
        updateWallet {
            copy(extraSlotCredits = (extraSlotCredits - 1).coerceAtLeast(0))
        }
    }

    fun bonusWordsRemaining(): Int = _wallet.value.bonusWordsRemaining

    fun hasUnusedBonusWords(): Boolean = _wallet.value.bonusWordsRemaining > 0

    fun extraSlotCredits(): Int = _wallet.value.extraSlotCredits

    fun hasUnusedExtraSlotCredit(): Boolean = _wallet.value.extraSlotCredits > 0

    fun isSpeechUnlockedNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        return _wallet.value.speechUnlockUntilMs > nowMs
    }

    fun isAdFreeNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        return _wallet.value.adFreeUntilMs > nowMs
    }

    private fun updateWallet(transform: RewardsWallet.() -> RewardsWallet) {
        val updated = _wallet.value.transform()
        persistWallet(updated)
        _wallet.value = updated
    }

    private fun loadWallet(): RewardsWallet {
        return RewardsWallet(
            bonusWordsRemaining = prefs.getInt(PREF_REWARD_BONUS_WORDS, 0),
            extraSlotCredits = prefs.getInt(PREF_REWARD_EXTRA_SLOT_CREDITS, 0),
            speechUnlockUntilMs = prefs.getLong(PREF_REWARD_SPEECH_UNLOCK_UNTIL, 0L),
            adFreeUntilMs = prefs.getLong(PREF_REWARD_AD_FREE_UNTIL, 0L)
        )
    }

    private fun persistWallet(wallet: RewardsWallet) {
        prefs.edit {
            putInt(PREF_REWARD_BONUS_WORDS, wallet.bonusWordsRemaining)
            putInt(PREF_REWARD_EXTRA_SLOT_CREDITS, wallet.extraSlotCredits)
            putLong(PREF_REWARD_SPEECH_UNLOCK_UNTIL, wallet.speechUnlockUntilMs)
            putLong(PREF_REWARD_AD_FREE_UNTIL, wallet.adFreeUntilMs)
        }
    }
}
