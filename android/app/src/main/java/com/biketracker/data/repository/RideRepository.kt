package com.biketracker.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.biketracker.data.mapper.RideMapper
import com.biketracker.data.model.Ride
import com.biketracker.data.model.TrackPoint
import com.biketracker.domain.util.GpxGenerator
import com.biketracker.domain.util.PolylineEncoder
import com.biketracker.domain.util.StatsCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface RideRepository {
    suspend fun saveRide(points: List<TrackPoint>): Result<String>
    fun getRides(): Flow<List<Ride>>
    suspend fun getRide(rideId: String): Result<Ride>
    suspend fun getGpxContent(rideId: String): Result<String>
    suspend fun deleteRide(rideId: String): Result<Unit>
}

@Singleton
class RideRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RideRepository {

    private val ridesCollection = firestore.collection("rides")

    private suspend fun resolveLocationName(lat: Double, lng: Double): String =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext ""
                val geocoder = Geocoder(context, Locale("pl", "PL"))
                val addresses: List<Address> =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(
                                lat,
                                lng,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (cont.isActive) {
                                            cont.resume(addresses)
                                        }
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (cont.isActive) {
                                            cont.resume(emptyList())
                                        }
                                    }
                                })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lng, 1) ?: emptyList()
                    }

                val address = addresses.firstOrNull() ?: return@withContext ""
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val district = address.subLocality ?: ""
                when {
                    city.isNotBlank() && district.isNotBlank() && !city.equals(
                        district,
                        ignoreCase = true
                    ) -> "$city, $district"

                    city.isNotBlank() -> city
                    district.isNotBlank() -> district
                    !address.featureName.isNullOrBlank() -> address.featureName
                    else -> ""
                }
            } catch (e: Exception) {
                ""
            }
        }

    override suspend fun saveRide(points: List<TrackPoint>): Result<String> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User not authenticated")
        require(points.size >= 2) { "At least 2 GPS points are required" }
        val rideRef = ridesCollection.document()
        val rideId = rideRef.id

        // 1. Calculate statistics
        val stats = StatsCalculator.calculate(points)

        // 2. Generate encoded polyline
        val encodedPolyline = PolylineEncoder.encode(points)

        // 3. Resolve location name and ride title
        val locationName = resolveLocationName(points.first().latitude, points.first().longitude)
        val gpxDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl", "PL"))
        val formattedDate = gpxDateFormat.format(Date(points.first().timestamp))
        val rideTitle = if (locationName.isNotBlank()) {
            "$locationName • $formattedDate"
        } else {
            "Trening $formattedDate"
        }
        val gpxXml = GpxGenerator.generate(points, rideTitle)

        // 4. Create Ride model
        val ride = Ride(
            id = rideId,
            userId = user.uid,
            title = rideTitle,
            locationName = locationName,
            startTime = Date(points.first().timestamp),
            endTime = Date(points.last().timestamp),
            distanceKm = stats.distanceKm,
            durationSeconds = stats.durationSeconds,
            avgSpeedKmh = stats.avgSpeedKmh,
            maxSpeedKmh = stats.maxSpeedKmh,
            elevationGain = stats.elevationGain,
            encodedPolyline = encodedPolyline
        )

        // 5. Batch write to Firestore (Spark Plan / 0 MB Storage)
        val batch = firestore.batch()
        val gpxDocRef = rideRef.collection("details").document("gpx")

        batch.set(rideRef, RideMapper.toFirestore(ride))
        batch.set(gpxDocRef, mapOf("gpxContent" to gpxXml))

        batch.commit().await()

        rideId
    }

    override fun getRides(): Flow<List<Ride>> {
        val user = auth.currentUser ?: return flowOf(emptyList())

        return ridesCollection
            .whereEqualTo("userId", user.uid)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.map { RideMapper.fromFirestore(it) }
            }
    }

    override suspend fun getRide(rideId: String): Result<Ride> = runCatching {
        val doc = ridesCollection.document(rideId).get().await()
        if (!doc.exists()) throw NoSuchElementException("Ride $rideId not found")
        RideMapper.fromFirestore(doc)
    }

    override suspend fun getGpxContent(rideId: String): Result<String> = runCatching {
        val doc = ridesCollection.document(rideId)
            .collection("details")
            .document("gpx")
            .get()
            .await()

        doc.getString("gpxContent") ?: throw NoSuchElementException("No GPX data found for ride $rideId")
    }

    override suspend fun deleteRide(rideId: String): Result<Unit> = runCatching {
        val rideRef = ridesCollection.document(rideId)
        val gpxRef = rideRef.collection("details").document("gpx")

        val batch = firestore.batch()
        batch.delete(gpxRef)
        batch.delete(rideRef)
        batch.commit().await()
    }
}
