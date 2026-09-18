package com.biketracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.biketracker.data.repository.AuthRepository
import com.biketracker.ui.navigation.NavGraph
import com.biketracker.ui.navigation.Screen
import com.biketracker.ui.theme.BikeTrackerTheme
import com.biketracker.ui.theme.DarkBackground
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import com.biketracker.service.LocationTrackingService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private var onNewIntentAction: (() -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onNewIntentAction?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (authRepository.currentUser != null) {
            if (LocationTrackingService.isTracking.value) {
                Screen.Tracking.route
            } else {
                Screen.Home.route
            }
        } else {
            Screen.Login.route
        }

        setContent {
            BikeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        onNewIntentAction = {
                            if (LocationTrackingService.isTracking.value) {
                                navController.navigate(Screen.Tracking.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
