# Changelog • Bike Tracker

Wszystkie istotne zmiany i nowości w projekcie Bike Tracker (wersja Android oraz Web).

## [1.3.1] - 2026-10-29

### Dodano
- **Natywny motyw Darkroom (Aesthetics & Design System - Android):** Pełna implementacja estetyki Darkroom w aplikacji Jetpack Compose — ciepła czerń węgla (`#141312`), kafelki przyrządowe (`#1A1817`), ciepła kość słoniowa (`#EDE8E1`) oraz sportowe akcenty bursztynowe (`#E58A2B`) i cyjanowe (`#38BDF8`), całkowicie eliminujące generyczny, fioletowo-niebieski motyw Material 3.
- **Dedykowany kokpit telemetryczny na żywo (Android):** Reorganizacja ekranu rejestracji treningu w styl sportowego komputera rowerowego — wskaźnik prędkości z plakietką jednostki `KM/H`, 4 niezależne kafelki instrumentów telemetrycznych z obwódkami `1.dp` oraz ergonomiczny przycisk pauzy i zapisu.
- **Przebudowany ekran logowania (Android):** Techniczny identyfikator `[ TELEMETRY · GPS COMPASS ]`, wyrazista typografia marki, minimalistyczne sloty formularza `6.dp` z obwódką hairline i ciemny przycisk logowania Google.
- **Interaktywny profil trasy w trybie pełnoekranowym (Android):** Możliwość podglądu profilu wysokości i prędkości bezpośrednio na pełnym ekranie mapy w szczegółach treningu, z pełną synchronizacją punktu na trasie podczas przesuwania palcem po wykresie.
- **Zwijany i rozwijany wykres profilu (Android):** Możliwość zwinięcia komponentu `RideProfileChart` do kompaktowego paska za pomocą strzałki w prawym górnym rogu z płynną animacją, zapewniająca maksymalną widoczność mapy zarówno w widoku pełnoekranowym, jak i standardowym.

## [1.3.0] - 2026-10-26

### Dodano
- **Nowy motyw graficzny Darkroom (Aesthetics & Design System - Web):** Autorska oprawa wizualna aplikacji webowej odchodząca od generycznych niebiesko-szarych szablonów na rzecz ciepłej czerni odbitkowej (`oklch(12.5% 0.008 55)`), struktury papieru fotograficznego oraz bursztynowych akcentów telemetrycznych (amber safelight).
- **Nowa typografia i wektorowe ikony (Web):** Wdrożenie krojów Sofia Sans Extra Condensed (wyraziste nagłówki i metryki) oraz Barlow (treść i dane telemetryczne), a także zastąpienie znaków emoji ostrymi, skalowalnymi ikonami SVG.
- **Scentralizowany system tokenów (`tokens.css`):** Spójna skala odstępów (oparta na siatce 4px), typografia, tokeny semantyczne dla aktywności i stanów interfejsu oraz pełna zgodność wsteczna zmiennych stylów.
- **Sportowa typografia telemetryczna (Android):** Wdrożenie cyfr tabelarycznych (`fontFeatureSettings = "tnum"`) dla licznika prędkościomierza, tempa, czasu trwania, dystansu i wskaźnika tooltipu na wykresie wysokości, całkowicie eliminujące mikro-drgania układu podczas dynamicznych zmian liczb.
- **3-poziomowa hierarchia sportowa (Android):** Przebudowa ekranu szczegółów treningu z dawnej siatki 2×3 na strukturę z wyrazistym blokiem Hero (Dystans + Czas), blokiem prędkości/tempa oraz kompaktowym paskiem pracy fizycznej (Kalorie + Przewyższenie).
- **Spłaszczone panele i wektorowe ikony (Android):** Eliminacja cieni kart i zbędnych zagnieżdżeń na rzecz lekkich paneli `Surface` z obramowaniem `1.dp`, usunięcie surowych emotikonów oraz pełna migracja do spójnych ikon `Icons.Rounded.*` i `Icons.AutoMirrored.Rounded.*`.

### Zmieniono
- **Domyślny podkład mapy (Web & Android):** Ustawiono klasyczną, jasną wersję OpenStreetMap (Standardowa) jako domyślny podkład mapy w aplikacji webowej oraz w natywnej aplikacji mobilnej Android (`MapBaseSource.OSM_STANDARD`).

### Poprawiono
- **Hierarchia warstw z-index na mapie:** Wyeliminowano błąd przykrywania panelu szczegółów treningu przez ładujące się kafelki mapy Leaflet (`tile-pane`) dzięki dostosowaniu skali z-index do architektury warstw Leaflet.
- **Rozmieszczenie elementów sterujących mapy:** Rozwiązano problem kolizji panelu szczegółów treningu z selektorem źródeł mapy (`map-layer-control`) — panel szczegółów został odsunięty poniżej przycisku, a menu wyboru mapy zyskało wyższy priorytet wyświetlania (`z-index: 1100`), co zapewnia pełną czytelność obu kontrolek.

## [1.2.0] - 2026-10-21

### Dodano
- **Wykrywanie i instalacja aktualizacji w aplikacji (In-App Updates):** Automatyczne wykrywanie nowych wydań APK na GitHub Releases przy każdym uruchomieniu aplikacji Android.
- **Wygodny instalator aktualizacji:** Możliwość pobrania paczki APK z dynamicznym paskiem postępu pobierania oraz bezpośrednie wywołanie instalatora systemowego Androida.
- **Elastyczne opcje dla użytkownika:** Możliwość pobrania i zainstalowania, odłożenia decyzji („Pomiń”) lub trwałego zignorowania danej wersji („Nie proponuj ponownie”), dopóki nie pojawi się nowsze wydanie.
- **Inteligentne porównywanie wersji:** Obsługa wydań z tagami semantycznymi (SemVer) oraz wydań z kanału `latest` na podstawie identyfikatorów commitów Git (`commit sha`) i znaczników czasu.

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
