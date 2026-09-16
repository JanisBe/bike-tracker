package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import kotlin.math.roundToInt

object PolylineEncoder {

    /**
     * Encodes a list of track points into a Google Polyline encoded string.
     */
    fun encode(points: List<TrackPoint>): String {
        if (points.isEmpty()) return ""

        val result = StringBuilder()
        var lastLat = 0
        var lastLng = 0

        for (point in points) {
            val lat = (point.latitude * 1e5).roundToInt()
            val lng = (point.longitude * 1e5).roundToInt()

            val dLat = lat - lastLat
            val dLng = lng - lastLng

            encodeValue(dLat, result)
            encodeValue(dLng, result)

            lastLat = lat
            lastLng = lng
        }

        return result.toString()
    }

    private fun encodeValue(value: Int, result: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else (value shl 1)
        while (v >= 0x20) {
            result.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
    }

    /**
     * Decodes a Google Polyline string into a list of pair coordinates (lat, lng).
     */
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            points.add(Pair(lat / 1e5, lng / 1e5))
        }

        return points
    }
}
