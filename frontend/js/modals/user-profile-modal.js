import {saveUserProfile} from '../user-service.js';
import {showToast} from '../toast.js';

let profileGender = 'male';

const userProfileTrigger = document.getElementById("user-profile-trigger");
const userAvatarEl = document.getElementById("user-avatar");
const userProfileModal = document.getElementById("user-profile-modal");
const btnUserProfileClose = document.getElementById("btn-user-profile-close");
const btnUserProfileCancel = document.getElementById("btn-user-profile-cancel");
const userProfileForm = document.getElementById("user-profile-form");
const btnUserProfileSubmit = document.getElementById("btn-user-profile-submit");
const profileSpinner = document.getElementById("profile-spinner");
const profileSubmitText = document.getElementById("profile-submit-text");
const profileAlert = document.getElementById("profile-alert");
const profileAlertMsg = document.getElementById("profile-alert-msg");
const genderChipMale = document.getElementById("gender-chip-male");
const genderChipFemale = document.getElementById("gender-chip-female");
const profileWeight = document.getElementById("profile-weight");
const profileHeight = document.getElementById("profile-height");
const profileCyclingThreshold = document.getElementById("profile-cycling-threshold");
const profileWalkingThreshold = document.getElementById("profile-walking-threshold");
const valCyclingThreshold = document.getElementById("val-cycling-threshold");
const valWalkingThreshold = document.getElementById("val-walking-threshold");

export function isUserProfileModalOpen() {
    return userProfileModal && userProfileModal.style.display !== "none" && userProfileModal.style.display !== "";
}

export function updateGenderChips(gender) {
    profileGender = gender;
    if (genderChipMale) {
        genderChipMale.classList.toggle("active", gender === "male");
    }
    if (genderChipFemale) {
        genderChipFemale.classList.toggle("active", gender === "female");
    }
}

export function openUserProfileModal({currentUser, currentUserProfile, onRequireAuth} = {}) {
    if (!currentUser) {
        showToast("Zaloguj się, aby wyświetlić i edytować profil.", "info");
        if (typeof onRequireAuth === 'function') {
            onRequireAuth();
        }
        return;
    }

    const profile = currentUserProfile || {
        gender: "male",
        weightKg: 75.0,
        heightCm: 175.0,
        cyclingSpeedThresholdKmh: 1.5,
        walkingSpeedThresholdKmh: 0.5
    };

    profileGender = profile.gender || "male";
    updateGenderChips(profileGender);

    if (profileWeight) profileWeight.value = profile.weightKg != null ? Number(profile.weightKg).toFixed(1) : "75.0";
    if (profileHeight) profileHeight.value = profile.heightCm != null ? Math.round(Number(profile.heightCm)) : "175";

    const cyclingTh = profile.cyclingSpeedThresholdKmh != null ? Number(profile.cyclingSpeedThresholdKmh) : 1.5;
    if (profileCyclingThreshold) profileCyclingThreshold.value = cyclingTh;
    if (valCyclingThreshold) valCyclingThreshold.textContent = `${cyclingTh.toFixed(1)} km/h`;

    const walkingTh = profile.walkingSpeedThresholdKmh != null ? Number(profile.walkingSpeedThresholdKmh) : 0.5;
    if (profileWalkingThreshold) profileWalkingThreshold.value = walkingTh;
    if (valWalkingThreshold) valWalkingThreshold.textContent = `${walkingTh.toFixed(1)} km/h`;

    hideProfileAlert();
    setProfileLoading(false);

    if (userProfileModal) {
        userProfileModal.style.display = "flex";
    }
}

export function closeUserProfileModal() {
    if (userProfileModal) {
        userProfileModal.style.display = "none";
    }
    hideProfileAlert();
    setProfileLoading(false);
}

export function showProfileAlert(msg) {
    if (profileAlert && profileAlertMsg) {
        profileAlertMsg.textContent = msg;
        profileAlert.style.display = "flex";
    }
}

export function hideProfileAlert() {
    if (profileAlert) {
        profileAlert.style.display = "none";
    }
}

export function setProfileLoading(isLoading) {
    if (btnUserProfileSubmit) btnUserProfileSubmit.disabled = isLoading;
    if (btnUserProfileCancel) btnUserProfileCancel.disabled = isLoading;
    if (profileSpinner) profileSpinner.style.display = isLoading ? "inline-block" : "none";
    if (profileSubmitText) profileSubmitText.style.display = isLoading ? "none" : "inline";
}

/**
 * Initializes User Profile modal listeners
 * @param {object} options
 * @param {Function} options.getCurrentUser Returns current user
 * @param {Function} options.getCurrentProfile Returns current profile data
 * @param {Function} options.onProfileSaved Callback when profile is saved (updatedProfile)
 * @param {Function} options.onRequireAuth Callback to open login modal
 */
export function initUserProfileModal({getCurrentUser, getCurrentProfile, onProfileSaved, onRequireAuth} = {}) {
    const triggerOpen = () => {
        const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
        const profile = typeof getCurrentProfile === 'function' ? getCurrentProfile() : null;
        openUserProfileModal({currentUser: user, currentUserProfile: profile, onRequireAuth});
    };

    if (userProfileTrigger) {
        userProfileTrigger.addEventListener("click", triggerOpen);
        userProfileTrigger.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                triggerOpen();
            }
        });
    }

    if (userAvatarEl) {
        userAvatarEl.addEventListener("click", () => {
            const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
            if (user) {
                triggerOpen();
            }
        });
    }

    btnUserProfileClose?.addEventListener("click", closeUserProfileModal);
    btnUserProfileCancel?.addEventListener("click", closeUserProfileModal);

    userProfileModal?.addEventListener("click", (e) => {
        if (e.target === userProfileModal) {
            closeUserProfileModal();
        }
    });

    genderChipMale?.addEventListener("click", () => updateGenderChips("male"));
    genderChipFemale?.addEventListener("click", () => updateGenderChips("female"));

    if (profileCyclingThreshold && valCyclingThreshold) {
        profileCyclingThreshold.addEventListener("input", (e) => {
            valCyclingThreshold.textContent = `${Number.parseFloat(e.target.value).toFixed(1)} km/h`;
        });
    }

    if (profileWalkingThreshold && valWalkingThreshold) {
        profileWalkingThreshold.addEventListener("input", (e) => {
            valWalkingThreshold.textContent = `${Number.parseFloat(e.target.value).toFixed(1)} km/h`;
        });
    }

    userProfileForm?.addEventListener("submit", (e) => {
        e.preventDefault();
        void (async () => {
            const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;
            if (!user) return;

            const weight = Number.parseFloat(profileWeight?.value || "75");
            const height = Number.parseFloat(profileHeight?.value || "175");
            const cyclingTh = Number.parseFloat(profileCyclingThreshold?.value || "1.5");
            const walkingTh = Number.parseFloat(profileWalkingThreshold?.value || "0.5");

            if (Number.isNaN(weight) || weight <= 0) {
                showProfileAlert("Podaj prawidłową wagę w kilogramach.");
                return;
            }
            if (Number.isNaN(height) || height <= 0) {
                showProfileAlert("Podaj prawidłowy wzrost w centymetrach.");
                return;
            }

            setProfileLoading(true);
            hideProfileAlert();

            try {
                const updated = {
                    gender: profileGender,
                    weightKg: weight,
                    heightCm: height,
                    cyclingSpeedThresholdKmh: cyclingTh,
                    walkingSpeedThresholdKmh: walkingTh,
                    displayName: user.displayName || "",
                    email: user.email || ""
                };

                await saveUserProfile(user.uid, updated);

                closeUserProfileModal();
                showToast("Profil użytkownika został pomyślnie zaktualizowany! ✨", "success");

                if (typeof onProfileSaved === 'function') {
                    onProfileSaved(updated);
                }
            } catch (err) {
                console.error("Failed to save profile:", err);
                showProfileAlert(err.message || "Wystąpił błąd podczas zapisywania profilu.");
            } finally {
                setProfileLoading(false);
            }
        })();
    });
}
