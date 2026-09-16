package com.biketracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.biketracker.MainActivity
import com.biketracker.R
import com.biketracker.data.model.TrackPoint
import com.biketracker.domain.util.DistanceCalculator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_isPaused.value) return

            for (location in result.locations) {
                // Filter inaccurate readings
                if (location.accuracy <= 25.0f) {
                    val speedKmh = if (location.hasSpeed()) {
                        (location.speed * 3.6).coerceAtLeast(0.0)
                    } else null

                    val point = TrackPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        elevation = if (location.hasAltitude()) location.altitude else null,
                        timestamp = location.time,
                        accuracy = location.accuracy,
                        speedKmh = speedKmh
                    )

                    val currentList = _trackPoints.value
                    if (currentList.isNotEmpty()) {
                        val lastPoint = currentList.last()
                        val distMeters = DistanceCalculator.distanceBetweenMeters(
                            lastPoint.latitude, lastPoint.longitude,
                            point.latitude, point.longitude
                        )
                        _currentDistanceKm.value += (distMeters / 1000.0)
                    }

                    if (speedKmh != null) {
                        _currentSpeedKmh.value = speedKmh
                    }

                    _trackPoints.value = currentList + point
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_isTracking.value) return

        _trackPoints.value = emptyList()
        _currentDistanceKm.value = 0.0
        _currentSpeedKmh.value = 0.0
        _elapsedSeconds.value = 0L
        _isTracking.value = true
        _isPaused.value = false

        acquireWakeLock()

        val notification = buildNotification("Starting GPS tracking...")
        startForeground(NOTIFICATION_ID, notification)

        requestLocationUpdates()
        startTimer()
    }

    private fun pauseTracking() {
        _isPaused.value = true
        _currentSpeedKmh.value = 0.0
        updateNotification()
    }

    private fun resumeTracking() {
        _isPaused.value = false
        updateNotification()
    }

    private fun stopTracking() {
        _isTracking.value = false
        _isPaused.value = false
        _currentSpeedKmh.value = 0.0

        timerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3_000L // update every 3 seconds
        ).apply {
            setMinUpdateDistanceMeters(5f) // or 5 meters
            setMinUpdateIntervalMillis(1_500L)
            setWaitForAccurateLocation(true)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                if (_isTracking.value && !_isPaused.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BikeTracker:LocationTrackingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // 4 hours timeout max
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bike)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun updateNotification() {
        val dist = "%.2f km".format(_currentDistanceKm.value)
        val speed = "%.1f km/h".format(_currentSpeedKmh.value)
        val minutes = _elapsedSeconds.value / 60
        val seconds = _elapsedSeconds.value % 60
        val time = "%02d:%02d".format(minutes, seconds)

        val status = if (_isPaused.value) "PAUSED" else "ACTIVE"
        val text = "[$status] $dist • $time • $speed"

        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.biketracker.action.START"
        const val ACTION_PAUSE = "com.biketracker.action.PAUSE"
        const val ACTION_RESUME = "com.biketracker.action.RESUME"
        const val ACTION_STOP = "com.biketracker.action.STOP"

        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "bike_tracking_channel"

        // Global state flows observable by UI ViewModels
        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
        val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

        private val _currentDistanceKm = MutableStateFlow(0.0)
        val currentDistanceKm: StateFlow<Double> = _currentDistanceKm.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0.0)
        val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()
    }
}
