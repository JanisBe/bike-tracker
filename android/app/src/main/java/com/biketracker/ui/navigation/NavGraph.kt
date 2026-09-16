package com.biketracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.biketracker.ui.detail.RideDetailScreen
import com.biketracker.ui.home.HomeScreen
import com.biketracker.ui.login.LoginScreen
import com.biketracker.ui.tracking.TrackingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onStartRide = {
                    navController.navigate(Screen.Tracking.route)
                },
                onRideSelected = { rideId ->
                    navController.navigate(Screen.RideDetail.createRoute(rideId))
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Tracking.route) {
            TrackingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRideSaved = { rideId ->
                    navController.navigate(Screen.RideDetail.createRoute(rideId)) {
                        popUpTo(Screen.Tracking.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RideDetail.route,
            arguments = listOf(
                navArgument("rideId") { type = NavType.StringType }
            )
        ) {
            RideDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
