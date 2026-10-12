import {signInWithEmail, signInWithGoogle, signOutUser, signUpWithEmail} from '../auth-service.js';

let authMode = 'login'; // 'login' | 'register'

const authModal = document.getElementById("auth-modal");
const btnAuthClose = document.getElementById("btn-auth-close");
const btnLoginOpen = document.getElementById("btn-login-open");
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
const btnLogout = document.getElementById("btn-logout");

export function isAuthModalOpen() {
    return authModal && authModal.style.display !== "none" && authModal.style.display !== "";
}

export function switchAuthMode(mode) {
    authMode = mode;
    hideAuthAlert();

    if (mode === 'login') {
        tabLogin?.classList.add("active");
        tabRegister?.classList.remove("active");
        if (authSubmitText) authSubmitText.textContent = "Zaloguj się";
        if (authModalSubtitle) authModalSubtitle.textContent = "Zaloguj się, aby wyświetlić swoje treningi";
    } else {
        tabRegister?.classList.add("active");
        tabLogin?.classList.remove("active");
        if (authSubmitText) authSubmitText.textContent = "Utwórz konto";
        if (authModalSubtitle) authModalSubtitle.textContent = "Zarejestruj się, aby zapisywać i przeglądać treningi";
    }
}

export function openAuthModal(mode = 'login') {
    switchAuthMode(mode);
    if (authModal) authModal.style.display = "flex";
    authEmailInput?.focus();
}

export function closeAuthModal() {
    if (authModal) authModal.style.display = "none";
    hideAuthAlert();
    authForm?.reset();
    setAuthLoading(false);
}

export function showAuthAlert(message) {
    if (authAlert && authAlertMsg) {
        authAlertMsg.textContent = message;
        authAlert.style.display = "flex";
    }
}

export function hideAuthAlert() {
    if (authAlert) {
        authAlert.style.display = "none";
    }
}

export function setAuthLoading(isLoading) {
    if (btnAuthSubmit) btnAuthSubmit.disabled = isLoading;
    if (btnGoogleAuth) btnGoogleAuth.disabled = isLoading;
    if (authSpinner) authSpinner.style.display = isLoading ? "inline-block" : "none";
    if (authSubmitText) authSubmitText.style.display = isLoading ? "none" : "inline";
}

/**
 * Initializes Authentication modal listeners
 * @param {object} [callbacks]
 * @param {Function} [callbacks.onSignOut]
 */
export function initAuthModal({onSignOut} = {}) {
    btnLoginOpen?.addEventListener("click", () => openAuthModal('login'));
    btnAuthClose?.addEventListener("click", () => closeAuthModal());

    authModal?.addEventListener("click", (e) => {
        if (e.target === authModal) {
            closeAuthModal();
        }
    });

    tabLogin?.addEventListener("click", () => switchAuthMode('login'));
    tabRegister?.addEventListener("click", () => switchAuthMode('register'));

    btnLogout?.addEventListener("click", () => {
        void (async () => {
            try {
                if (typeof onSignOut === 'function') {
                    onSignOut();
                }
                await signOutUser();
            } catch (err) {
                console.error("Sign out error:", err);
            }
        })();
    });

    authForm?.addEventListener("submit", (e) => {
        e.preventDefault();
        void (async () => {
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
                // onAuthStateChange handles UI update & modal close
            } catch (err) {
                console.error("Auth error:", err);
                showAuthAlert(err.message || "Wystąpił błąd autoryzacji.");
            } finally {
                setAuthLoading(false);
            }
        })();
    });

    btnGoogleAuth?.addEventListener("click", () => {
        void (async () => {
            setAuthLoading(true);
            hideAuthAlert();

            try {
                await signInWithGoogle();
                // onAuthStateChange handles UI update & modal close
            } catch (err) {
                console.error("Google auth error:", err);
                showAuthAlert(err.message || "Błąd logowania przez Google.");
            } finally {
                setAuthLoading(false);
            }
        })();
    });
}
