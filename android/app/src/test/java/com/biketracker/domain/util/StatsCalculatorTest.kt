package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {

    @Test
    fun testStatsCalculationWithRealisticPoints() {
        val points = listOf(
            TrackPoint(latitude = 52.000, longitude = 21.000, elevation = 100.0, timestamp = 1000000L),
            TrackPoint(latitude = 52.001, longitude = 21.000, elevation = 105.0, timestamp = 1010000L),
            TrackPoint(latitude = 52.002, longitude = 21.000, elevation = 108.0, timestamp = 1020000L)
        )

        val stats = StatsCalculator.calculate(points)

        assertTrue(stats.distanceKm > 0.2)
        assertEquals(20L, stats.durationSeconds)
        assertTrue(stats.avgSpeedKmh > 0.0)
        assertEquals(8.0, stats.elevationGain ?: 0.0, 0.5)
    }

    @Test
    fun testEmptyPointsReturnsZeroStats() {
        val stats = StatsCalculator.calculate(emptyList())

        assertEquals(0.0, stats.distanceKm, 0.001)
        assertEquals(0L, stats.durationSeconds)
        assertEquals(0.0, stats.avgSpeedKmh, 0.001)
    }
}
