package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxGeneratorTest {

    @Test
    fun testGpxFormatContainsRequiredXmlTags() {
        val points = listOf(
            TrackPoint(latitude = 52.2297, longitude = 21.0122, elevation = 110.5, timestamp = 1700000000000L),
            TrackPoint(latitude = 52.2300, longitude = 21.0130, elevation = 112.0, timestamp = 1700000003000L)
        )

        val gpx = GpxGenerator.generate(points, "Morning Bike Ride")

        assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(gpx.contains("""<gpx version="1.1""""))
        assertTrue(gpx.contains("""<name>Morning Bike Ride</name>"""))
        assertTrue(gpx.contains("""<trkpt lat="52.2297" lon="21.0122">"""))
        assertTrue(gpx.contains("""<ele>110.5</ele>"""))
        assertTrue(gpx.contains("""</trkpt>"""))
        assertTrue(gpx.contains("""</trkseg>"""))
        assertTrue(gpx.contains("""</trk>"""))
        assertTrue(gpx.contains("""</gpx>"""))
    }
}
