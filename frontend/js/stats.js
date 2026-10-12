/**
 * Determines the Metabolic Equivalent of Task (MET) based on activity type and speed.
 * Compendium of Physical Activities (Ainsworth et al.).
 * @param {string} activityType
 * @param {number} speedKmh
 * @returns {number}
 */
export function getMet(activityType, speedKmh) {
    const isWalking = activityType?.toLowerCase() === 'walking';
    if (isWalking) {
        if (speedKmh < 0.5) return 1.2;
        if (speedKmh < 3.0) return 2.3;
        if (speedKmh < 4.5) return 3.3;
        if (speedKmh < 5.5) return 3.8;
        if (speedKmh < 6.5) return 5.0;
        return 6.5;
    }
    // Cycling
    if (speedKmh < 1.0) return 1.2;
    if (speedKmh < 15.0) return 4.5;
    if (speedKmh < 19.0) return 6.8;
    if (speedKmh < 22.5) return 8.0;
    if (speedKmh < 26.0) return 10.0;
    if (speedKmh < 30.0) return 12.0;
    return 14.0;
}

/**
 * Calculates additional calories burned climbing elevation.
 * Formula: (weightKg * elevationGainMeters) / 94.0
 * @param {number} elevationGainMeters
 * @param {number} [weightKg=75]
 * @returns {number}
 */
export function calculateElevationCalories(elevationGainMeters, weightKg = 75.0) {
    if (!elevationGainMeters || elevationGainMeters <= 0) return 0;
    const validWeight = weightKg > 0 ? weightKg : 75.0;
    return (validWeight * elevationGainMeters) / 94.0;
}

/**
 * Calculates total estimated calories burned for a ride.
 * @param {Object} ride
 * @param {number} [weightKg=75]
 * @returns {number}
 */
export function calculateCaloriesBurned(ride, weightKg = 75.0) {
    if (!ride) return 0;
    if (ride.caloriesKcal != null && !Number.isNaN(Number(ride.caloriesKcal)) && Number(ride.caloriesKcal) > 0) {
        return Math.round(Number(ride.caloriesKcal));
    }
    const durationSeconds = Number(ride.durationSeconds) || 0;
    if (durationSeconds <= 0) return 0;
    const avgSpeedKmh = Number(ride.avgSpeedKmh) || 0;
    const activityType = ride.activityType || 'cycling';
    const elevationGain = Number(ride.elevationGain) || 0;
    const validWeight = weightKg > 0 ? weightKg : 75.0;

    const met = getMet(activityType, avgSpeedKmh);
    const durationHours = durationSeconds / 3600.0;
    const baseCalories = met * validWeight * durationHours;
    const elevationCalories = calculateElevationCalories(elevationGain, validWeight);

    return Math.max(1, Math.round(baseCalories + elevationCalories));
}

/**
 * Calculates cumulative statistics from an array of rides.
 * @param {Array<Object>} rides
 * @returns {Object}
 */
export function calculateOverallStats(rides) {
  if (!rides || rides.length === 0) {
    return {
      totalRides: 0,
      totalDistanceKm: 0,
      totalDurationHours: 0,
      avgSpeedKmh: 0,
      maxSpeedKmh: 0,
        totalElevationGain: 0,
        totalCaloriesKcal: 0
    };
  }

  const totalRides = rides.length;
  const totalDistanceKm = rides.reduce((sum, r) => sum + (r.distanceKm || 0), 0);
  const totalSeconds = rides.reduce((sum, r) => sum + (r.durationSeconds || 0), 0);
  const totalDurationHours = totalSeconds / 3600;

  const validAvgSpeeds = rides.filter(r => r.avgSpeedKmh > 0);
  const avgSpeedKmh = validAvgSpeeds.length > 0
    ? validAvgSpeeds.reduce((sum, r) => sum + r.avgSpeedKmh, 0) / validAvgSpeeds.length
    : 0;

  const maxSpeedKmh = Math.max(0, ...rides.map(r => r.maxSpeedKmh || 0));
  const totalElevationGain = rides.reduce((sum, r) => sum + (r.elevationGain || 0), 0);
    const totalCaloriesKcal = rides.reduce((sum, r) => sum + calculateCaloriesBurned(r), 0);

  return {
    totalRides,
    totalDistanceKm: Number(totalDistanceKm.toFixed(1)),
    totalDurationHours: Number(totalDurationHours.toFixed(1)),
    avgSpeedKmh: Number(avgSpeedKmh.toFixed(1)),
    maxSpeedKmh: Number(maxSpeedKmh.toFixed(1)),
      totalElevationGain: Math.round(totalElevationGain),
      totalCaloriesKcal: Math.round(totalCaloriesKcal)
  };
}

/**
 * Formats a date object to readable string.
 * @param {Date} date
 * @returns {string}
 */
export function formatDate(date) {
    if (!(date instanceof Date) || Number.isNaN(date)) return "Nieznana data";
    return new Intl.DateTimeFormat("pl-PL", {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
        minute: "2-digit",
        hour12: false
  }).format(date);
}

/**
 * Formats seconds into HH:MM:SS or MM:SS string.
 * @param {number} totalSeconds
 * @returns {string}
 */
export function formatDuration(totalSeconds) {
  if (!totalSeconds || totalSeconds <= 0) return "00:00";
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = Math.floor(totalSeconds % 60);

  if (hours > 0) {
    return `${hours}h ${minutes.toString().padStart(2, '0')}m`;
  }
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * Formats a Date into 'yyyy-MM-dd HH:mm:ss.gpx' filename format.
 * @param {Date} date
 * @returns {string}
 */
export function formatGpxFileName(date) {
    const d = date instanceof Date ? date : new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.gpx`;
}

