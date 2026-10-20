# Changelog • Bike Tracker

Wszystkie istotne zmiany i nowości w projekcie Bike Tracker (wersja Android oraz Web).

## [1.1.0] - 2026-10-20

### Dodano
- **Skróty na ikonie aplikacji (App Shortcuts):** Szybki start treningu po przytrzymaniu palca na ikonie aplikacji: „Trening rower” oraz „Trening pieszy”.
- **Wybór źródeł mapy:** Możliwość wyboru podkładu mapy (OpenStreetMap Dark, CyclOSM z siecią tras rowerowych, OpenTopoMap z poziomicami) oraz nakładek szlaków Waymarked Trails (szlaki rowerowe i piesze).
- **Rozróżnienie aktywności (Rower / Spacer):** Dedykowany tryb dla marszu i spaceru z obliczaniem tempa (min/km), dedykowanym liczeniem kalorii oraz akcentem kolorystycznym.
- **Profil użytkownika i progi prędkości:** Możliwość konfiguracji wagi, wzrostu, płci oraz progów prędkości wykrywania ruchu dla roweru i spaceru.
- **Wykrywanie trybu oszczędzania energii:** Inteligentne ostrzeżenie przed treningiem na telefonie, gdy włączone oszczędzanie energii mogłoby zaburzyć odczyty GPS w tle.
- **Zintegrowany Changelog:** Podgląd historii wersji i wprowadzonych funkcji bezpośrednio z poziomu aplikacji webowej oraz mobilnej.

### Poprawiono
- **Stabilność i filtracja GPS:** Zaawansowana eliminacja anomalii i fałszywych skoków prędkości/pozycji przy słabym sygnale satelitów.
- **Centrowanie i nawigacja po mapie:** Wyłączono niepożądane resetowanie pozycji widoku mapy przy przeglądaniu listy tras.
- **Wydajność i modularyzacja:** Modułowa architektura kodu JS, podział arkuszy stylów CSS oraz płynniejsze gesty przesuwania (swipe) do edycji i usuwania tras na telefonach.

## [1.0.0] - 2026-10-12

### Dodano
- **Rejestracja tras GPS w tle (Android):** Niezawodny serwis działający na pierwszym planie (Foreground Service) z powiadomieniami o statusie GPS, czasie i dystansie.
- **Interaktywny kalendarz aktywności:** Przeglądanie dni z treningami i szybkie filtrowanie historii.
- **Wykresy i telemetria trasy:** Interaktywny profil wysokości i wykres prędkości na osi dystansu trasy.
- **Synchronizacja z chmurą:** Zapis podsumowań treningów oraz surowych śladów telemetrycznych GPX w bazie Cloud Firestore.
- **Import i eksport plików GPX:** Pełna obsługa importu plików GPX z zewnętrznych aplikacji oraz pobieranie plików GPX.
- **Autoryzacja i konta:** Bezpieczne logowanie przez Google Sign-In oraz e-mail/hasło.
