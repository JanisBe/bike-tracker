# Plan 1: Flutter App – Bike Ride Tracker

## Cel

Aplikacja mobilna (Android/iOS) do śledzenia tras rowerowych w czasie rzeczywistym, generowania plików GPX i wysyłania danych do Firebase.

---

## 1. Inicjalizacja projektu

```bash
flutter create --org com.biketracker --project-name bike_tracker bike_tracker_app
cd bike_tracker_app
```

### Wymagane zależności (`pubspec.yaml`)

| Pakiet | Cel |
|--------|-----|
| `firebase_core` | Inicjalizacja Firebase |
| `firebase_auth` | Logowanie użytkownika |
| `cloud_firestore` | Metadane wycieczek + GPX |
| `google_sign_in` | Google Sign-In |
| `geolocator` | Dostęp do GPS |
| `flutter_background_geolocation` **lub** `background_locator_2` | Śledzenie GPS w tle |
| `flutter_map` + `latlong2` | Wyświetlanie mapy (OpenStreetMap, darmowe) |
| `xml` | Generowanie plików GPX |
| `path_provider` | Ścieżki do pliku tymczasowego |
| `intl` | Formatowanie dat |
| `provider` **lub** `riverpod` | State management |

```bash
flutter pub add firebase_core firebase_auth cloud_firestore \
  google_sign_in geolocator flutter_map latlong2 xml path_provider intl provider
```

### Konfiguracja Firebase w projekcie Flutter

```bash
# Instalacja FlutterFire CLI
dart pub global activate flutterfire_cli

# Konfiguracja (generuje firebase_options.dart)
flutterfire configure --project=<FIREBASE_PROJECT_ID>
```

---

## 2. Architektura aplikacji

```
lib/
├── main.dart                    # Entry point, Firebase init
├── firebase_options.dart        # Auto-generated
├── models/
│   ├── ride.dart                # Model wycieczki (metadata)
│   └── track_point.dart         # Model punktu GPS
├── services/
│   ├── auth_service.dart        # Firebase Auth + Google Sign-In
│   ├── location_service.dart    # GPS tracking (foreground + background)
│   ├── gpx_service.dart         # Generowanie pliku GPX z punktów
│   ├── ride_service.dart        # CRUD wycieczek w Firestore
│   └── storage_service.dart     # Upload GPX do Firebase Storage
├── screens/
│   ├── login_screen.dart        # Ekran logowania
│   ├── home_screen.dart         # Lista wycieczek + przycisk "Start"
│   ├── tracking_screen.dart     # Aktywne śledzenie trasy (mapa live)
│   └── ride_detail_screen.dart  # Szczegóły wycieczki
├── widgets/
│   ├── ride_card.dart           # Karta wycieczki na liście
│   ├── stats_bar.dart           # Pasek statystyk (dystans, czas, prędkość)
│   └── live_map.dart            # Mapa z aktualną trasą
└── utils/
    ├── formatters.dart          # Formatowanie dystansu, czasu
    └── constants.dart           # Stałe konfiguracyjne
```

---

## 3. Model danych

### `TrackPoint`

```dart
class TrackPoint {
  final double latitude;
  final double longitude;
  final double? elevation;
  final DateTime timestamp;

  TrackPoint({
    required this.latitude,
    required this.longitude,
    this.elevation,
    required this.timestamp,
  });
}
```

### `Ride` (dokument Firestore)

```dart
class Ride {
  final String id;
  final String userId;
  final DateTime startTime;
  final DateTime endTime;
  final double distanceKm;       // Dystans w km
  final Duration duration;
  final double avgSpeedKmh;      // Średnia prędkość
  final double maxSpeedKmh;      // Max prędkość
  final double? elevationGain;   // Suma przewyższeń
  final String gpxStoragePath;   // Ścieżka do pliku w Firebase Storage
  final String encodedPolyline;  // Encoded polyline do szybkiego wyświetlania na mapie

  // fromFirestore() / toFirestore() converters
}
```

### Struktura Firestore

```
rides/ (collection)
  └── {rideId}/ (document)
        ├── userId: string
        ├── startTime: timestamp
        ├── endTime: timestamp
        ├── distanceKm: number
        ├── durationSeconds: number
        ├── avgSpeedKmh: number
        ├── maxSpeedKmh: number
        ├── elevationGain: number (nullable)
        ├── gpxStoragePath: string
        ├── encodedPolyline: string
        └── createdAt: timestamp
```

---

## 4. Kluczowe moduły

### 4.1 Autentykacja (`auth_service.dart`)

- Google Sign-In na mobile (z użyciem `google_sign_in` 7.x API – `authenticate()`)
- Na mobile: `GoogleSignIn.instance.initialize()` → `authenticate()` → `GoogleAuthProvider.credential(idToken:)`
- Przechowywanie stanu auth w `StreamBuilder` / `Provider`
- Obsługa wylogowania (conditional `GoogleSignIn.instance.signOut()` + `FirebaseAuth.instance.signOut()`)

### 4.2 Śledzenie GPS (`location_service.dart`)

```
┌─────────────────────────────────────┐
│  LocationService                    │
├─────────────────────────────────────┤
│  - Stream<Position> positionStream  │
│  - List<TrackPoint> currentTrack    │
│  - bool isTracking                  │
├─────────────────────────────────────┤
│  + startTracking()                  │
│  + stopTracking() → List<TrackPoint>│
│  + pauseTracking()                  │
│  + resumeTracking()                 │
└─────────────────────────────────────┘
```

**Konfiguracja GPS:**
- Dokładność: `LocationAccuracy.high`
- Interwał: co 3-5 sekund lub co 10 metrów (filtrowanie szumu)
- Filtrowanie punktów z accuracy > 20m (odrzucanie niedokładnych)
- Background mode: notyfikacja foreground service (Android), background location (iOS)

**Uprawnienia:**
- `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` (Android)
- `NSLocationWhenInUseUsageDescription` + `NSLocationAlwaysUsageDescription` (iOS)

### 4.3 Generowanie GPX (`gpx_service.dart`)

Konwersja `List<TrackPoint>` → plik XML w formacie GPX 1.1:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="BikeTracker">
  <metadata>
    <name>Ride 2026-09-16</name>
    <time>2026-09-16T09:00:00Z</time>
  </metadata>
  <trk>
    <name>Ride</name>
    <trkseg>
      <trkpt lat="52.2297" lon="21.0122">
        <ele>100.5</ele>
        <time>2026-09-16T09:00:00Z</time>
      </trkpt>
      <!-- ... -->
    </trkseg>
  </trk>
</gpx>
```

- Użycie pakietu `xml` do budowania dokumentu
- Zapis tymczasowy do `getApplicationDocumentsDirectory()`

### 4.4 Upload danych (`ride_service.dart`)

Po zakończeniu wycieczki (stopTracking):

1. **Generuj GPX XML** z `List<TrackPoint>`
2. **Oblicz statystyki**: dystans (Haversine), czas, prędkości, encoded polyline
3. **Zapisz metadata** → Firestore `rides/{rideId}`
4. **Zapisz GPX XML** → Firestore `rides/{rideId}/details/gpx`

```dart
Future<void> saveRide(List<TrackPoint> points) async {
  final gpxContent = gpxService.generateGpxString(points);
  final rideRef = FirebaseFirestore.instance.collection('rides').doc();
  final gpxRef = rideRef.collection('details').doc('gpx');
  
  // Calculate stats
  final stats = calculateStats(points);
  
  // Batch write: metadata + GPX content
  final batch = FirebaseFirestore.instance.batch();
  batch.set(rideRef, {
    'userId': auth.currentUser!.uid,
    'startTime': points.first.timestamp,
    'endTime': points.last.timestamp,
    'distanceKm': stats.distance,
    'durationSeconds': stats.duration.inSeconds,
    'avgSpeedKmh': stats.avgSpeed,
    'maxSpeedKmh': stats.maxSpeed,
    'elevationGain': stats.elevationGain,
    'encodedPolyline': encodePolyline(points),
    'createdAt': FieldValue.serverTimestamp(),
  });
  batch.set(gpxRef, {
    'gpxContent': gpxContent,
  });

  await batch.commit();
}
```

---

## 5. Ekrany (UI)

### 5.1 Login Screen
- Przycisk "Zaloguj przez Google"
- Logo aplikacji, minimalistyczny design

### 5.2 Home Screen
- Lista ukończonych wycieczek (sortowana po dacie)
- Każda karta: data, dystans, czas trwania, miniaturka trasy
- FAB: "Start nowej wycieczki"

### 5.3 Tracking Screen
- Mapa na żywo (`flutter_map` + OpenStreetMap tiles)
- Rysowanie trasy w czasie rzeczywistym
- Statystyki live: aktualny dystans, czas, prędkość
- Przyciski: Pauza / Wznów / Zakończ

### 5.4 Ride Detail Screen
- Mapa z pełną trasą
- Szczegółowe statystyki
- Opcja pobrania/udostępnienia pliku GPX

---

## 6. Obliczenia

### Dystans (Haversine formula)
Suma odległości między kolejnymi punktami GPS.

### Encoded Polyline
Algorytm Google Polyline Encoding do kompresji trasy → jeden string w Firestore, szybkie dekodowanie na frontendzie.

### Prędkość
- Średnia: dystans / czas
- Maksymalna: max(dystans_między_punktami / czas_między_punktami)

### Przewyższenie
Suma dodatnich różnic wysokości między kolejnymi punktami (z filtrowaniem szumu).

---

## 7. Uwagi implementacyjne

> [!IMPORTANT]
> **Background GPS na Androidzie** wymaga Foreground Service z widoczną notyfikacją. Bez tego system zabije aplikację po kilku minutach.

> [!WARNING]
> **`google_sign_in` 7.x breaking changes**: Metoda `signIn()` została zastąpiona przez `authenticate()`. Token `accessToken` nie jest już domyślnie dostępny — dla Firebase Auth wystarczy `idToken`.

> [!TIP]
> **Oszczędność baterii**: Filtruj punkty GPS z accuracy > 20m i stosuj `distanceFilter: 10` żeby nie zbierać punktów gdy rowerzysta stoi.

---

## 8. Plan testowania

- **Unit tests**: Obliczenia dystansu, generowanie GPX, encoded polyline
- **Widget tests**: Ekrany z mockowanymi serwisami
- **Integration test**: Pełny flow — logowanie → tracking → save → lista
- **Manual**: Test GPS tracking w tle na fizycznym urządzeniu (emulator nie ma dobrego GPS)
