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
            totalDistanceKm += distKm

            val timeDiffSec = (p2.timestamp - p1.timestamp) / 1000.0
            if (timeDiffSec > 0.5) {
                val speedKmh = (distKm / (timeDiffSec / 3600.0))
                // Ignore unreasonable GPS jump speeds (> 100 km/h for bike)
                if (speedKmh in 0.5..100.0) {
                    maxSpeedKmh = max(maxSpeedKmh, speedKmh)
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
