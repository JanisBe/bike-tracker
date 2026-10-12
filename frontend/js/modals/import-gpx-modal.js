import {parseGpxFileForImport, saveImportedRide} from '../ride-service.js';
import {formatDate, formatDuration} from '../stats.js';
import {showToast} from '../toast.js';

let pendingImportData = null;
let pendingImportGpxXml = null;

const btnImportGpx = document.getElementById("btn-import-gpx");
const gpxFileInput = document.getElementById("gpx-file-input");
const importGpxModal = document.getElementById("import-gpx-modal");
const btnImportGpxClose = document.getElementById("btn-import-gpx-close");
const btnImportGpxCancel = document.getElementById("btn-import-gpx-cancel");
const importGpxForm = document.getElementById("import-gpx-form");
const importGpxTitleInput = document.getElementById("import-gpx-title-input");
const btnImportGpxSubmit = document.getElementById("btn-import-gpx-submit");
const importGpxSpinner = document.getElementById("import-gpx-spinner");
const importGpxSubmitText = document.getElementById("import-gpx-submit-text");
const importGpxAlert = document.getElementById("import-gpx-alert");
const importGpxAlertMsg = document.getElementById("import-gpx-alert-msg");
const importPreviewDist = document.getElementById("import-preview-dist");
const importPreviewTime = document.getElementById("import-preview-time");
const importPreviewSpeed = document.getElementById("import-preview-speed");
const importPreviewElev = document.getElementById("import-preview-elev");
const importPreviewCalories = document.getElementById("import-preview-calories");
const importPreviewLocation = document.getElementById("import-preview-location");

export function isImportGpxModalOpen() {
    return importGpxModal && importGpxModal.style.display !== "none" && importGpxModal.style.display !== "";
}

export function openImportGpxModal(parsed) {
    if (!parsed) return;

    if (importPreviewDist) {
        importPreviewDist.textContent = `${parsed.distanceKm.toFixed(1)} km`;
    }
    if (importPreviewTime) {
        importPreviewTime.textContent = formatDuration(parsed.durationSeconds);
    }
    if (importPreviewSpeed) {
        importPreviewSpeed.textContent = `${parsed.avgSpeedKmh.toFixed(1)} km/h`;
    }
    if (importPreviewElev) {
        importPreviewElev.textContent = parsed.elevationGain > 0 ? `+${Math.round(parsed.elevationGain)} m` : "—";
    }
    if (importPreviewCalories) {
        importPreviewCalories.textContent = parsed.caloriesKcal ? `${parsed.caloriesKcal.toLocaleString('pl-PL')} kcal` : "—";
    }
    if (importPreviewLocation) {
        importPreviewLocation.textContent = parsed.locationName
            ? `📍 Lokalizacja: ${parsed.locationName}`
            : `📅 Data: ${formatDate(parsed.startTime)}`;
    }
    if (importGpxTitleInput) {
        importGpxTitleInput.value = parsed.title || "Nowy trening";
    }

    hideImportGpxAlert();
    if (importGpxModal) {
        importGpxModal.style.display = "flex";
    }

    if (importGpxTitleInput) {
        setTimeout(() => {
            importGpxTitleInput.focus();
            importGpxTitleInput.select();
        }, 60);
    }
}

export function closeImportGpxModal() {
    if (importGpxModal) {
        importGpxModal.style.display = "none";
    }
    hideImportGpxAlert();
    pendingImportData = null;
    pendingImportGpxXml = null;
    setImportGpxLoading(false);
}

export function showImportGpxAlert(message) {
    if (importGpxAlert && importGpxAlertMsg) {
        importGpxAlertMsg.textContent = message;
        importGpxAlert.style.display = "flex";
    }
}

export function hideImportGpxAlert() {
    if (importGpxAlert) {
        importGpxAlert.style.display = "none";
    }
}

export function setImportGpxLoading(isLoading) {
    if (btnImportGpxSubmit) btnImportGpxSubmit.disabled = isLoading;
    if (btnImportGpxCancel) btnImportGpxCancel.disabled = isLoading;
    if (importGpxSpinner) importGpxSpinner.style.display = isLoading ? "inline-block" : "none";
    if (importGpxSubmitText) importGpxSubmitText.style.display = isLoading ? "none" : "inline";
}

/**
 * Initializes GPX import trigger and modal listeners
 * @param {object} options
 * @param {Function} options.getCurrentUser Returns current authenticated user
 * @param {Function} options.onRideImported Callback when ride is imported (newRide)
 * @param {Function} options.onRequireAuth Callback to open login modal if user is not authenticated
 */
export function initImportGpxModal({getCurrentUser, onRideImported, onRequireAuth} = {}) {
    if (btnImportGpx && gpxFileInput) {
        btnImportGpx.addEventListener("click", () => {
            const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
            if (!user) {
                showToast("Zaloguj się, aby zaimportować własny trening GPX", "info");
                if (typeof onRequireAuth === 'function') {
                    onRequireAuth();
                }
                return;
            }
            gpxFileInput.value = "";
            gpxFileInput.click();
        });

        gpxFileInput.addEventListener("change", (e) => {
            void (async () => {
                const file = e.target.files?.[0];
                if (!file) return;

                try {
                    showToast("Wczytywanie i analiza pliku GPX...", "info");
                    const gpxXml = await file.text();
                    const parsed = await parseGpxFileForImport(gpxXml);

                    pendingImportData = parsed;
                    pendingImportGpxXml = gpxXml;

                    openImportGpxModal(parsed);
                } catch (err) {
                    console.error("Failed to parse GPX file:", err);
                    showToast(err.message || "Błąd podczas parsowania pliku GPX.", "error");
                }
            })();
        });
    }

    btnImportGpxClose?.addEventListener("click", closeImportGpxModal);
    btnImportGpxCancel?.addEventListener("click", closeImportGpxModal);

    importGpxModal?.addEventListener("click", (e) => {
        if (e.target === importGpxModal) {
            closeImportGpxModal();
        }
    });

    importGpxForm?.addEventListener("submit", (e) => {
        e.preventDefault();
        void (async () => {
            if (!pendingImportData || !pendingImportGpxXml) return;

            const title = importGpxTitleInput ? importGpxTitleInput.value.trim() : "";
            if (!title) {
                showImportGpxAlert("Podaj nazwę treningu.");
                return;
            }

            const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
            if (!user) {
                showImportGpxAlert("Musisz być zalogowany, aby zapisać trening.");
                return;
            }

            setImportGpxLoading(true);
            hideImportGpxAlert();

            try {
                pendingImportData.title = title;
                const newRide = await saveImportedRide(pendingImportData, pendingImportGpxXml, user.uid);

                closeImportGpxModal();
                showToast("Trening został pomyślnie zaimportowany! 🎉", "success");

                if (typeof onRideImported === 'function') {
                    await onRideImported(newRide);
                }
            } catch (err) {
                console.error("Error saving imported ride:", err);
                showImportGpxAlert(err.message || "Nie udało się zapisać zaimportowanego treningu.");
            } finally {
                setImportGpxLoading(false);
            }
        })();
    });
}
