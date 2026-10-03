package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import kotlin.math.max

data class RideStats(
    val distanceKm: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGain: Double?
)

object StatsCalculator {

    fun calculate(points: List<TrackPoint>): RideStats {
        if (points.size < 2) {
            return RideStats(
                distanceKm = 0.0,
                durationSeconds = 0,
                avgSpeedKmh = 0.0,
                maxSpeedKmh = 0.0,
                elevationGain = null
            )
        }

        var totalDistanceKm = 0.0
        var maxSpeedKmh = 0.0
        var totalElevationGain = 0.0
        var hasElevation = false

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val distKm = DistanceCalculator.distanceBetweenKm(
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
            val timeDiffSec = (p2.timestamp - p1.timestamp) / 1000.0
            val speedKmh = if (timeDiffSec > 0.5) (distKm / (timeDiffSec / 3600.0)) else 0.0

            // Filter out stationary GPS jitter (speed below 1.0 km/h with negligible displacement < 5m)
            val isStationaryJitter = (p2.speedKmh != null && p2.speedKmh < 1.0 && distKm < 0.005) ||
                    (p2.speedKmh == null && speedKmh < 1.0 && distKm < 0.005)

            if (!isStationaryJitter) {
                totalDistanceKm += distKm
            }

            if (timeDiffSec > 0.5) {
                val effectiveSpeed = p2.speedKmh ?: speedKmh
                // Ignore unreasonable GPS jump speeds (> 100 km/h for bike) and stationary speeds (< 1.0 km/h)
                if (effectiveSpeed in 1.0..100.0) {
                    maxSpeedKmh = max(maxSpeedKmh, effectiveSpeed)
                }
            }

            if (p1.elevation != null && p2.elevation != null) {
                hasElevation = true
                val eleDiff = p2.elevation - p1.elevation
                if (eleDiff > 1.0) { // filter noise threshold: 1 meter
                    totalElevationGain += eleDiff
                }
            }
        }

        val totalDurationSeconds = max(1L, (points.last().timestamp - points.first().timestamp) / 1000L)
        val hours = totalDurationSeconds / 3600.0
        val avgSpeedKmh = if (hours > 0) totalDistanceKm / hours else 0.0

        return RideStats(
            distanceKm = (totalDistanceKm * 100).toInt() / 100.0, // round to 2 decimals
            durationSeconds = totalDurationSeconds,
            avgSpeedKmh = (avgSpeedKmh * 10).toInt() / 10.0,
            maxSpeedKmh = (maxSpeedKmh * 10).toInt() / 10.0,
            elevationGain = if (hasElevation) totalElevationGain.toInt().toDouble() else null
        )
    }
}
