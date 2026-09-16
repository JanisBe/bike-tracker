# Plan 3: Static Frontend – Ride Viewer (GitHub Pages)

## Cel

Statyczna strona webowa (HTML/CSS/JS) hostowana na GitHub Pages, która pobiera dane wycieczek z Firebase i prezentuje je na interaktywnej mapie z listą i statystykami.

---

## 1. Technologie

| Technologia | Cel | Koszt |
|-------------|-----|-------|
| **Vanilla HTML/CSS/JS** | Struktura i logika | $0 |
| **Firebase JS SDK v9+** (modular) | Odczyt danych z Firestore + Storage | $0 |
| **Leaflet.js** | Interaktywna mapa | $0 (open-source) |
| **OpenStreetMap tiles** | Podkład mapowy | $0 |
| **Chart.js** (opcjonalnie) | Wykresy statystyk | $0 (open-source) |
| **GitHub Pages** | Hosting | $0 |

> [!TIP]
> Brak build stepu, bundlera, czy node_modules. Czyste ES Modules z CDN — zero kosztów utrzymania. Alternatywnie można użyć Vite dla lepszego DX, ale dla prostej strony to overkill.

---

## 2. Struktura plików

```
frontend/
├── index.html              # Główna strona
├── css/
│   └── style.css           # Style
├── js/
│   ├── app.js              # Entry point, inicjalizacja
│   ├── firebase-config.js  # Firebase config + init
│   ├── ride-service.js     # Pobieranie danych z Firestore
│   ├── map-renderer.js     # Renderowanie trasy na mapie Leaflet
│   ├── ride-list.js        # Renderowanie listy wycieczek
│   ├── stats.js            # Obliczanie i wyświetlanie statystyk
│   └── polyline-decoder.js # Dekodowanie encoded polyline
├── assets/
│   ├── icons/              # Ikony (rower, pin, etc.)
│   └── favicon.ico
└── README.md
```

---

## 3. Firebase Config (`firebase-config.js`)

```javascript
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "...",           // z `firebase apps:sdkconfig WEB`
  authDomain: "...",
  projectId: "...",
  storageBucket: "...",
  messagingSenderId: "...",
  appId: "..."
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
```

> [!NOTE]
> Klucze Firebase w kodzie frontendowym to **normalna praktyka**. Bezpieczeństwo opiera się na Security Rules (public read), nie na ukrywaniu kluczy. API key sam w sobie nie daje uprawnień do zapisu.

---

## 4. Pobieranie danych (`ride-service.js`)

```javascript
import { db } from './firebase-config.js';
import { collection, getDocs, query, orderBy, doc, getDoc } from 
  "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

// Pobierz wszystkie wycieczki (posortowane od najnowszej)
export async function fetchAllRides() {
  const q = query(
    collection(db, 'rides'), 
    orderBy('startTime', 'desc')
  );
  const snapshot = await getDocs(q);
  return snapshot.docs.map(doc => ({
    id: doc.id,
    ...doc.data(),
    // Konwertuj Firestore Timestamps na JS Date
    startTime: doc.data().startTime?.toDate(),
    endTime: doc.data().endTime?.toDate(),
  }));
}

// Pobierz pojedynczą wycieczke
export async function fetchRide(rideId) {
  const docRef = doc(db, 'rides', rideId);
  const docSnap = await getDoc(docRef);
  if (docSnap.exists()) {
    const data = docSnap.data();
    return {
      id: docSnap.id,
      ...data,
      startTime: data.startTime?.toDate(),
      endTime: data.endTime?.toDate(),
    };
  }
  return null;
}
```

---

## 5. Dekodowanie Polyline (`polyline-decoder.js`)

```javascript
// Google Polyline Algorithm decoder
export function decodePolyline(encoded) {
  const points = [];
  let index = 0, lat = 0, lng = 0;

  while (index < encoded.length) {
    let shift = 0, result = 0, byte;
    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    lat += (result & 1) ? ~(result >> 1) : (result >> 1);

    shift = 0; result = 0;
    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    lng += (result & 1) ? ~(result >> 1) : (result >> 1);

    points.push([lat / 1e5, lng / 1e5]);
  }
  return points;
}
```

---

## 6. Mapa (`map-renderer.js`)

```javascript
import { decodePolyline } from './polyline-decoder.js';

let map;
let currentRouteLayer;

export function initMap(containerId) {
  map = L.map(containerId).setView([52.23, 21.01], 12); // Default: Warszawa
  
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19,
  }).addTo(map);
}

export function displayRoute(encodedPolyline, options = {}) {
  // Usuń poprzednią trasę
  if (currentRouteLayer) {
    map.removeLayer(currentRouteLayer);
  }

  const points = decodePolyline(encodedPolyline);
  
  currentRouteLayer = L.polyline(points, {
    color: options.color || '#ff6b35',
    weight: 4,
    opacity: 0.8,
  }).addTo(map);

  // Start/End markers
  if (points.length > 0) {
    L.marker(points[0], { title: 'Start' }).addTo(map);
    L.marker(points[points.length - 1], { title: 'Finish' }).addTo(map);
  }

  // Auto-zoom na trasę
  map.fitBounds(currentRouteLayer.getBounds(), { padding: [20, 20] });
}
```

---

## 7. Lista wycieczek (`ride-list.js`)

```javascript
export function renderRideList(rides, container, onRideClick) {
  container.innerHTML = '';
  
  rides.forEach(ride => {
    const card = document.createElement('div');
    card.className = 'ride-card';
    card.innerHTML = `
      <div class="ride-date">${formatDate(ride.startTime)}</div>
      <div class="ride-stats">
        <span class="stat">
          <span class="stat-icon">📏</span>
          ${ride.distanceKm.toFixed(1)} km
        </span>
        <span class="stat">
          <span class="stat-icon">⏱️</span>
          ${formatDuration(ride.durationSeconds)}
        </span>
        <span class="stat">
          <span class="stat-icon">⚡</span>
          ${ride.avgSpeedKmh.toFixed(1)} km/h
        </span>
      </div>
    `;
    card.addEventListener('click', () => onRideClick(ride));
    container.appendChild(card);
  });
}
```

---

## 8. Design / UI

### Layout

```
┌───────────────────────────────────────────────────┐
│  🚴 Bike Tracker                     [dark mode]  │
├──────────────┬────────────────────────────────────┤
│              │                                    │
│  Ride List   │           Map (Leaflet)            │
│  ─────────   │                                    │
│  📅 16 Sep   │       ╭─── route ───╮              │
│  12.5 km     │       │             │              │
│  45 min      │       ╰─────────────╯              │
│  ─────────   │                                    │
│  📅 15 Sep   │                                    │
│  8.2 km      │                                    │
│  32 min      │                                    │
│  ─────────   │                                    │
│              │                                    │
│              ├────────────────────────────────────┤
│              │  📊 Stats Panel                    │
│              │  Total: 156 km | Rides: 12         │
│              │  Avg: 13 km/ride | ↑ 450m          │
└──────────────┴────────────────────────────────────┘
```

### Styl

- **Dark mode** domyślnie (ciemne tło, gradient akcenty)
- **Glassmorphism** na kartach wycieczek (backdrop-filter: blur)
- **Gradient** na trasie na mapie (np. prędkość → kolor)
- **Animacje**: smooth scroll, fade-in kart, hover effects
- **Responsive**: na mobile lista i mapa stack'ują się pionowo
- **Google Fonts**: Inter lub Outfit
- **Paleta kolorów**:
  - Background: `#0f0f23` → `#1a1a3e`
  - Accent: `#ff6b35` (pomarańczowy, dynamiczny)
  - Secondary: `#4ecdc4` (morski)
  - Text: `#e8e8e8`
  - Cards: `rgba(255, 255, 255, 0.05)` z border `rgba(255, 255, 255, 0.1)`

### Responsive Breakpoints

```css
/* Desktop: side-by-side layout */
@media (min-width: 1024px) {
  .app-layout { grid-template-columns: 350px 1fr; }
}

/* Tablet: narrower sidebar */
@media (min-width: 768px) and (max-width: 1023px) {
  .app-layout { grid-template-columns: 280px 1fr; }
}

/* Mobile: stacked layout */
@media (max-width: 767px) {
  .app-layout { grid-template-columns: 1fr; }
}
```

---

## 9. Dodatkowe funkcje

### 9.1 Pobieranie GPX

Pobieranie surowego pliku GPX bezpośrednio z podkolekcji `details/gpx` w Firestore i wywołanie pobierania w przeglądarce (Blob):

```javascript
import { db } from './firebase-config.js';
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

async function downloadGpx(rideId, rideDate = 'ride') {
  const gpxDocRef = doc(db, 'rides', rideId, 'details', 'gpx');
  const snap = await getDoc(gpxDocRef);
  if (!snap.exists() || !snap.data().gpxContent) {
    alert("Brak pliku GPX dla tej trasy.");
    return;
  }
  
  const gpxXml = snap.data().gpxContent;
  const blob = new Blob([gpxXml], { type: 'application/gpx+xml;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  
  const a = document.createElement('a');
  a.href = url;
  a.download = `ride_${rideDate}.gpx`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
```

### 9.2 Statystyki zbiorcze

```javascript
function calculateOverallStats(rides) {
  return {
    totalRides: rides.length,
    totalDistanceKm: rides.reduce((sum, r) => sum + r.distanceKm, 0),
    totalDurationHours: rides.reduce((sum, r) => sum + r.durationSeconds, 0) / 3600,
    avgDistanceKm: rides.reduce((sum, r) => sum + r.distanceKm, 0) / rides.length,
    avgSpeedKmh: rides.reduce((sum, r) => sum + r.avgSpeedKmh, 0) / rides.length,
    totalElevationGain: rides.reduce((sum, r) => sum + (r.elevationGain || 0), 0),
    longestRideKm: Math.max(...rides.map(r => r.distanceKm)),
    fastestAvgSpeed: Math.max(...rides.map(r => r.avgSpeedKmh)),
  };
}
```

### 9.3 Filtrowanie / wyszukiwanie

- Filtr po zakresie dat (date picker)
- Filtr po minimalnym dystansie
- Sortowanie: data / dystans / prędkość

### 9.4 Heatmap (opcjonalnie)

Nałożenie wszystkich tras na jedną mapę → wizualizacja najczęściej jeżdżonych dróg. Leaflet.heat plugin.

---

## 10. Hosting na GitHub Pages

### Setup

```bash
# W repozytorium bike-tracker, frontend jest w folderze /frontend
# GitHub Pages → Settings → Source: Deploy from branch
# Branch: main, folder: /frontend (lub /docs)
```

**Alternatywnie** — dedykowane repo:

```bash
# Utwórz repo np. bike-tracker-web
# Push folder frontend/ jako root
# Włącz GitHub Pages z brancha main
```

### Custom domain (opcjonalnie)

1. Kup domenę (np. `bikerides.dev`)
2. Dodaj plik `CNAME` z zawartością domeny
3. Skonfiguruj DNS (CNAME → `<username>.github.io`)

---

## 11. SEO & Meta

```html
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Bike Tracker – My Cycling Adventures</title>
  <meta name="description" content="Interactive map of my cycling routes and ride statistics">
  <meta name="theme-color" content="#0f0f23">
  <link rel="icon" href="assets/favicon.ico">
  
  <!-- Open Graph -->
  <meta property="og:title" content="Bike Tracker">
  <meta property="og:description" content="Interactive cycling ride tracker with maps and stats">
  <meta property="og:type" content="website">
  
  <!-- Fonts -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  
  <!-- Leaflet CSS -->
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
</head>
```

---

## 12. Performance

- **Lazy loading**: Ładuj wycieczki w porcjach (Firestore pagination z `startAfter`)
- **Cache**: `localStorage` dla ostatnio pobranych danych
- **Encoded polyline**: Lekkie dekodowanie client-side zamiast pobierania surowych koordynatów
- **Defer GPX download**: Pliki GPX ładowane dopiero po kliknięciu "Download"

---

## 13. Plan testowania

- **Lokalne testowanie**: `python -m http.server 8080` lub VS Code Live Server
- **Cross-browser**: Chrome, Firefox, Safari, Edge
- **Mobile responsive**: Chrome DevTools device mode
- **Firebase**: Sprawdź czy dane ładują się poprawnie z Firestore (Network tab)
- **Lighthouse**: Uruchom audit na performance, accessibility, SEO
