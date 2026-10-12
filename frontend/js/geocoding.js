import {decodePolyline} from './polyline-decoder.js';

const GEO_CACHE_KEY = "bike_tracker_geo_cache_v1";

/**
 * Retrieves reverse geocoding cache from localStorage
 * @returns {Record<string, string>}
 */
export function getGeoCache() {
    try {
        return JSON.parse(localStorage.getItem(GEO_CACHE_KEY) || "{}");
    } catch {
        return {};
    }
}

/**
 * Saves reverse geocoding cache to localStorage
 * @param {Record<string, string>} cache
 */
export function saveGeoCache(cache) {
    try {
        localStorage.setItem(GEO_CACHE_KEY, JSON.stringify(cache));
    } catch {
        // Ignore storage quota errors
    }
}

/**
 * Helper to pause execution for a given duration
 * @param {number} ms
 * @returns {Promise<void>}
 */
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Fetches location name for given coordinates from OpenStreetMap Nominatim
 * @param {number} lat
 * @param {number} lon
 * @returns {Promise<string|null>}
 */
async function fetchLocationNameFromCoordinates(lat, lon) {
    const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=14&addressdetails=1`,
        {headers: {"Accept-Language": "pl"}}
    );

    if (!response.ok) return null;

    const data = await response.json();
    const address = data.address;
    const city = address?.city || address?.town || address?.village || address?.municipality || address?.county || "";
    const district = address?.suburb || address?.city_district || address?.neighbourhood || "";

    if (city && district && city.toLowerCase() !== district.toLowerCase()) {
        return `${city}, ${district}`;
    }
    if (city) return city;
    if (district) return district;
    if (data.display_name) return data.display_name.split(",")[0];

    return null;
}

let isEnriching = false;

/**
 * Enriches legacy rides with location names from OpenStreetMap Nominatim in background.
 * Requests are processed sequentially with throttling to strictly adhere to OSM Nominatim Usage Policy (max 1 req/sec).
 * @param {Array<object>} rides
 * @param {Function} [onLocationUpdated] Callback fired when new locations are discovered
 */
export async function enrichRidesWithLocation(rides, onLocationUpdated) {
    if (!rides || rides.length === 0 || isEnriching) return;
    isEnriching = true;
    try {
        const cache = getGeoCache();
        let updatedCount = 0;

        for (const ride of rides) {
            if (ride.locationName) continue;
            if (!ride.encodedPolyline) continue;

            if (cache[ride.id]) {
                ride.locationName = cache[ride.id];
                updatedCount++;
                continue;
            }

            const coords = decodePolyline(ride.encodedPolyline);
            if (!coords || coords.length === 0) continue;

            const [lat, lon] = coords[0];
            try {
                // eslint-disable-next-line no-await-in-loop -- Rate limiting is required by OSM Nominatim usage policy (max 1 req/sec)
                const loc = await fetchLocationNameFromCoordinates(lat, lon);

                if (loc) {
                    ride.locationName = loc;
                    cache[ride.id] = loc;
                    saveGeoCache(cache);
                    updatedCount++;
                }

                // eslint-disable-next-line no-await-in-loop -- Throttle delay to respect OSM Nominatim rate limits
                await sleep(1100);
            } catch (err) {
                console.warn("Geocoding failed for ride", ride.id, err);
            }
        }

        if (updatedCount > 0 && typeof onLocationUpdated === "function") {
            onLocationUpdated();
        }
    } finally {
        isEnriching = false;
    }
}
