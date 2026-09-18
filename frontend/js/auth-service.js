/**
 * Firebase Authentication Service.
 * Provides Email/Password and Google Sign-In with friendly Polish error messages.
 */
import {auth} from './firebase-config.js';
import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

const googleProvider = new GoogleAuthProvider();

/**
 * Sign in existing user with email and password.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<User>}
 */
export async function signInWithEmail(email, password) {
    try {
        const userCredential = await signInWithEmailAndPassword(auth, email.trim(), password);
        return userCredential.user;
    } catch (error) {
        throw new Error(getAuthErrorMessage(error.code) || error.message);
    }
}

/**
 * Register a new user with email and password.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<User>}
 */
export async function signUpWithEmail(email, password) {
    try {
        const userCredential = await createUserWithEmailAndPassword(auth, email.trim(), password);
        return userCredential.user;
    } catch (error) {
        throw new Error(getAuthErrorMessage(error.code) || error.message);
    }
}

/**
 * Sign in or sign up with Google popup.
 * @returns {Promise<User|null>}
 */
export async function signInWithGoogle() {
    try {
        const userCredential = await signInWithPopup(auth, googleProvider);
        return userCredential.user;
    } catch (error) {
        // If user closed the popup, don't show a loud error
        if (error.code === 'auth/popup-closed-by-user') {
            return null;
        }
        throw new Error(getAuthErrorMessage(error.code) || error.message);
    }
}

/**
 * Sign out current user.
 * @returns {Promise<void>}
 */
export async function signOutUser() {
    return await signOut(auth);
}

/**
 * Get the currently logged-in user or null.
 * @returns {User|null}
 */
export function getCurrentUser() {
    return auth.currentUser;
}

/**
 * Listen to authentication state changes.
 * @param {Function} callback - (user) => void
 * @returns {Function} unsubscribe function
 */
export function onAuthStateChange(callback) {
    return onAuthStateChanged(auth, callback);
}

/**
 * Map Firebase Auth error codes to user-friendly Polish messages.
 * @param {string} code
 * @returns {string}
 */
export function getAuthErrorMessage(code) {
    switch (code) {
        case 'auth/invalid-email':
            return 'Podany adres e-mail jest nieprawidłowy.';
        case 'auth/user-disabled':
            return 'To konto użytkownika zostało zablokowane.';
        case 'auth/user-not-found':
        case 'auth/wrong-password':
        case 'auth/invalid-credential':
            return 'Nieprawidłowy adres e-mail lub hasło.';
        case 'auth/email-already-in-use':
            return 'Konto z tym adresem e-mail już istnieje. Zaloguj się.';
        case 'auth/weak-password':
            return 'Hasło jest za słabe. Musi mieć co najmniej 6 znaków.';
        case 'auth/popup-closed-by-user':
            return 'Logowanie przez Google zostało przerwane.';
        case 'auth/popup-blocked':
            return 'Wyskakujące okno logowania zostało zablokowane przez przeglądarkę.';
        case 'auth/network-request-failed':
            return 'Błąd połączenia z siecią. Sprawdź swoje połączenie internetowe.';
        case 'auth/unauthorized-domain':
            return 'Ta domena nie jest autoryzowana w projekcie Firebase do logowania OAuth.';
        case 'auth/too-many-requests':
            return 'Zbyt wiele nieudanych prób logowania. Odczekaj chwilę i spróbuj ponownie.';
        default:
            return 'Wystąpił błąd autoryzacji. Spróbuj ponownie.';
    }
}
