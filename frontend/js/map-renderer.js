import { decodePolyline } from './polyline-decoder.js';

let mapInstance = null;
let currentRouteLayer = null;
let markersLayer = null;
let allRoutesLayerGroup = null;

// Custom SVG marker icons
function createCustomMarkerIcon(type) {
  const isStart = type === 'start';
  const color = isStart ? '#2ECC71' : '#FF4757';
  const label = isStart ? 'A' : 'B';

  const svgHtml = `
    <div style="
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: #0F0F23;
      border: 2px solid ${color};
      border-radius: 50%;
      box-shadow: 0 0 12px ${color}88, 0 4px 8px rgba(0,0,0,0.5);
      color: ${color};
      font-weight: 800;
      font-size: 14px;
      font-family: 'Outfit', sans-serif;
    ">
      ${label}
    </div>
  `;

  return L.divIcon({
    html: svgHtml,
    className: 'custom-map-pin',
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -16]
  });
}

/**
 * Initializes the Leaflet map in the given container.
 * @param {string} containerId
 */
export function initMap(containerId = "map") {
  if (mapInstance) {
    mapInstance.remove();
  }

  // Default coordinates: Central Poland / Warsaw
  mapInstance = L.map(containerId, {
    zoomControl: false,
    attributionControl: true
  }).setView([52.2297, 21.0122], 12);

  // Add zoom control on top right
  L.control.zoom({ position: "topright" }).addTo(mapInstance);

  // Standard OpenStreetMap tiles (100% free, no API key required)
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 19
  }).addTo(mapInstance);

  markersLayer = L.layerGroup().addTo(mapInstance);
  allRoutesLayerGroup = L.layerGroup().addTo(mapInstance);

  return mapInstance;
}

/**
 * Renders a single selected route on the map, fits bounds and adds start/finish pins.
 * @param {Object} ride
 */
export function displaySingleRoute(ride) {
  if (!mapInstance) return;

  // Clear previous single route and markers
  if (currentRouteLayer) {
    mapInstance.removeLayer(currentRouteLayer);
    currentRouteLayer = null;
  }
  markersLayer.clearLayers();

  const coords = decodePolyline(ride.encodedPolyline);
  if (coords.length < 2) {
    console.warn("Not enough coordinates to render polyline");
    return;
  }

  // Draw Route Polyline with a subtle glowing aura and main colored line
  currentRouteLayer = L.layerGroup();

  // Glow aura
  const aura = L.polyline(coords, {
    color: '#FF6B35',
    weight: 10,
    opacity: 0.25,
    lineCap: 'round',
    lineJoin: 'round'
  });

  // Solid bright line
  const mainLine = L.polyline(coords, {
    color: '#FF6B35',
    weight: 4,
    opacity: 0.95,
    lineCap: 'round',
    lineJoin: 'round'
  });

  currentRouteLayer.addLayer(aura);
  currentRouteLayer.addLayer(mainLine);
  currentRouteLayer.addTo(mapInstance);

  // Start Marker (Green Pin)
  const startPt = coords[0];
  const startMarker = L.marker(startPt, {
    icon: createCustomMarkerIcon('start'),
    title: 'Start'
  }).bindPopup(`<b>Start</b><br>${ride.distanceKm.toFixed(1)} km total`);
  markersLayer.addLayer(startMarker);

  // Finish Marker (Red Pin)
  const finishPt = coords[coords.length - 1];
  const finishMarker = L.marker(finishPt, {
    icon: createCustomMarkerIcon('finish'),
    title: 'Finish'
  }).bindPopup(`<b>Finish</b><br>Avg Speed: ${ride.avgSpeedKmh.toFixed(1)} km/h`);
  markersLayer.addLayer(finishMarker);

  // Smooth camera pan & zoom
  const bounds = mainLine.getBounds();
  mapInstance.flyToBounds(bounds, {
    padding: [60, 60],
    duration: 1.2,
    easeLinearity: 0.25
  });
}

/**
 * Fits view to encompass all available rides
 * @param {Array<Object>} rides
 */
export function displayAllRoutesOverview(rides) {
  if (!mapInstance || !rides || rides.length === 0) return;

  allRoutesLayerGroup.clearLayers();
  if (currentRouteLayer) {
    mapInstance.removeLayer(currentRouteLayer);
    currentRouteLayer = null;
  }
  markersLayer.clearLayers();

  const allPoints = [];

  rides.forEach(ride => {
    const coords = decodePolyline(ride.encodedPolyline);
    if (coords.length >= 2) {
      allPoints.push(...coords);

      const line = L.polyline(coords, {
        color: '#4ECDC4',
        weight: 3,
        opacity: 0.6,
        lineCap: 'round'
      });
      allRoutesLayerGroup.addLayer(line);
    }
  });

  if (allPoints.length > 0) {
    const polyline = L.polyline(allPoints);
    mapInstance.flyToBounds(polyline.getBounds(), { padding: [50, 50], duration: 1.0 });
  }
}
