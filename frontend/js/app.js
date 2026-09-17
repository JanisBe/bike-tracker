import {downloadGpx, fetchAllRides, fetchRideProfile} from './ride-service.js';
import {
    clearScrubMarker,
    displayAllRoutesOverview,
    displaySingleRoute,
    initMap,
    setScrubMarker
} from './map-renderer.js';
import {calculateOverallStats, formatDate, formatDuration} from './stats.js';
import {CalendarWidget} from './calendar-widget.js';
import {ProfileChart} from './profile-chart.js';

let allRides = [];
let selectedRideId = null;
let calendarWidget = null;
let profileChart = null;

// DOM Elements
const ridesListContainer = document.getElementById("rides-list");
const statTotalDistance = document.getElementById("stat-total-distance");
const statTotalRides = document.getElementById("stat-total-rides");
const statTotalHours = document.getElementById("stat-total-hours");
const searchInput = document.getElementById("search-input");
const btnOverview = document.getElementById("btn-overview");
const calendarContainer = document.getElementById("calendar-widget-container");

const floatingDetail = document.getElementById("floating-detail");
const detailTitle = document.getElementById("detail-title");
const detailDate = document.getElementById("detail-date");
const detailDist = document.getElementById("detail-dist");
const detailDur = document.getElementById("detail-dur");
const detailAvg = document.getElementById("detail-avg");
const detailEle = document.getElementById("detail-ele");
const btnDetailGpx = document.getElementById("btn-detail-gpx");

async function initApp() {
  // 1. Initialize Map
  initMap("map");

    // Initialize Profile Chart
    const profileContainer = document.getElementById("profile-chart-panel");
    const profileCanvas = document.getElementById("profile-canvas");
    if (profileContainer && profileCanvas) {
        profileChart = new ProfileChart({
            container: profileContainer,
            canvas: profileCanvas,
            btnSpeed: document.getElementById("btn-toggle-speed"),
            btnElevation: document.getElementById("btn-toggle-elevation"),
            btnCollapse: document.getElementById("btn-toggle-chart-collapse"),
            chartBody: document.getElementById("profile-chart-body"),
            tooltipContainer: document.getElementById("profile-scrub-tooltip"),
            scrubDist: document.getElementById("scrub-dist"),
            scrubSpeed: document.getElementById("scrub-speed"),
            scrubEle: document.getElementById("scrub-ele"),
            collapseIcon: document.getElementById("collapse-icon"),
            onPointHover: (point) => {
                if (point && point.lat != null && point.lon != null) {
                    setScrubMarker(point.lat, point.lon);
                } else {
                    clearScrubMarker();
                }
            }
        });
    }

  // 2. Fetch Rides from Firestore (with fallback)
  try {
    ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 40px; color: var(--text-muted);">
        <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
        <p style="margin-top: 12px; font-size: 13px;">Wczytywanie treningów rowerowych...</p>
      </div>
    `;

    allRides = await fetchAllRides();

    // 3. Update Overall Stats
    updateStatsBanner(allRides);

    // 4. Initialize Calendar Widget
    if (calendarContainer) {
      calendarWidget = new CalendarWidget(calendarContainer, {
        onSelectRide: (rideId) => {
          selectRide(rideId);
          // Also scroll to the card in the list
          const card = document.getElementById(`card-${rideId}`);
          if (card) {
            card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
          }
        }
      });
      calendarWidget.setRides(allRides);
    }

    // 5. Render Ride List
    renderRidesList(allRides);

    // 6. Select first ride by default or show overview
    if (allRides.length > 0) {
      selectRide(allRides[0].id);
    }
  } catch (error) {
    console.error("Initialization error:", error);
    ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 30px; color: var(--accent-red);">
        Nie udało się wczytać treningów. Sprawdź konsolę.
      </div>
    `;
  }

  // Setup Event Listeners
  setupEventListeners();
}

function updateStatsBanner(rides) {
  const stats = calculateOverallStats(rides);
  if (statTotalDistance) statTotalDistance.textContent = `${stats.totalDistanceKm} km`;
  if (statTotalRides) statTotalRides.textContent = stats.totalRides.toString();
  if (statTotalHours) statTotalHours.textContent = `${stats.totalDurationHours} h`;
}

function renderRidesList(rides) {
  ridesListContainer.innerHTML = "";

  if (rides.length === 0) {
    ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 40px; color: var(--text-muted); font-size: 14px;">
        Brak treningów pasujących do filtra.
      </div>
    `;
    return;
  }

  rides.forEach(ride => {
    const card = document.createElement("div");
    card.className = `ride-card ${ride.id === selectedRideId ? 'selected' : ''}`;
    card.id = `card-${ride.id}`;

    const dateFormatted = formatDate(ride.startTime);
    const durationFormatted = formatDuration(ride.durationSeconds);
    const elevationStr = ride.elevationGain != null ? `↑ ${ride.elevationGain}m` : '';

    card.innerHTML = `
      <div class="ride-card-header">
        <span class="ride-date">${dateFormatted}</span>
        ${ride.isDemo ? '<span class="badge-demo">Demo</span>' : ''}
      </div>
      <div class="ride-card-main">
        <div class="ride-distance">
          ${ride.distanceKm.toFixed(1)}<span>km</span>
        </div>
        <div class="ride-duration">${durationFormatted}</div>
      </div>
      <div class="ride-card-footer">
        <div class="ride-metric">
          <span>⚡</span> ${ride.avgSpeedKmh.toFixed(1)} km/h
          ${elevationStr ? `&nbsp;&nbsp;<span>⛰️</span> ${elevationStr}` : ''}
        </div>
        <button class="btn-download-gpx" data-ride-id="${ride.id}" title="Pobierz plik GPX">
          <span>⬇</span> GPX
        </button>
      </div>
    `;

    // Click on card selects route on map
    card.addEventListener("click", (e) => {
      // Don't trigger selection if download button was clicked
      if (e.target.closest(".btn-download-gpx")) return;
      selectRide(ride.id);
    });

    // GPX download click
    const downloadBtn = card.querySelector(".btn-download-gpx");
    downloadBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      downloadGpx(ride.id, `ride_${ride.id}`);
    });

    ridesListContainer.appendChild(card);
  });
}

async function selectRide(rideId) {
  selectedRideId = rideId;

  // Update card highlighting
  document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
  const activeCard = document.getElementById(`card-${rideId}`);
  if (activeCard) {
    activeCard.classList.add("selected");
  }

  const ride = allRides.find(r => r.id === rideId);
  if (!ride) return;

  // Render on Leaflet Map
  displaySingleRoute(ride);

  // Update floating detail panel
  showFloatingDetail(ride);

    // Fetch and display route profile chart
    if (profileChart) {
        try {
            const points = await fetchRideProfile(ride);
            if (selectedRideId === rideId) {
                profileChart.setData(points);
            }
        } catch (err) {
            console.warn("Failed to load profile points for ride:", err);
            profileChart.hide();
        }
    }
}

function showFloatingDetail(ride) {
  if (!floatingDetail) return;

  detailTitle.textContent = ride.title || `Trening z dnia ${formatDate(ride.startTime)}`;
  detailDate.textContent = formatDate(ride.startTime);
  detailDist.textContent = `${ride.distanceKm.toFixed(2)} km`;
  detailDur.textContent = formatDuration(ride.durationSeconds);
  detailAvg.textContent = `${ride.avgSpeedKmh.toFixed(1)} km/h`;
  detailEle.textContent = ride.elevationGain != null ? `+${ride.elevationGain} m` : '—';

  btnDetailGpx.onclick = () => {
    downloadGpx(ride.id, `ride_${ride.id}`);
  };

  floatingDetail.classList.add("active");
}

function setupEventListeners() {
  // Search input filter
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      const term = e.target.value.toLowerCase().trim();
      const filtered = allRides.filter(r => {
        const titleMatch = (r.title || "").toLowerCase().includes(term);
        const distMatch = r.distanceKm.toString().includes(term);
        const dateMatch = formatDate(r.startTime).toLowerCase().includes(term);
        return titleMatch || distMatch || dateMatch;
      });
      renderRidesList(filtered);
    });
  }

  // Show all routes overview
  if (btnOverview) {
    btnOverview.addEventListener("click", () => {
      document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
      selectedRideId = null;
      if (floatingDetail) floatingDetail.classList.remove("active");
        if (profileChart) profileChart.hide();
        clearScrubMarker();
      displayAllRoutesOverview(allRides);
    });
  }
}


// Add spinning animation for loading spinner
const style = document.createElement("style");
style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
document.head.appendChild(style);

// Start on DOMContentLoaded
document.addEventListener("DOMContentLoaded", initApp);
