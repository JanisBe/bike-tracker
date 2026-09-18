import {db} from './firebase-config.js';
import {
    collection,
    doc,
    getDoc,
    getDocs,
    orderBy,
    query,
    where
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";
import {decodePolyline} from './polyline-decoder.js';
import {formatGpxFileName} from './stats.js';

// Demo rides provided as high-quality fallback if no rides are recorded in Firestore yet
const MOCK_RIDES = [
  {
    id: "demo-ride-kampinos",
    userId: "demo-user",
    title: "Puszcza Kampinoska 🌲",
    startTime: new Date(Date.now() - 1000 * 60 * 60 * 24 * 1), // Wczoraj
    endTime: new Date(Date.now() - 1000 * 60 * 60 * 24 * 1 + 1000 * 60 * 85),
    distanceKm: 28.4,
    durationSeconds: 5100,
    avgSpeedKmh: 20.0,
    maxSpeedKmh: 36.5,
    elevationGain: 145,
    // Realistic polyline in Kampinos National Park near Warsaw
      encodedPolyline: "_{_iIe{m`Bg@k@_Ag@qAo@uAe@eAm@mB}@mCe@yAs@kC_AkDu@kCy@kCe@kBu@}Bq@cBk@_Bi@eBs@iBy@qB_AiB}@iBy@iBs@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB",
    isDemo: true
  },
  {
    id: "demo-ride-vistula",
    userId: "demo-user",
    title: "Bulwary Wiślane 🚴",
    startTime: new Date(Date.now() - 1000 * 60 * 60 * 24 * 4), // 4 dni temu
    endTime: new Date(Date.now() - 1000 * 60 * 60 * 24 * 4 + 1000 * 60 * 45),
    distanceKm: 16.2,
    durationSeconds: 2700,
    avgSpeedKmh: 21.6,
    maxSpeedKmh: 32.1,
    elevationGain: 40,
    // Realistic route along Warsaw Vistula
      encodedPolyline: "y~}hIk}f|Ue@fBg@`C_@~B_@~Bs@|Ci@~B_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC",
    isDemo: true
  }
];

/**
 * Fetches all rides belonging to a specific user from Firestore.
 * @param {string} userId
 * @returns {Promise<Array<Object>>}
 */
export async function fetchUserRides(userId) {
    if (!userId) {
        return [];
    }
    try {
        const q = query(
            collection(db, "rides"),
            where("userId", "==", userId),
            orderBy("startTime", "desc")
        );
        const snapshot = await getDocs(q);

        if (snapshot.empty) {
            return [];
        }

        return snapshot.docs.map(docSnap => {
            const data = docSnap.data();
            return {
                id: docSnap.id,
                ...data,
                startTime: data.startTime ? data.startTime.toDate() : new Date(),
                endTime: data.endTime ? data.endTime.toDate() : new Date(),
                distanceKm: Number(data.distanceKm) || 0,
                durationSeconds: Number(data.durationSeconds) || 0,
                avgSpeedKmh: Number(data.avgSpeedKmh) || 0,
                maxSpeedKmh: Number(data.maxSpeedKmh) || 0,
                elevationGain: data.elevationGain != null ? Number(data.elevationGain) : null,
                encodedPolyline: data.encodedPolyline || "",
                isDemo: false
            };
        });
    } catch (error) {
        console.error("Firestore fetch user rides error:", error);
        throw error;
    }
}

/**
 * Fetches all rides from Firestore. Falls back to demo rides if Firestore is empty or inaccessible.
 * @returns {Promise<Array<Object>>}
 */
export async function fetchAllRides() {
  try {
    const q = query(collection(db, "rides"), orderBy("startTime", "desc"));
    const snapshot = await getDocs(q);

    if (snapshot.empty) {
      console.info("No rides found in Firestore yet. Using demo rides for preview.");
      return MOCK_RIDES;
    }

    return snapshot.docs.map(docSnap => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        ...data,
        startTime: data.startTime ? data.startTime.toDate() : new Date(),
        endTime: data.endTime ? data.endTime.toDate() : new Date(),
        distanceKm: Number(data.distanceKm) || 0,
        durationSeconds: Number(data.durationSeconds) || 0,
        avgSpeedKmh: Number(data.avgSpeedKmh) || 0,
        maxSpeedKmh: Number(data.maxSpeedKmh) || 0,
        elevationGain: data.elevationGain != null ? Number(data.elevationGain) : null,
        encodedPolyline: data.encodedPolyline || "",
        isDemo: false
      };
    });
  } catch (error) {
    console.warn("Firestore fetch error, displaying demo data:", error);
    return MOCK_RIDES;
  }
}

/**
 * Downloads the GPX file for a given ride ID or ride object from Firestore subcollection.
 * @param {string|Object} rideOrId
 * @param {string|null} customFileName
 */
export async function downloadGpx(rideOrId, customFileName = null) {
    const isObj = rideOrId && typeof rideOrId === 'object';
    const rideId = isObj ? rideOrId.id : rideOrId;
    const rideDate = isObj ? rideOrId.startTime : null;

  // Check if it's a demo ride
  const demoRide = MOCK_RIDES.find(r => r.id === rideId);
    const effectiveDate = rideDate || (demoRide ? demoRide.startTime : null);
    const fileName = customFileName || (effectiveDate ? formatGpxFileName(effectiveDate) : `ride_${rideId}.gpx`);

  if (demoRide) {
    const demoGpx = generateDemoGpx(demoRide);
      triggerFileDownload(demoGpx, fileName);
    return;
  }

  try {
    const gpxDocRef = doc(db, "rides", rideId, "details", "gpx");
    const snap = await getDoc(gpxDocRef);

    if (!snap.exists() || !snap.data().gpxContent) {
      throw new Error("No GPX file content found for this ride in Firestore.");
    }

    const gpxXml = snap.data().gpxContent;
      triggerFileDownload(gpxXml, fileName);
  } catch (error) {
    console.error("Failed to download GPX:", error);
    alert(`Nie udało się pobrać pliku GPX: ${error.message}`);
  }
}


function triggerFileDownload(content, filename) {
  const blob = new Blob([content], { type: "application/gpx+xml;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function generateDemoGpx(ride) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="BikeTracker Web Demo" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>${ride.title || 'Demo Ride'}</name>
    <time>${ride.startTime.toISOString()}</time>
  </metadata>
  <trk>
    <name>${ride.title || 'Demo Ride'}</name>
    <trkseg>
      <trkpt lat="52.2297" lon="21.0122">
        <ele>110.0</ele>
        <time>${ride.startTime.toISOString()}</time>
      </trkpt>
      <trkpt lat="52.2350" lon="21.0180">
        <ele>112.5</ele>
        <time>${new Date(ride.startTime.getTime() + 1000 * 60 * 15).toISOString()}</time>
      </trkpt>
      <trkpt lat="52.2450" lon="21.0250">
        <ele>115.0</ele>
        <time>${ride.endTime.toISOString()}</time>
      </trkpt>
    </trkseg>
  </trk>
</gpx>`;
}

/**
 * Calculates Haversine distance in meters between two lat/lon points.
 */
function haversineDistanceM(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

/**
 * Parses GPX XML content and returns an array of route profile points.
 */
export function parseGpxXml(gpxXml) {
    if (!gpxXml || typeof gpxXml !== 'string') return [];
    try {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(gpxXml, "text/xml");
        const trkpts = xmlDoc.querySelectorAll("trkpt");
        if (!trkpts || trkpts.length === 0) return [];

        const rawPoints = [];
        trkpts.forEach(pt => {
            const lat = Number.parseFloat(pt.getAttribute("lat"));
            const lon = Number.parseFloat(pt.getAttribute("lon"));
            const eleNode = pt.querySelector("ele");
            const timeNode = pt.querySelector("time");
            const ele = eleNode ? Number.parseFloat(eleNode.textContent) : null;
            const time = timeNode ? new Date(timeNode.textContent).getTime() : null;

            if (!Number.isNaN(lat) && !Number.isNaN(lon)) {
                rawPoints.push({lat, lon, ele, time});
            }
        });

        if (rawPoints.length < 2) return [];

        let cumDistKm = 0;
        const count = rawPoints.length;
        const rawSpeeds = [];
        const distances = [];
        const elevations = [];
        let lastEle = rawPoints[0].ele != null ? rawPoints[0].ele : 100;

        for (let i = 0; i < count; i++) {
            const p = rawPoints[i];
            if (p.ele != null && !Number.isNaN(p.ele)) lastEle = p.ele;
            elevations.push(lastEle);

            if (i === 0) {
                distances.push(0);
                rawSpeeds.push(0);
            } else {
                const prev = rawPoints[i - 1];
                const stepM = haversineDistanceM(prev.lat, prev.lon, p.lat, p.lon);
                cumDistKm += stepM / 1000;
                distances.push(cumDistKm);

                let speedKmh = 0;
                if (p.time && prev.time) {
                    const deltaSec = (p.time - prev.time) / 1000;
                    if (deltaSec >= 0.5 && deltaSec <= 120 && stepM > 0.5) {
                        speedKmh = Math.min(95, Math.max(0, (stepM / deltaSec) * 3.6));
                    } else if (i > 1) {
                        speedKmh = rawSpeeds[i - 1];
                    }
                }
                rawSpeeds.push(speedKmh);
            }
        }

        // Smooth speed with rolling window (radius = 2)
        const smoothedSpeeds = [];
        for (let i = 0; i < count; i++) {
            let sum = 0;
            let samples = 0;
            const start = Math.max(0, i - 2);
            const end = Math.min(count - 1, i + 2);
            for (let j = start; j <= end; j++) {
                sum += rawSpeeds[j];
                samples++;
            }
            smoothedSpeeds.push(samples > 0 ? sum / samples : rawSpeeds[i]);
        }

        return rawPoints.map((pt, i) => ({
            distanceKm: distances[i],
            elevationM: elevations[i],
            speedKmh: smoothedSpeeds[i],
            lat: pt.lat,
            lon: pt.lon
        }));
    } catch (err) {
        console.error("Failed to parse GPX XML:", err);
        return [];
    }
}

/**
 * Generates realistic profile points from polyline coordinates and ride metrics.
 */
export function generateProfileFromPolyline(coords, ride) {
    if (!coords || coords.length < 2) return [];

    const count = coords.length;
    const distances = [0];
    let totalDistM = 0;

    for (let i = 1; i < count; i++) {
        const prev = coords[i - 1];
        const curr = coords[i];
        const stepM = haversineDistanceM(prev[0], prev[1], curr[0], curr[1]);
        totalDistM += stepM;
        distances.push(totalDistM / 1000);
    }

    const targetDistKm = ride.distanceKm > 0 ? ride.distanceKm : totalDistM / 1000;
    const distScale = totalDistM > 0 ? targetDistKm / (totalDistM / 1000) : 1;

    const baseEle = 105;
    const eleGain = ride.elevationGain != null && ride.elevationGain > 0 ? ride.elevationGain : 60;
    const avgSpeed = ride.avgSpeedKmh > 0 ? ride.avgSpeedKmh : 20.0;
    const maxSpeed = ride.maxSpeedKmh > 0 ? ride.maxSpeedKmh : avgSpeed * 1.5;

    const points = [];
    for (let i = 0; i < count; i++) {
        const frac = count > 1 ? i / (count - 1) : 0;
        const dKm = distances[i] * distScale;

        // Realistic natural elevation wave based on ride elevation gain
        const wave = Math.sin(frac * Math.PI * 3.5) * 0.5 + Math.cos(frac * Math.PI * 7.2) * 0.25;
        const ele = baseEle + (eleGain * 0.45) * (wave + 0.5);

        // Speed curve (slower on uphill, faster on descent/flats)
        const speedVariation = (Math.sin(frac * Math.PI * 5 + 1.2) * 0.2) - (wave * 0.15);
        const speed = Math.min(maxSpeed, Math.max(5, avgSpeed * (1 + speedVariation)));

        points.push({
            distanceKm: dKm,
            elevationM: ele,
            speedKmh: speed,
            lat: coords[i][0],
            lon: coords[i][1]
        });
    }

    return points;
}

/**
 * Fetches or generates route profile data for the chart.
 */
export async function fetchRideProfile(ride) {
    if (!ride) return [];

    // If real Firestore ride, attempt to load gpxContent
    if (!ride.isDemo) {
        try {
            const gpxDocRef = doc(db, "rides", ride.id, "details", "gpx");
            const snap = await getDoc(gpxDocRef);
            if (snap.exists() && snap.data().gpxContent) {
                const parsed = parseGpxXml(snap.data().gpxContent);
                if (parsed && parsed.length >= 2) return parsed;
            }
        } catch (e) {
            console.warn("Could not load Firestore GPX details:", e);
        }
    }

    // Fallback to decoded polyline
    const coords = decodePolyline(ride.encodedPolyline);
    return generateProfileFromPolyline(coords, ride);
}

