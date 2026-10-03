package com.biketracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.biketracker.data.repository.AuthRepository
import com.biketracker.service.LocationTrackingService
import com.biketracker.ui.navigation.NavGraph
import com.biketracker.ui.navigation.Screen
import com.biketracker.ui.theme.BikeTrackerTheme
import com.biketracker.ui.theme.DarkBackground
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private var onNewIntentAction: ((Intent) -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onNewIntentAction?.invoke(intent)
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

                    fun handleIntent(targetIntent: Intent?) {
                        if (targetIntent == null) return

                        if (LocationTrackingService.isTracking.value) {
                            navController.navigate(Screen.Tracking.route) {
                                launchSingleTop = true
                            }
                            return
                        }

                        val data = targetIntent.data
                        if (data != null) {
                            val fragment = data.fragment
                            val hashRideId = if (!fragment.isNullOrBlank()) {
                                val match = Regex("""(?:ride=|\/ride\/)([^&]+)""").find(fragment)
                                match?.groupValues?.getOrNull(1)
                            } else null

                            val rideId = data.getQueryParameter("ride")
                                ?: hashRideId
                                ?: data.lastPathSegment?.takeIf { it != "ride" && it != "bike-tracker" }

                            if (!rideId.isNullOrBlank()) {
                                navController.navigate(Screen.RideDetail.createRoute(rideId)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        handleIntent(intent)
                        onNewIntentAction = { newIntent ->
                            handleIntent(newIntent)
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
