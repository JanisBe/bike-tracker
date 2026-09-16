# Plan 4: Kotlin App – Bike Ride Tracker (Native Android)

## Cel

Natywna aplikacja Android (Kotlin + Jetpack Compose) do śledzenia tras rowerowych w czasie rzeczywistym, generowania plików GPX i wysyłania danych do Firebase.

---

## 1. Inicjalizacja projektu

### Android Studio → New Project
- **Template**: Empty Activity (Compose)
- **Name**: Bike Tracker
- **Package**: `com.biketracker`
- **Min SDK**: API 26 (Android 8.0) — pokrywa ~95% urządzeń
- **Build system**: Kotlin DSL (build.gradle.kts)

### Wymagane zależności (`build.gradle.kts` — app module)

```kotlin
dependencies {
    // Compose (BOM)
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Firebase (BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In (Credential Manager — nowy sposób)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Lokalizacja
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Mapy (osm / mapbox)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // LUB: MapCompose dla Compose-native map
    // implementation("ovh.plrapps:mapcompose:2.12.0")

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // DataStore (preferencje)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

### Pluginy (`build.gradle.kts` — app module)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")     // Firebase
    id("com.google.dagger.hilt.android")      // Hilt DI
    kotlin("kapt")
}
```

### Firebase config

```bash
# Pobierz google-services.json
npx -y firebase-tools@latest apps:sdkconfig ANDROID <APP_ID> --project <PROJECT_ID>
# Zapisz do app/google-services.json
```

---

## 2. Architektura aplikacji (MVVM + Clean Architecture)

```
app/src/main/java/com/biketracker/
├── BikeTrackerApp.kt                # Application class (Hilt)
├── MainActivity.kt                  # Single Activity (Compose)
├── navigation/
│   └── NavGraph.kt                  # Navigation Compose routes
│
├── data/
│   ├── model/
│   │   ├── Ride.kt                  # Data class wycieczki
│   │   └── TrackPoint.kt            # Data class punktu GPS
│   ├── repository/
│   │   ├── RideRepository.kt        # Interfejs
│   │   └── RideRepositoryImpl.kt    # Firebase Firestore + Storage
│   └── mapper/
│       └── RideMapper.kt            # Firestore ↔ domain mapping
│
├── domain/
│   ├── usecase/
│   │   ├── SaveRideUseCase.kt       # Zapis wycieczki (GPX + Firestore)
│   │   ├── GetRidesUseCase.kt       # Lista wycieczek
│   │   └── DeleteRideUseCase.kt     # Usunięcie wycieczki
│   └── util/
│       ├── GpxGenerator.kt          # Generowanie pliku GPX
│       ├── DistanceCalculator.kt    # Haversine formula
│       ├── PolylineEncoder.kt       # Encoded polyline
│       └── StatsCalculator.kt       # Statystyki wycieczki
│
├── service/
│   └── LocationTrackingService.kt   # Foreground Service (GPS w tle)
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                 # Material3 theme
│   │   ├── Color.kt                 # Paleta kolorów
│   │   └── Type.kt                  # Typografia
│   ├── login/
│   │   ├── LoginScreen.kt           # Composable
│   │   └── LoginViewModel.kt        # ViewModel
│   ├── home/
│   │   ├── HomeScreen.kt            # Lista wycieczek
│   │   ├── HomeViewModel.kt
│   │   └── RideCard.kt              # Composable karta wycieczki
│   ├── tracking/
│   │   ├── TrackingScreen.kt        # Aktywne śledzenie (mapa live)
│   │   └── TrackingViewModel.kt
│   └── detail/
│       ├── RideDetailScreen.kt      # Szczegóły wycieczki
│       └── RideDetailViewModel.kt
│
└── di/
    ├── AppModule.kt                 # Hilt module (singletons)
    └── RepositoryModule.kt          # Hilt bindings
```

---

## 3. Model danych

### `TrackPoint.kt`

```kotlin
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timestamp: Long,        // epoch millis
    val accuracy: Float? = null // accuracy w metrach
)
```

### `Ride.kt`

```kotlin
data class Ride(
    val id: String = "",
    val userId: String = "",
    val startTime: Date = Date(),
    val endTime: Date = Date(),
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevationGain: Double? = null,
    val gpxStoragePath: String = "",
    val encodedPolyline: String = "",
    val createdAt: Date = Date()
)
```

### Firestore mapping

```kotlin
object RideMapper {
    fun toFirestore(ride: Ride): Map<String, Any?> = mapOf(
        "userId" to ride.userId,
        "startTime" to Timestamp(ride.startTime),
        "endTime" to Timestamp(ride.endTime),
        "distanceKm" to ride.distanceKm,
        "durationSeconds" to ride.durationSeconds,
        "avgSpeedKmh" to ride.avgSpeedKmh,
        "maxSpeedKmh" to ride.maxSpeedKmh,
        "elevationGain" to ride.elevationGain,
        "gpxStoragePath" to ride.gpxStoragePath,
        "encodedPolyline" to ride.encodedPolyline,
        "createdAt" to FieldValue.serverTimestamp()
    )

    fun fromFirestore(doc: DocumentSnapshot): Ride = Ride(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        startTime = doc.getTimestamp("startTime")?.toDate() ?: Date(),
        endTime = doc.getTimestamp("endTime")?.toDate() ?: Date(),
        distanceKm = doc.getDouble("distanceKm") ?: 0.0,
        durationSeconds = doc.getLong("durationSeconds") ?: 0,
        avgSpeedKmh = doc.getDouble("avgSpeedKmh") ?: 0.0,
        maxSpeedKmh = doc.getDouble("maxSpeedKmh") ?: 0.0,
        elevationGain = doc.getDouble("elevationGain"),
        gpxStoragePath = doc.getString("gpxStoragePath") ?: "",
        encodedPolyline = doc.getString("encodedPolyline") ?: "",
        createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
    )
}
```

---

## 4. Kluczowe moduły

### 4.1 Autentykacja — Credential Manager (nowy sposób Google Sign-In)

> [!IMPORTANT]
> Stary `GoogleSignInClient` jest **deprecated** od 2024. Android używa teraz **Credential Manager API**.

```kotlin
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val googleIdToken = GoogleIdTokenCredential
                .createFrom(result.credential.data).idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
```

### 4.2 Foreground Service — GPS tracking w tle ⭐

To jest **kluczowa przewaga** natywnego Androida. Bezpośredni dostęp do `ForegroundService` bez warstwy abstrakcji.

#### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <service
        android:name=".service.LocationTrackingService"
        android:foregroundServiceType="location"
        android:exported="false" />
</application>
```

#### LocationTrackingService.kt

```kotlin
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var rideRepository: RideRepository

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 
        3_000L  // co 3 sekundy
    ).apply {
        setMinUpdateDistanceMeters(10f)  // lub co 10 metrów
        setWaitForAccurateLocation(true)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                // Filtruj niedokładne punkty
                if (location.accuracy <= 20f) {
                    val point = TrackPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        elevation = if (location.hasAltitude()) location.altitude else null,
                        timestamp = location.time,
                        accuracy = location.accuracy
                    )
                    _trackPoints.value = _trackPoints.value + point
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        _isTracking.value = true
        _trackPoints.value = emptyList()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun stopTracking() {
        _isTracking.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseTracking() {
        _isTracking.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun resumeTracking() {
        _isTracking.value = true
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Ride Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚴 Ride in progress")
            .setContentText("Tracking your bike ride...")
            .setSmallIcon(R.drawable.ic_bike)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null // nie bindujemy

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "ride_tracking"
    }
}
```

> [!TIP]
> Foreground Service z `foregroundServiceType="location"` gwarantuje, że Android **nie zabije** procesu GPS nawet gdy apka jest w tle. W Flutterze potrzebujesz do tego pluginu, który wewnętrznie robi to samo — ale z dodatkową warstwą abstrakcji.

### 4.3 Generowanie GPX (`GpxGenerator.kt`)

```kotlin
object GpxGenerator {

    fun generate(points: List<TrackPoint>, rideName: String): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="BikeTracker">""")
            appendLine("  <metadata>")
            appendLine("    <name>$rideName</name>")
            appendLine("    <time>${Instant.now()}</time>")
            appendLine("  </metadata>")
            appendLine("  <trk>")
            appendLine("    <name>$rideName</name>")
            appendLine("    <trkseg>")
            points.forEach { pt ->
                appendLine("""      <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""")
                pt.elevation?.let { appendLine("        <ele>$it</ele>") }
                appendLine("        <time>${Instant.ofEpochMilli(pt.timestamp)}</time>")
                appendLine("      </trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    fun saveToFile(context: Context, gpxContent: String, fileName: String): File {
        val file = File(context.cacheDir, "$fileName.gpx")
        file.writeText(gpxContent)
        return file
    }
}
```

### 4.4 Upload i zapis (`RideRepositoryImpl.kt`)

```kotlin
class RideRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : RideRepository {

    private val ridesCollection = firestore.collection("rides")

    override suspend fun saveRide(points: List<TrackPoint>): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val rideId = ridesCollection.document().id

            // 1. Generate GPX content
            val gpxContent = GpxGenerator.generate(points, "Ride $rideId")

            // 2. Calculate stats
            val stats = StatsCalculator.calculate(points)

            // 3. Save metadata to Firestore (rides/{rideId})
            val ride = Ride(
                id = rideId,
                userId = userId,
                startTime = Date(points.first().timestamp),
                endTime = Date(points.last().timestamp),
                distanceKm = stats.distanceKm,
                durationSeconds = stats.durationSeconds,
                avgSpeedKmh = stats.avgSpeedKmh,
                maxSpeedKmh = stats.maxSpeedKmh,
                elevationGain = stats.elevationGain,
                encodedPolyline = PolylineEncoder.encode(points)
            )

            // Batch write: metadata + GPX subcollection
            val batch = firestore.batch()
            val rideRef = ridesCollection.document(rideId)
            val gpxRef = rideRef.collection("details").document("gpx")

            batch.set(rideRef, RideMapper.toFirestore(ride))
            batch.set(gpxRef, mapOf("gpxContent" to gpxContent))
            batch.commit().await()

            Result.success(rideId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRides(): Flow<List<Ride>> {
        val userId = auth.currentUser?.uid ?: return flowOf(emptyList())
        return ridesCollection
            .whereEqualTo("userId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { RideMapper.fromFirestore(it) }
            }
    }

    override suspend fun getGpxContent(rideId: String): Result<String> {
        return try {
            val doc = ridesCollection.document(rideId)
                .collection("details").document("gpx").get().await()
            val gpx = doc.getString("gpxContent") ?: throw Exception("GPX not found")
            Result.success(gpx)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRide(ride: Ride): Result<Unit> {
        return try {
            // Delete GPX subdocument and ride document
            val batch = firestore.batch()
            batch.delete(ridesCollection.document(ride.id).collection("details").document("gpx"))
            batch.delete(ridesCollection.document(ride.id))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 5. Ekrany (Jetpack Compose)

### 5.1 Login Screen

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DirectionsBike, contentDescription = null, 
                 modifier = Modifier.size(96.dp),
                 tint = MaterialTheme.colorScheme.primary)
            
            Text("Bike Tracker", style = MaterialTheme.typography.headlineLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = { viewModel.signIn() }) {
                Text("Sign in with Google")
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
        }
    }
}
```

### 5.2 Home Screen — lista wycieczek

- `LazyColumn` z kartami wycieczek
- Każda karta: data, dystans, czas, mini-preview trasy
- FAB "Start ride" → nawigacja do TrackingScreen
- Pull-to-refresh

### 5.3 Tracking Screen — aktywne śledzenie

- `osmdroid` MapView (lub MapCompose) z live polyline
- Live statystyki: dystans, czas, prędkość (obserwacja `StateFlow` z Service)
- Przyciski: Pause / Resume / Stop
- Komunikacja z `LocationTrackingService` przez Intent actions

```kotlin
@Composable
fun TrackingScreen(viewModel: TrackingViewModel = hiltViewModel()) {
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val stats by viewModel.liveStats.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Map (70% screen)
        Box(modifier = Modifier.weight(0.7f)) {
            OsmMapView(points = trackPoints)
        }
        
        // Live stats bar
        StatsBar(
            distance = stats.distanceKm,
            duration = stats.duration,
            speed = stats.currentSpeedKmh
        )
        
        // Control buttons
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            if (isTracking) {
                IconButton(onClick = { viewModel.pause() }) {
                    Icon(Icons.Default.Pause, "Pause")
                }
            } else {
                IconButton(onClick = { viewModel.resume() }) {
                    Icon(Icons.Default.PlayArrow, "Resume")
                }
            }
            
            Button(onClick = { viewModel.stopAndSave() },
                   colors = ButtonDefaults.buttonColors(
                       containerColor = MaterialTheme.colorScheme.error)) {
                Text("Finish Ride")
            }
        }
    }
}
```

### 5.4 Ride Detail Screen

- Pełna mapa z trasą
- Pełne statystyki (dystans, czas, prędkości, przewyższenie)
- Przycisk "Download GPX" / "Share GPX"
- Przycisk "Delete ride" z potwierdzeniem

---

## 6. Dependency Injection (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindRideRepository(impl: RideRepositoryImpl): RideRepository
}
```

---

## 7. Komunikacja Activity ↔ Service

```kotlin
// Startowanie / zatrzymywanie service z Composable:
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rideRepository: RideRepository
) : ViewModel() {

    fun startTracking() {
        Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
            context.startForegroundService(this)
        }
    }

    fun stopAndSave() {
        // Pobierz punkty z Service (przez bound service lub SharedFlow)
        Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
            context.startService(this)
        }
        
        viewModelScope.launch {
            rideRepository.saveRide(collectedPoints)
        }
    }
}
```

> [!NOTE]
> Do przekazywania danych (track points) z Service do ViewModel, najlepiej użyć **bound service** z `Binder` lub **singleton `StateFlow`** (np. w `@Singleton` companion object). Prostsze podejście dla osobistej apki: statyczny `StateFlow` w companion object Service.

---

## 8. Porównanie z Flutter

| Aspekt | Kotlin (ten plan) | Flutter (plan 01) |
|--------|:--:|:--:|
| Background GPS | natywny `ForegroundService` | plugin `flutter_background_geolocation` |
| Liczba zależności 3rd party | **3** (Firebase, GMS Location, osmdroid) | **~12** pluginów |
| Rozmiar APK | ~8 MB | ~25 MB |
| Debugging GPS | Android Studio + Logcat natywne | Flutter DevTools + plugin logs |
| Czas buildu | wolniejszy (Gradle) | szybszy (hot reload) |
| Krzywa uczenia | Kotlin + Compose + Android lifecycle | Dart + Flutter (prostsze API) |
| Ryzyko pluginów | **niskie** — wszystko natywne | średnie — zależność od pluginów |
| Hot reload | ❌ (ale Live Edit w AS) | ✅ (szybki iteracyjny development) |

---

## 9. Uwagi implementacyjne

> [!IMPORTANT]
> **Android 14+ (API 34)**: Wymaga explicit deklaracji `foregroundServiceType="location"` w manifeście. Bez tego `startForeground()` rzuci `ForegroundServiceTypeNotAllowedException`.

> [!WARNING]
> **Runtime permissions**: Na Androidzie 13+ (API 33) trzeba osobno prosić o `POST_NOTIFICATIONS`. Na Androidzie 12+ (API 31) `ACCESS_FINE_LOCATION` jest granularne — user może przyznać tylko `COARSE`.

> [!TIP]
> **Google Sign-In**: Stary `GoogleSignInClient` jest deprecated. Użyj **Credential Manager API** (`androidx.credentials`). Wymaga mniej konfiguracji i obsługuje passkeys w przyszłości.

> [!TIP]
> **Testowanie GPS**: Android Studio → Emulator → Extended Controls → Location → Load GPX/KML. Możesz symulować jazdę rowerem bez wychodzenia z domu.

---

## 10. Plan testowania

- **Unit tests**: `StatsCalculator`, `GpxGenerator`, `PolylineEncoder`, `RideMapper`
- **UI tests**: Compose testing (`composeTestRule`) dla ekranów
- **Instrumented tests**: `FusedLocationProviderClient` mock, Foreground Service lifecycle
- **Manual**: Test na fizycznym urządzeniu z prawdziwym GPS (krótka przejażdżka)
- **Emulator GPS**: Load GPX w emulatorze → weryfikacja parsowania i zapisu

---

## 11. Struktura AndroidManifest.xml (kompletna)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".BikeTrackerApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Bike Tracker"
        android:theme="@style/Theme.BikeTracker">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.BikeTracker">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.LocationTrackingService"
            android:foregroundServiceType="location"
            android:exported="false" />

    </application>
</manifest>
```
