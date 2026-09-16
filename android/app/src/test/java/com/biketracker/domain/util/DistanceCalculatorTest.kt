package com.biketracker.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun testDistanceBetweenKnownCities() {
        // Warsaw (52.2297, 21.0122) to Krakow (50.0647, 19.9450)
        // Straight line distance is ~252 km
        val distanceKm = DistanceCalculator.distanceBetweenKm(
            52.2297, 21.0122,
            50.0647, 19.9450
        )

        assertTrue(distanceKm in 250.0..255.0)
    }

    @Test
    fun testZeroDistanceForSamePoint() {
        val distanceKm = DistanceCalculator.distanceBetweenKm(
            52.2297, 21.0122,
            52.2297, 21.0122
        )

        assertEquals(0.0, distanceKm, 0.001)
    }

    @Test
    fun testShortDistanceInMeters() {
        // ~111 meters is roughly 0.001 degrees latitude
        val distanceMeters = DistanceCalculator.distanceBetweenMeters(
            52.000, 21.000,
            52.001, 21.000
        )

        assertTrue(distanceMeters in 110.0..112.0)
    }
}
