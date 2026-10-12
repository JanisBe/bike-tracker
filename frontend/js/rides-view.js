import {calculateOverallStats, formatDate, formatDuration} from './stats.js';
import {downloadGpx} from './ride-service.js';
import {buildShareUrl} from './url-routing.js';
import {showToast} from './toast.js';

// DOM Elements
const statTotalDistance = document.getElementById("stat-total-distance");
const statTotalRides = document.getElementById("stat-total-rides");
const statTotalHours = document.getElementById("stat-total-hours");
const statTotalCalories = document.getElementById("stat-total-calories");
const ridesListContainer = document.getElementById("rides-list");

const floatingDetail = document.getElementById("floating-detail");
const detailTitle = document.getElementById("detail-title");
const detailDate = document.getElementById("detail-date");
const detailDist = document.getElementById("detail-dist");
const detailDur = document.getElementById("detail-dur");
const detailAvg = document.getElementById("detail-avg");
const detailMax = document.getElementById("detail-max");
const detailEle = document.getElementById("detail-ele");
const detailCalories = document.getElementById("detail-calories");
const btnDetailGpx = document.getElementById("btn-detail-gpx");
const btnEditRide = document.getElementById("btn-edit-ride");
const btnMobileEdit = document.getElementById("btn-mobile-edit");
const btnDeleteRide = document.getElementById("btn-delete-ride");
const btnMobileDelete = document.getElementById("btn-mobile-delete");
const btnShareRide = document.getElementById("btn-share-ride");

/**
 * Updates aggregate top stats banner
 * @param {Array<object>} rides
 */
export function updateStatsBanner(rides) {
    const stats = calculateOverallStats(rides);
    if (statTotalDistance) statTotalDistance.textContent = `${stats.totalDistanceKm} km`;
    if (statTotalRides) statTotalRides.textContent = stats.totalRides.toString();
    if (statTotalHours) statTotalHours.textContent = `${stats.totalDurationHours} h`;
    if (statTotalCalories) statTotalCalories.textContent = `${stats.totalCaloriesKcal.toLocaleString('pl-PL')} kcal`;
}

/**
 * Hides floating details panel
 */
export function hideFloatingDetail() {
    if (floatingDetail) {
        floatingDetail.classList.remove("active");
    }
}

/**
 * Shows floating details panel with current ride metrics
 * @param {object} ride
 * @param {object} options
 * @param {object|null} options.currentUser
 * @param {Function} [options.onEditRide]
 * @param {Function} [options.onDeleteRide]
 * @param {Function} [options.onShareRide]
 */
export function showFloatingDetail(ride, {currentUser, onEditRide, onDeleteRide, onShareRide} = {}) {
    if (!floatingDetail || !ride) return;

    const formattedDate = formatDate(ride.startTime);

    const displayTitle = (ride.title && ride.title.trim().length > 0)
        ? ride.title
        : (ride.locationName ? `📍 ${ride.locationName}` : `Trening z dnia ${formattedDate}`);

    if (detailTitle) detailTitle.textContent = displayTitle;

    const isWalking = ride.activityType?.toLowerCase() === 'walking';
    const activityLabel = isWalking ? 'Marsz / Spacer' : 'Trening rowerowy';
    if (detailDate) {
        if (ride.locationName && ride.title && ride.title.trim().length > 0 && ride.locationName !== ride.title) {
            detailDate.textContent = `📍 ${ride.locationName} • ${formattedDate} • ${activityLabel}`;
        } else if (ride.locationName) {
            detailDate.textContent = `${formattedDate} • ${activityLabel}`;
        } else {
            detailDate.textContent = `${formattedDate} • ${activityLabel}`;
        }
    }

    if (detailDist) detailDist.textContent = `${ride.distanceKm.toFixed(2)} km`;
    if (detailDur) detailDur.textContent = formatDuration(ride.durationSeconds);
    if (detailAvg) detailAvg.textContent = `${ride.avgSpeedKmh.toFixed(1)} km/h`;
    if (detailMax) detailMax.textContent = ride.maxSpeedKmh > 0 ? `${ride.maxSpeedKmh.toFixed(1)} km/h` : '—';
    if (detailEle) detailEle.textContent = ride.elevationGain != null ? `+${ride.elevationGain} m` : '—';
    if (detailCalories) detailCalories.textContent = ride.caloriesKcal != null ? `${ride.caloriesKcal.toLocaleString('pl-PL')} kcal` : '—';

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
    if (btnDeleteRide) {
        btnDeleteRide.style.display = isSharedOther ? "none" : "inline-flex";
        btnDeleteRide.setAttribute("title", `Usuń trening (${displayTitle})`);
    }
    if (btnMobileDelete) {
        btnMobileDelete.style.display = isSharedOther ? "none" : "inline-flex";
        btnMobileDelete.setAttribute("title", `Usuń trening (${displayTitle})`);
    }

    if (btnDetailGpx) {
        btnDetailGpx.onclick = () => {
            void downloadGpx(ride).catch((err) => {
                console.error("Error downloading GPX:", err);
                showToast("Nie udało się pobrać pliku GPX.", "error");
            });
        };
    }

    floatingDetail.classList.add("active");
}

/**
 * Renders list of ride cards in sidebar
 * @param {Array<object>} rides
 * @param {object} options
 * @param {string|null} options.selectedRideId
 * @param {object|null} options.currentUser
 * @param {string} [options.searchInputValue]
 * @param {Function} options.onSelectRide
 * @param {Function} options.onDeleteRide
 * @param {Function} options.onOpenAuth
 */
export function renderRidesList(rides, {
    selectedRideId = null,
    currentUser = null,
    searchInputValue = "",
    onSelectRide,
    onDeleteRide,
    onOpenAuth
} = {}) {
    if (!ridesListContainer) return;
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
            if (btn && typeof onOpenAuth === 'function') {
                btn.addEventListener("click", () => onOpenAuth('login'));
            }
        } else if (searchInputValue && searchInputValue.trim().length > 0) {
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
        const caloriesStr = ride.caloriesKcal != null ? `🔥 ${ride.caloriesKcal} kcal` : '';
        const isWalking = ride.activityType?.toLowerCase() === 'walking';
        const activityBadge = isWalking
            ? '<span class="badge-activity" title="Marsz / Spacer">🚶 Marsz</span>'
            : '<span class="badge-activity" title="Rower">🚴 Rower</span>';
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
          ${activityBadge}
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
          ${caloriesStr ? `&nbsp;&nbsp;<span>${caloriesStr}</span>` : ''}
        </div>
        <div class="ride-card-actions">
          <button class="btn-download-gpx" data-ride-id="${ride.id}" title="Pobierz plik GPX">
            <span>⬇</span> GPX
          </button>
          ${!ride.isShared ? `
          <button class="btn-card-delete" data-ride-id="${ride.id}" title="Usuń trening">
            <svg fill="none" height="13" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" viewBox="0 0 24 24" width="13">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              <line x1="10" y1="11" x2="10" y2="17"></line>
              <line x1="14" y1="11" x2="14" y2="17"></line>
            </svg>
          </button>
          ` : ''}
        </div>
      </div>
    `;

        // Click on card selects route on map
        card.addEventListener("click", (e) => {
            if (e.target.closest(".btn-download-gpx") || e.target.closest(".btn-card-delete")) return;
            if (typeof onSelectRide === 'function') {
                void onSelectRide(ride.id);
            }
        });

        // GPX download click
        const downloadBtn = card.querySelector(".btn-download-gpx");
        downloadBtn?.addEventListener("click", (e) => {
            e.stopPropagation();
            void downloadGpx(ride).catch((err) => {
                console.error("Error downloading GPX:", err);
                showToast("Nie udało się pobrać pliku GPX.", "error");
            });
        });

        // Delete button click
        const deleteBtn = card.querySelector(".btn-card-delete");
        deleteBtn?.addEventListener("click", (e) => {
            e.stopPropagation();
            if (typeof onDeleteRide === 'function') {
                onDeleteRide(ride);
            }
        });

        ridesListContainer.appendChild(card);
    });
}

/**
 * Filters rides by search term and re-renders the list
 * @param {Array<object>} allRides
 * @param {string} term
 * @param {object} options
 */
export function filterAndRenderRides(allRides, term, options = {}) {
    const normalizedTerm = term.toLowerCase().trim();
    if (!normalizedTerm) {
        renderRidesList(allRides, {...options, searchInputValue: ""});
        return;
    }

    const filtered = allRides.filter(r => {
        const titleMatch = (r.title || "").toLowerCase().includes(normalizedTerm);
        const locationMatch = (r.locationName || "").toLowerCase().includes(normalizedTerm);
        const distMatch = r.distanceKm.toString().includes(normalizedTerm);
        const dateMatch = formatDate(r.startTime).toLowerCase().includes(normalizedTerm);
        return titleMatch || locationMatch || distMatch || dateMatch;
    });

    renderRidesList(filtered, {...options, searchInputValue: normalizedTerm});
}
