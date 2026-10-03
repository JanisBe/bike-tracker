package com.biketracker.ui.detail

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.Ride
import com.biketracker.data.model.RouteProfilePoint
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.RideRepository
import com.biketracker.domain.util.GpxGenerator
import com.biketracker.domain.util.GpxParser
import com.biketracker.domain.util.PolylineEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: Ride? = null,
    val isOwner: Boolean = false,
    val routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    val profilePoints: List<RouteProfilePoint> = emptyList(),
    val isLoadingProfile: Boolean = false,
    val errorMessage: String? = null,
    val isSharing: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val rideRepository: RideRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val rideId: String = checkNotNull(savedStateHandle["rideId"])

    private val _uiState = MutableStateFlow(RideDetailUiState())
    val uiState: StateFlow<RideDetailUiState> = _uiState.asStateFlow()

    init {
        loadRide()
    }

    private fun loadRide() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = rideRepository.getRide(rideId)
            if (result.isSuccess) {
                val ride = result.getOrThrow()
                val coords = PolylineEncoder.decode(ride.encodedPolyline)
                val isOwner = authRepository.currentUser?.uid == ride.userId
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        ride = ride,
                        isOwner = isOwner,
                        routeCoordinates = coords,
                        isLoadingProfile = true
                    )
                }
                loadProfileData(ride, coords)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nie znaleziono treningu"
                    )
                }
            }
        }
    }

    fun shareRideLink() {
        val currentRide = _uiState.value.ride ?: return
        val title =
            currentRide.locationName.ifBlank { currentRide.title }.ifBlank { "Trening rowerowy" }
        val shareUrl = "https://janisbe.github.io/bike-tracker/?ride=${currentRide.id}"
        val shareText = "Zobacz mój trening rowerowy ($title, ${
            String.format(
                Locale.US,
                "%.1f",
                currentRide.distanceKm
            )
        } km) w Bike Tracker:\n$shareUrl"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Trening rowerowy: $title")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Udostępnij link do treningu").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun loadProfileData(ride: Ride, coords: List<Pair<Double, Double>>) {
        viewModelScope.launch {
            val result = rideRepository.getGpxContent(rideId)
            val parsedPoints = if (result.isSuccess) {
                val xml = result.getOrNull().orEmpty()
                val points = GpxParser.parse(xml)
                points.ifEmpty {
                    GpxParser.fromCoordinates(
                        coords,
                        ride.elevationGain ?: 0.0,
                        ride.avgSpeedKmh
                    )
                }
            } else {
                GpxParser.fromCoordinates(coords, ride.elevationGain ?: 0.0, ride.avgSpeedKmh)
            }

            _uiState.update {
                it.copy(
                    profilePoints = parsedPoints,
                    isLoadingProfile = false
                )
            }
        }
    }

    fun shareGpx() {
        val currentRide = _uiState.value.ride ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            val result = rideRepository.getGpxContent(rideId)
            _uiState.update { it.copy(isSharing = false) }

            if (result.isSuccess) {
                val gpxContent = result.getOrNull() ?: return@launch
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val fileName = "${dateFormat.format(currentRide.startTime)}.gpx"

                val file = GpxGenerator.saveToCache(context.cacheDir, fileName, gpxContent)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/gpx+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Plik GPX z treningu")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Udostępnij plik GPX").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Nie udało się załadować pliku GPX: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun deleteRide(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = rideRepository.deleteRide(rideId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isDeleted = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Nie udało się usunąć treningu: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun updateRideTitle(newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val result = rideRepository.updateRideTitle(rideId, trimmed)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        ride = state.ride?.copy(title = trimmed)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Nie udało się zaktualizować nazwy treningu: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
