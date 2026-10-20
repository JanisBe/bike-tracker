package com.biketracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import com.biketracker.data.model.ActivityType
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.PowerSaveRepository
import com.biketracker.service.LocationTrackingService
import com.biketracker.ui.components.RestorePowerSaveDialog
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

    @Inject
    lateinit var powerSaveRepository: PowerSaveRepository

    private var onNewIntentAction: ((Intent) -> Unit)? = null

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "activityType"
    }

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
                    val shouldPromptRestorePowerSave by powerSaveRepository.shouldPromptRestoreFlow.collectAsStateWithLifecycle()

                    fun handleIntent(targetIntent: Intent?) {
                        if (targetIntent == null) return

                        val shortcutActivityType = targetIntent.getStringExtra(EXTRA_ACTIVITY_TYPE)
                            ?: targetIntent.data?.let { uri ->
                                if (uri.scheme == "biketracker" && (uri.host == "track" || uri.host == "tracking")) {
                                    uri.getQueryParameter("activityType")
                                } else null
                            }

                        if (LocationTrackingService.isTracking.value) {
                            navController.navigate(Screen.Tracking.route) {
                                launchSingleTop = true
                            }
                            if (!shortcutActivityType.isNullOrBlank()) {
                                targetIntent.removeExtra(EXTRA_ACTIVITY_TYPE)
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.toast_tracking_already_active),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return
                        }

                        if (!shortcutActivityType.isNullOrBlank()) {
                            targetIntent.removeExtra(EXTRA_ACTIVITY_TYPE)
                            if (targetIntent.data?.scheme == "biketracker" && (targetIntent.data?.host == "track" || targetIntent.data?.host == "tracking")) {
                                targetIntent.data = null
                            }
                            if (authRepository.currentUser != null) {
                                val validatedType = ActivityType.fromId(shortcutActivityType).id
                                navController.navigate(Screen.Tracking.createRoute(validatedType)) {
                                    popUpTo(Screen.Home.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
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
                                targetIntent.data = null
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

                    if (shouldPromptRestorePowerSave) {
                        RestorePowerSaveDialog(
                            onRestore = {
                                powerSaveRepository.dismissRestorePrompt()
                                try {
                                    startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                                } catch (e: Exception) {
                                    try {
                                        startActivity(Intent(Settings.ACTION_SETTINGS))
                                    } catch (ignored: Exception) {}
                                }
                            },
                            onDismiss = {
                                powerSaveRepository.dismissRestorePrompt()
                            }
                        )
                    }
                }
            }
        }
    }
}
