import {decodePolyline} from './polyline-decoder.js';

let mapInstance = null;
let currentRouteLayer = null;
let markersLayer = null;
let allRoutesLayerGroup = null;

let currentBaseLayer = null;
let currentBaseMapKey = 'osm-dark';
const activeOverlayLayers = new Map();

export const BASEMAPS = {
  'osm-dark': {
    name: 'OpenStreetMap (Ciemna)',
    badge: 'Ciemna',
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    options: {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors',
      className: 'tile-filter-dark'
    }
  },
  'cyclosm': {
    name: 'CyclOSM (Rowerowa)',
    badge: 'CyclOSM',
    url: 'https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png',
    options: {
      subdomains: ['a', 'b', 'c'],
      maxZoom: 20,
      attribution: '&copy; <a href="https://www.cyclosm.org" target="_blank" rel="noopener">CyclOSM</a> | &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>'
    }
  },
  'opentopomap': {
    name: 'OpenTopoMap (Topograficzna)',
    badge: 'OpenTopo',
    url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
    options: {
      subdomains: ['a', 'b', 'c'],
      maxNativeZoom: 17,
      maxZoom: 19,
      attribution: 'Mapa: &copy; <a href="https://openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>, SRTM | Widok: &copy; <a href="https://opentopomap.org" target="_blank" rel="noopener">OpenTopoMap</a>'
    }
  },
  'ump': {
    name: 'UMP-pcPL (Polska)',
    badge: 'UMP-pcPL',
    url: 'https://tiles.ump.waw.pl/ump_tiles/{z}/{x}/{y}.png',
    options: {
      maxNativeZoom: 18,
      maxZoom: 19,
      attribution: '&copy; <a href="https://ump.waw.pl/" target="_blank" rel="noopener">UMP-pcPL</a> contributors'
    }
  },
  'osm-standard': {
    name: 'OpenStreetMap (Standardowa)',
    badge: 'OSM Jasna',
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    options: {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors'
    }
  }
};

export const OVERLAYS = {
  'waymarked-cycling': {
    name: 'Szlaki rowerowe',
    url: 'https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png',
    options: {
      maxNativeZoom: 18,
      maxZoom: 19,
      opacity: 0.9,
      attribution: 'Szlaki rowerowe: &copy; <a href="https://cycling.waymarkedtrails.org" target="_blank" rel="noopener">Waymarked Trails</a>'
    }
  },
  'waymarked-hiking': {
    name: 'Szlaki piesze',
    url: 'https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png',
    options: {
      maxNativeZoom: 18,
      maxZoom: 19,
      opacity: 0.9,
      attribution: 'Szlaki piesze: &copy; <a href="https://hiking.waymarkedtrails.org" target="_blank" rel="noopener">Waymarked Trails</a>'
    }
  },
  'waymarked-mtb': {
    name: 'Trasy MTB',
    url: 'https://tile.waymarkedtrails.org/mtb/{z}/{x}/{y}.png',
    options: {
      maxNativeZoom: 18,
      maxZoom: 19,
      opacity: 0.9,
      attribution: 'Trasy MTB: &copy; <a href="https://mtb.waymarkedtrails.org" target="_blank" rel="noopener">Waymarked Trails</a>'
    }
  }
};

/**
 * Sets the active basemap tile layer.
 * @param {string} key
 */
export function setBaseMap(key) {
  if (!mapInstance || !BASEMAPS[key]) return;

  if (currentBaseLayer) {
    mapInstance.removeLayer(currentBaseLayer);
    currentBaseLayer = null;
  }

  const def = BASEMAPS[key];
  currentBaseMapKey = key;
  currentBaseLayer = L.tileLayer(def.url, def.options);
  currentBaseLayer.addTo(mapInstance);
  currentBaseLayer.bringToBack();

  try {
    localStorage.setItem('bike_tracker_basemap', key);
  } catch (err) {
    console.warn('Unable to persist basemap preference:', err);
  }

  const badgeEl = document.getElementById('map-layer-badge');
  if (badgeEl) {
    badgeEl.textContent = def.badge;
  }

  const radio = document.querySelector(`input[name="basemap-option"][value="${key}"]`);
  if (radio) {
    radio.checked = true;
  }
}

/**
 * Enables or disables a tile overlay layer.
 * @param {string} key
 * @param {boolean} enable
 */
export function toggleOverlay(key, enable) {
  if (!mapInstance || !OVERLAYS[key]) return;

  const def = OVERLAYS[key];
  if (enable) {
    if (!activeOverlayLayers.has(key)) {
      const layer = L.tileLayer(def.url, def.options);
      layer.addTo(mapInstance);
      activeOverlayLayers.set(key, layer);
    }
  } else if (activeOverlayLayers.has(key)) {
    const layer = activeOverlayLayers.get(key);
    mapInstance.removeLayer(layer);
    activeOverlayLayers.delete(key);
  }

  try {
    const activeKeys = Array.from(activeOverlayLayers.keys());
    localStorage.setItem('bike_tracker_overlays', JSON.stringify(activeKeys));
  } catch (err) {
    console.warn('Unable to persist overlay preference:', err);
  }

  const checkbox = document.querySelector(`input[name="overlay-option"][value="${key}"]`);
  if (checkbox) {
    checkbox.checked = enable;
  }
}

/**
 * Hooks up event handlers for the map layer selector dropdown.
 */
export function initLayerControls() {
  const container = document.getElementById('map-layer-control');
  const btn = document.getElementById('btn-map-layers');
  const dropdown = document.getElementById('map-layers-dropdown');
  if (!btn || !dropdown) return;

  if (container) {
    L.DomEvent.disableClickPropagation(container);
    L.DomEvent.disableScrollPropagation(container);
  }

  const openDropdown = () => {
    dropdown.classList.add('is-open');
    btn.setAttribute('aria-expanded', 'true');
  };

  const closeDropdown = () => {
    dropdown.classList.remove('is-open');
    btn.setAttribute('aria-expanded', 'false');
  };

  btn.onclick = (e) => {
    e?.stopPropagation();
    if (dropdown.classList.contains('is-open')) {
      closeDropdown();
    } else {
      openDropdown();
    }
  };

  document.addEventListener('click', (e) => {
    if (!dropdown.contains(e.target) && !btn.contains(e.target)) {
      closeDropdown();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && dropdown.classList.contains('is-open')) {
      closeDropdown();
      btn.focus();
    }
  });

  if (mapInstance) {
    mapInstance.on('movestart zoomstart', closeDropdown);
  }

  const basemapRadios = dropdown.querySelectorAll('input[name="basemap-option"]');
  basemapRadios.forEach((radio) => {
    radio.onchange = () => {
      if (radio.checked) {
        setBaseMap(radio.value);
      }
    };
  });

  const overlayCheckboxes = dropdown.querySelectorAll('input[name="overlay-option"]');
  overlayCheckboxes.forEach((cb) => {
    cb.onchange = () => {
      toggleOverlay(cb.value, cb.checked);
    };
  });
}

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
    activeOverlayLayers.clear();
    currentBaseLayer = null;
  }

  // Default coordinates: Central Poland / Warsaw
  mapInstance = L.map(containerId, {
    zoomControl: false,
    attributionControl: true
  }).setView([52.2297, 21.0122], 12);

  // Add zoom control on top right
  L.control.zoom({ position: "topright" }).addTo(mapInstance);

  // Load saved preferences or defaults
  let savedBaseMap = 'osm-dark';
  let savedOverlays = [];
  try {
    const basemapPref = localStorage.getItem('bike_tracker_basemap');
    if (basemapPref && BASEMAPS[basemapPref]) {
      savedBaseMap = basemapPref;
    }
    const overlaysPref = localStorage.getItem('bike_tracker_overlays');
    if (overlaysPref) {
      const parsed = JSON.parse(overlaysPref);
      if (Array.isArray(parsed)) {
        savedOverlays = parsed;
      }
    }
  } catch (err) {
    console.warn('Error reading map preferences:', err);
  }

  // Set base layer
  setBaseMap(savedBaseMap);

  // Apply saved overlays
  savedOverlays.forEach((key) => {
    if (OVERLAYS[key]) {
      toggleOverlay(key, true);
    }
  });

  markersLayer = L.layerGroup().addTo(mapInstance);
  allRoutesLayerGroup = L.layerGroup().addTo(mapInstance);

  // Wire up UI layer controls
  initLayerControls();

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
  clearScrubMarker();

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
  }).bindPopup(`<b>Start</b><br>Łącznie: ${ride.distanceKm.toFixed(1)} km`);
  markersLayer.addLayer(startMarker);

  // Finish Marker (Red Pin)
  const finishPt = coords.at(-1);
  const finishMarker = L.marker(finishPt, {
    icon: createCustomMarkerIcon('finish'),
    title: 'Meta'
  }).bindPopup(`<b>Meta</b><br>Śr. prędkość: ${ride.avgSpeedKmh.toFixed(1)} km/h`);
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
  if (!mapInstance) return;

  allRoutesLayerGroup.clearLayers();
  if (currentRouteLayer) {
    mapInstance.removeLayer(currentRouteLayer);
    currentRouteLayer = null;
  }
  markersLayer.clearLayers();

  if (!rides || rides.length === 0) return;

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

let scrubMarker = null;

/**
 * Places or updates a pulsing synchronized marker on the map during chart scrubbing.
 * @param {number} lat
 * @param {number} lon
 */
export function setScrubMarker(lat, lon) {
  if (!mapInstance || lat == null || lon == null) return;

  if (!scrubMarker) {
    const icon = L.divIcon({
      html: `
        <div class="scrub-pulsing-wrapper">
          <div class="scrub-pulsing-glow"></div>
          <div class="scrub-pulsing-dot"></div>
        </div>
      `,
      className: 'scrub-marker-icon',
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    });
    scrubMarker = L.marker([lat, lon], {icon, zIndexOffset: 2000}).addTo(mapInstance);
  } else {
    scrubMarker.setLatLng([lat, lon]);
  }
}

/**
 * Removes the scrub marker from the map.
 */
export function clearScrubMarker() {
  if (scrubMarker && mapInstance) {
    mapInstance.removeLayer(scrubMarker);
    scrubMarker = null;
  }
}

/**
 * Recomputes map container size and redraws tiles
 */
export function invalidateMapSize() {
  if (mapInstance) {
    mapInstance.invalidateSize();
  }
}

