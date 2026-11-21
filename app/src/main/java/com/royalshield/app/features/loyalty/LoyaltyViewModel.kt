package com.royalshield.app.features.loyalty

import androidx.lifecycle.ViewModel
import com.royalshield.app.data.LoyaltyRepository
import com.royalshield.app.managers.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class LoyaltyUiState(
    val points: Int = 0,
    val tier: String = "Bronze",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoyaltyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        LoyaltyUiState(
            points = PreferencesManager.getLoyaltyPoints(),
            tier = PreferencesManager.getLoyaltyTier().ifBlank { "Bronze" },
        )
    )
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

    init {
        claimDailyLogin()
    }

    fun loadLoyaltyStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        LoyaltyRepository.getStatus { success, points, tier, error ->
            if (success && points != null && tier != null) {
                applyServerBalance(points, tier)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error ?: "Unable to refresh Loyalty",
                )
            }
        }
    }

    fun claimDailyLogin() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        LoyaltyRepository.claimDailyLogin { success, points, tier, _, error ->
            if (success && points != null && tier != null) {
                applyServerBalance(points, tier)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                loadLoyaltyStatus()
            }
        }
    }

    fun redeemReward(
        rewardId: String,
        rewardName: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        LoyaltyRepository.redeemReward(
            rewardId = rewardId,
            idempotencyKey = UUID.randomUUID().toString(),
        ) { success, newTotal, tier, redemptionStatus, error ->
            if (success && newTotal != null && tier != null) {
                applyServerBalance(newTotal, tier)
                onResult(true, "$rewardName: ${redemptionStatus ?: "PENDING"}")
            } else {
                val message = error ?: "Unable to redeem reward"
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
                onResult(false, message)
            }
        }
    }

    private fun applyServerBalance(points: Int, tier: String) {
        // Preferences are an offline visual cache only; the backend remains authoritative.
        PreferencesManager.saveLoyaltyPoints(points)
        PreferencesManager.saveLoyaltyTier(tier)
        _uiState.value = LoyaltyUiState(points = points, tier = tier, isLoading = false)
    }
}
