package com.biketracker.ui.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biketracker.data.model.SignalQuality
import com.biketracker.data.model.TrackPoint
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.DarkSurfaceVariant
import com.biketracker.ui.theme.ErrorRed
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.SuccessGreen
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onNavigateBack: () -> Unit,
    onRideSaved: (String) -> Unit,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val isWaitingForGps by viewModel.isWaitingForGps.collectAsStateWithLifecycle()
    val satelliteInfo by viewModel.satelliteInfo.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val currentDistanceKm by viewModel.currentDistanceKm.collectAsStateWithLifecycle()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showFinishDialog by remember { mutableStateOf(false) }
    var showGpsWarningDialog by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToRequest = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = fineLocationGranted
    }

    var hasEverTracked by remember { mutableStateOf(isTracking) }
    var hasInitiatedStart by remember { mutableStateOf(false) }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            hasEverTracked = true
        } else if (hasEverTracked && !uiState.isSaving && uiState.savedRideId == null) {
            // Śledzenie zakończone zewnętrznie (np. przyciskiem Stop w powiadomieniu)
            onNavigateBack()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(permissionsToRequest)
        } else if (!isTracking && !hasInitiatedStart && !hasEverTracked) {
            hasInitiatedStart = true
            val hasFix = viewModel.checkHasGpsFix()
            if (hasFix) {
                viewModel.startTracking(waitForGps = false)
            } else {
                showGpsWarningDialog = true
            }
        }
    }

    LaunchedEffect(uiState.savedRideId) {
        uiState.savedRideId?.let { rideId ->
            onRideSaved(rideId)
        }
    }

    BackHandler(enabled = isTracking) {
        showFinishDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isWaitingForGps -> "Oczekiwanie na GPS..."
                            isPaused -> "Trening wstrzymany"
                            else -> "Rejestrowanie treningu"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isWaitingForGps -> OrangeAccent
                            isPaused -> TealAccent
                            else -> OrangeAccent
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isTracking) {
                            showFinishDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    val iconColor = when (satelliteInfo.signalQuality) {
                        SignalQuality.EXCELLENT, SignalQuality.GOOD -> SuccessGreen
                        SignalQuality.POOR -> OrangeAccent
                        SignalQuality.NONE -> TextSecondary
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Satelity GPS",
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${satelliteInfo.used}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                        if (satelliteInfo.total > 0) {
                            Text(
                                text = "/${satelliteInfo.total}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Map View (Top 55%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                ) {
                    if (hasLocationPermission) {
                        TrackingOsmMapView(trackPoints = trackPoints)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Uprawnienie do lokalizacji jest wymagane, aby rejestrować trening",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Live Stats & Controls Panel (Bottom 45%)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .border(
                            1.dp,
                            CardBorder,
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // GPS Waiting Banner
                        if (isWaitingForGps) {
                            val qualityBadgeText = when (satelliteInfo.signalQuality) {
                                SignalQuality.EXCELLENT -> "Sygnał idealny"
                                SignalQuality.GOOD -> "Sygnał dobry"
                                SignalQuality.POOR -> "Sygnał słaby"
                                SignalQuality.NONE -> "Brak fix-a"
                            }
                            val qualityColor = when (satelliteInfo.signalQuality) {
                                SignalQuality.EXCELLENT, SignalQuality.GOOD -> SuccessGreen
                                SignalQuality.POOR -> OrangeAccent
                                SignalQuality.NONE -> TextSecondary
                            }
                            val satelliteTitle = if (satelliteInfo.total > 0) {
                                "Satelity: ${satelliteInfo.used}/${satelliteInfo.total}"
                            } else {
                                "Szukanie satelitów GPS..."
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Row 1: Icon/Spinner + Satellite title + Quality Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            if (satelliteInfo.isGoodSignal) {
                                                Icon(
                                                    imageVector = Icons.Default.Sensors,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = OrangeAccent,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = satelliteTitle,
                                                fontWeight = FontWeight.Bold,
                                                color = if (satelliteInfo.isGoodSignal) SuccessGreen else OrangeAccent,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Surface(
                                            color = qualityColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = qualityBadgeText,
                                                color = qualityColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 3.dp
                                                )
                                            )
                                        }
                                    }

                                    // Row 2: Subtitle guidance + "Start teraz" button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when {
                                                satelliteInfo.isGoodSignal -> "Stabilny sygnał – czekam na fix..."
                                                satelliteInfo.used in 4..6 -> "Wykryto ${satelliteInfo.used} satelity – stabilizacja..."
                                                else -> "Start po wykryciu min. 4 satelitów"
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        TextButton(
                                            onClick = { viewModel.forceStartTracking() },
                                            contentPadding = PaddingValues(
                                                horizontal = 8.dp,
                                                vertical = 0.dp
                                            ),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = "Start teraz",
                                                color = OrangeAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Main Speed Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "%.1f".format(currentSpeedKmh),
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "km/h",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangeAccent,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        // Secondary Stats Row (Distance & Time)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val minutes = elapsedSeconds / 60
                            val seconds = elapsedSeconds % 60
                            val durationStr = "%02d:%02d".format(minutes, seconds)

                            LiveStatItem(
                                icon = Icons.Default.Straighten,
                                label = "Dystans",
                                value = "%.2f km".format(currentDistanceKm),
                                tint = OrangeAccent
                            )
                            LiveStatItem(
                                icon = Icons.Default.Schedule,
                                label = "Czas",
                                value = durationStr,
                                tint = TealAccent
                            )
                            LiveStatItem(
                                icon = Icons.Default.Speed,
                                label = "Punkty",
                                value = "${trackPoints.size}",
                                tint = TextSecondary
                            )
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause / Resume Toggle
                            FilledIconButton(
                                onClick = {
                                    if (isPaused) viewModel.resumeTracking() else viewModel.pauseTracking()
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isPaused) SuccessGreen else OrangeAccent
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Wznów" else "Wstrzymaj",
                                    tint = DarkBackground,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Finish & Save Ride Button
                            Button(
                                onClick = { showFinishDialog = true },
                                modifier = Modifier
                                    .height(56.dp)
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = DarkBackground
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zakończ i zapisz",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Saving Overlay
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = OrangeAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Zapisywanie treningu i generowanie GPX...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // GPS Warning Dialog
        if (showGpsWarningDialog) {
            val isLocationEnabled = viewModel.isLocationServiceEnabled()
            AlertDialog(
                onDismissRequest = {
                    showGpsWarningDialog = false
                    onNavigateBack()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Brak sygnału GPS",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = if (!isLocationEnabled) {
                            "Lokalizacja w urządzeniu jest wyłączona. Włącz usługi lokalizacji w telefonie, aby poprawnie rejestrować trasę treningu."
                        } else {
                            "Brak sygnału GPS. Czy chcesz kontynuować?"
                        },
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                },
                confirmButton = {
                    if (!isLocationEnabled) {
                        Button(
                            onClick = {
                                showGpsWarningDialog = false
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Włącz GPS w ustawieniach",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Option 3: Czekaj na sygnał GPS
                            Button(
                                onClick = {
                                    showGpsWarningDialog = false
                                    viewModel.startTracking(waitForGps = true)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Czekaj na sygnał GPS",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Option 1: Tak (Rozpocznij mimo to)
                            Button(
                                onClick = {
                                    showGpsWarningDialog = false
                                    viewModel.startTracking(waitForGps = false)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Tak, rozpocznij mimo to",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Option 2: Nie (Powrót do ekranu głównego)
                            TextButton(
                                onClick = {
                                    showGpsWarningDialog = false
                                    onNavigateBack()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nie (wróć)",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                dismissButton = if (!isLocationEnabled) {
                    {
                        TextButton(onClick = {
                            showGpsWarningDialog = false
                            onNavigateBack()
                        }) {
                            Text("Anuluj", color = TextSecondary)
                        }
                    }
                } else null,
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Finish Confirmation Dialog
        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text(text = "Zakończyć trening?", color = TextPrimary) },
                text = {
                    Text(
                        text = if (trackPoints.size < 2) {
                            "Zarejestrowano mniej niż 2 punkty GPS. Czy chcesz zakończyć bez zapisywania, czy kontynuować jazdę?"
                        } else {
                            "Czy na pewno chcesz zakończyć rejestrowanie i zapisać ten trening w Firebase?"
                        },
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    if (trackPoints.size >= 2) {
                        Button(
                            onClick = {
                                showFinishDialog = false
                                viewModel.stopAndSaveRide()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                        ) {
                            Text(
                                text = "Zapisz",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                showFinishDialog = false
                                viewModel.stopTrackingWithoutSaving()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text(
                                text = "Zakończ bez zapisywania",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trackPoints.size >= 2) {
                            TextButton(
                                onClick = {
                                    showFinishDialog = false
                                    viewModel.stopTrackingWithoutSaving()
                                    onNavigateBack()
                                }
                            ) {
                                Text(text = "Odrzuć", color = ErrorRed)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = { showFinishDialog = false }) {
                            Text(text = "Kontynuuj jazdę", color = TextSecondary)
                        }
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Error Dialog
        uiState.saveError?.let { err ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(text = "Uwaga", color = TextPrimary) },
                text = { Text(text = err, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearError() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("OK", color = DarkBackground)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
fun LiveStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun TrackingOsmMapView(
    trackPoints: List<TrackPoint>
) {
    val polylineColor = OrangeAccent.toArgb()

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(17.0)
                isTilesScaledToDpi = true
            }
        },
        update = { mapView ->
            if (trackPoints.isNotEmpty()) {
                mapView.overlays.removeAll { it is Polyline }

                val geoPoints = trackPoints.map { GeoPoint(it.latitude, it.longitude) }
                val polyline = Polyline(mapView).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = polylineColor
                    outlinePaint.strokeWidth = 12f
                }
                mapView.overlays.add(polyline)

                val latest = geoPoints.last()
                mapView.controller.animateTo(latest)
                mapView.invalidate()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
