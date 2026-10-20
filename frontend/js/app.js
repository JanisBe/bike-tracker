import {fetchRideById, fetchRideProfile, fetchUserRides} from './ride-service.js';
import {getUserProfile} from './user-service.js';
import {
  clearScrubMarker,
  displayAllRoutesOverview,
  displaySingleRoute,
  initMap,
  invalidateMapSize,
  setScrubMarker
} from './map-renderer.js?v=3';
import {CalendarWidget} from './calendar-widget.js';
import {ProfileChart} from './profile-chart.js';
import {onAuthStateChange} from './auth-service.js';
import {showToast} from './toast.js';
import {getRideIdFromUrl, handleShareClick, updateUrlWithRideId} from './url-routing.js';
import {enrichRidesWithLocation} from './geocoding.js';
import {
  filterAndRenderRides,
  hideFloatingDetail,
  renderRidesList,
  showFloatingDetail,
  updateStatsBanner
} from './rides-view.js';
import {closeAuthModal, initAuthModal, isAuthModalOpen, openAuthModal} from './modals/auth-modal.js';
import {
  closeEditTitleModal,
  initEditTitleModal,
  isEditTitleModalOpen,
  openEditTitleModal
} from './modals/edit-title-modal.js';
import {closeImportGpxModal, initImportGpxModal, isImportGpxModalOpen} from './modals/import-gpx-modal.js';
import {
  closeDeleteModal,
  initDeleteRideModal,
  isDeleteRideModalOpen,
  openDeleteModal
} from './modals/delete-ride-modal.js';
import {closeUserProfileModal, initUserProfileModal, isUserProfileModalOpen} from './modals/user-profile-modal.js';
import {closeChangelogModal, initChangelogModal, isChangelogModalOpen} from './modals/changelog-modal.js';

// Application State
let allRides = [];
let selectedRideId = null;
let calendarWidget = null;
let profileChart = null;
let currentUser = null;
let currentUserProfile = null;
let isMapFullscreen = false;

// DOM Elements
const mapContainer = document.getElementById("map-container");
const mapWrapper = document.getElementById("map-wrapper");
const btnBackList = document.getElementById("btn-back-list");
const btnMobileEdit = document.getElementById("btn-mobile-edit");
const btnMobileDelete = document.getElementById("btn-mobile-delete");
const btnMobileShare = document.getElementById("btn-mobile-share");
const btnMapFullscreen = document.getElementById("btn-map-fullscreen");
const ridesListContainer = document.getElementById("rides-list");
const searchInput = document.getElementById("search-input");
const btnOverview = document.getElementById("btn-overview");
const calendarContainer = document.getElementById("calendar-widget-container");
const detailTitle = document.getElementById("detail-title");
const btnEditRide = document.getElementById("btn-edit-ride");
const btnDeleteRide = document.getElementById("btn-delete-ride");
const btnShareRide = document.getElementById("btn-share-ride");

const userInfoEl = document.getElementById("user-info");
const userGuestEl = document.getElementById("user-guest");
const userAvatarEl = document.getElementById("user-avatar");
const userNameEl = document.getElementById("user-name");
const userEmailEl = document.getElementById("user-email");

function getRidesListOptions() {
  return {
    selectedRideId,
    currentUser,
    searchInputValue: searchInput ? searchInput.value : "",
    onSelectRide: (rideId) => selectRide(rideId),
    onDeleteRide: (ride) => openDeleteModal(ride),
    onOpenAuth: (mode) => openAuthModal(mode)
  };
}

function getFloatingDetailOptions() {
  return {
    currentUser,
    onEditRide: (ride) => openEditTitleModal(ride, {currentUser, onRequireAuth: () => openAuthModal('login')}),
    onDeleteRide: (ride) => openDeleteModal(ride),
    onShareRide: (ride, e) => handleShareClick(ride, e)
  };
}

function refreshRidesView() {
  updateStatsBanner(allRides);
  if (calendarWidget) {
    calendarWidget.setRides(allRides);
  }
  const term = searchInput ? searchInput.value.trim() : "";
  if (term) {
    filterAndRenderRides(allRides, term, getRidesListOptions());
  } else {
    renderRidesList(allRides, getRidesListOptions());
  }
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

export function clearSelectedRide() {
  selectedRideId = null;
  updateUrlWithRideId(null);
  document.body.classList.remove("has-selected-ride");
  document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
  hideFloatingDetail();
  if (profileChart) profileChart.hide();
  clearScrubMarker();
  displayAllRoutesOverview(allRides);
  const appContainer = document.querySelector(".app-container");
  if (appContainer) {
    appContainer.scrollTop = 0;
  }
  setTimeout(() => {
    invalidateMapSize();
  }, 60);
}

export async function selectRide(rideId) {
  selectedRideId = rideId;
  updateUrlWithRideId(rideId);
  document.body.classList.add("has-selected-ride");

  // Highlight active card in list
  document.querySelectorAll(".ride-card").forEach(c => c.classList.remove("selected"));
  const activeCard = document.getElementById(`card-${rideId}`);
  if (activeCard) {
    activeCard.classList.add("selected");
  }

  const ride = allRides.find(r => r.id === rideId);
  if (!ride) return;

  // On mobile scroll to top of details view
  if (mapContainer) mapContainer.scrollTop = 0;
  const appContainer = document.querySelector(".app-container");
  if (appContainer) appContainer.scrollTop = 0;

  // Ensure map is measured with new layout before centering route
  invalidateMapSize();
  setTimeout(() => {
    invalidateMapSize();
  }, 50);

  // Render on Leaflet Map
  displaySingleRoute(ride);

  // Update floating detail panel
  showFloatingDetail(ride, getFloatingDetailOptions());

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

/**
 * Loads rides for authenticated user
 * @param {string} userId
 */
async function loadUserRides(userId) {
  const urlRideId = getRideIdFromUrl();
  try {
    if (ridesListContainer) {
      ridesListContainer.innerHTML = `
        <div style="text-align: center; padding: 40px; color: var(--text-muted);">
          <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
          <p style="margin-top: 12px; font-size: 13px;">Wczytywanie Twoich treningów...</p>
        </div>
      `;
    }

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
    if (!targetRideId && allRides.length > 0 && !isMobile) {
      targetRideId = allRides[0].id;
    }

    refreshRidesView();

    // Enrich legacy rides with location from OSM in background
    void enrichRidesWithLocation(allRides, () => {
      refreshRidesView();
      if (selectedRideId) {
        const cur = allRides.find(r => r.id === selectedRideId);
        if (cur) showFloatingDetail(cur, getFloatingDetailOptions());
      }
    });

    // Select targeted ride or clear map
    if (targetRideId) {
      await selectRide(targetRideId);
      const card = document.getElementById(`card-${targetRideId}`);
      if (card) {
        card.scrollIntoView({behavior: 'smooth', block: 'nearest'});
      }
    } else {
      hideFloatingDetail();
      if (profileChart) profileChart.hide();
      clearScrubMarker();
      displayAllRoutesOverview([]);
    }
  } catch (error) {
    console.error("Error loading user rides:", error);
    if (ridesListContainer) {
      ridesListContainer.innerHTML = `
        <div style="text-align: center; padding: 30px; color: var(--accent-red);">
          <p style="font-weight: 600; margin-bottom: 6px;">Nie udało się wczytać treningów.</p>
          <p id="rides-error-msg" style="font-size: 12px; color: var(--text-muted); word-break: break-word;"></p>
        </div>
      `;
      const errorMsgEl = document.getElementById("rides-error-msg");
      if (errorMsgEl) {
        errorMsgEl.textContent = error?.message || String(error);
      }
    }
  }
}

async function initApp() {
  // 1. Initialize Leaflet Map
  initMap("map");

  // 2. Initialize Route Profile Chart
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

  // 3. Initialize Calendar Widget
  if (calendarContainer) {
    calendarWidget = new CalendarWidget(calendarContainer, {
      onSelectRide: (rideId) => {
        void selectRide(rideId);
        const card = document.getElementById(`card-${rideId}`);
        if (card) {
          card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      }
    });
  }

  // 4. Setup Modals
  initAuthModal({
    onSignOut: () => {
      updateUrlWithRideId(null);
      clearSelectedRide();
      allRides = [];
      displayAllRoutesOverview([]);
    }
  });

  initEditTitleModal({
    onTitleUpdated: (rideId, newTitle) => {
      const ride = allRides.find(r => r.id === rideId);
      if (ride) {
        ride.title = newTitle;
        if (selectedRideId === ride.id) {
          showFloatingDetail(ride, getFloatingDetailOptions());
        }
        refreshRidesView();
      }
    }
  });

  initImportGpxModal({
    getCurrentUser: () => currentUser,
    onRideImported: async (newRide) => {
      allRides = [newRide, ...allRides.filter(r => !r.isDemo)];
      refreshRidesView();
      await selectRide(newRide.id);
    },
    onRequireAuth: () => openAuthModal('login')
  });

  initDeleteRideModal({
    onRideDeleted: (targetId) => {
      allRides = allRides.filter(r => r.id !== targetId);
      refreshRidesView();
      if (selectedRideId === targetId) {
        clearSelectedRide();
      }
    }
  });

  initUserProfileModal({
    getCurrentUser: () => currentUser,
    getCurrentProfile: () => currentUserProfile,
    onProfileSaved: (updated) => {
      currentUserProfile = {...currentUserProfile, ...updated};
      if (updated.displayName && userNameEl) {
        userNameEl.textContent = updated.displayName;
      }
    },
    onRequireAuth: () => openAuthModal('login')
  });

  initChangelogModal();

  // 5. Setup UI Event Listeners
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      const term = e.target.value.trim();
      filterAndRenderRides(allRides, term, getRidesListOptions());
    });
  }

  btnOverview?.addEventListener("click", () => clearSelectedRide());
  btnBackList?.addEventListener("click", () => clearSelectedRide());
  btnMapFullscreen?.addEventListener("click", toggleMapFullscreen);

  btnMobileEdit?.addEventListener("click", () => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) openEditTitleModal(ride, {currentUser, onRequireAuth: () => openAuthModal('login')});
  });

  btnEditRide?.addEventListener("click", () => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) openEditTitleModal(ride, {currentUser, onRequireAuth: () => openAuthModal('login')});
  });

  detailTitle?.addEventListener("click", () => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) openEditTitleModal(ride, {currentUser, onRequireAuth: () => openAuthModal('login')});
  });

  btnMobileDelete?.addEventListener("click", () => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) openDeleteModal(ride);
  });

  btnDeleteRide?.addEventListener("click", () => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) openDeleteModal(ride);
  });

  btnMobileShare?.addEventListener("click", (e) => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) void handleShareClick(ride, e);
  });

  btnShareRide?.addEventListener("click", (e) => {
    const ride = allRides.find(r => r.id === selectedRideId);
    if (ride) void handleShareClick(ride, e);
  });

  // Global ESC key listener to close modals
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      if (isAuthModalOpen()) closeAuthModal();
      if (isEditTitleModalOpen()) closeEditTitleModal();
      if (isDeleteRideModalOpen()) closeDeleteModal();
      if (isUserProfileModalOpen()) closeUserProfileModal();
      if (isImportGpxModalOpen()) closeImportGpxModal();
      if (isChangelogModalOpen()) closeChangelogModal();
    }
  });

  // Handle browser back/forward history navigation
  window.addEventListener("popstate", () => {
    void (async () => {
      const urlRideId = getRideIdFromUrl();
      if (urlRideId && urlRideId !== selectedRideId) {
        const found = allRides.find(r => r.id === urlRideId);
        if (found) {
          await selectRide(found.id);
        } else {
          try {
            const sharedRide = await fetchRideById(urlRideId);
            if (sharedRide) {
              sharedRide.isShared = true;
              allRides = [sharedRide, ...allRides];
              refreshRidesView();
              await selectRide(sharedRide.id);
            }
          } catch (err) {
            console.error("Error fetching shared ride on popstate:", err);
          }
        }
      } else if (!urlRideId && selectedRideId) {
        clearSelectedRide();
      }
    })();
  });

  // 6. Watch Firebase Auth State
  let wasAuthenticated = false;

  onAuthStateChange((user) => {
    void (async () => {
      currentUser = user;
      if (user) {
        wasAuthenticated = true;
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
        try {
          currentUserProfile = await getUserProfile(user.uid);
          if (currentUserProfile?.displayName && userNameEl) {
            userNameEl.textContent = currentUserProfile.displayName;
          }
        } catch (e) {
          console.warn("Failed to load user profile:", e);
        }
        await loadUserRides(user.uid);
      } else {
        const wasUserLoggedIn = wasAuthenticated;
        wasAuthenticated = false;
        currentUserProfile = null;

        if (userInfoEl) userInfoEl.style.display = "none";
        if (userGuestEl) userGuestEl.style.display = "flex";

        if (wasUserLoggedIn) {
          updateUrlWithRideId(null);
        }

        const urlRideId = getRideIdFromUrl();

        if (urlRideId && !wasUserLoggedIn) {
          // Guest opened a shared ride link directly
          closeAuthModal();
          if (ridesListContainer) {
            ridesListContainer.innerHTML = `
              <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                <div style="display: inline-block; width: 24px; height: 24px; border: 3px solid var(--accent-orange); border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite;"></div>
                <p style="margin-top: 12px; font-size: 13px;">Wczytywanie udostępnionego treningu...</p>
              </div>
            `;
          }

          try {
            const sharedRide = await fetchRideById(urlRideId);
            if (sharedRide) {
              sharedRide.isShared = true;
              allRides = [sharedRide];
              refreshRidesView();
              void enrichRidesWithLocation(allRides, () => {
                refreshRidesView();
                if (selectedRideId) {
                  const cur = allRides.find(r => r.id === selectedRideId);
                  if (cur) showFloatingDetail(cur, getFloatingDetailOptions());
                }
              });
              await selectRide(sharedRide.id);
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
        if (calendarWidget) calendarWidget.setRides([]);
        hideFloatingDetail();
        if (profileChart) profileChart.hide();
        clearScrubMarker();
        displayAllRoutesOverview([]);
        renderRidesList([], getRidesListOptions());

        openAuthModal('login');
      }
    })().catch((err) => {
      console.error("Auth state change error:", err);
    });
  });
}

// Add spinning animation for loading spinner
const style = document.createElement("style");
style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
document.head.appendChild(style);

// Start on DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
  void initApp().catch((err) => {
    console.error("Failed to initialize app:", err);
  });
});
