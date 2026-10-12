import {db} from './firebase-config.js';
import {doc, getDoc, serverTimestamp, setDoc} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

/**
 * Fetches the user profile from Firestore users/{userId}.
 * @param {string} userId
 * @returns {Promise<Object>}
 */
export async function getUserProfile(userId) {
    if (!userId) return null;

    try {
        const docRef = doc(db, "users", userId);
        const snap = await getDoc(docRef);

        if (!snap.exists()) {
            return {
                userId,
                gender: "male",
                weightKg: 75.0,
                heightCm: 175.0,
                cyclingSpeedThresholdKmh: 1.5,
                walkingSpeedThresholdKmh: 0.5
            };
        }

        const data = snap.data();
        return {
            userId,
            email: data.email || "",
            displayName: data.displayName || "",
            gender: data.gender || "male",
            weightKg: data.weightKg != null ? Number(data.weightKg) : 75.0,
            heightCm: data.heightCm != null ? Number(data.heightCm) : 175.0,
            cyclingSpeedThresholdKmh: data.cyclingSpeedThresholdKmh != null ? Number(data.cyclingSpeedThresholdKmh) : 1.5,
            walkingSpeedThresholdKmh: data.walkingSpeedThresholdKmh != null ? Number(data.walkingSpeedThresholdKmh) : 0.5
        };
    } catch (error) {
        console.error("Firestore get user profile error:", error);
        return {
            userId,
            gender: "male",
            weightKg: 75.0,
            heightCm: 175.0,
            cyclingSpeedThresholdKmh: 1.5,
            walkingSpeedThresholdKmh: 0.5
        };
    }
}

/**
 * Saves or updates user profile in Firestore users/{userId}.
 * @param {string} userId
 * @param {Object} profileData
 * @returns {Promise<void>}
 */
export async function saveUserProfile(userId, profileData) {
    if (!userId) {
        throw new Error("Użytkownik nie jest zalogowany.");
    }

    try {
        const docRef = doc(db, "users", userId);
        const updateData = {
            userId,
            gender: profileData.gender || "male",
            weightKg: Number(profileData.weightKg) || 75.0,
            heightCm: Number(profileData.heightCm) || 175.0,
            cyclingSpeedThresholdKmh: Number(profileData.cyclingSpeedThresholdKmh) || 1.5,
            walkingSpeedThresholdKmh: Number(profileData.walkingSpeedThresholdKmh) || 0.5,
            updatedAt: serverTimestamp()
        };

        if (profileData.displayName) {
            updateData.displayName = profileData.displayName;
        }
        if (profileData.email) {
            updateData.email = profileData.email;
        }

        await setDoc(docRef, updateData, {merge: true});
    } catch (error) {
        console.error("Firestore save user profile error:", error);
        throw error;
    }
}
