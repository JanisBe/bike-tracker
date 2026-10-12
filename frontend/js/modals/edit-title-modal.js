import {updateRideTitle} from '../ride-service.js';
import {formatDate} from '../stats.js';
import {showToast} from '../toast.js';

let currentEditingRide = null;

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

export function isEditTitleModalOpen() {
    return editTitleModal && editTitleModal.style.display !== "none" && editTitleModal.style.display !== "";
}

export function openEditTitleModal(ride, {currentUser, onRequireAuth} = {}) {
    if (!ride) return;

    if (!currentUser && !ride.isDemo) {
        showToast("Zaloguj się, aby edytować trening", "info");
        if (typeof onRequireAuth === 'function') {
            onRequireAuth();
        }
        return;
    }

    if (currentUser && !ride.isDemo && ride.userId && ride.userId !== currentUser.uid) {
        showToast("Możesz edytować tylko własne treningi", "error");
        return;
    }

    currentEditingRide = ride;

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

export function closeEditTitleModal() {
    if (editTitleModal) {
        editTitleModal.style.display = "none";
    }
    hideEditTitleAlert();
    currentEditingRide = null;
    setEditTitleLoading(false);
}

export function showEditTitleAlert(message) {
    if (editTitleAlert && editTitleAlertMsg) {
        editTitleAlertMsg.textContent = message;
        editTitleAlert.style.display = "flex";
    }
}

export function hideEditTitleAlert() {
    if (editTitleAlert) {
        editTitleAlert.style.display = "none";
    }
}

export function setEditTitleLoading(isLoading) {
    if (btnEditTitleSubmit) btnEditTitleSubmit.disabled = isLoading;
    if (btnEditTitleCancel) btnEditTitleCancel.disabled = isLoading;
    if (editTitleSpinner) editTitleSpinner.style.display = isLoading ? "inline-block" : "none";
    if (editTitleSubmitText) editTitleSubmitText.style.display = isLoading ? "none" : "inline";
}

/**
 * Initializes Edit Title modal listeners
 * @param {object} options
 * @param {Function} options.onTitleUpdated Callback when title is successfully updated (rideId, newTitle)
 */
export function initEditTitleModal({onTitleUpdated} = {}) {
    btnEditTitleClose?.addEventListener("click", closeEditTitleModal);
    btnEditTitleCancel?.addEventListener("click", closeEditTitleModal);

    editTitleModal?.addEventListener("click", (e) => {
        if (e.target === editTitleModal) {
            closeEditTitleModal();
        }
    });

    editTitleForm?.addEventListener("submit", (e) => {
        e.preventDefault();
        void (async () => {
            if (!currentEditingRide) return;

            const newTitle = editTitleInput ? editTitleInput.value.trim() : "";
            if (!newTitle) {
                showEditTitleAlert("Podaj nazwę treningu.");
                return;
            }

            setEditTitleLoading(true);
            hideEditTitleAlert();

            try {
                await updateRideTitle(currentEditingRide.id, newTitle);

                const targetId = currentEditingRide.id;
                currentEditingRide.title = newTitle;

                if (typeof onTitleUpdated === 'function') {
                    onTitleUpdated(targetId, newTitle);
                }

                closeEditTitleModal();
                showToast("Zaktualizowano nazwę treningu!", "success");
            } catch (err) {
                console.error("Error updating ride title:", err);
                showEditTitleAlert(err.message || "Nie udało się zaktualizować nazwy treningu.");
            } finally {
                setEditTitleLoading(false);
            }
        })();
    });
}
