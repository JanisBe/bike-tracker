package com.biketracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.Ride
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }
}
