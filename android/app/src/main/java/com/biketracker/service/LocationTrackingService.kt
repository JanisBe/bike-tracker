package com.biketracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.GnssStatus
import android.location.Location
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
import com.biketracker.data.model.ActivityType
import com.biketracker.data.model.SatelliteInfo
import com.biketracker.data.model.TrackPoint
import com.biketracker.data.repository.RideRepository
import com.biketracker.data.repository.UserRepository
import com.biketracker.domain.util.CalorieCalculator
import com.biketracker.domain.util.DistanceCalculator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var rideRepository: RideRepository

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var isGnssRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var gpsAlertDismissJob: Job? = null
    private var hadGpsFix = false
    private var lastLocationTimeMs = 0L
    private var accumulatedCalories = 0.0


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
                if (used >= 4) {
                    checkPendingGpsFix()
                }
            } else if (_isTracking.value && !_isPaused.value && hadGpsFix) {
                checkGpsSignalStatus()
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_isPaused.value) return
            for (location in result.locations) {
                processLocation(location)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun checkPendingGpsFix() {
        if (!_isWaitingForGps.value) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _isWaitingForGps.value) {
                    val ageMs = System.currentTimeMillis() - loc.time
                    val satellitesUsed = _satelliteInfo.value.used
                    val isDirectGps = loc.provider?.equals(LocationManager.GPS_PROVIDER, ignoreCase = true) == true
                    val hasGenuineGpsFix = satellitesUsed >= 4 || (isDirectGps && loc.accuracy <= 20.0f)
                    if (ageMs < 10_000L && loc.accuracy <= 25.0f && hasGenuineGpsFix) {
                        processLocation(loc)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored if permission not ready
        }
    }

    private fun processLocation(location: Location) {
        if (_isPaused.value) return
        // Filter inaccurate readings
        if (location.accuracy > 25.0f) return

        val satellitesUsed = _satelliteInfo.value.used
        val isDirectGps = location.provider?.equals(LocationManager.GPS_PROVIDER, ignoreCase = true) == true
        val hasGenuineGpsFix = satellitesUsed >= 4 || (isDirectGps && location.accuracy <= 20.0f)

        lastLocationTimeMs = System.currentTimeMillis()

        if (_isWaitingForGps.value) {
            // While waiting for GPS signal, require genuine GPS fix (>=4 satellites or high-accuracy GPS provider)
            if (!hasGenuineGpsFix) {
                return
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

        val currentList = _trackPoints.value

        // Initial fix warmup: require good accuracy (<= 20m) for the initial anchor point
        if (currentList.isEmpty() && location.accuracy > 20.0f) {
            return
        }

        // When starting from standstill, anchor the first point with zero speed
        if (currentList.isEmpty()) {
            _currentSpeedKmh.value = 0.0
            val initialPoint = TrackPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                elevation = if (location.hasAltitude()) location.altitude else null,
                timestamp = location.time,
                accuracy = location.accuracy,
                speedKmh = 0.0
            )
            _trackPoints.value = listOf(initialPoint)
            updateNotification()
            return
        }

        // Check if speed reading is noisy or unreliable (Android 8.0+)
        val isSpeedNoisy =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
                location.hasSpeed() && (
                        (location.speedAccuracyMetersPerSecond > 0 && location.speed < location.speedAccuracyMetersPerSecond) ||
                                location.speedAccuracyMetersPerSecond > MAX_ACCEPTABLE_SPEED_ACCURACY_MPS
                        )
            } else false

        val rawSpeedKmh = if (location.hasSpeed() && !isSpeedNoisy) {
            (location.speed * 3.6).coerceAtLeast(0.0)
        } else null

        val lastPoint = currentList.last()
        val distMeters = DistanceCalculator.distanceBetweenMeters(
            lastPoint.latitude, lastPoint.longitude,
            location.latitude, location.longitude
        )
        val timeDiffSec = (location.time - lastPoint.timestamp) / 1000.0

        // If provider did not supply speed, estimate from distance to previous point
        val estimatedSpeedKmh = if (rawSpeedKmh == null && timeDiffSec > 0.5) {
            (distMeters / timeDiffSec) * 3.6
        } else null

        var candidateSpeedKmh = rawSpeedKmh ?: estimatedSpeedKmh ?: 0.0

        // Anti-jump filter for stationary state and initial GPS fix jumps:
        val lastSpeedKmh = lastPoint.speedKmh ?: 0.0
        val isPreviouslyStationary = lastSpeedKmh == 0.0

        if (isPreviouslyStationary) {
            // 1. If previously stationary, movement distance must exceed GPS accuracy noise radius
            // unless genuine high-accuracy sensor speed confirms motion.
            val minSignificantMoveMeters = ((location.accuracy + (lastPoint.accuracy ?: location.accuracy)) * 0.45f)
                .coerceIn(5.0f, 20.0f)
            val hasValidSensorSpeed = rawSpeedKmh != null && rawSpeedKmh >= MIN_SPEED_THRESHOLD_KMH

            if (distMeters < minSignificantMoveMeters && !hasValidSensorSpeed) {
                candidateSpeedKmh = 0.0
            }

            // 2. Physical acceleration check from standstill:
            // A human/cyclist cannot accelerate faster than ~12 km/h per second from 0.
            if (timeDiffSec in 0.1..5.0) {
                val maxPlausibleStartSpeedKmh = (MAX_START_ACCELERATION_KMH_PER_SEC * timeDiffSec).coerceAtLeast(15.0)
                if (candidateSpeedKmh > maxPlausibleStartSpeedKmh && !hasValidSensorSpeed) {
                    candidateSpeedKmh = 0.0
                }
            }
        }

        // Cap extreme GPS spikes based on activity type
        val maxActivitySpeedKmh = if (_activityType.value == ActivityType.WALKING.id) {
            MAX_WALKING_SPEED_KMH
        } else {
            MAX_CYCLING_SPEED_KMH
        }
        candidateSpeedKmh = candidateSpeedKmh.coerceAtMost(maxActivitySpeedKmh)

        // Zero speed below activity threshold or when measurement is noisy
        val thresholdKmh = userRepository.getSpeedThreshold(_activityType.value)
        val filteredSpeedKmh = if (candidateSpeedKmh < thresholdKmh || isSpeedNoisy) {
            0.0
        } else {
            candidateSpeedKmh
        }

        _currentSpeedKmh.value = filteredSpeedKmh

        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            elevation = if (location.hasAltitude()) location.altitude else null,
            timestamp = location.time,
            accuracy = location.accuracy,
            speedKmh = filteredSpeedKmh
        )

        val isStationary = filteredSpeedKmh == 0.0

        if (isStationary) {
            // Blokada naliczania fałszywego dystansu na postoju:
            // 1. Dystans całkowity NIE jest zwiększany.
            // 2. Jeśli poprzedni punkt był w ruchu (speed > 0), zapisujemy jeden punkt oznaczający zatrzymanie.
            // 3. Kolejne punkty postoju są ignorowane, aby uniknąć pętli/plątaniny dryfu GPS na mapie.
            if (lastPoint.speedKmh != null && lastPoint.speedKmh > 0.0) {
                _trackPoints.value = currentList + point
            }
        } else {
            // Użytkownik w ruchu (prędkość >= próg): naliczamy dystans i rejestrujemy punkt
            if (distMeters >= MIN_MOVEMENT_DISTANCE_METERS) {
                _currentDistanceKm.value += (distMeters / 1000.0)
                _trackPoints.value = currentList + point
                updateAverageSpeed()

                if (point.elevation != null && lastPoint.elevation != null) {
                    val eleDiff = point.elevation - lastPoint.elevation
                    if (eleDiff > 1.0) {
                        val weightKg = userRepository.getUserWeightKg()
                        accumulatedCalories += CalorieCalculator.calculateElevationCalories(eleDiff, weightKg)
                        _caloriesBurned.value = kotlin.math.round(accumulatedCalories).toInt()
                    }
                }
            }
        }

        updateNotification()
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
                val type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: ActivityType.CYCLING.id
                _activityType.value = type
                startTracking(waitForGps)
            }

            ACTION_FORCE_START -> forceStartTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
            ACTION_STOP_AND_SAVE -> stopAndSaveTracking()
        }
        return START_STICKY
    }

    private fun stopAndSaveTracking() {
        val pointsToSave = _trackPoints.value
        val distanceKm = _currentDistanceKm.value
        val currentType = _activityType.value

        val caloriesToSave = _caloriesBurned.value
        accumulatedCalories = 0.0
        _caloriesBurned.value = 0

        _isTracking.value = false
        _isWaitingForGps.value = false
        _isPaused.value = false
        _currentSpeedKmh.value = 0.0
        _averageSpeedKmh.value = 0.0

        hadGpsFix = false
        lastLocationTimeMs = 0L
        _isGpsLost.value = false
        clearGpsLostNotification()

        timerJob?.cancel()
        unregisterGnssCallback()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _satelliteInfo.value = SatelliteInfo()

        if (pointsToSave.size < 2) {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val iconRes =
            if (currentType == ActivityType.WALKING.id) R.drawable.ic_walk else R.drawable.ic_bike
        val savingNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                if (currentType == ActivityType.WALKING.id) "🚶 Zapisywanie spaceru" else getString(
                    R.string.notification_title_active
                )
            )
            .setContentText("Zapisywanie...")
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, savingNotification)

        serviceScope.launch {
            try {
                val result = rideRepository.saveRide(pointsToSave, currentType, caloriesToSave)
                if (result.isSuccess) {
                    showRideSavedNotification(distanceKm, currentType)
                } else {
                    showRideSaveFailedNotification()
                }
            } catch (e: Exception) {
                showRideSaveFailedNotification()
            } finally {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun showRideSavedNotification(distanceKm: Double, currentType: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 3, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val workoutName = if (currentType == ActivityType.WALKING.id) "spaceru" else "treningu"
        val text = "Zapisano %.2f km $workoutName".format(distanceKm)
        val iconRes =
            if (currentType == ActivityType.WALKING.id) R.drawable.ic_walk else R.drawable.ic_bike
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_ride_saved_title))
            .setContentText(text)
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .setTimeoutAfter(8000L)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_SAVED_ID, notification)
    }

    private fun showRideSaveFailedNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_ride_save_failed))
            .setSmallIcon(R.drawable.ic_bike)
            .setAutoCancel(true)
            .setTimeoutAfter(8000L)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_SAVED_ID, notification)
    }

    private fun startTracking(waitForGps: Boolean = false) {
        if (_isTracking.value) return

        _trackPoints.value = emptyList()
        _currentDistanceKm.value = 0.0
        _currentSpeedKmh.value = 0.0
        _averageSpeedKmh.value = 0.0
        _elapsedSeconds.value = 0L
        accumulatedCalories = 0.0
        _caloriesBurned.value = 0
        _isTracking.value = true
        _isWaitingForGps.value = waitForGps
        _isPaused.value = false

        hadGpsFix = !waitForGps && _satelliteInfo.value.used >= 4
        lastLocationTimeMs = if (hadGpsFix) System.currentTimeMillis() else 0L
        _isGpsLost.value = false

        acquireWakeLock()

        val notification = buildTrackingNotification()
        startForeground(NOTIFICATION_ID, notification)

        registerGnssCallback()
        requestLocationUpdates()
        if (!waitForGps) {
            startTimer()
        } else {
            checkPendingGpsFix()
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
        _averageSpeedKmh.value = 0.0
        accumulatedCalories = 0.0
        _caloriesBurned.value = 0

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
            setMinUpdateDistanceMeters(0f)
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
                delay(1000L)
                if (_isTracking.value && !_isPaused.value) {
                    _elapsedSeconds.value += 1
                    updateAverageSpeed()

                    val weightKg = userRepository.getUserWeightKg()
                    val kcalThisSec = CalorieCalculator.calculateCaloriesPerSecond(
                        _activityType.value,
                        _currentSpeedKmh.value,
                        weightKg
                    )
                    accumulatedCalories += kcalThisSec
                    _caloriesBurned.value = kotlin.math.round(accumulatedCalories).toInt()

                    // Jeśli przez >5s nie otrzymano nowej lokalizacji na postoju, wyzeruj licznik prędkości
                    val now = System.currentTimeMillis()
                    if (lastLocationTimeMs > 0 && (now - lastLocationTimeMs) > 5000L && _currentSpeedKmh.value > 0.0) {
                        _currentSpeedKmh.value = 0.0
                    }
                    checkGpsSignalStatus()
                    updateNotification()
                }
            }
        }
    }

    private fun updateAverageSpeed() {
        val seconds = _elapsedSeconds.value
        val distance = _currentDistanceKm.value
        _averageSpeedKmh.value = if (seconds > 0) {
            distance / (seconds / 3600.0)
        } else {
            0.0
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
        } else if (!isSignalLostNow && !_isGpsLost.value && satellitesUsed >= 4 && (gpsAlertDismissJob == null || gpsAlertDismissJob?.isActive == false)) {
                clearGpsLostNotification()
        }
    }

    private fun handleGpsLost() {
        _isGpsLost.value = true
        if (userRepository.isGpsLostVibrationEnabled()) {
            vibrate(longArrayOf(0, 400, 200, 400))
        }
        if (userRepository.isGpsLostNotificationEnabled()) {
            showGpsLostNotification()
        }
        updateNotification()
    }

    private fun handleGpsRecovered() {
        _isGpsLost.value = false
        clearGpsLostNotification()
        if (userRepository.isGpsLostNotificationEnabled()) {
            showGpsRecoveredNotification()
        }
        if (userRepository.isGpsLostVibrationEnabled()) {
            vibrate(longArrayOf(0, 150))
        }
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
        gpsAlertDismissJob?.cancel()
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (userRepository.isGpsLostVibrationEnabled()) {
            GPS_ALERT_CHANNEL_ID
        } else {
            GPS_ALERT_SILENT_CHANNEL_ID
        }

        val notification = NotificationCompat.Builder(this, channelId)
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
        gpsAlertDismissJob?.cancel()
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(GPS_ALERT_NOTIFICATION_ID)
    }

    private fun showGpsRecoveredNotification() {
        gpsAlertDismissJob?.cancel()
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (userRepository.isGpsLostVibrationEnabled()) {
            GPS_ALERT_CHANNEL_ID
        } else {
            GPS_ALERT_SILENT_CHANNEL_ID
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.gps_recovered_title))
            .setContentText(getString(R.string.gps_recovered_desc))
            .setSmallIcon(R.drawable.ic_bike)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(3000L)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(GPS_ALERT_NOTIFICATION_ID, notification)

        // Aktywne wyczyszczenie powiadomienia po 3 sekundach
        gpsAlertDismissJob = serviceScope.launch {
            delay(3000L)
            clearGpsLostNotification()
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

        val alertSilentChannel = NotificationChannel(
            GPS_ALERT_SILENT_CHANNEL_ID,
            getString(R.string.gps_alert_channel_name) + " (bez wibracji)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.gps_alert_channel_desc)
            enableVibration(false)
            vibrationPattern = null
            setShowBadge(true)
        }
        manager.createNotificationChannel(alertSilentChannel)
    }

    private fun buildTrackingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isWalking = _activityType.value == ActivityType.WALKING.id
        val iconRes = if (isWalking) R.drawable.ic_walk else R.drawable.ic_bike
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (_isWaitingForGps.value) {
            val sats = _satelliteInfo.value
            val statusText = if (sats.total > 0) {
                "Satelity: ${sats.used}/${sats.total} • Oczekiwanie na sygnał GPS..."
            } else {
                "Szukanie sygnału GPS..."
            }

            builder.setContentTitle(getString(R.string.notification_title_waiting_gps))
                .setContentText(statusText)

            // Akcja: Wymuś start
            val forceStartIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_FORCE_START
            }
            val forceStartPendingIntent = PendingIntent.getService(
                this, 12, forceStartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_play_arrow,
                "Start teraz",
                forceStartPendingIntent
            )

            // Akcja: Zakończ
            val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 11, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_stop,
                getString(R.string.action_stop),
                stopPendingIntent
            )

            return builder.build()
        }

        // Aktywny lub wstrzymany trening
        val hours = _elapsedSeconds.value / 3600
        val minutes = (_elapsedSeconds.value % 3600) / 60
        val seconds = _elapsedSeconds.value % 60
        val timeStr = if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        val distStr = "%.2f km".format(_currentDistanceKm.value)
        val speedStr = "%.1f km/h".format(_currentSpeedKmh.value)
        val calStr = "${_caloriesBurned.value} kcal"

        val title = when {
            _isPaused.value -> if (isWalking) "⏸️ Spacer wstrzymany" else getString(R.string.notification_title_paused)
            _isGpsLost.value -> "⚠️ Utrata sygnału GPS"
            else -> if (isWalking) "🚶 Rejestrowanie spaceru" else getString(R.string.notification_title_active)
        }

        val contentText = when {
            _isPaused.value -> "Dystans: $distStr • Czas: $timeStr • $calStr"
            _isGpsLost.value -> "Dystans: $distStr • Czas: $timeStr • $calStr (szukanie GPS)"
            else -> "Dystans: $distStr • Czas: $timeStr ($speedStr) • $calStr"
        }

        builder.setContentTitle(title)
            .setContentText(contentText)

        // Akcja 1: Pauza / Wznów
        val pauseResumeIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = if (_isPaused.value) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            10,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeIcon = if (_isPaused.value) R.drawable.ic_play_arrow else R.drawable.ic_pause
        val pauseResumeTitle =
            if (_isPaused.value) getString(R.string.action_resume) else getString(R.string.action_pause)
        builder.addAction(pauseResumeIcon, pauseResumeTitle, pauseResumePendingIntent)

        // Akcja 2: Stop (Zakończ i zapisz)
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_AND_SAVE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            R.drawable.ic_stop,
            getString(R.string.action_stop),
            stopPendingIntent
        )

        return builder.build()
    }

    private fun updateNotification() {
        if (!_isTracking.value) return
        val notification = buildTrackingNotification()
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
        const val ACTION_STOP_AND_SAVE = "com.biketracker.action.STOP_AND_SAVE"

        const val EXTRA_WAIT_FOR_GPS = "extra_wait_for_gps"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        const val MIN_SPEED_THRESHOLD_KMH = 1.0
        const val MIN_MOVEMENT_DISTANCE_METERS = 1.0
        private const val MAX_ACCEPTABLE_SPEED_ACCURACY_MPS = 2.5f
        private const val MAX_START_ACCELERATION_KMH_PER_SEC = 12.0
        private const val MAX_CYCLING_SPEED_KMH = 85.0
        private const val MAX_WALKING_SPEED_KMH = 25.0

        private const val NOTIFICATION_ID = 101
        private const val NOTIFICATION_SAVED_ID = 103
        private const val CHANNEL_ID = "bike_tracking_channel"
        private const val GPS_ALERT_NOTIFICATION_ID = 102
        private const val GPS_ALERT_CHANNEL_ID = "bike_gps_alert_channel"
        private const val GPS_ALERT_SILENT_CHANNEL_ID = "bike_gps_alert_silent_channel"

        // Global state flows observable by UI ViewModels
        private val _activityType = MutableStateFlow(ActivityType.CYCLING.id)
        val activityType: StateFlow<String> = _activityType.asStateFlow()

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

        private val _averageSpeedKmh = MutableStateFlow(0.0)
        val averageSpeedKmh: StateFlow<Double> = _averageSpeedKmh.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _caloriesBurned = MutableStateFlow(0)
        val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()
    }
}
