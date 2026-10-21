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

- 🛰️ **Śledzenie GPS w tle** – foreground service z WakeLock, działający niezawodnie nawet przy wygaszonym ekranie
- 🚴 / 🚶 **Rozróżnianie aktywności** – obsługa kolarstwa (`cycling`) oraz spaceru/marszu (`walking`), z automatycznym
  doborem jednostek (km/h vs tempo min/km) i precyzyjnym licznikiem kalorii MET
- 🗺️ **Zaawansowane źródła map i szlaki** – wybór podkładów (OpenStreetMap Ciemna/Jasna, CyclOSM, OpenTopoMap, UMP-pcPL)
  oraz nakładek szlaków (Waymarked Trails: rowerowe, piesze, MTB) z trwałym zapamiętywaniem wyboru
- 📡 **Monitorowanie satelitów GNSS** – wyświetlanie liczby satelitów używanych do fiksacji (`used/total`) oraz jakości
  sygnału
- 🔋 **Wykrywanie oszczędzania energii** – detekcja trybu Battery Saver i ostrzeżenie przed uśpieniem GPS z szybkim
  skrótem do ustawień
- ⚠️ **Wykrywanie utraty sygnału GPS** – konfigurowalne powiadomienia i wibracje przy utracie oraz odzyskaniu sygnału
- 🎯 **Sterowanie mapą i wyśrodkowanie** – detekcja ręcznego przesuwania mapy przez użytkownika z przyciskiem ponownego
  centrowania
- ⏸️ **Pauza i wznowienie** – sterowanie treningiem bezpośrednio z powiadomienia systemowego
- 📊 **Profil wysokości i prędkości** – interaktywny wykres elewacji i profilu trasy z podglądem scrubbingu
- 📅 **Kalendarz aktywności** – miesięczny kalendarz z zaznaczonymi dniami treningowymi i podsumowaniami
- 💾 **Zapis do Firestore** – atomowy zapis dokumentu trasy i subkolekcji `details/gpx` w Firebase
- 📤 **Eksport i import GPX** – pobieranie oraz import plików GPX
- 🔄 **Aktualizacje w aplikacji (Self-Updater)** – automatyczne sprawdzanie wydań GitHub Releases przy starcie z możliwością pobrania APK, bezpośredniej instalacji, pominięcia lub trwałego ignorowania wersji
- 🤖 **Automatyczne wydania APK** – GitHub Actions automatycznie buduje i publikuje plik `.apk` w GitHub Releases

### Stos technologiczny

| Warstwa                  | Technologia                                 |
|--------------------------|---------------------------------------------|
| UI                       | Jetpack Compose + Material 3                |
| Nawigacja                | Navigation Compose                          |
| Wstrzykiwanie zależności | Hilt                                        |
| Lokalizacja              | FusedLocationProviderClient (Play Services) |
| Mapa                     | osmdroid (obsługa wielu źródeł kafelków)    |
| Backend / baza danych    | Firebase Firestore + Firebase Auth          |
| Asynchroniczność         | Kotlin Coroutines + StateFlow               |
| Min. Android             | API 26 (Android 8.0)                        |

### Struktura kodu (`android/app/src/main/java/com/biketracker/`)

```
├── BikeTrackerApp.kt       # Klasa Application (inicjalizacja Hilt i osmdroid)
├── MainActivity.kt         # Punkt wejścia UI (routing, obsługa intentów i aktualizacji)
├── data/
│   ├── model/              # Modele danych (TrackPoint, SatelliteInfo, UserProfile, AppUpdateInfo, …)
│   ├── mapper/             # Mapowanie między modelami a Firestore
│   └── repository/         # RideRepository, UserRepository, MapPreferencesRepository, AppUpdateRepository
├── domain/
│   └── util/               # Narzędzia domenowe (GpxGenerator, GpxParser, CalorieCalculator)
├── di/                     # Moduły wstrzykiwania zależności Hilt
├── service/
│   └── LocationTrackingService.kt  # Foreground service GPS z powiadomieniami
└── ui/
    ├── home/               # Ekran główny – lista jazd, kalendarz aktywności
    ├── detail/             # Szczegóły trasy (mapa, profil, statystyki, pełny ekran)
    ├── tracking/           # Ekran aktywnego śledzenia z mapą i telemetrią
    ├── map/                # Definicje źródeł map, nakładki szlaków, arkusz wyboru warstw
    ├── profile/            # Profil użytkownika, waga, preferencje powiadomień
    ├── login/              # Logowanie Google i Email/Hasło
    ├── update/             # ViewModel aktualizatora aplikacji
    ├── components/         # Komponenty współdzielone (AppUpdateDialog, RestorePowerSaveDialog)
    ├── navigation/         # Graf nawigacji Compose
    └── theme/              # Kolory, typografia, kształty Material 3 (ciemny motyw)
```

### Uprawnienia wymagane przez aplikację

| Uprawnienie                   | Powód                                                 |
|-------------------------------|-------------------------------------------------------|
| `ACCESS_FINE_LOCATION`        | Precyzyjne GPS                                        |
| `FOREGROUND_SERVICE_LOCATION` | Śledzenie w tle                                       |
| `WAKE_LOCK`                   | Utrzymanie CPU aktywnego podczas jazdy                |
| `POST_NOTIFICATIONS`          | Powiadomienia treningowe (Android 13+)                |
| `VIBRATE`                     | Sygnały o utracie/odzyskaniu GPS                      |
| `REQUEST_INSTALL_PACKAGES`    | Instalacja aktualizacji aplikacji z GitHub Releases   |

---

## 🌐 Frontend webowy

Nowoczesna aplikacja Single Page Application (Vanilla ES Modules + Vanilla CSS) – wdrażana automatycznie na **GitHub
Pages** pod adresem:
👉 **[https://janisbe.github.io/bike-tracker/](https://janisbe.github.io/bike-tracker/)**

### Główne funkcje

- 🗺️ **Wielowarstwowa mapa ze szlakami** – rozwijane menu wyboru podkładów (OpenStreetMap Ciemna/Jasna, CyclOSM
  rowerowa, OpenTopoMap topograficzna, UMP-pcPL) oraz nakładek szlaków (Waymarked Trails: rowerowe, piesze, MTB) z
  zapamiętywaniem w `localStorage`
- 📅 **Interaktywny widżet kalendarza** – przegląd treningów w wybranym miesiącu z oznaczonymi dniami aktywności i
  statystykami
- 📈 **Profil wysokości i prędkości** – interaktywny wykres Canvas ze wskaźnikiem scrubbingu powiązanym z pulsującym
  znacznikiem na mapie
- 📱 **Responsywny interfejs mobilny** – dopasowany widok szczegółów na smartfonach oraz tryb pełnoekranowy mapy
- 📊 **Zbiorcza telemetria** – łączny dystans, czas trwania, liczba treningów i spalone kalorie
- 🔍 **Wyszukiwarka i filtry** – szybkie wyszukiwanie treningów po nazwie i lokalizacji
- 🔐 **Firebase Auth** – logowanie Google / e-mail oraz tryb gościa
- 📥 **Eksport i import GPX** – pobieranie tras do GPX oraz importowanie zewnętrznych plików GPX
- 📲 **Pobieranie APK** – bezpośredni przycisk pobierania najnowszej wersji aplikacji Android z GitHub Releases

### Uruchomienie lokalne

```bash
cd frontend

# Python
python -m http.server 8080

# Node.js
npx serve .
```

Otwórz: `http://localhost:8080`

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
