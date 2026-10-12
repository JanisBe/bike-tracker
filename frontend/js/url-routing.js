import {formatDate} from './stats.js';
import {showToast} from './toast.js';

/**
 * Extracts ride ID from current window URL (query param ?ride=... or hash #ride=...)
 * @returns {string|null}
 */
export function getRideIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const rideParam = params.get('ride');
    if (rideParam) return rideParam.trim();

    const hash = window.location.hash;
    if (hash) {
        const hashMatch = new RegExp(/[#\/?&]ride=([^&]+)/).exec(hash) || new RegExp(/#\/?ride\/(.+)/).exec(hash);
        if (hashMatch?.[1]) {
            return decodeURIComponent(hashMatch[1]).trim();
        }
    }
    return null;
}

/**
 * Builds a shareable URL targeting a specific ride
 * @param {string} rideId
 * @returns {string}
 */
export function buildShareUrl(rideId) {
    const url = new URL(window.location.href);
    url.searchParams.set('ride', rideId);
    url.hash = '';
    return url.toString();
}

/**
 * Updates browser URL history state with selected ride ID
 * @param {string|null} rideId
 */
export function updateUrlWithRideId(rideId) {
    const url = new URL(window.location.href);
    if (rideId) {
        url.searchParams.set('ride', rideId);
    } else {
        url.searchParams.delete('ride');
    }
    url.hash = '';
    window.history.replaceState({rideId}, '', url.toString());
}

/**
 * Handles sharing a ride via Web Share API or copying link to clipboard
 * @param {object} ride
 * @param {Event} [e]
 */
export async function handleShareClick(ride, e) {
    if (e) e.preventDefault();
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

    // Fallback: Copy to clipboard using modern Clipboard API
    try {
        if (!navigator.clipboard?.writeText) {
            throw new Error("Clipboard API not available in this environment.");
        }
        await navigator.clipboard.writeText(shareUrl);

        const btnShareRide = document.getElementById("btn-share-ride");
        const btnMobileShare = document.getElementById("btn-mobile-share");
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
