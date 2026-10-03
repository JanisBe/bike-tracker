import { downloadGpx, fetchRideById, fetchRideProfile, fetchUserRides, updateRideTitle } from './ride-service.js';
import { decodePolyline } from './polyline-decoder.js';
import {
  clearScrubMarker,
  displayAllRoutesOverview,
  displaySingleRoute,
  initMap,
  invalidateMapSize,
  setScrubMarker
} from './map-renderer.js?v=3';
import { calculateOverallStats, formatDate, formatDuration } from './stats.js';
import { CalendarWidget } from './calendar-widget.js';
import { ProfileChart } from './profile-chart.js';
import { onAuthStateChange, signInWithEmail, signInWithGoogle, signOutUser, signUpWithEmail } from './auth-service.js';

let allRides = [];
let selectedRideId = null;
let calendarWidget = null;
let profileChart = null;
let currentUser = null;
let authMode = 'login'; // 'login' | 'register'
let isMapFullscreen = false;
let editingRideId = null;

// DOM Elements
const mapContainer = document.getElementById("map-container");
const mapWrapper = document.getElementById("map-wrapper");
const btnBackList = document.getElementById("btn-back-list");
const btnMobileEdit = document.getElementById("btn-mobile-edit");
const btnMobileShare = document.getElementById("btn-mobile-share");
const btnMapFullscreen = document.getElementById("btn-map-fullscreen");

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
const btnEditRide = document.getElementById("btn-edit-ride");
const btnShareRide = document.getElementById("btn-share-ride");
const toastContainer = document.getElementById("toast-container");

// Edit Title Modal DOM Elements
const editTitleModal = document.getElementById("edit-title-modal");
const editTitleModalSubtitle = document.getElementById("edit-title-modal-subtitle");
const btnEditTitleClose = document.getElementById("btn-edit-title-close");
const btnEditTitleCancel = document.getElementById("btn-edit-title-cancel");
const editTitleForm = document.getElementById("edit-title-form");
const editTitleInput = document.getElementById("edit-title-input");
const btnEditTitleSubmit = document.getElementById("btn-edit-title-submit");
const editTitleSpinner = document.getElementById("edit-title-spinner");
const editTitleSubmitText = document.getElementById("edit-title-submit-text");
const editTitleAlert = document.getElementById("edit-title-alert");
const editTitleAlertMsg = document.getElementById("edit-title-alert-msg");

// Auth DOM Elements
const userInfoEl = document.getElementById("user-info");
const userGuestEl = document.getElementById("user-guest");
const userAvatarEl = document.getElementById("user-avatar");
const userNameEl = document.getElementById("user-name");
const userEmailEl = document.getElementById("user-email");
const btnLogout = document.getElementById("btn-logout");
const btnLoginOpen = document.getElementById("btn-login-open");

const authModal = document.getElementById("auth-modal");
const btnAuthClose = document.getElementById("btn-auth-close");
const tabLogin = document.getElementById("tab-login");
const tabRegister = document.getElementById("tab-register");
const authModalSubtitle = document.getElementById("auth-modal-subtitle");
const authAlert = document.getElementById("auth-alert");
const authAlertMsg = document.getElementById("auth-alert-msg");
const btnGoogleAuth = document.getElementById("btn-google-auth");
const authForm = document.getElementById("auth-form");
const authEmailInput = document.getElementById("auth-email");
const authPasswordInput = document.getElementById("auth-password");
const btnAuthSubmit = document.getElementById("btn-auth-submit");
const authSubmitText = document.getElementById("auth-submit-text");
const authSpinner = document.getElementById("auth-spinner");

// ==========================================================================
// Deep Linking, URL Routing & Share Helpers
// ==========================================================================

function getRideIdFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const rideParam = params.get('ride');
  if (rideParam) return rideParam.trim();

  const hash = window.location.hash;
  if (hash) {
    const hashMatch = new RegExp(/(?:#|\/|\?|&)ride=([^&]+)/).exec(hash) || hash.match(/#\/?ride\/(.+)/);
    if (hashMatch?.[1]) {
      return decodeURIComponent(hashMatch[1]).trim();
    }
  }
  return null;
}

function buildShareUrl(rideId) {
  const url = new URL(window.location.href);
  url.searchParams.set('ride', rideId);
  url.hash = '';
  return url.toString();
}

function updateUrlWithRideId(rideId) {
  const url = new URL(window.location.href);
  if (rideId) {
    url.searchParams.set('ride', rideId);
  } else {
    url.searchParams.delete('ride');
  }
  url.hash = '';
  window.history.replaceState({ rideId }, '', url.toString());
}

let toastTimeout = null;
function showToast(message, type = "info") {
  if (!toastContainer) return;
  toastContainer.innerHTML = "";
  if (toastTimeout) clearTimeout(toastTimeout);

  const toast = document.createElement("div");
  toast.className = `toast-notification toast-${type}`;

  let icon = "ℹ️";
  if (type === "success") icon = "✅";
  if (type === "error") icon = "⚠️";

  toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
  toastContainer.appendChild(toast);

  requestAnimationFrame(() => {
    toast.classList.add("active");
  });

  toastTimeout = setTimeout(() => {
    toast.classList.remove("active");
    setTimeout(() => toast.remove(), 300);
  }, 2800);
}

function toggleMapFullscreen() {
  isMapFullscreen = !isMapFullscreen;
  if (mapWrapper) {
    mapWrapper.classList.toggle("is-fullscreen", isMapFullscreen);
    const enterIcon = mapWrapper.querySelector(".icon-enter-fullscreen");
    const exitIcon = mapWrapper.querySelector(".icon-exit-fullscreen");
    if (enterIcon) enterIcon.style.display = isMapFullscreen ? "none" : "block";
    if (exitIcon) exitIcon.style.display = isMapFullscreen ? "block" : "none";
  }
  invalidateMapSize();
}

async function handleShareClick(e) {
  if (e) e.preventDefault();
  if (!selectedRideId) return;

  const ride = allRides.find(r => r.id === selectedRideId);
  if (!ride) return;

  const shareUrl = buildShareUrl(ride.id);
  const rideTitle = ride.locationName || ride.title || `Trening ${formatDate(ride.startTime)}`;
  const shareText = `Zobacz mój trening rowerowy (${ride.distanceKm.toFixed(1)} km) w aplikacji Bike Tracker!`;

  // Try Web Share API (native mobile share sheet / macOS / Windows)
  if (navigator.share) {
    try {
      await navigator.share({
        title: `Bike Tracker • ${rideTitle}`,
        text: shareText,
        url: shareUrl
      });
      showToast("Udostępniono trening!", "success");
      return;
    } catch (err) {
      if (err.name === 'AbortError') return; // User closed sheet
      console.warn("navigator.share failed, fallback to clipboard:", err);
    }
  }

  // Fallback: Copy to clipboard
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(shareUrl);
    } else {
      const textArea = document.createElement("textarea");
      textArea.value = shareUrl;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand("copy");
      textArea.remove();
    }

    if (btnShareRide) {
      btnShareRide.classList.add("copied");
      setTimeout(() => {
        btnShareRide.classList.remove("copied");
      }, 2000);
    }
    if (btnMobileShare) {
      btnMobileShare.classList.add("copied");
      setTimeout(() => {
        btnMobileShare.classList.remove("copied");
      }, 2000);
    }

    showToast("Skopiowano link do treningu do schowka! 🔗", "success");
  } catch (err) {
    console.error("Clipboard copy failed:", err);
    showToast("Nie udało się skopiować linku automatycznie.", "error");
  }
}

async function initApp() {
  // 1. Initialize Map
  initMap("map");

  // 2. Initialize Profile Chart
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
        if (point?.lat != null && point?.lon != null) {
          setScrubMarker(point.lat, point.lon);
        } else {
          clearScrubMarker();
        }
      }
    });
  }

  // 3. Setup UI & Auth Listeners
  setupEventListeners();
  setupAuthListeners();

  // 4. Initialize Calendar Widget skeleton
  if (calendarContainer) {
    calendarWidget = new CalendarWidget(calendarContainer, {
      onSelectRide: (rideId) => {
        selectRide(rideId);
        const card = document.getElementById(`card-${rideId}`);
        if (card) {
          card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      }
    });
  }

  // 5. Watch Auth State
  onAuthStateChange(async (user) => {
    currentUser = user;
    if (user) {
      // User signed in
      if (userInfoEl) userInfoEl.style.display = "flex";
      if (userGuestEl) userGuestEl.style.display = "none";

      const displayName = user.displayName || (user.email ? user.email.split('@')[0] : 'Użytkownik');
      if (userNameEl) userNameEl.textContent = displayName;
      if (userEmailEl) userEmailEl.textContent = user.email || '';

      if (userAvatarEl) {
        if (user.photoURL) {
          userAvatarEl.innerHTML = `<img src="${user.photoURL}" alt="Avatar" referrerpolicy="no-referrer">`;
        } else {
          userAvatarEl.textContent = displayName.charAt(0).toUpperCase();
        }
      }

      closeAuthModal();
      await loadUserRides(user.uid);
    } else {
      // User signed out
      if (userInfoEl) userInfoEl.style.display = "none";
      if (userGuestEl) userGuestEl.style.display = "flex";

      const urlRideId = getRideIdFromUrl();

      if (urlRideId) {
        // Guest opened a shared ride link directly!
        closeAuthModal();
        ridesListContainer.innerHTML = `
                  <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                    <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
                    <p style="margin-top: 12px; font-size: 13px;">Wczytywanie udostępnionego treningu...</p>
                  </div>
                `;

        try {
          const sharedRide = await fetchRideById(urlRideId);
          if (sharedRide) {
            sharedRide.isShared = true;
            allRides = [sharedRide];
            updateStatsBanner(allRides);
            if (calendarWidget) calendarWidget.setRides(allRides);
            renderRidesList(allRides);
            enrichRidesWithLocation(allRides);
            selectRide(sharedRide.id);
            showToast("Wyświetlasz udostępniony trening", "info");
            return;
          } else {
            showToast("Nie znaleziono wskazanego treningu", "error");
          }
        } catch (err) {
          console.error("Error loading shared ride for guest:", err);
        }
      }

      allRides = [];
      selectedRideId = null;

      updateStatsBanner([]);
      if (calendarWidget) {
        calendarWidget.setRides([]);
      }
      if (floatingDetail) {
        floatingDetail.classList.remove("active");
      }
      if (profileChart) {
        profileChart.hide();
      }
      clearScrubMarker();
      displayAllRoutesOverview([]);
      renderRidesList([]);

      if (!urlRideId) {
        openAuthModal('login');
      }
    }
  });
}

/**
 * Loads rides for authenticated user
 * @param {string} userId
 */
async function loadUserRides(userId) {
  const urlRideId = getRideIdFromUrl();
  try {
    ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 40px; color: var(--text-muted);">
        <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
        <p style="margin-top: 12px; font-size: 13px;">Wczytywanie Twoich treningów...</p>
      </div>
    `;

    allRides = await fetchUserRides(userId);

    let targetRideId = null;

    // If URL has a specific ride ID, check if it's in allRides or fetch as shared ride
    if (urlRideId) {
      const existing = allRides.find(r => r.id === urlRideId);
      if (existing) {
        targetRideId = existing.id;
      } else {
        try {
          const sharedRide = await fetchRideById(urlRideId);
          if (sharedRide) {
            sharedRide.isShared = true;
            allRides = [sharedRide, ...allRides];
            targetRideId = sharedRide.id;
            showToast("Wczytano udostępniony trening", "info");
          } else {
            showToast("Nie znaleziono treningu z podanego linku", "error");
          }
        } catch (err) {
          console.warn("Could not fetch shared ride:", err);
        }
      }
    }

    const isMobile = window.innerWidth <= 860;
    if (!targetRideId && allRides.length > 0) {
      if (!isMobile) {
        targetRideId = allRides[0].id;
      }
    }


    // Update Overall Stats
    updateStatsBanner(allRides);

    // Update Calendar Widget
    if (calendarWidget) {
      calendarWidget.setRides(allRides);
    }

    // Render Ride List
    renderRidesList(allRides);

    // Enrich legacy rides with location from OSM in background
    enrichRidesWithLocation(allRides);

    // Select targeted ride or clear map
    if (targetRideId) {
      selectRide(targetRideId);
      const card = document.getElementById(`card-${targetRideId}`);
      if (card) {
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    } else {
      if (floatingDetail) floatingDetail.classList.remove("active");
      if (profileChart) profileChart.hide();
      clearScrubMarker();
      displayAllRoutesOverview([]);
    }
  } catch (error) {
    console.error("Error loading user rides:", error);
    ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 30px; color: var(--accent-red);">
        Nie udało się wczytać treningów. Sprawdź konsolę.
      </div>
    `;
  }
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
    if (!currentUser) {
      ridesListContainer.innerHTML = `
        <div style="text-align: center; padding: 40px 16px; color: var(--text-muted); font-size: 13px;">
          <p style="margin-bottom: 14px;">Zaloguj się, aby wyświetlić swoje treningi.</p>
          <button class="btn-login-open" style="display: inline-flex; width: auto; margin: 0 auto;" id="btn-list-login">
            Zaloguj się
          </button>
        </div>
      `;
      const btn = document.getElementById("btn-list-login");
      if (btn) btn.addEventListener("click", () => openAuthModal('login'));
    } else if (searchInput && searchInput.value.trim().length > 0) {
      ridesListContainer.innerHTML = `
        <div style="text-align: center; padding: 40px; color: var(--text-muted); font-size: 13px;">
          Brak treningów pasujących do wyszukiwania.
        </div>
      `;
    } else {
      ridesListContainer.innerHTML = `
        <div style="text-align: center; padding: 40px 20px; color: var(--text-muted); font-size: 13px; line-height: 1.6;">
          <div style="font-size: 32px; margin-bottom: 10px;">🚴</div>
          <strong style="color: var(--text-main); font-size: 14px; display: block; margin-bottom: 6px;">Brak zarejestrowanych treningów</strong>
          <p>Uruchom aplikację mobilną Bike Tracker na telefonie i nagraj swój pierwszy przejazd!</p>
        </div>
      `;
    }
    return;
  }

  rides.forEach(ride => {
    const card = document.createElement("div");
    card.className = `ride-card ${ride.id === selectedRideId ? 'selected' : ''}`;
    card.id = `card-${ride.id}`;

    const dateFormatted = formatDate(ride.startTime);
    const durationFormatted = formatDuration(ride.durationSeconds);
    const elevationStr = ride.elevationGain != null ? `↑ ${ride.elevationGain}m` : '';
    const locationBadge = ride.locationName
      ? `<span class="ride-location" title="${ride.locationName}">📍 ${ride.locationName}</span>`
      : '';
    const sharedBadge = ride.isShared
      ? '<span class="badge-shared" title="Udostępniony trening">🔗 Udostępniony</span>'
      : '';
    const titleHtml = (ride.title && ride.title.trim().length > 0)
      ? `<div class="ride-card-title" title="${ride.title}">${ride.title}</div>`
      : '';

    card.innerHTML = `
      <div class="ride-card-header">
        <span class="ride-date">${dateFormatted}</span>
        <div class="ride-card-badges">
          ${sharedBadge}
          ${locationBadge}
          ${ride.isDemo ? '<span class="badge-demo">Demo</span>' : ''}
        </div>
      </div>
      ${titleHtml}
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
      if (e.target.closest(".btn-download-gpx")) return;
      selectRide(ride.id);
    });

    // GPX download click
    const downloadBtn = card.querySelector(".btn-download-gpx");
    downloadBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      downloadGpx(ride);
    });

    ridesListContainer.appendChild(card);
  });
}

// Reverse Geocoding Cache & Background Enrichment
const GEO_CACHE_KEY = "bike_tracker_geo_cache_v1";

function getGeoCache() {
  try {
    return JSON.parse(localStorage.getItem(GEO_CACHE_KEY) || "{}");
  } catch (e) {
    return {};
  }
}

function saveGeoCache(cache) {
  try {
    localStorage.setItem(GEO_CACHE_KEY, JSON.stringify(cache));
  } catch (e) {
    // Ignore storage quota errors
  }
}

let isEnriching = false;
async function enrichRidesWithLocation(rides) {
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
        const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=14&addressdetails=1`, {
          headers: { "Accept-Language": "pl" }
        });
        if (response.ok) {
          const data = await response.json();
          const city = data.address?.city || data.address?.town || data.address?.village || data.address?.municipality || data.address?.county || "";
          const district = data.address?.suburb || data.address?.city_district || data.address?.neighbourhood || "";
          let loc = "";
          if (city && district && city.toLowerCase() !== district.toLowerCase()) {
            loc = `${city}, ${district}`;
          } else if (city) {
            loc = city;
          } else if (district) {
            loc = district;
          } else if (data.display_name) {
            loc = data.display_name.split(",")[0];
          }

          if (loc) {
            ride.locationName = loc;
            cache[ride.id] = loc;
            saveGeoCache(cache);
            updatedCount++;
          }
        }
        // Small throttle for OSM Nominatim usage policy
        await new Promise(r => setTimeout(r, 1100));
      } catch (err) {
        console.warn("Geocoding failed for ride", ride.id, err);
      }
    }

    if (updatedCount > 0) {
      const term = searchInput ? searchInput.value.toLowerCase().trim() : "";
      if (term) {
        filterAndRenderRides(term);
      } else {
        renderRidesList(allRides);
      }
      if (selectedRideId) {
        const current = allRides.find(r => r.id === selectedRideId);
        if (current) showFloatingDetail(current);
      }
    }
  } finally {
    isEnriching = false;
  }
}

function filterAndRenderRides(term) {
  const filtered = allRides.filter(r => {
    const titleMatch = (r.title || "").toLowerCase().includes(term);
    const locationMatch = (r.locationName || "").toLowerCase().includes(term);
    const distMatch = r.distanceKm.toString().includes(term);
    const dateMatch = formatDate(r.startTime).toLowerCase().includes(term);
    return titleMatch || locationMatch || distMatch || dateMatch;
  });
  renderRidesList(filtered);
}

export function clearSelectedRide() {
  selectedRideId = null;
  updateUrlWithRideId(null);
  document.body.classList.remove("has-selected-ride");
  document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
  if (floatingDetail) floatingDetail.classList.remove("active");
  if (profileChart) profileChart.hide();
  clearScrubMarker();
  displayAllRoutesOverview(allRides);
  setTimeout(() => {
    invalidateMapSize();
  }, 60);
}

async function selectRide(rideId) {
  selectedRideId = rideId;
  updateUrlWithRideId(rideId);
  document.body.classList.add("has-selected-ride");

  // Update card highlighting
  document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
  const activeCard = document.getElementById(`card-${rideId}`);
  if (activeCard) {
    activeCard.classList.add("selected");
  }

  const ride = allRides.find(r => r.id === rideId);
  if (!ride) return;

  // On mobile scroll to top of details view
  if (mapContainer) {
    mapContainer.scrollTop = 0;
  }

  // Ensure map is measured with new layout before centering route
  invalidateMapSize();
  setTimeout(() => {
    invalidateMapSize();
  }, 50);

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

  const displayTitle = (ride.title && ride.title.trim().length > 0)
    ? ride.title
    : (ride.locationName ? `📍 ${ride.locationName}` : `Trening z dnia ${formatDate(ride.startTime)}`);

  detailTitle.textContent = displayTitle;

  const formattedDate = formatDate(ride.startTime);
  if (ride.locationName && ride.title && ride.title.trim().length > 0 && ride.locationName !== ride.title) {
    detailDate.textContent = `📍 ${ride.locationName} • ${formattedDate}`;
  } else if (ride.locationName) {
    detailDate.textContent = `${formattedDate} • Trening rowerowy`;
  } else {
    detailDate.textContent = formattedDate;
  }

  detailDist.textContent = `${ride.distanceKm.toFixed(2)} km`;
  detailDur.textContent = formatDuration(ride.durationSeconds);
  detailAvg.textContent = `${ride.avgSpeedKmh.toFixed(1)} km/h`;
  detailEle.textContent = ride.elevationGain != null ? `+${ride.elevationGain} m` : '—';

  const shareUrl = buildShareUrl(ride.id);
  if (btnShareRide) {
    btnShareRide.href = shareUrl;
    btnShareRide.dataset.shareUrl = shareUrl;
    btnShareRide.setAttribute("title", `Udostępnij trening (${ride.distanceKm.toFixed(1)} km)`);
  }

  const isOwner = currentUser && ride.userId === currentUser.uid;
  const isSharedOther = ride.isShared && !isOwner && !ride.isDemo;

  if (btnEditRide) {
    btnEditRide.style.display = isSharedOther ? "none" : "inline-flex";
    btnEditRide.setAttribute("title", `Edytuj nazwę treningu (${displayTitle})`);
  }
  if (btnMobileEdit) {
    btnMobileEdit.style.display = isSharedOther ? "none" : "inline-flex";
    btnMobileEdit.setAttribute("title", `Edytuj nazwę treningu (${displayTitle})`);
  }

  btnDetailGpx.onclick = () => {
    downloadGpx(ride);
  };

  floatingDetail.classList.add("active");
}

function setupEventListeners() {
  // Search input filter
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      const term = e.target.value.toLowerCase().trim();
      filterAndRenderRides(term);
    });
  }

  // Show all routes overview
  if (btnOverview) {
    btnOverview.addEventListener("click", () => {
      clearSelectedRide();
    });
  }

  // Back button on mobile detail view
  if (btnBackList) {
    btnBackList.addEventListener("click", () => {
      clearSelectedRide();
    });
  }

  // Mobile top-bar edit button
  if (btnMobileEdit) {
    btnMobileEdit.addEventListener("click", () => openEditTitleModal(selectedRideId));
  }

  // Mobile top-bar share button
  if (btnMobileShare) {
    btnMobileShare.addEventListener("click", handleShareClick);
  }

  // Fullscreen map button
  if (btnMapFullscreen) {
    btnMapFullscreen.addEventListener("click", toggleMapFullscreen);
  }

  // Edit ride button click in detail panel
  if (btnEditRide) {
    btnEditRide.addEventListener("click", () => openEditTitleModal(selectedRideId));
  }

  // Clicking on detail title opens edit modal as well
  if (detailTitle) {
    detailTitle.addEventListener("click", () => openEditTitleModal(selectedRideId));
    detailTitle.style.cursor = "pointer";
    detailTitle.setAttribute("title", "Kliknij, aby edytować nazwę treningu");
  }

  // Share button click in detail panel
  if (btnShareRide) {
    btnShareRide.addEventListener("click", handleShareClick);
  }

  // Edit Ride Title Modal listeners
  if (btnEditTitleClose) {
    btnEditTitleClose.addEventListener("click", closeEditTitleModal);
  }

  if (btnEditTitleCancel) {
    btnEditTitleCancel.addEventListener("click", closeEditTitleModal);
  }

  if (editTitleModal) {
    editTitleModal.addEventListener("click", (e) => {
      if (e.target === editTitleModal) {
        closeEditTitleModal();
      }
    });
  }

  if (editTitleForm) {
    editTitleForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      if (!editingRideId) return;

      const ride = allRides.find(r => r.id === editingRideId);
      if (!ride) return;

      const newTitle = editTitleInput ? editTitleInput.value.trim() : "";
      if (!newTitle) {
        showEditTitleAlert("Podaj nazwę treningu.");
        return;
      }

      setEditTitleLoading(true);
      hideEditTitleAlert();

      try {
        await updateRideTitle(ride.id, newTitle);

        // Update local object
        ride.title = newTitle;

        // If currently displayed in detail panel, refresh panel
        if (selectedRideId === ride.id) {
          showFloatingDetail(ride);
        }

        // Re-render list so card displays the new title
        renderRidesList(allRides);

        // Update calendar widget
        if (calendarWidget) {
          calendarWidget.setRides(allRides);
        }

        closeEditTitleModal();
        showToast("Zaktualizowano nazwę treningu!", "success");
      } catch (err) {
        console.error("Error updating ride title:", err);
        showEditTitleAlert(err.message || "Nie udało się zaktualizować nazwy treningu.");
      } finally {
        setEditTitleLoading(false);
      }
    });
  }

  // Handle browser back/forward history navigation
  window.addEventListener("popstate", () => {
    const urlRideId = getRideIdFromUrl();
    if (urlRideId && urlRideId !== selectedRideId) {
      const found = allRides.find(r => r.id === urlRideId);
      if (found) {
        selectRide(found.id);
      } else {
        fetchRideById(urlRideId).then(sharedRide => {
          if (sharedRide) {
            sharedRide.isShared = true;
            allRides = [sharedRide, ...allRides];
            renderRidesList(allRides);
            selectRide(sharedRide.id);
          }
        });
      }
    } else if (!urlRideId && selectedRideId) {
      clearSelectedRide();
    }
  });
}

// ==========================================================================
// Authentication Modal & UI Handlers
// ==========================================================================

function switchAuthMode(mode) {
  authMode = mode;
  hideAuthAlert();

  if (mode === 'login') {
    if (tabLogin) tabLogin.classList.add("active");
    if (tabRegister) tabRegister.classList.remove("active");
    if (authSubmitText) authSubmitText.textContent = "Zaloguj się";
    if (authModalSubtitle) authModalSubtitle.textContent = "Zaloguj się, aby wyświetlić swoje treningi";
  } else {
    if (tabRegister) tabRegister.classList.add("active");
    if (tabLogin) tabLogin.classList.remove("active");
    if (authSubmitText) authSubmitText.textContent = "Utwórz konto";
    if (authModalSubtitle) authModalSubtitle.textContent = "Zarejestruj się, aby zapisywać i przeglądać treningi";
  }
}

function openAuthModal(mode = 'login') {
  switchAuthMode(mode);
  if (authModal) authModal.style.display = "flex";
  if (authEmailInput) authEmailInput.focus();
}

function closeAuthModal() {
  if (authModal) authModal.style.display = "none";
  hideAuthAlert();
  if (authForm) authForm.reset();
  setAuthLoading(false);
}

function showAuthAlert(message) {
  if (authAlert && authAlertMsg) {
    authAlertMsg.textContent = message;
    authAlert.style.display = "flex";
  }
}

function hideAuthAlert() {
  if (authAlert) {
    authAlert.style.display = "none";
  }
}

function setAuthLoading(isLoading) {
  if (btnAuthSubmit) btnAuthSubmit.disabled = isLoading;
  if (btnGoogleAuth) btnGoogleAuth.disabled = isLoading;
  if (authSpinner) authSpinner.style.display = isLoading ? "inline-block" : "none";
  if (authSubmitText) authSubmitText.style.display = isLoading ? "none" : "inline";
}

function setupAuthListeners() {
  // Open / Close modal
  if (btnLoginOpen) {
    btnLoginOpen.addEventListener("click", () => openAuthModal('login'));
  }

  if (btnAuthClose) {
    btnAuthClose.addEventListener("click", () => closeAuthModal());
  }

  if (authModal) {
    authModal.addEventListener("click", (e) => {
      if (e.target === authModal) {
        closeAuthModal();
      }
    });
  }

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      if (authModal && authModal.style.display !== "none") {
        closeAuthModal();
      }
      if (editTitleModal && editTitleModal.style.display !== "none") {
        closeEditTitleModal();
      }
    }
  });

  // Tab switching
  if (tabLogin) {
    tabLogin.addEventListener("click", () => switchAuthMode('login'));
  }
  if (tabRegister) {
    tabRegister.addEventListener("click", () => switchAuthMode('register'));
  }

  // Logout button
  if (btnLogout) {
    btnLogout.addEventListener("click", async () => {
      try {
        await signOutUser();
      } catch (err) {
        console.error("Sign out error:", err);
      }
    });
  }

  // Email/password form submission
  if (authForm) {
    authForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = authEmailInput ? authEmailInput.value.trim() : '';
      const password = authPasswordInput ? authPasswordInput.value : '';

      if (!email || !password) {
        showAuthAlert("Wypełnij wszystkie pola formularza.");
        return;
      }

      setAuthLoading(true);
      hideAuthAlert();

      try {
        if (authMode === 'login') {
          await signInWithEmail(email, password);
        } else {
          await signUpWithEmail(email, password);
        }
        // onAuthStateChange callback handles UI update & modal close
      } catch (err) {
        console.error("Auth error:", err);
        showAuthAlert(err.message || "Wystąpił błąd autoryzacji.");
      } finally {
        setAuthLoading(false);
      }
    });
  }

  // Google sign in button
  if (btnGoogleAuth) {
    btnGoogleAuth.addEventListener("click", async () => {
      setAuthLoading(true);
      hideAuthAlert();

      try {
        await signInWithGoogle();
        // onAuthStateChange callback handles UI update & modal close
      } catch (err) {
        console.error("Google auth error:", err);
        showAuthAlert(err.message || "Błąd logowania przez Google.");
      } finally {
        setAuthLoading(false);
      }
    });
  }
}

// ==========================================================================
// Edit Title Modal Handlers
// ==========================================================================

function openEditTitleModal(rideId = selectedRideId) {
  if (!rideId) return;
  const ride = allRides.find(r => r.id === rideId);
  if (!ride) return;

  if (!currentUser && !ride.isDemo) {
    showToast("Zaloguj się, aby edytować trening", "info");
    openAuthModal('login');
    return;
  }

  if (currentUser && !ride.isDemo && ride.userId && ride.userId !== currentUser.uid) {
    showToast("Możesz edytować tylko własne treningi", "error");
    return;
  }

  editingRideId = ride.id;

  if (editTitleInput) {
    editTitleInput.value = (ride.title && ride.title.trim().length > 0)
      ? ride.title
      : (ride.locationName || `Trening ${formatDate(ride.startTime)}`);
  }

  if (editTitleModalSubtitle) {
    editTitleModalSubtitle.textContent = `Trening z dnia ${formatDate(ride.startTime)}`;
  }

  hideEditTitleAlert();
  if (editTitleModal) {
    editTitleModal.style.display = "flex";
  }

  if (editTitleInput) {
    setTimeout(() => {
      editTitleInput.focus();
      editTitleInput.select();
    }, 60);
  }
}

function closeEditTitleModal() {
  if (editTitleModal) {
    editTitleModal.style.display = "none";
  }
  hideEditTitleAlert();
  editingRideId = null;
  setEditTitleLoading(false);
}

function showEditTitleAlert(message) {
  if (editTitleAlert && editTitleAlertMsg) {
    editTitleAlertMsg.textContent = message;
    editTitleAlert.style.display = "flex";
  }
}

function hideEditTitleAlert() {
  if (editTitleAlert) {
    editTitleAlert.style.display = "none";
  }
}

function setEditTitleLoading(isLoading) {
  if (btnEditTitleSubmit) btnEditTitleSubmit.disabled = isLoading;
  if (btnEditTitleCancel) btnEditTitleCancel.disabled = isLoading;
  if (editTitleSpinner) editTitleSpinner.style.display = isLoading ? "inline-block" : "none";
  if (editTitleSubmitText) editTitleSubmitText.style.display = isLoading ? "none" : "inline";
}

// Add spinning animation for loading spinner
const style = document.createElement("style");
style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
document.head.appendChild(style);

// Start on DOMContentLoaded
document.addEventListener("DOMContentLoaded", initApp);
