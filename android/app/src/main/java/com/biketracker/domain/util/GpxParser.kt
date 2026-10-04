package com.biketracker.domain.util

import android.location.Location
import android.util.Xml
import com.biketracker.data.model.RouteProfilePoint
import com.biketracker.data.model.TrackPoint
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GpxParseResult(
    val name: String? = null,
    val points: List<TrackPoint> = emptyList(),
    val profile: List<RouteProfilePoint> = emptyList()
)

object GpxParser {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val isoFormatWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private data class RawTrackPoint(
        val lat: Double,
        val lon: Double,
        val ele: Double?,
        val time: Long
    )

    /**
     * Parses a GPX XML string into full GpxParseResult with name, TrackPoints and profile.
     */
    fun parseFull(gpxXml: String): GpxParseResult {
        if (gpxXml.isBlank()) return GpxParseResult()

        val rawPoints = mutableListOf<RawTrackPoint>()
        var gpxName: String? = null

        try {
            val parser = Xml.newPullParser().apply {
                setInput(StringReader(gpxXml))
            }

            var eventType = parser.eventType
            var currentLat: Double? = null
            var currentLon: Double? = null
            var currentEle: Double? = null
            var currentTime: Long? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("trkpt", ignoreCase = true)) {
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            currentEle = null
                            currentTime = null
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when {
                                currentTag.equals("name", ignoreCase = true) && gpxName == null -> {
                                    gpxName = text
                                }
                                currentTag.equals("ele", ignoreCase = true) -> {
                                    currentEle = text.toDoubleOrNull()
                                }
                                currentTag.equals("time", ignoreCase = true) -> {
                                    currentTime = parseIsoTimestamp(text)
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("trkpt", ignoreCase = true)) {
                            if (currentLat != null && currentLon != null) {
                                rawPoints.add(
                                    RawTrackPoint(
                                        lat = currentLat,
                                        lon = currentLon,
                                        ele = currentEle,
                                        time = currentTime ?: (System.currentTimeMillis() + rawPoints.size * 1000L)
                                    )
                                )
                            }
                            currentLat = null
                            currentLon = null
                            currentEle = null
                            currentTime = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (rawPoints.isEmpty()) return GpxParseResult(name = gpxName)

        val profile = convertRawPointsToProfile(rawPoints)
        val trackPoints = profile.map { p ->
            TrackPoint(
                latitude = p.latitude,
                longitude = p.longitude,
                elevation = p.elevationM,
                timestamp = p.timestamp,
                speedKmh = p.speedKmh
            )
        }

        return GpxParseResult(
            name = gpxName,
            points = trackPoints,
            profile = profile
        )
    }

    /**
     * Intelligently suggests a title for an imported ride based on GPX metadata, location, and time of day.
     */
    fun suggestTitle(
        gpxName: String?,
        locationName: String,
        startTime: Date
    ): String {
        val trimmed = gpxName?.trim()
        if (!trimmed.isNullOrBlank() && !trimmed.startsWith("202") && !trimmed.equals("track", ignoreCase = true) && !trimmed.equals("activity", ignoreCase = true)) {
            return trimmed
        }
        val cal = Calendar.getInstance().apply { time = startTime }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val prefix = when (hour) {
            in 5..11 -> "Poranna trasa"
            in 12..17 -> "Popołudniowa trasa"
            in 18..21 -> "Wieczorny przejazd"
            else -> "Nocna trasa"
        }
        return if (locationName.isNotBlank()) {
            "$prefix – $locationName"
        } else {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("pl", "PL"))
            "$prefix • ${dateFormat.format(startTime)}"
        }
    }

    /**
     * Parses a GPX XML string into a sequence of RouteProfilePoint items
     * containing cumulative distance, smoothed speed, elevation, coordinates, and timestamp.
     */
    fun parse(gpxXml: String): List<RouteProfilePoint> {
        if (gpxXml.isBlank()) return emptyList()

        val rawPoints = mutableListOf<RawTrackPoint>()

        try {
            val parser = Xml.newPullParser().apply {
                setInput(StringReader(gpxXml))
            }

            var eventType = parser.eventType
            var currentLat: Double? = null
            var currentLon: Double? = null
            var currentEle: Double? = null
            var currentTime: Long? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("trkpt", ignoreCase = true)) {
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            currentEle = null
                            currentTime = null
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when {
                                currentTag.equals("ele", ignoreCase = true) -> {
                                    currentEle = text.toDoubleOrNull()
                                }

                                currentTag.equals("time", ignoreCase = true) -> {
                                    currentTime = parseIsoTimestamp(text)
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("trkpt", ignoreCase = true)) {
                            if (currentLat != null && currentLon != null) {
                                rawPoints.add(
                                    RawTrackPoint(
                                        lat = currentLat,
                                        lon = currentLon,
                                        ele = currentEle,
                                        time = currentTime ?: (System.currentTimeMillis() + rawPoints.size * 1000L)
                                    )
                                )
                            }
                            currentLat = null
                            currentLon = null
                            currentEle = null
                            currentTime = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (rawPoints.isEmpty()) return emptyList()

        return convertRawPointsToProfile(rawPoints)
    }

    private fun parseIsoTimestamp(text: String): Long {
        return try {
            isoFormat.parse(text)?.time
                ?: isoFormatWithMillis.parse(text)?.time
                ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                isoFormatWithMillis.parse(text)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun convertRawPointsToProfile(rawPoints: List<RawTrackPoint>): List<RouteProfilePoint> {
        val count = rawPoints.size
        if (count == 0) return emptyList()

        val cumulativeDistancesKm = DoubleArray(count)
        val rawSpeedsKmh = DoubleArray(count)
        val elevations = DoubleArray(count)

        var totalDistanceM = 0.0
        var lastEle = rawPoints.firstOrNull()?.ele ?: 0.0

        val resultsArray = FloatArray(1)

        for (i in 0 until count) {
            val pt = rawPoints[i]

            // Maintain elevation continuity
            if (pt.ele != null) {
                lastEle = pt.ele
            }
            elevations[i] = lastEle

            if (i == 0) {
                cumulativeDistancesKm[i] = 0.0
                rawSpeedsKmh[i] = 0.0
            } else {
                val prev = rawPoints[i - 1]
                Location.distanceBetween(prev.lat, prev.lon, pt.lat, pt.lon, resultsArray)
                val distStepM = resultsArray[0].toDouble()
                totalDistanceM += distStepM
                cumulativeDistancesKm[i] = totalDistanceM / 1000.0

                val deltaSeconds = (pt.time - prev.time) / 1000.0
                if (deltaSeconds in 0.5..120.0 && distStepM > 0.5) {
                    val speed = (distStepM / deltaSeconds) * 3.6
                    // Cap extreme GPS jump spikes (e.g. 100 km/h for bike)
                    rawSpeedsKmh[i] = speed.coerceIn(0.0, 95.0)
                } else if (i > 1) {
                    rawSpeedsKmh[i] = rawSpeedsKmh[i - 1]
                } else {
                    rawSpeedsKmh[i] = 0.0
                }
            }
        }

        // Apply a 3-point rolling average window for smooth speed curves
        val smoothedSpeedsKmh = DoubleArray(count)
        for (i in 0 until count) {
            var sum = 0.0
            var sampleCount = 0
            for (j in (i - 2).coerceAtLeast(0)..(i + 2).coerceAtMost(count - 1)) {
                sum += rawSpeedsKmh[j]
                sampleCount++
            }
            smoothedSpeedsKmh[i] = if (sampleCount > 0) sum / sampleCount else rawSpeedsKmh[i]
        }

        return List(count) { i ->
            val pt = rawPoints[i]
            RouteProfilePoint(
                distanceKm = cumulativeDistancesKm[i],
                elevationM = elevations[i],
                speedKmh = smoothedSpeedsKmh[i],
                latitude = pt.lat,
                longitude = pt.lon,
                timestamp = pt.time
            )
        }
    }

    /**
     * Fallback generator when GPX XML is unavailable, using decoded polyline coordinates.
     */
    fun fromCoordinates(
        coordinates: List<Pair<Double, Double>>,
        defaultElevation: Double = 0.0,
        defaultSpeedKmh: Double = 0.0
    ): List<RouteProfilePoint> {
        if (coordinates.isEmpty()) return emptyList()

        val resultsArray = FloatArray(1)
        var totalDistM = 0.0

        return coordinates.mapIndexed { index, pair ->
            if (index > 0) {
                val prev = coordinates[index - 1]
                Location.distanceBetween(prev.first, prev.second, pair.first, pair.second, resultsArray)
                totalDistM += resultsArray[0].toDouble()
            }
            RouteProfilePoint(
                distanceKm = totalDistM / 1000.0,
                elevationM = defaultElevation,
                speedKmh = defaultSpeedKmh,
                latitude = pair.first,
                longitude = pair.second,
                timestamp = System.currentTimeMillis() + index * 1000L
            )
        }
    }
}
