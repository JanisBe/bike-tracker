package com.biketracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketracker.data.model.RouteProfilePoint
import com.biketracker.ui.theme.CardBorder
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.DarkSurfaceVariant
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun RideProfileChart(
    points: List<RouteProfilePoint>,
    onPointSelected: (RouteProfilePoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    var showSpeed by remember { mutableStateOf(true) }
    var showElevation by remember { mutableStateOf(true) }

    var activePoint by remember { mutableStateOf<RouteProfilePoint?>(null) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    val totalDistance = points.last().distanceKm.coerceAtLeast(0.01)
    val maxSpeed = points.maxOfOrNull { it.speedKmh }?.coerceAtLeast(15.0) ?: 30.0
    val rawMinEle = points.minOfOrNull { it.elevationM } ?: 0.0
    val rawMaxEle = points.maxOfOrNull { it.elevationM } ?: 10.0
    val eleSpan = (rawMaxEle - rawMinEle).coerceAtLeast(10.0)
    val minEle = rawMinEle - (eleSpan * 0.1)
    val maxEle = rawMaxEle + (eleSpan * 0.1)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title + Interactive Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profil trasy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Legend with clickable toggle buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed Legend (Clickable)
                    Surface(
                        color = if (showSpeed) OrangeAccent.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (showSpeed && !showElevation) {
                                    showElevation = true
                                }
                                showSpeed = !showSpeed
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (showSpeed) OrangeAccent else TextSecondary.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Prędkość",
                                fontSize = 11.sp,
                                color = if (showSpeed) OrangeAccent else TextSecondary.copy(alpha = 0.45f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Elevation Legend (Clickable)
                    Surface(
                        color = if (showElevation) TealAccent.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (showElevation && !showSpeed) {
                                    showSpeed = true
                                }
                                showElevation = !showElevation
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (showElevation) TealAccent else TextSecondary.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wysokość",
                                fontSize = 11.sp,
                                color = if (showElevation) TealAccent else TextSecondary.copy(alpha = 0.45f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tooltip Banner for Scrubbed Values
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                activePoint?.let { pt ->
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "%.2f km".format(pt.distanceKm),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )

                            if (showSpeed) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = OrangeAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "%.1f km/h".format(pt.speedKmh),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OrangeAccent
                                    )
                                }
                            }

                            if (showElevation) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = TealAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "%.0f m".format(pt.elevationM),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealAccent
                                    )
                                }
                            }
                        }
                    }
                }

                if (activePoint == null) {
                    Text(
                        text = "Przesuń palcem po wykresie, aby sprawdzić punkt na mapie",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dual-Axis Chart Canvas
            val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(points, showSpeed, showElevation) {
                        detectTapGestures(
                            onPress = { offset ->
                                val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                scrubFraction = frac
                                val targetDist = frac * totalDistance
                                val found = points.minByOrNull { kotlin.math.abs(it.distanceKm - targetDist) }
                                activePoint = found
                                onPointSelected(found)
                            }
                        )
                    }
                    .pointerInput(points, showSpeed, showElevation) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                scrubFraction = frac
                                val targetDist = frac * totalDistance
                                val found = points.minByOrNull { kotlin.math.abs(it.distanceKm - targetDist) }
                                activePoint = found
                                onPointSelected(found)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                scrubFraction = frac
                                val targetDist = frac * totalDistance
                                val found = points.minByOrNull { kotlin.math.abs(it.distanceKm - targetDist) }
                                activePoint = found
                                onPointSelected(found)
                            },
                            onDragEnd = {
                                // Keep last selected point visible on map
                            },
                            onDragCancel = {
                                // Keep last selected point
                            }
                        )
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val paddingBottom = 22.dp.toPx()
                val paddingTop = 12.dp.toPx()
                val plotWidth = canvasWidth
                val plotHeight = canvasHeight - paddingBottom - paddingTop

                if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

                // 1. Draw 3 Horizontal Grid lines (0%, 50%, 100%)
                val gridColor = Color.White.copy(alpha = 0.08f)
                for (ratio in listOf(0f, 0.5f, 1f)) {
                    val y = paddingTop + plotHeight * (1f - ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // 2. Compute Elevation Path & Fill (if showElevation)
                if (showElevation) {
                    val elevationPath = Path()
                    val elevationFillPath = Path()

                    points.forEachIndexed { index, pt ->
                        val x = ((pt.distanceKm / totalDistance).toFloat().coerceIn(0f, 1f)) * plotWidth
                        val eleNorm = ((pt.elevationM - minEle) / (maxEle - minEle)).toFloat().coerceIn(0f, 1f)
                        val y = paddingTop + plotHeight * (1f - eleNorm)

                        if (index == 0) {
                            elevationPath.moveTo(x, y)
                            elevationFillPath.moveTo(x, paddingTop + plotHeight)
                            elevationFillPath.lineTo(x, y)
                        } else {
                            elevationPath.lineTo(x, y)
                            elevationFillPath.lineTo(x, y)
                        }
                    }

                    elevationFillPath.lineTo(plotWidth, paddingTop + plotHeight)
                    elevationFillPath.close()

                    // Draw translucent elevation fill gradient
                    drawPath(
                        path = elevationFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TealAccent.copy(alpha = 0.28f),
                                TealAccent.copy(alpha = 0.02f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + plotHeight
                        )
                    )

                    // Draw elevation outline
                    drawPath(
                        path = elevationPath,
                        color = TealAccent.copy(alpha = 0.75f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 3. Compute Speed Path (if showSpeed)
                if (showSpeed) {
                    val speedPath = Path()
                    points.forEachIndexed { index, pt ->
                        val x = ((pt.distanceKm / totalDistance).toFloat().coerceIn(0f, 1f)) * plotWidth
                        val speedNorm = (pt.speedKmh / maxSpeed).toFloat().coerceIn(0f, 1f)
                        val y = paddingTop + plotHeight * (1f - speedNorm)

                        if (index == 0) {
                            speedPath.moveTo(x, y)
                        } else {
                            speedPath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = speedPath,
                        color = OrangeAccent,
                        style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // 4. Draw Scrub Indicator Line & Points if active
                scrubFraction?.let { frac ->
                    val scrubX = frac * plotWidth
                    // Vertical cursor line
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(scrubX, paddingTop - 4.dp.toPx()),
                        end = Offset(scrubX, paddingTop + plotHeight + 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    activePoint?.let { pt ->
                        if (showElevation) {
                            // Elevation marker circle
                            val eleNorm = ((pt.elevationM - minEle) / (maxEle - minEle)).toFloat().coerceIn(0f, 1f)
                            val eleY = paddingTop + plotHeight * (1f - eleNorm)
                            drawCircle(
                                color = DarkSurface,
                                radius = 6.dp.toPx(),
                                center = Offset(scrubX, eleY)
                            )
                            drawCircle(
                                color = TealAccent,
                                radius = 4.dp.toPx(),
                                center = Offset(scrubX, eleY)
                            )
                        }

                        if (showSpeed) {
                            // Speed marker circle
                            val speedNorm = (pt.speedKmh / maxSpeed).toFloat().coerceIn(0f, 1f)
                            val speedY = paddingTop + plotHeight * (1f - speedNorm)
                            drawCircle(
                                color = DarkSurface,
                                radius = 6.dp.toPx(),
                                center = Offset(scrubX, speedY)
                            )
                            drawCircle(
                                color = OrangeAccent,
                                radius = 4.dp.toPx(),
                                center = Offset(scrubX, speedY)
                            )
                        }
                    }
                }
            }

            // Bottom X-Axis Distance Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0 km", fontSize = 11.sp, color = TextSecondary)
                Text(
                    text = "%.1f km".format(totalDistance / 2),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "%.1f km".format(totalDistance),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
