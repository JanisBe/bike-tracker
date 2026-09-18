package com.biketracker.ui.tracking

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.SatelliteInfo
import com.biketracker.data.model.TrackPoint
import com.biketracker.data.repository.RideRepository
import com.biketracker.service.LocationTrackingService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val isWaitingForGps = LocationTrackingService.isWaitingForGps
    val isGpsLost = LocationTrackingService.isGpsLost
    val satelliteInfo: StateFlow<SatelliteInfo> = LocationTrackingService.satelliteInfo
    val trackPoints: StateFlow<List<TrackPoint>> = LocationTrackingService.trackPoints
    val currentDistanceKm = LocationTrackingService.currentDistanceKm
    val currentSpeedKmh = LocationTrackingService.currentSpeedKmh
    val elapsedSeconds = LocationTrackingService.elapsedSeconds

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @Suppress("MissingPermission")
    suspend fun checkHasGpsFix(): Boolean {
        if (!isLocationServiceEnabled()) return false
        if (satelliteInfo.value.used >= 4) return true

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val isGps = location.provider?.equals(
                    LocationManager.GPS_PROVIDER,
                    ignoreCase = true
                ) == true
                val ageMs = System.currentTimeMillis() - location.time
                // Only consider it a genuine GPS fix if it came directly from GPS provider, is fresh (<10s) and accurate
                isGps && ageMs < 10_000L && location.accuracy <= 20.0f
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun startTracking(waitForGps: Boolean = false) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            putExtra(LocationTrackingService.EXTRA_WAIT_FOR_GPS, waitForGps)
        }
        context.startForegroundService(intent)
    }

    fun forceStartTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_FORCE_START
        }
        context.startService(intent)
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
            _uiState.update { it.copy(saveError = "Trening za krótki do zapisania (wymagane są co najmniej 2 punkty)") }
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
                            ?: "Nie udało się zapisać treningu"
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
