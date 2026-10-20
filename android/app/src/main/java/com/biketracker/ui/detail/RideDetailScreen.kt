package com.biketracker.ui.detail

import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biketracker.data.model.ActivityType
import com.biketracker.data.model.RouteProfilePoint
import com.biketracker.domain.util.CalorieCalculator
import com.biketracker.ui.map.MapBaseSource
import com.biketracker.ui.map.MapLayersBottomSheet
import com.biketracker.ui.map.MapOverlaySource
import com.biketracker.ui.map.applyMapConfiguration
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.ErrorRed
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.SuccessGreen
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf("") }
    var selectedProfilePoint by remember { mutableStateOf<RouteProfilePoint?>(null) }
    var isMapFullscreen by rememberSaveable { mutableStateOf(false) }
    var showPaceInDetail by rememberSaveable { mutableStateOf(false) }
    val baseSource by viewModel.mapBaseSource.collectAsStateWithLifecycle()
    val activeOverlays by viewModel.mapActiveOverlays.collectAsStateWithLifecycle()
    var showMapLayersSheet by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Szczegóły treningu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Wróć",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.shareRideLink() }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Udostępnij link do treningu",
                                tint = OrangeAccent
                            )
                        }
                        if (uiState.isOwner) {
                            IconButton(onClick = {
                                val ride = uiState.ride
                                editTitleText = ride?.title?.ifBlank { ride.locationName } ?: ""
                                showEditTitleDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edytuj nazwę treningu",
                                    tint = OrangeAccent
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Usuń",
                                    tint = ErrorRed
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.importSharedRide {
                                        Toast.makeText(
                                            context,
                                            "Trening został zaimportowany!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = !uiState.isImporting
                            ) {
                                if (uiState.isImporting) {
                                    CircularProgressIndicator(
                                        color = SuccessGreen,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Zapisz w moich treningach",
                                        tint = SuccessGreen
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangeAccent)
                }
            } else {
                val ride = uiState.ride
                if (ride == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nie znaleziono treningu", color = TextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Map View Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clipToBounds()
                        ) {
                            DetailOsmMapView(
                                coordinates = uiState.routeCoordinates,
                                baseSource = baseSource,
                                activeOverlays = activeOverlays,
                                selectedPoint = selectedProfilePoint?.let {
                                    Pair(
                                        it.latitude,
                                        it.longitude
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            )

                            // Subtle bottom gradient shadow overlay for smooth transition into dark content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                DarkBackground
                                            )
                                        )
                                    )
                            )

                            // Map Layer Selector Button
                            IconButton(
                                onClick = { showMapLayersSheet = true },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                                    .border(1.dp, CardBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Źródła mapy i szlaki",
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { isMapFullscreen = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                                    .border(1.dp, CardBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Pełny ekran",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                    // Content details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground)
                                .padding(16.dp)
                        ) {
                        val isWalking = ride.activityType == ActivityType.WALKING.id
                        val defaultTitle = if (isWalking) "Spacer" else "Trening rowerowy"
                        val activityIcon =
                            if (isWalking) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.AutoMirrored.Filled.DirectionsBike
                        val activityColor = if (isWalking) TealAccent else OrangeAccent
                        val displayTitle =
                            ride.title.ifBlank { ride.locationName.ifBlank { defaultTitle } }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = activityIcon,
                                contentDescription = null,
                                tint = activityColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                            if (ride.locationName.isNotBlank() && ride.locationName != displayTitle && !displayTitle.startsWith(
                                    ride.locationName
                                )
                            ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ride.locationName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val dateFormat =
                            SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", Locale.forLanguageTag("pl-PL"))
                        Text(
                            text = dateFormat.format(ride.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (!uiState.isOwner) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Udostępniony trening",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Elevation & Speed Chart
                        if (uiState.profilePoints.isNotEmpty()) {
                            RideProfileChart(
                                points = uiState.profilePoints,
                                onPointSelected = { pt ->
                                    selectedProfilePoint = pt
                                }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Big Stats Cards
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DetailMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Dystans",
                                value = "%.2f km".format(ride.distanceKm),
                                icon = Icons.Default.Straighten,
                                tint = OrangeAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            val minutes = ride.durationSeconds / 60
                            val seconds = ride.durationSeconds % 60
                            DetailMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Czas trwania",
                                value = "%02d:%02d".format(minutes, seconds),
                                icon = Icons.Default.Schedule,
                                tint = TealAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            val avgSpeedOrPaceValue = if (isWalking && showPaceInDetail) {
                                if (ride.avgSpeedKmh > 0.5) {
                                    val paceSec = (3600.0 / ride.avgSpeedKmh).roundToLong()
                                    "%d:%02d min/km".format(paceSec / 60, paceSec % 60)
                                } else "--:--"
                            } else {
                                "%.1f km/h".format(ride.avgSpeedKmh)
                            }
                            val avgSpeedOrPaceLabel = if (isWalking) {
                                if (showPaceInDetail) "Śr. tempo (stuknij: km/h)" else "Śr. prędkość (stuknij: tempo)"
                            } else {
                                "Śr. prędkość"
                            }

                            DetailMetricCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isWalking) {
                                            Modifier.clickable {
                                                showPaceInDetail = !showPaceInDetail
                                            }
                                        } else Modifier
                                    ),
                                label = avgSpeedOrPaceLabel,
                                value = avgSpeedOrPaceValue,
                                icon = Icons.Default.Speed,
                                tint = activityColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            DetailMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Maks. prędkość",
                                value = "%.1f km/h".format(ride.maxSpeedKmh),
                                icon = Icons.Default.Speed,
                                tint = TextPrimary
                            )
                        }

                            Spacer(modifier = Modifier.height(12.dp))
                            val caloriesKcal = ride.caloriesKcal ?: CalorieCalculator.calculateCalories(
                                activityType = ride.activityType,
                                avgSpeedKmh = ride.avgSpeedKmh,
                                durationSeconds = ride.durationSeconds,
                                elevationGainMeters = ride.elevationGain
                            )

                            if (ride.elevationGain != null) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "Spalone kalorie",
                                        value = "$caloriesKcal kcal",
                                        icon = Icons.Default.LocalFireDepartment,
                                        tint = OrangeAccent
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "Przewyższenie",
                                        value = "↑ %.0f m".format(ride.elevationGain),
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        tint = TealAccent
                                    )
                                }
                            } else {
                                DetailMetricCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "Spalone kalorie",
                                    value = "$caloriesKcal kcal",
                                    icon = Icons.Default.LocalFireDepartment,
                                    tint = OrangeAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!uiState.isOwner) {
                            Button(
                                onClick = {
                                    viewModel.importSharedRide {
                                        Toast.makeText(
                                            context,
                                            "Trening został pomyślnie zaimportowany!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                enabled = !uiState.isImporting
                            ) {
                                if (uiState.isImporting) {
                                    CircularProgressIndicator(
                                        color = DarkBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        tint = DarkBackground
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Zapisz w moich treningach",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Share / Export GPX button
                        Button(
                            onClick = { viewModel.shareGpx() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            enabled = !uiState.isSharing
                        ) {
                            if (uiState.isSharing) {
                                CircularProgressIndicator(
                                    color = DarkBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = DarkBackground
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Udostępnij / Eksportuj plik GPX",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

            // Edit Title Dialog
            if (showEditTitleDialog) {
                AlertDialog(
                    onDismissRequest = { showEditTitleDialog = false },
                    title = {
                        Text(
                            text = "Edytuj nazwę treningu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editTitleText,
                                onValueChange = { editTitleText = it },
                                label = { Text("Nazwa treningu") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = OrangeAccent,
                                    cursorColor = OrangeAccent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = editTitleText.trim()
                                if (trimmed.isNotBlank()) {
                                    viewModel.updateRideTitle(trimmed)
                                }
                                showEditTitleDialog = false
                            },
                            enabled = editTitleText.isNotBlank()
                        ) {
                            Text(
                                "Zapisz",
                                color = if (editTitleText.isNotBlank()) OrangeAccent else TextSecondary
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditTitleDialog = false }) {
                            Text("Anuluj", color = TextSecondary)
                        }
                    },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Usunąć trening?", color = TextPrimary) },
                text = {
                    Text(
                        "Spowoduje to trwałe usunięcie tego treningu oraz jego danych GPX.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteRide(onSuccess = onNavigateBack)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Usuń", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Anuluj", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Error message dialog
        uiState.errorMessage?.let { err ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Błąd", color = TextPrimary) },
                text = { Text(err, color = TextSecondary) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                },
                containerColor = DarkSurface
            )
        }
    }

        if (isMapFullscreen) {
            BackHandler {
                isMapFullscreen = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                DetailOsmMapView(
                    coordinates = uiState.routeCoordinates,
                    baseSource = baseSource,
                    activeOverlays = activeOverlays,
                    selectedPoint = selectedProfilePoint?.let { Pair(it.latitude, it.longitude) }
                )

                // Top Controls: Back button on top-start, Exit Fullscreen button on top-end
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMapFullscreen = false },
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zamknij pełny ekran",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(
                            onClick = { showMapLayersSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, CardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Źródła mapy i szlaki",
                                tint = OrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { isMapFullscreen = false },
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, CardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Zmniejsz mapę",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Map Layers Selection Bottom Sheet
        if (showMapLayersSheet) {
            MapLayersBottomSheet(
                selectedBaseSource = baseSource,
                activeOverlays = activeOverlays,
                onSelectBaseSource = { viewModel.setMapBaseSource(it) },
                onToggleOverlay = { overlay, enabled -> viewModel.toggleMapOverlay(overlay, enabled) },
                onDismissRequest = { showMapLayersSheet = false }
            )
        }
    }
}

@Composable
fun DetailMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

private class ScrubMarker(mapView: MapView) : Marker(mapView)

@Composable
fun DetailOsmMapView(
    coordinates: List<Pair<Double, Double>>,
    baseSource: MapBaseSource = MapBaseSource.OSM_DARK,
    activeOverlays: Set<MapOverlaySource> = emptySet(),
    selectedPoint: Pair<Double, Double>? = null,
    modifier: Modifier = Modifier
) {
    val polylineColor = OrangeAccent.toArgb()

    AndroidView(
        factory = { ctx ->
            object : MapView(ctx) {
                override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    return super.dispatchTouchEvent(ev)
                }
            }.apply {
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                clipToOutline = true
            }
        },
        update = { mapView ->
            applyMapConfiguration(mapView, baseSource, activeOverlays)

            if (coordinates.isNotEmpty()) {
                val hasPolyline = mapView.overlays.any { it is Polyline }
                if (!hasPolyline) {
                    mapView.overlays.removeAll { it is Polyline }

                    val geoPoints = coordinates.map { GeoPoint(it.first, it.second) }
                    val polyline = Polyline(mapView).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = polylineColor
                        outlinePaint.strokeWidth = 10f
                        infoWindow = null
                    }
                    mapView.overlays.add(0, polyline)

                    // Zoom to fit bounding box
                    val minLat = coordinates.minOf { it.first }
                    val maxLat = coordinates.maxOf { it.first }
                    val minLng = coordinates.minOf { it.second }
                    val maxLng = coordinates.maxOf { it.second }

                    val boundingBox = BoundingBox(maxLat, maxLng, minLat, minLng)
                    mapView.post {
                        mapView.zoomToBoundingBox(boundingBox, true, 60)
                    }
                }

                // Handle selectedPoint marker from chart scrubbing
                mapView.overlays.removeAll { it is ScrubMarker }
                if (selectedPoint != null) {
                    val scrubMarker = ScrubMarker(mapView).apply {
                        position = GeoPoint(selectedPoint.first, selectedPoint.second)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        infoWindow = null
                        icon = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(android.graphics.Color.parseColor("#FF6B35"))
                            setStroke(5, android.graphics.Color.WHITE)
                            setSize(44, 44)
                        }
                    }
                    mapView.overlays.add(scrubMarker)
                }

                mapView.invalidate()
            }
        },
        modifier = modifier
    )
}
