import {downloadGpx, fetchRideProfile, fetchUserRides} from './ride-service.js';
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
import {onAuthStateChange, signInWithEmail, signInWithGoogle, signOutUser, signUpWithEmail} from './auth-service.js';

let allRides = [];
let selectedRideId = null;
let calendarWidget = null;
let profileChart = null;
let currentUser = null;
let authMode = 'login'; // 'login' | 'register'

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
                if (point && point.lat != null && point.lon != null) {
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
                    card.scrollIntoView({behavior: 'smooth', block: 'nearest'});
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
                    const initial = displayName.charAt(0).toUpperCase();
                    userAvatarEl.textContent = initial;
                }
            }

            closeAuthModal();
            await loadUserRides(user.uid);
        } else {
            // User signed out
            if (userInfoEl) userInfoEl.style.display = "none";
            if (userGuestEl) userGuestEl.style.display = "flex";

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

            openAuthModal('login');
        }
    });
}

/**
 * Loads rides for authenticated user
 * @param {string} userId
 */
async function loadUserRides(userId) {
    try {
        ridesListContainer.innerHTML = `
      <div style="text-align: center; padding: 40px; color: var(--text-muted);">
        <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
        <p style="margin-top: 12px; font-size: 13px;">Wczytywanie Twoich treningów...</p>
      </div>
    `;

        allRides = await fetchUserRides(userId);

        // Update Overall Stats
        updateStatsBanner(allRides);

        // Update Calendar Widget
        if (calendarWidget) {
      calendarWidget.setRides(allRides);
    }

        // Render Ride List
    renderRidesList(allRides);

        // Select first ride or clear map
    if (allRides.length > 0) {
      selectRide(allRides[0].id);
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
      downloadGpx(ride);
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
        if (e.key === "Escape" && authModal && authModal.style.display !== "none") {
            closeAuthModal();
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

// Add spinning animation for loading spinner
const style = document.createElement("style");
style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
document.head.appendChild(style);

// Start on DOMContentLoaded
document.addEventListener("DOMContentLoaded", initApp);
