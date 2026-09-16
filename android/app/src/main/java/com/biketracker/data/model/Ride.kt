package com.biketracker.data.model

import java.util.Date

data class Ride(
    val id: String = "",
    val userId: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevationGain: Double? = null,
    val encodedPolyline: String = "",
    val createdAt: Date = Date()
)
