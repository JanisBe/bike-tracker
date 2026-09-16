package com.biketracker.data.model

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null,
    val speedKmh: Double? = null
)
