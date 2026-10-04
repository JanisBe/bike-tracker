# AI Agent Guidelines

## 1. Project Structure & Target Modules
- `android/` - Primary native Android application (Kotlin, Jetpack Compose, Hilt, Coroutines, MVVM).
- `frontend/` - Production Web app (Vanilla ES Modules JavaScript, Leaflet.js, CSS variables, Firebase Auth & Firestore SDK v11).
- `bike_tracker_app/` - Legacy / experimental Flutter module (do not modify unless explicitly requested by user).

## 2. Android UI Development & Device Testing
When working on the Android UI:
1. Build the application.
2. Deploy it to the connected Android device.
3. Launch the application (`adb shell am start -n com.biketracker/.MainActivity`).
4. Use `screenshot` to inspect the actual rendered UI.
5. Use `snapshot_ui` to inspect Compose semantics and element positions.
6. If modifying UI, verify the result on the actual device.
7. Do not assume that the Compose preview represents the actual device rendering.
8. When testing interactions, use the MCP UI tools instead of assuming coordinates.

## 3. Firebase & Firestore Data Conventions
- **Data Model:**
  - Main ride document: `rides/{rideId}` (contains summary metrics: `title`, `locationName`, `startTime`, `endTime`, `distanceKm`, `durationSeconds`, `avgSpeedKmh`, `maxSpeedKmh`, `elevationGain`, `encodedPolyline`, `userId`).
  - Heavy GPX payload: `rides/{rideId}/details/gpx` with field `{ gpxContent: "<xml>..." }` (Spark Plan / 0 MB Storage design).
- **Atomic Operations:** Always use batch writes (`writeBatch` / `firestore.batch()`) when creating or deleting a ride so the main document and its `details/gpx` subcollection remain consistent.
- **Rules & Auth:** Every ride document must include `userId` matching `request.auth.uid`.

## 4. CI/CD & GitHub Actions Automation
- **APK Builds:** The `.github/workflows/build-apks.yml` workflow triggers on branch `main` when the commit message contains `[APK]`, `[DEPLOY]`, or `[KOTLIN]`, or on tags `v*`.
- **Releases:** Automatically publishes latest APK to GitHub Releases under tag `latest` (`bike-tracker.apk` and `app-release.apk`).
- **Frontend Deployment:** Pushing changes to `frontend/**` automatically triggers GitHub Pages deployment (`deploy-frontend.yml`).

## 5. Code & Language Standards
- **Language:** All source code, comments, docstrings, commit messages, and variable/function names must be written in **English**.
- **Java / Kotlin:** Never use Fully Qualified Class Names in code body — always use explicit top-level imports.
- **Web Frontend:** Use modern ES Modules (`import/export`), avoid unnecessary third-party libraries, and adhere to the established Dark Theme CSS variables (`--bg-dark`, `--accent-orange`, `--accent-green`, `--accent-cyan`).

## 6. Verification Steps Before Turn Completion
- **Android:** Always run `./gradlew compileDebugKotlin` in `android/` directory to verify there are no compilation or type errors.
- **Frontend:** Run `node -c <modified_js_files>` to check for JavaScript syntax errors and verify UI with local server (`python -m http.server 8080` in `frontend/`).
