package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineEncoderTest {

    @Test
    fun testEmptyListEncodesToEmptyString() {
        val encoded = PolylineEncoder.encode(emptyList())
        assertEquals("", encoded)
    }

    @Test
    fun testEncodeAndDecodeRoundTrip() {
        val points = listOf(
            TrackPoint(latitude = 52.22970, longitude = 21.01220, timestamp = 1000L),
            TrackPoint(latitude = 52.23000, longitude = 21.01300, timestamp = 2000L),
            TrackPoint(latitude = 52.23150, longitude = 21.01500, timestamp = 3000L)
        )

        val encoded = PolylineEncoder.encode(points)
        assertTrue(encoded.isNotEmpty())

        val decoded = PolylineEncoder.decode(encoded)
        assertEquals(points.size, decoded.size)

        for (i in points.indices) {
            assertEquals(points[i].latitude, decoded[i].first, 0.0001)
            assertEquals(points[i].longitude, decoded[i].second, 0.0001)
        }
    }
}
