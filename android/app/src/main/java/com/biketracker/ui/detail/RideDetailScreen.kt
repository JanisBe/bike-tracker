package com.biketracker.ui.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.ErrorRed
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.TealAccent
import android.graphics.drawable.GradientDrawable
import com.biketracker.data.model.RouteProfilePoint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedProfilePoint by remember { mutableStateOf<RouteProfilePoint?>(null) }

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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń",
                            tint = ErrorRed
                        )
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
                    // Map View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        DetailOsmMapView(
                            coordinates = uiState.routeCoordinates,
                            selectedPoint = selectedProfilePoint?.let { Pair(it.latitude, it.longitude) }
                        )
                    }

                    // Content details
                    Column(modifier = Modifier.padding(16.dp)) {
                        val dateFormat =
                            SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", Locale("pl", "PL"))
                        Text(
                            text = dateFormat.format(ride.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

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
                            DetailMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Śr. prędkość",
                                value = "%.1f km/h".format(ride.avgSpeedKmh),
                                icon = Icons.Default.Speed,
                                tint = TextPrimary
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

                        ride.elevationGain?.let { ele ->
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailMetricCard(
                                modifier = Modifier.fillMaxWidth(),
                                label = "Przewyższenie",
                                value = "↑ %.0f m".format(ele),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                tint = TealAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

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
    selectedPoint: Pair<Double, Double>? = null
) {
    val polylineColor = OrangeAccent.toArgb()

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
            }
        },
        update = { mapView ->
            if (coordinates.isNotEmpty()) {
                val hasPolyline = mapView.overlays.any { it is Polyline }
                if (!hasPolyline) {
                    mapView.overlays.removeAll { it is Polyline }

                    val geoPoints = coordinates.map { GeoPoint(it.first, it.second) }
                    val polyline = Polyline(mapView).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = polylineColor
                        outlinePaint.strokeWidth = 10f
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
        modifier = Modifier.fillMaxSize()
    )
}
