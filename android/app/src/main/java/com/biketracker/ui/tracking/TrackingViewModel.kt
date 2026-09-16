package com.biketracker.ui.tracking

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.TrackPoint
import com.biketracker.data.repository.RideRepository
import com.biketracker.service.LocationTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedRideId: String? = null
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rideRepository: RideRepository
) : ViewModel() {

    val isTracking = LocationTrackingService.isTracking
    val isPaused = LocationTrackingService.isPaused
    val trackPoints: StateFlow<List<TrackPoint>> = LocationTrackingService.trackPoints
    val currentDistanceKm = LocationTrackingService.currentDistanceKm
    val currentSpeedKmh = LocationTrackingService.currentSpeedKmh
    val elapsedSeconds = LocationTrackingService.elapsedSeconds

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    fun startTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun pauseTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopAndSaveRide() {
        val pointsToSave = trackPoints.value

        // Stop the foreground service
        val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(stopIntent)

        if (pointsToSave.size < 2) {
            _uiState.update { it.copy(saveError = "Ride too short to save (need at least 2 points)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            val result = rideRepository.saveRide(pointsToSave)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, savedRideId = result.getOrNull()) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = result.exceptionOrNull()?.localizedMessage
                            ?: "Failed to save ride"
                    )
                }
            }
        }
    }

    fun stopTrackingWithoutSaving() {
        val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(stopIntent)
    }

    fun clearError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
