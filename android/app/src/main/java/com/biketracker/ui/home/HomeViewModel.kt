package com.biketracker.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.Ride
import com.biketracker.data.model.TrackPoint
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.RideRepository
import com.biketracker.domain.util.GpxParser
import com.biketracker.domain.util.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class GpxImportPreview(
    val gpxXml: String,
    val points: List<TrackPoint>,
    val suggestedTitle: String,
    val locationName: String,
    val distanceKm: Double,
    val durationSeconds: Long,
    val elevationGain: Double?,
    val avgSpeedKmh: Double,
    val startTime: Date
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val rides: StateFlow<List<Ride>> = rideRepository.getRides()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentUserEmail: String?
        get() = authRepository.currentUser?.email ?: if (authRepository.currentUser?.isAnonymous == true) "Guest User" else null

    var importPreview by mutableStateOf<GpxImportPreview?>(null)
        private set

    var isImporting by mutableStateOf(false)
        private set

    var importErrorMessage by mutableStateOf<String?>(null)
        private set

    fun processGpxContent(gpxXml: String) {
        viewModelScope.launch {
            try {
                importErrorMessage = null
                val result = GpxParser.parseFull(gpxXml)
                if (result.points.size < 2) {
                    importErrorMessage = "Plik GPX musi zawierać co najmniej 2 punkty trasy."
                    return@launch
                }

                val stats = StatsCalculator.calculate(result.points)
                val firstPoint = result.points.first()
                val locationName = rideRepository.resolveLocation(firstPoint.latitude, firstPoint.longitude)
                val startTime = Date(firstPoint.timestamp)
                val suggestedTitle = GpxParser.suggestTitle(result.name, locationName, startTime)

                importPreview = GpxImportPreview(
                    gpxXml = gpxXml,
                    points = result.points,
                    suggestedTitle = suggestedTitle,
                    locationName = locationName,
                    distanceKm = stats.distanceKm,
                    durationSeconds = stats.durationSeconds,
                    elevationGain = stats.elevationGain,
                    avgSpeedKmh = stats.avgSpeedKmh,
                    startTime = startTime
                )
            } catch (e: Exception) {
                importErrorMessage = e.message ?: "Błąd parsowania pliku GPX"
            }
        }
    }

    fun confirmGpxImport(title: String, onSuccess: (String) -> Unit) {
        val preview = importPreview ?: return
        viewModelScope.launch {
            isImporting = true
            importErrorMessage = null
            try {
                val finalTitle = title.trim().ifEmpty { preview.suggestedTitle }
                val result = rideRepository.importGpxRide(
                    points = preview.points,
                    gpxXml = preview.gpxXml,
                    title = finalTitle,
                    locationName = preview.locationName
                )
                result.onSuccess { rideId ->
                    importPreview = null
                    onSuccess(rideId)
                }.onFailure { err ->
                    importErrorMessage = err.message ?: "Nie udało się zapisać treningu."
                }
            } finally {
                isImporting = false
            }
        }
    }

    fun dismissGpxImport() {
        importPreview = null
        importErrorMessage = null
        isImporting = false
    }

    fun dismissImportError() {
        importErrorMessage = null
    }

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }

    fun deleteRide(rideId: String) {
        viewModelScope.launch {
            rideRepository.deleteRide(rideId)
        }
    }

    fun updateRideTitle(rideId: String, newTitle: String) {
        viewModelScope.launch {
            rideRepository.updateRideTitle(rideId, newTitle.trim())
        }
    }
}
