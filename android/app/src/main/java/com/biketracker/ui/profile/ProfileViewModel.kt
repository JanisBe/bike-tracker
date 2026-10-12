package com.biketracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.UserProfile
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val gender: String = "male",
    val weightText: String = "75.0",
    val heightText: String = "175.0",
    val cyclingThreshold: Double = 1.5,
    val walkingThreshold: Double = 0.5,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.getUserProfile()
            if (result.isSuccess) {
                val profile = result.getOrNull() ?: UserProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = profile.email.ifBlank { authRepository.currentUser?.email ?: "" },
                        gender = profile.gender,
                        weightText = if (profile.weightKg > 0) "%.1f".format(profile.weightKg)
                            .replace(',', '.') else "75.0",
                        heightText = if (profile.heightCm > 0) "%.0f".format(profile.heightCm) else "175",
                        cyclingThreshold = profile.cyclingSpeedThresholdKmh,
                        walkingThreshold = profile.walkingSpeedThresholdKmh
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = authRepository.currentUser?.email ?: "",
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }

    fun onGenderSelected(gender: String) {
        _uiState.update { it.copy(gender = gender, saveSuccess = false) }
    }

    fun onWeightChanged(weight: String) {
        _uiState.update { it.copy(weightText = weight, saveSuccess = false) }
    }

    fun onHeightChanged(height: String) {
        _uiState.update { it.copy(heightText = height, saveSuccess = false) }
    }

    fun onCyclingThresholdChanged(threshold: Double) {
        _uiState.update { it.copy(cyclingThreshold = threshold, saveSuccess = false) }
    }

    fun onWalkingThresholdChanged(threshold: Double) {
        _uiState.update { it.copy(walkingThreshold = threshold, saveSuccess = false) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val weight = state.weightText.replace(',', '.').toDoubleOrNull() ?: 75.0
        val height = state.heightText.replace(',', '.').toDoubleOrNull() ?: 175.0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            val profile = UserProfile(
                email = state.email,
                gender = state.gender,
                weightKg = weight,
                heightCm = height,
                cyclingSpeedThresholdKmh = state.cyclingThreshold,
                walkingSpeedThresholdKmh = state.walkingThreshold
            )
            val result = userRepository.saveUserProfile(profile)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Nie udało się zapisać profilu"
                    )
                }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }
}
