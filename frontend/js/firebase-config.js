import {initializeApp} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import {getFirestore} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";
import {getAuth} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

export const firebaseConfig = {
    apiKey: "AIzaSyDs-6wzyTCe08CYYuw5LkEumMKcOlWnpSk",
  authDomain: "bike-tracker-97e13.firebaseapp.com",
  projectId: "bike-tracker-97e13",
  storageBucket: "bike-tracker-97e13.firebasestorage.app",
  messagingSenderId: "654235244168",
  appId: "1:654235244168:web:7d131cb98a0d128b1cf13d"
};

export const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
