# 🚴 Bike Tracker – Static Web Frontend

Nowoczesna, responsywna aplikacja Single Page Application (SPA) w architekturze **Vanilla ES Modules** i **Vanilla CSS**
do prezentacji tras rowerowych, telemetrii treningów oraz profili wysokości z możliwością pobierania i importowania
plików GPX.

Dostępna online pod adresem:  
👉 **[https://janisbe.github.io/bike-tracker/](https://janisbe.github.io/bike-tracker/)**

---

## ✨ Główne funkcje

- 🗺️ **Wielowarstwowa mapa ze szlakami turystycznymi i rowerowymi:**
  - **Podkłady mapowe:** OpenStreetMap (Ciemna), CyclOSM (infrastruktura rowerowa), OpenTopoMap (rzeźba terenu i
    poziomice), UMP-pcPL (polska mapa społecznościowa), OpenStreetMap (Jasna).
  - **Nakładki szlaków (Waymarked Trails):** Szlaki rowerowe, szlaki piesze/górskie, trasy MTB.
  - Płynny przełącznik warstw w lewym górnym rogu z trwałym zapisem wyboru w `localStorage`.
- 📅 **Interaktywny widżet kalendarza:**
  - Zaznaczanie dni treningowych, przełączanie miesięcy, szybki powrót do „Dziś”, filtrowanie listy po kliknięciu w dany
    dzień.
- 📈 **Profil wysokości i prędkości (Scrubbing):**
  - Wykres Canvas z płynnym śledzeniem kursora i synchronizacją ze wskaźnikiem pozycji na mapie.
- 📱 **Responsywny interfejs mobilny:**
  - Zoptymalizowany pod smartfony (układ listy i szczegółów, pasek nawigacji z powrotem, edycja i udostępnianie, pełny
    ekran mapy).
- 📊 **Zbiorcze podsumowania i metryki:**
  - Całkowity dystans, czas, spalone kalorie, liczba treningów, średnia i maksymalna prędkość, przewyższenia.
- 🔐 **Autoryzacja i profil użytkownika:**
  - Logowanie przez konto Google, email/hasło, tryb gościa, edycja profilu (waga, imię).
- 📥 **Import i eksport GPX:**
  - Pobieranie śladu każdej trasy do formatu `.gpx` oraz bezpośredni import tras z plików GPX do Firebase.
- 📲 **Pobieranie aplikacji Android:**
  - Przycisk szybkiego pobierania najnowszego pliku `.apk` z GitHub Releases w nagłówku strony.

---

## 🏗️ Architektura kodu

Projekt opiera się wyłącznie na natywnych standardach przeglądarkowych bez bundlerów i ciężkich frameworków:

```
frontend/
├── index.html                  # Główny szablon HTML aplikacji
├── css/
│   ├── base.css                # Zmienne CSS (--bg-dark, --accent-orange itp.), reset, animacje
│   ├── layout.css              # Kontenery aplikacji, pasek boczny, siatki
│   ├── map.css                 # Stylizacja Leaflet, filtry ciemnego motywu, kontrolka warstw
│   ├── calendar.css            # Widżet kalendarza aktywności
│   ├── chart.css               # Wykres profilu wysokości i telemetrii
│   ├── rides.css               # Karty treningów, statystyki, lista
│   ├── modals.css              # Okna modalne (logowanie, edycja, usuwanie, profil)
│   ├── responsive.css          # Breakpointy mobilne (<= 860px)
│   ├── style.css               # Punkt wejściowy importujący arkusze CSS
│   └── toast.css               # Powiadomienia Toast
└── js/
    ├── app.js                  # Główny kontroler aplikacji i przepływ zdarzeń
    ├── map-renderer.js         # Leaflet: podkłady, nakładki, polylines, scrub-markery
    ├── calendar-widget.js      # Logika generowania i obsługi kalendarza
    ├── profile-chart.js        # Rysowanie wykresu wysokości/prędkości (Canvas)
    ├── ride-service.js         # Operacje Firestore na kolekcjach rides/{id}
    ├── rides-view.js           # Renderowanie listy tras i kart podsumowań
    ├── geocoding.js            # Odwrócone geokodowanie (OpenStreetMap Nominatim)
    ├── url-routing.js          # Synchronizacja wybranego treningu z adresem URL (?ride=...)
    ├── auth-service.js         # Integracja z Firebase Authentication
    ├── user-service.js         # Profil użytkownika w Firestore
    ├── polyline-decoder.js     # Dekoder Google Encoded Polyline
    ├── toast.js                # System powiadomień na ekranie
    └── modals/                 # Moduły obsługi okien modalnych
```

---

## 🚀 Jak uruchomić lokalnie

W katalogu `frontend/`:

```bash
# Opcja 1: Python
python -m http.server 8080

# Opcja 2: Node.js npx serve
npx serve .
```

Otwórz w przeglądarce: `http://localhost:8080`

---

## 🌐 Wdrożenie na GitHub Pages

Repozytorium posiada automatyczny workflow GitHub Actions (`.github/workflows/deploy-frontend.yml`), który przy każdym
wypchnięciu zmian w katalogu `frontend/**` automatycznie publikuje najnowszą wersję pod adresem:
👉 **[https://janisbe.github.io/bike-tracker/](https://janisbe.github.io/bike-tracker/)**

---

## 🔒 Bezpieczeństwo kluczy Firebase

Plik `js/firebase-config.js` zawiera publiczne identyfikatory projektu Firebase. Jest to oficjalny i bezpieczny standard dla aplikacji frontendowych SPA. 

Bezpieczeństwo danych opiera się na regułach **Firestore Security Rules** wdrożonych w pliku `firestore.rules`:
- **Odczyt:** publiczny dla każdego użytkownika odwiedzającego stronę.
- **Zapis / Modyfikacja:** zablokowane dla osób nieautoryzowanych, możliwe tylko dla zalogowanego właściciela trasy
  (`request.auth.uid == resource.data.userId`).

