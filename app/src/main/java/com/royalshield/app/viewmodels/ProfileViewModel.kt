package com.royalshield.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalshield.app.data.UserProfile
import com.royalshield.app.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isAuthenticated: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (!repository.isAuthenticated()) {
            _uiState.value = ProfileUiState(isLoading = false, isAuthenticated = false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadProfile() }
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile, isLoading = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No se pudo cargar el perfil")
                    }
                }
        }
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        _uiState.update { it.copy(profile = transform(it.profile), errorMessage = null) }
    }

    fun saveProfile() {
        val profile = _uiState.value.profile
        val validationError = validate(profile)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching { repository.saveProfile(profile) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Perfil guardado") }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "No se pudo guardar el perfil")
                    }
                }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signOut() = repository.signOut()

    private fun validate(profile: UserProfile): String? {
        if (profile.displayName.trim().length !in 2..60) {
            return "El nombre debe tener entre 2 y 60 caracteres"
        }
        if (!isValidPhone(profile.phone)) return "El teléfono no tiene un formato válido"
        if (!isValidPhone(profile.emergencyPhone)) return "El teléfono de emergencia no es válido"
        if (profile.city.length > 80 || profile.country.length > 80) {
            return "Ciudad y país deben tener menos de 80 caracteres"
        }
        return null
    }

    private fun isValidPhone(value: String): Boolean {
        if (value.isBlank()) return true
        return value.trim().matches(Regex("^\\+?[0-9 ()-]{7,25}$"))
    }
}
