package com.biketracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.biketracker.MainActivity
import com.biketracker.R
import com.biketracker.data.model.SatelliteInfo
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
    private lateinit var locationManager: LocationManager
    private var isGnssRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var hadGpsFix = false
    private var lastLocationTimeMs = 0L


    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val total = status.satelliteCount
            var used = 0
            for (i in 0 until total) {
                if (status.usedInFix(i)) {
                    used++
                }
            }
            _satelliteInfo.value = SatelliteInfo(used = used, total = total)
            if (_isWaitingForGps.value) {
                updateNotification()
            } else if (_isTracking.value && !_isPaused.value && hadGpsFix) {
                checkGpsSignalStatus()
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_isPaused.value) return

            for (location in result.locations) {
                // Filter inaccurate readings
                if (location.accuracy <= 25.0f) {
                    val satellitesUsed = _satelliteInfo.value.used
                    lastLocationTimeMs = System.currentTimeMillis()

                    if (_isWaitingForGps.value) {
                        // While waiting for GPS signal, we REQUIRE at least 4 satellites actually used in fix!
                        // Wi-Fi or cellular network fixes (where satellitesUsed < 4) must NOT trigger workout start.
                        if (satellitesUsed < 4) {
                            continue
                        }
                        _isWaitingForGps.value = false
                        hadGpsFix = true
                        startTimer()
                    } else {
                        hadGpsFix = true
                        if (_isGpsLost.value) {
                            handleGpsRecovered()
                        }
                    }

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
        locationManager = getSystemService(LocationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val waitForGps = intent.getBooleanExtra(EXTRA_WAIT_FOR_GPS, false)
                startTracking(waitForGps)
            }

            ACTION_FORCE_START -> forceStartTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking(waitForGps: Boolean = false) {
        if (_isTracking.value) return

        _trackPoints.value = emptyList()
        _currentDistanceKm.value = 0.0
        _currentSpeedKmh.value = 0.0
        _elapsedSeconds.value = 0L
        _isTracking.value = true
        _isWaitingForGps.value = waitForGps
        _isPaused.value = false

        hadGpsFix = !waitForGps && _satelliteInfo.value.used >= 4
        lastLocationTimeMs = if (hadGpsFix) System.currentTimeMillis() else 0L
        _isGpsLost.value = false

        acquireWakeLock()

        val initialSats = _satelliteInfo.value
        val notificationText = if (waitForGps) {
            if (initialSats.total > 0) {
                "Oczekiwanie na sygnał GPS (Satelity: ${initialSats.used}/${initialSats.total})..."
            } else {
                "Oczekiwanie na sygnał GPS..."
            }
        } else {
            "Starting GPS tracking..."
        }
        val notification = buildNotification(notificationText)
        startForeground(NOTIFICATION_ID, notification)

        registerGnssCallback()
        requestLocationUpdates()
        if (!waitForGps) {
            startTimer()
        }
    }

    private fun forceStartTracking() {
        if (_isTracking.value && _isWaitingForGps.value) {
            _isWaitingForGps.value = false
            hadGpsFix = _satelliteInfo.value.used >= 4
            lastLocationTimeMs = if (hadGpsFix) System.currentTimeMillis() else 0L
            startTimer()
            updateNotification()
        }
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
        _isWaitingForGps.value = false
        _isPaused.value = false
        _currentSpeedKmh.value = 0.0

        hadGpsFix = false
        lastLocationTimeMs = 0L
        _isGpsLost.value = false
        clearGpsLostNotification()

        timerJob?.cancel()
        unregisterGnssCallback()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _satelliteInfo.value = SatelliteInfo()

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }


    @Suppress("MissingPermission")
    private fun registerGnssCallback() {
        if (isGnssRegistered) return
        try {
            locationManager.registerGnssStatusCallback(
                gnssStatusCallback,
                Handler(Looper.getMainLooper())
            )
            isGnssRegistered = true
        } catch (e: Exception) {
            // Ignored if device does not support GNSS callback
        }
    }

    private fun unregisterGnssCallback() {
        if (!isGnssRegistered) return
        try {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {
            // Ignored if device does not support GNSS callback
        } finally {
            isGnssRegistered = false
        }
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
                    checkGpsSignalStatus()
                }
            }
        }
    }

    private fun checkGpsSignalStatus() {
        if (!_isTracking.value || _isWaitingForGps.value || _isPaused.value) return

        val now = System.currentTimeMillis()
        val satellitesUsed = _satelliteInfo.value.used
        val timeSinceLocation =
            if (lastLocationTimeMs > 0) now - lastLocationTimeMs else Long.MAX_VALUE

        // GPS is lost if we previously had a fix, but now have < 4 satellites OR no location received for > 8 seconds
        val isSignalLostNow =
            hadGpsFix && (satellitesUsed < 4 || (lastLocationTimeMs > 0 && timeSinceLocation > 8000L))

        if (isSignalLostNow && !_isGpsLost.value) {
            handleGpsLost()
        } else if (!isSignalLostNow && _isGpsLost.value && satellitesUsed >= 4 && timeSinceLocation <= 4000L) {
            handleGpsRecovered()
        }
    }

    private fun handleGpsLost() {
        _isGpsLost.value = true
        vibrate(longArrayOf(0, 400, 200, 400))
        showGpsLostNotification()
        updateNotification()
    }

    private fun handleGpsRecovered() {
        _isGpsLost.value = false
        clearGpsLostNotification()
        showGpsRecoveredNotification()
        vibrate(longArrayOf(0, 150))
        updateNotification()
    }

    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            // Ignored if device vibrator is unavailable
        }
    }

    private fun showGpsLostNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, GPS_ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.gps_lost_title))
            .setContentText(getString(R.string.gps_lost_desc))
            .setSmallIcon(R.drawable.ic_bike)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(GPS_ALERT_NOTIFICATION_ID, notification)
    }

    private fun clearGpsLostNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(GPS_ALERT_NOTIFICATION_ID)
    }

    private fun showGpsRecoveredNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, GPS_ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.gps_recovered_title))
            .setContentText(getString(R.string.gps_recovered_desc))
            .setSmallIcon(R.drawable.ic_bike)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(6000L)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(GPS_ALERT_NOTIFICATION_ID, notification)
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
        val manager = getSystemService(NotificationManager::class.java)

        val trackingChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(trackingChannel)

        val alertChannel = NotificationChannel(
            GPS_ALERT_CHANNEL_ID,
            getString(R.string.gps_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.gps_alert_channel_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            setShowBadge(true)
        }
        manager.createNotificationChannel(alertChannel)
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
        if (_isWaitingForGps.value) {
            val sats = _satelliteInfo.value
            val text = if (sats.total > 0) {
                "Oczekiwanie na sygnał GPS (Satelity: ${sats.used}/${sats.total})..."
            } else {
                "Oczekiwanie na sygnał GPS..."
            }
            val notification = buildNotification(text)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
            return
        }

        val dist = "%.2f km".format(_currentDistanceKm.value)
        val speed = "%.1f km/h".format(_currentSpeedKmh.value)
        val minutes = _elapsedSeconds.value / 60
        val seconds = _elapsedSeconds.value % 60
        val time = "%02d:%02d".format(minutes, seconds)

        val status =
            if (_isPaused.value) "WSTRZYMANY" else if (_isGpsLost.value) "BRAK GPS" else "AKTYWNY"
        val text = if (_isGpsLost.value) {
            "[$status] $dist • $time • Szukanie satelitów..."
        } else {
            "[$status] $dist • $time • $speed"
        }

        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearGpsLostNotification()
        unregisterGnssCallback()
        releaseWakeLock()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.biketracker.action.START"
        const val ACTION_FORCE_START = "com.biketracker.action.FORCE_START"
        const val ACTION_PAUSE = "com.biketracker.action.PAUSE"
        const val ACTION_RESUME = "com.biketracker.action.RESUME"
        const val ACTION_STOP = "com.biketracker.action.STOP"

        const val EXTRA_WAIT_FOR_GPS = "extra_wait_for_gps"

        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "bike_tracking_channel"
        private const val GPS_ALERT_NOTIFICATION_ID = 102
        private const val GPS_ALERT_CHANNEL_ID = "bike_gps_alert_channel"

        // Global state flows observable by UI ViewModels
        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _isWaitingForGps = MutableStateFlow(false)
        val isWaitingForGps: StateFlow<Boolean> = _isWaitingForGps.asStateFlow()

        private val _isGpsLost = MutableStateFlow(false)
        val isGpsLost: StateFlow<Boolean> = _isGpsLost.asStateFlow()


        private val _satelliteInfo = MutableStateFlow(SatelliteInfo())
        val satelliteInfo: StateFlow<SatelliteInfo> = _satelliteInfo.asStateFlow()

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
