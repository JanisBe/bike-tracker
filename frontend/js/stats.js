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
      totalElevationGain: 0
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

  return {
    totalRides,
    totalDistanceKm: Number(totalDistanceKm.toFixed(1)),
    totalDurationHours: Number(totalDurationHours.toFixed(1)),
    avgSpeedKmh: Number(avgSpeedKmh.toFixed(1)),
    maxSpeedKmh: Number(maxSpeedKmh.toFixed(1)),
    totalElevationGain: Math.round(totalElevationGain)
  };
}

/**
 * Formats a date object to readable string.
 * @param {Date} date
 * @returns {string}
 */
export function formatDate(date) {
  if (!(date instanceof Date) || isNaN(date)) return "Unknown date";
  return new Intl.DateTimeFormat("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
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
