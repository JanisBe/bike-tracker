package com.biketracker.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Tracking : Screen("tracking")
    data object RideDetail : Screen("ride_detail/{rideId}") {
        fun createRoute(rideId: String) = "ride_detail/$rideId"
    }
}
