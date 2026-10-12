package com.biketracker.data.model

import java.util.Date

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val gender: String = "male",
    val weightKg: Double = 75.0,
    val heightCm: Double = 175.0,
    val cyclingSpeedThresholdKmh: Double = 1.5,
    val walkingSpeedThresholdKmh: Double = 0.5,
    val gpsLostVibration: Boolean = true,
    val gpsLostNotification: Boolean = true,
    val updatedAt: Date = Date()
)
