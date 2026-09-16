# Plan 2: Firebase Setup – Backend Infrastructure (100% Darmowy Spark Plan)

## Cel

Konfiguracja Firebase jako darmowego backendu bez konieczności podpinania karty płatniczej:
- **Cloud Firestore**: metadane wycieczek, statystyki, zakodowana trasa (Polyline) oraz treść GPX XML
- **Firebase Authentication**: zabezpieczenie zapisu danych (Anonymous / Email-Password / Google Sign-In)
- **Zero Firebase Storage**: ominięcie wymogu konta bilingowego (Blaze)

---

## 1. Architektura danych w Firestore

Ponieważ Firebase Storage w nowych projektach wymaga karty płatniczej (plan Blaze), cały zapis realizujemy w **Cloud Firestore (Standard Edition)**, która w Spark Plan jest w 100% darmowa (1 GB danych, 50 000 odczytów/dzień, 20 000 zapisów/dzień).

Dokument w Firestore może mieć do **1 MB**.
- Metadane + encoded polyline dla 4-godzinnej trasy to zaledwie **~10 KB**.
- Pełen plik XML GPX trasy to **~100–250 KB**.

### Struktura kolekcji i dokumentów

```
rides/                          ← kolekcja główna
  └── {rideId}/                 ← dokument metadanych wycieczki
        ├── userId: string      ← UID właściciela
        ├── startTime: timestamp
        ├── endTime: timestamp
        ├── distanceKm: number
        ├── durationSeconds: number
        ├── avgSpeedKmh: number
        ├── maxSpeedKmh: number
        ├── elevationGain: number | null
        ├── encodedPolyline: string  ← lekki string do rysowania mapy
        ├── createdAt: timestamp
        │
        └── details/            ← podkolekcja na cięższe dane
              └── gpx           ← dokument zawierający pełną treść GPX
                    └── gpxContent: string (XML GPX)
```

> [!TIP]
> **Dlaczego podkolekcja `details/gpx`?**
> Lista wycieczek pobiera tylko dokument główny `rides/{rideId}` (kilka KB), dzięki czemu lista ładuje się natychmiastowo.
> Dokument `details/gpx` jest pobierany **tylko na żądanie**, gdy użytkownik kliknie „Pobierz plik GPX”.

---

## 2. Security Rules (`firestore.rules`)

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Rides collection
    match /rides/{rideId} {
      // Public read (for GitHub Pages frontend)
      allow read: if true;
      
      // Only authenticated user can create their own ride
      allow create: if request.auth != null 
                    && request.resource.data.userId == request.auth.uid;
      
      // Only the owner can update or delete
      allow update, delete: if request.auth != null 
                            && resource.data.userId == request.auth.uid;
      
      // Details subcollection (e.g. /rides/{rideId}/details/gpx)
      match /details/{detailId} {
        allow read: if true;
        allow write: if request.auth != null 
                     && request.auth.uid == get(/databases/$(database)/documents/rides/$(rideId)).data.userId;
      }
    }
    
    // Deny everything else
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 3. Indeksy (`firestore.indexes.json`)

Wdrożony indeks wielopolowy dla szybkiego sortowania wycieczek użytkownika:

```json
{
  "indexes": [
    {
      "collectionGroup": "rides",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "startTime", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

---

## 4. Konfiguracja Firebase (`firebase.json`)

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "auth": {
    "providers": {
      "anonymous": true,
      "emailPassword": true
    }
  }
}
```

---

## 5. Zarejestrowane aplikacje

- **Web App**: `Bike Tracker Web` (`1:654235244168:web:7d131cb98a0d128b1cf13d`)
  - Konfiguracja w `config/firebase-web-config.json`
- **Android App**: `Bike Tracker Android` (`com.biketracker`)
  - Plik `config/google-services.json`

---

## 6. Limity i koszty Spark Plan

| Zasób | Limit darmowy | Szacowane zużycie osobiste |
|---|---|---|
| **Firestore Storage** | 1 GiB | ~100 MB na 1000 wycieczek (10% limitu) |
| **Firestore Reads** | 50 000 / dzień | ~50–100 / dzień |
| **Firestore Writes** | 20 000 / dzień | ~1–5 / dzień |
| **Firebase Auth** | 10 000 / miesiąc | 1 użytkownik |
| **Koszt całkowity** | **0 zł** | **0 zł (Brak karty kredytowej)** |
