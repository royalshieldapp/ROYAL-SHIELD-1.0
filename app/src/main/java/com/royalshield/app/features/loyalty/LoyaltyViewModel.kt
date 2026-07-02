package com.royalshield.app.features.loyalty

import androidx.lifecycle.ViewModel
import com.royalshield.app.data.LoyaltyRepository
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoyaltyUiState(
    val points: Int = 0,
    val tier: String = "Starter",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoyaltyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        LoyaltyUiState(
            points = PreferencesManager.getLoyaltyPoints(),
            tier = PreferencesManager.getLoyaltyTier()
        )
    )
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

    init {
        loadLoyaltyStatus()
    }

    fun loadLoyaltyStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        LoyaltyRepository.getStatus { success, points, tier ->
            if (success) {
                val finalPoints = points ?: 0
                val finalTier = tier ?: "Starter"
                PreferencesManager.saveLoyaltyPoints(finalPoints)
                PreferencesManager.saveLoyaltyTier(finalTier)
                _uiState.value = _uiState.value.copy(
                    points = finalPoints,
                    tier = finalTier,
                    isLoading = false,
                    error = null
                )
            } else {
                val cachedPoints = PreferencesManager.getLoyaltyPoints()
                val cachedTier = PreferencesManager.getLoyaltyTier()
                _uiState.value = _uiState.value.copy(
                    points = cachedPoints,
                    tier = cachedTier,
                    isLoading = false,
                    error = if (cachedPoints == 0 && cachedTier == "Starter") {
                        "Failed to load loyalty data"
                    } else {
                        "Offline mode: Using cached points"
                    }
                )
            }
        }
    }

    fun syncActivityPoints(points: Int, action: String) {
        LoyaltyRepository.syncPoints(points, action) { success, newTotal ->
            if (success && newTotal != null) {
                PreferencesManager.saveLoyaltyPoints(newTotal)
                val newTier = when {
                    newTotal >= 1500 -> "Elite"
                    newTotal >= 800 -> "Gold"
                    newTotal >= 300 -> "Silver"
                    else -> "Bronze"
                }
                PreferencesManager.saveLoyaltyTier(newTier)
                _uiState.value = _uiState.value.copy(points = newTotal, tier = newTier)
            }
        }
    }

    fun redeemReward(pointsCost: Int, rewardName: String, onResult: (Boolean, String) -> Unit) {
        val currentPoints = _uiState.value.points
        if (currentPoints < pointsCost) {
            onResult(false, "Insufficient points. You need $pointsCost pts.")
            return
        }
        val remainingPoints = currentPoints - pointsCost
        
        PreferencesManager.saveLoyaltyPoints(remainingPoints)
        val newTier = when {
            remainingPoints >= 1500 -> "Elite"
            remainingPoints >= 800 -> "Gold"
            remainingPoints >= 300 -> "Silver"
            else -> "Bronze"
        }
        PreferencesManager.saveLoyaltyTier(newTier)
        
        _uiState.value = _uiState.value.copy(
            points = remainingPoints,
            tier = newTier
        )
        
        LoyaltyRepository.syncPoints(-pointsCost, "redeem_$rewardName") { success, newTotal ->
            if (success && newTotal != null) {
                PreferencesManager.saveLoyaltyPoints(newTotal)
                val finalTier = when {
                    newTotal >= 1500 -> "Elite"
                    newTotal >= 800 -> "Gold"
                    newTotal >= 300 -> "Silver"
                    else -> "Bronze"
                }
                PreferencesManager.saveLoyaltyTier(finalTier)
                _uiState.value = _uiState.value.copy(points = newTotal, tier = finalTier)
            }
        }
        
        onResult(true, "Successfully redeemed $rewardName!")
    }
}

