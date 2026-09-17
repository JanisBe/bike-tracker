import {db} from './firebase-config.js';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

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
    encodedPolyline: "_{_iIe{m`Bg@k@_Ag@qAo@uAe@eAm@mB}@mCe@yAs@kC_AkDu@kCy@kCe@kBu@}Bq@cBk@_Bi@eBs@iBy@qB_AiB}@iBy@iBs@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB_AkBy@kBu@mB{@oB",
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
    encodedPolyline: "y~}hIk}f|Ue@fBg@`C_@~B_@~Bs@|Ci@~B_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC_@`Ca@bCq@bCw@bC{@dCs@bC",
    isDemo: true
  }
];

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
 * Downloads the GPX file for a given ride ID from Firestore subcollection.
 * @param {string} rideId
 * @param {string} fileNamePrefix
 */
export async function downloadGpx(rideId, fileNamePrefix = "ride") {
  // Check if it's a demo ride
  const demoRide = MOCK_RIDES.find(r => r.id === rideId);
  if (demoRide) {
    const demoGpx = generateDemoGpx(demoRide);
    triggerFileDownload(demoGpx, `${demoRide.id}.gpx`);
    return;
  }

  try {
    const gpxDocRef = doc(db, "rides", rideId, "details", "gpx");
    const snap = await getDoc(gpxDocRef);

    if (!snap.exists() || !snap.data().gpxContent) {
      throw new Error("No GPX file content found for this ride in Firestore.");
    }

    const gpxXml = snap.data().gpxContent;
    triggerFileDownload(gpxXml, `${fileNamePrefix}_${rideId}.gpx`);
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
