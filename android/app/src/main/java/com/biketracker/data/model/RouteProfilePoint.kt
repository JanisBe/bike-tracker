package com.biketracker.data.model

data class RouteProfilePoint(
    val distanceKm: Double,
    val elevationM: Double,
    val speedKmh: Double,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
