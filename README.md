# 🚴 Bike Tracker

Kompleksowa aplikacja do śledzenia tras rowerowych składająca się z natywnej aplikacji Android oraz webowego panelu do
przeglądania historii jazd.

---

## 📐 Architektura projektu

```
bike-tracker/
├── android/          # Aplikacja mobilna (Kotlin, Jetpack Compose)
└── frontend/         # Panel webowy (HTML, CSS, Vanilla JS)
```

Dane są przechowywane w **Google Firestore** i synchronizowane między aplikacją mobilną a frontendem webowym.

---

## 📱 Aplikacja Android

Natywna aplikacja Android napisana w **Kotlin** z wykorzystaniem **Jetpack Compose**.

### Główne funkcje

- 🛰️ **Śledzenie GPS w tle** – foreground service z WakeLock, który działa niezawodnie nawet przy wygaszonym ekranie
- 📡 **Monitorowanie satelitów GNSS** – wyświetlanie liczby satelitów używanych do fiksacji (`used/total`)
- ⚠️ **Wykrywanie utraty sygnału GPS** – powiadomienie i wibracja przy utracie/odzyskaniu sygnału
- ⏸️ **Pauza i wznowienie** – sterowanie treningiem bezpośrednio z powiadomienia systemowego
- 📊 **Profil wysokości** – interaktywny wykres elewacji dla każdej trasy
- 🗺️ **Mapa trasy** – wizualizacja przejechanych dróg na mapie OSM
- 💾 **Zapis do Firestore** – automatyczny zapis po zakończeniu treningu z wyświetleniem podsumowania
- 📤 **Eksport GPX** – pobieranie pliku GPX z każdej trasy

### Stos technologiczny

| Warstwa                  | Technologia                                 |
|--------------------------|---------------------------------------------|
| UI                       | Jetpack Compose + Material 3                |
| Nawigacja                | Navigation Compose                          |
| Wstrzykiwanie zależności | Hilt                                        |
| Lokalizacja              | FusedLocationProviderClient (Play Services) |
| Mapa                     | osmdroid                                    |
| Backend / baza danych    | Firebase Firestore + Firebase Auth          |
| Asynchroniczność         | Kotlin Coroutines + StateFlow               |
| Min. Android             | API 26 (Android 8.0)                        |

### Struktura kodu (`android/app/src/main/java/com/biketracker/`)

```
├── BikeTrackerApp.kt       # Klasa Application (inicjalizacja Hilt)
├── MainActivity.kt         # Punkt wejścia UI
├── data/
│   ├── model/              # Modele danych (TrackPoint, SatelliteInfo, …)
│   ├── mapper/             # Mapowanie między modelami a Firestore
│   └── repository/         # RideRepository – zapis/odczyt tras
├── domain/
│   └── util/               # Narzędzia domenowe (DistanceCalculator)
├── di/                     # Moduły Hilt
├── service/
│   └── LocationTrackingService.kt  # Foreground service GPS
└── ui/
    ├── home/               # Ekran główny – lista jazd, kalendarz
    ├── detail/             # Szczegóły trasy (mapa, profil, statystyki)
    ├── tracking/           # Ekran aktywnego śledzenia
    ├── login/              # Logowanie Google
    ├── navigation/         # Graf nawigacji Compose
    └── theme/              # Kolory, typografia, kształty Material 3
```

### Uprawnienia wymagane przez aplikację

| Uprawnienie                   | Powód                                  |
|-------------------------------|----------------------------------------|
| `ACCESS_FINE_LOCATION`        | Precyzyjne GPS                         |
| `FOREGROUND_SERVICE_LOCATION` | Śledzenie w tle                        |
| `WAKE_LOCK`                   | Utrzymanie CPU aktywnego podczas jazdy |
| `POST_NOTIFICATIONS`          | Powiadomienia treningowe (Android 13+) |
| `VIBRATE`                     | Sygnały o utracie/odzyskaniu GPS       |

---

## 🌐 Frontend webowy

Statyczna strona HTML/CSS/JS bez żadnego frameworka – wdrażana na **GitHub Pages** lub **Firebase Hosting**.

### Główne funkcje

- 📅 **Widżet kalendarza** – przegląd jazd z zaznaczeniem aktywnych dni
- 🗺️ **Renderowanie mapy** – trasy na mapie Leaflet.js z OpenStreetMap
- 📈 **Profil wysokości** – interaktywny wykres Chart.js
- 📊 **Statystyki** – dystans, czas, prędkość dla każdej jazdy
- 🔐 **Firebase Auth** – tylko zalogowany użytkownik może przeglądać dane
- 📥 **Pobieranie GPX** – eksport trasy do pliku GPX

### Uruchomienie lokalne

```bash
cd frontend

# Python
python -m http.server 3000

# Node.js
npx serve .
```

Otwórz: `http://localhost:3000`

---

## 🔥 Firebase / Backend

Projekt wykorzystuje **Google Firebase** jako bezserwerowy backend:

| Usługa               | Zastosowanie                        |
|----------------------|-------------------------------------|
| **Firestore**        | Przechowywanie tras i punktów GPS   |
| **Firebase Auth**    | Uwierzytelnianie przez konto Google |
| **Firebase Hosting** | Opcjonalny hosting frontendu        |

### Struktura kolekcji Firestore

```
rides/{rideId}
  ├── userId          (string)
  ├── startTime       (timestamp)
  ├── endTime         (timestamp)
  ├── distanceKm      (number)
  ├── durationSeconds (number)
  └── details/{detailId}
        └── encodedPolyline / trackPoints / …
```

### Reguły bezpieczeństwa

- **Odczyt** tras – publiczny (frontend może wyświetlać trasy bez logowania)
- **Zapis / modyfikacja / usunięcie** – tylko uwierzytelniony właściciel trasy

---

## 🚀 Wdrożenie

### Aplikacja Android

```bash
cd android
./gradlew assembleRelease
```

Plik APK znajdziesz w `android/app/build/outputs/apk/release/`.

### Frontend (GitHub Pages)

1. Wypchnij kod na GitHub
2. W ustawieniach repozytorium: **Settings → Pages → Branch: `main`, Folder: `/frontend`**
3. Strona będzie dostępna pod: `https://<username>.github.io/<repo>/`

### Frontend (Firebase Hosting)

```bash
firebase login
firebase deploy --only hosting
```

---

## 🔒 Bezpieczeństwo

Plik `frontend/js/firebase-config.js` zawiera publiczne identyfikatory projektu Firebase. Jest to standardowe i
bezpieczne podejście dla aplikacji SPA – bezpieczeństwo danych egzekwują **Firestore Security Rules**
(`firestore.rules`), a nie ukrywanie kluczy.

---

## 📋 Wymagania deweloperskie

| Narzędzie          | Wersja            |
|--------------------|-------------------|
| Android Studio     | Ladybug+          |
| JDK                | 21                |
| Kotlin             | 2.x               |
| Gradle             | 8.x               |
| Node.js (frontend) | 18+ (opcjonalnie) |
| Firebase CLI       | latest            |
