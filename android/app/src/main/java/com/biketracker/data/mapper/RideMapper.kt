package com.biketracker.data.mapper

import com.biketracker.data.model.Ride
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.util.Date

object RideMapper {

    fun toFirestore(ride: Ride): Map<String, Any?> = mapOf(
        "userId" to ride.userId,
        "startTime" to Timestamp(ride.startTime),
        "endTime" to Timestamp(ride.endTime),
        "distanceKm" to ride.distanceKm,
        "durationSeconds" to ride.durationSeconds,
        "avgSpeedKmh" to ride.avgSpeedKmh,
        "maxSpeedKmh" to ride.maxSpeedKmh,
        "elevationGain" to ride.elevationGain,
        "encodedPolyline" to ride.encodedPolyline,
        "createdAt" to FieldValue.serverTimestamp()
    )

    fun fromFirestore(doc: DocumentSnapshot): Ride = Ride(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        startTime = doc.getTimestamp("startTime")?.toDate() ?: Date(),
        endTime = doc.getTimestamp("endTime")?.toDate() ?: Date(),
        distanceKm = doc.getDouble("distanceKm") ?: 0.0,
        durationSeconds = doc.getLong("durationSeconds") ?: 0L,
        avgSpeedKmh = doc.getDouble("avgSpeedKmh") ?: 0.0,
        maxSpeedKmh = doc.getDouble("maxSpeedKmh") ?: 0.0,
        elevationGain = doc.getDouble("elevationGain"),
        encodedPolyline = doc.getString("encodedPolyline") ?: "",
        createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
    )
}
