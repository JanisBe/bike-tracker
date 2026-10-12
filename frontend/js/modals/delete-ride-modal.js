import {deleteRide} from '../ride-service.js';
import {formatDate} from '../stats.js';
import {showToast} from '../toast.js';

let ridePendingDelete = null;

const deleteRideModal = document.getElementById("delete-ride-modal");
const btnDeleteRideClose = document.getElementById("btn-delete-ride-close");
const btnDeleteRideCancel = document.getElementById("btn-delete-ride-cancel");
const btnDeleteRideConfirm = document.getElementById("btn-delete-ride-confirm");
const deleteRideSpinner = document.getElementById("delete-ride-spinner");
const deleteRideConfirmText = document.getElementById("delete-ride-confirm-text");
const deleteRideAlert = document.getElementById("delete-ride-alert");
const deleteRideAlertMsg = document.getElementById("delete-ride-alert-msg");
const deleteRidePreviewTitle = document.getElementById("delete-ride-preview-title");
const deleteRidePreviewSubtitle = document.getElementById("delete-ride-preview-subtitle");

export function isDeleteRideModalOpen() {
    return deleteRideModal && deleteRideModal.style.display !== "none" && deleteRideModal.style.display !== "";
}

export function openDeleteModal(ride) {
    if (!ride) return;
    ridePendingDelete = ride;

    if (deleteRidePreviewTitle) {
        deleteRidePreviewTitle.textContent = ride.title || (ride.locationName ? `📍 ${ride.locationName}` : "Trening");
    }
    if (deleteRidePreviewSubtitle) {
        const isWalking = ride.activityType?.toLowerCase() === "walking";
        const typeStr = isWalking ? "🚶 Marsz" : "🚴 Rower";
        deleteRidePreviewSubtitle.textContent = `${formatDate(ride.startTime)} • ${ride.distanceKm.toFixed(1)} km • ${typeStr}`;
    }

    hideDeleteRideAlert();
    setDeleteRideLoading(false);

    if (deleteRideModal) {
        deleteRideModal.style.display = "flex";
    }
}

export function closeDeleteModal() {
    if (deleteRideModal) {
        deleteRideModal.style.display = "none";
    }
    ridePendingDelete = null;
    hideDeleteRideAlert();
    setDeleteRideLoading(false);
}

export function showDeleteRideAlert(msg) {
    if (deleteRideAlert && deleteRideAlertMsg) {
        deleteRideAlertMsg.textContent = msg;
        deleteRideAlert.style.display = "flex";
    }
}

export function hideDeleteRideAlert() {
    if (deleteRideAlert) {
        deleteRideAlert.style.display = "none";
    }
}

export function setDeleteRideLoading(isLoading) {
    if (btnDeleteRideConfirm) btnDeleteRideConfirm.disabled = isLoading;
    if (btnDeleteRideCancel) btnDeleteRideCancel.disabled = isLoading;
    if (deleteRideSpinner) deleteRideSpinner.style.display = isLoading ? "inline-block" : "none";
    if (deleteRideConfirmText) deleteRideConfirmText.style.display = isLoading ? "none" : "inline";
}

/**
 * Initializes Delete Ride modal listeners
 * @param {object} options
 * @param {Function} options.onRideDeleted Callback when ride is deleted (rideId)
 */
export function initDeleteRideModal({onRideDeleted} = {}) {
    btnDeleteRideClose?.addEventListener("click", closeDeleteModal);
    btnDeleteRideCancel?.addEventListener("click", closeDeleteModal);

    deleteRideModal?.addEventListener("click", (e) => {
        if (e.target === deleteRideModal) {
            closeDeleteModal();
        }
    });

    btnDeleteRideConfirm?.addEventListener("click", () => {
        void (async () => {
            if (!ridePendingDelete) return;
            const targetId = ridePendingDelete.id;

            setDeleteRideLoading(true);
            hideDeleteRideAlert();

            try {
                await deleteRide(targetId);

                closeDeleteModal();
                showToast("Trening został trwale usunięty.", "success");

                if (typeof onRideDeleted === 'function') {
                    onRideDeleted(targetId);
                }
            } catch (err) {
                console.error("Failed to delete ride:", err);
                showDeleteRideAlert(err.message || "Nie udało się usunąć treningu.");
            } finally {
                setDeleteRideLoading(false);
            }
        })();
    });
}
