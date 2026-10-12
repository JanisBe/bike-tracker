package com.biketracker.domain.util

import com.biketracker.data.model.ActivityType
import kotlin.math.max
import kotlin.math.roundToInt

object CalorieCalculator {

    /**
     * Determines the Metabolic Equivalent of Task (MET) based on activity type and speed.
     * Sources: Compendium of Physical Activities (Ainsworth et al.).
     */
    fun getMet(activityType: String, speedKmh: Double): Double {
        val isWalking = activityType.equals(ActivityType.WALKING.id, ignoreCase = true)
        return if (isWalking) {
            when {
                speedKmh < 0.5 -> 1.2 // Resting / stationary
                speedKmh < 3.0 -> 2.3 // Slow walk (< 3 km/h)
                speedKmh < 4.5 -> 3.3 // Moderate walk (3.0 - 4.5 km/h)
                speedKmh < 5.5 -> 3.8 // Standard brisk walk (4.5 - 5.5 km/h)
                speedKmh < 6.5 -> 5.0 // Fast walk (5.5 - 6.5 km/h)
                else -> 6.5 // Very brisk / race walk (> 6.5 km/h)
            }
        } else {
            // Cycling
            when {
                speedKmh < 1.0 -> 1.2 // Resting / stationary
                speedKmh < 15.0 -> 4.5 // Leisure / light cycling (< 15 km/h)
                speedKmh < 19.0 -> 6.8 // Moderate effort (15 - 19 km/h)
                speedKmh < 22.5 -> 8.0 // Vigorous effort (19 - 22.5 km/h)
                speedKmh < 26.0 -> 10.0 // Fast pace (22.5 - 26 km/h)
                speedKmh < 30.0 -> 12.0 // Very fast pace (26 - 30 km/h)
                else -> 14.0 // Racing / very high intensity (> 30 km/h)
            }
        }
    }

    /**
     * Calculates calories burned in one second based on MET and weight:
     * kcal/sec = (MET * weightKg) / 3600
     */
    fun calculateCaloriesPerSecond(activityType: String, currentSpeedKmh: Double, weightKg: Double): Double {
        val validWeight = if (weightKg > 0.0) weightKg else 75.0
        val met = getMet(activityType, currentSpeedKmh)
        return (met * validWeight) / 3600.0
    }

    /**
     * Calculates additional calories burned climbing elevation against gravity.
     * Work = weight * g * height; with human metabolic efficiency ~22%:
     * Additional kcal = (weightKg * heightMeters) / 94.0
     */
    fun calculateElevationCalories(elevationGainMeters: Double, weightKg: Double): Double {
        if (elevationGainMeters <= 0.0) return 0.0
        val validWeight = if (weightKg > 0.0) weightKg else 75.0
        return (validWeight * elevationGainMeters) / 94.0
    }

    /**
     * Calculates total estimated calories for a completed ride or legacy ride document.
     */
    fun calculateCalories(
        activityType: String,
        avgSpeedKmh: Double,
        durationSeconds: Long,
        elevationGainMeters: Double? = null,
        weightKg: Double = 75.0
    ): Int {
        if (durationSeconds <= 0) return 0
        val validWeight = if (weightKg > 0.0) weightKg else 75.0
        val met = getMet(activityType, avgSpeedKmh)
        val durationHours = durationSeconds / 3600.0
        val baseCalories = met * validWeight * durationHours
        val elevationCalories = calculateElevationCalories(elevationGainMeters ?: 0.0, validWeight)
        return max(1, (baseCalories + elevationCalories).roundToInt())
    }
}
