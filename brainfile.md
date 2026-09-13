# 🧠 BRAINFILE — Drigo Application Master Project Context & State Log

**Last Updated:** September 8, 2026
**Repository:** `https://github.com/naeemullah-silverdale/Drigo`
**Project Type:** Mass-Market Ride-Sharing Android App (Passenger + Driver Modes)

---

## 📌 1. EXECUTIVE SUMMARY & TECH STACK

- **Language & Framework:** Kotlin, Jetpack Compose, Material Design 3.
- **Min SDK:** 23 (Android 6.0 compatibility for low-end / budget devices, e.g., 2GB-4GB RAM, Samsung A12-class).
- **Target SDK:** 36 / Android 16.
- **Backend & Realtime Database:**
  - Firebase Realtime Database (`https://drigo-8b15c-default-rtdb.firebaseio.com`)
  - Cloud Firestore
  - Firebase Authentication & Google Sign-In
- **Mapping & Location:** OsmDroid (OpenStreetMap vector tiles), OpenRouteService / OSRM routing, Android Location Services.

---

## 🏗️ 2. CORE ARCHITECTURE & SINGLETONS

### 🔑 `RideManager` (`com.example.data.remote.RideManager`)
- **Role:** Thread-safe singleton managing the active trip reactive state flow across the entire app.
- **Exposed State:** `val activeTrip: StateFlow<PassengerOrder?>`
- **Synchronization Mechanism:**
  - Observes `active_trips/{tripId}` in **Firestore** and **Realtime Database** simultaneously.
  - Ensures both Passenger (`HomeScreen.kt`) and Driver (`DriverModeView.kt`) views receive `COMPLETED` or `CANCELLED` status changes synchronously without state desync.
- **Methods:**
  - `observeActiveTrip(tripId: String)`
  - `saveActiveTrip(order: PassengerOrder)`
  - `updateTripStatus(orderId, status, requestId, passengerId, driverId, finalFare)`
  - `clearActiveTrip()`

### 📦 `FirebaseRepository` (`com.example.data.remote.FirebaseRepository`)
- Singleton handling network and database interactions:
  - Exposes `dataConnectManager` for Firebase Data Connect (Cloud SQL / PostgreSQL) queries/mutations (`drigo-8b15c`).
  - Driver & Passenger order queries (`listenToPassengerOrders`, `listenToDriverActiveTrip`)
  - Live location streaming (`updateDriverLocation`, `observeLiveDriverLocation`)
  - Rating system (`saveRideRating`, `hasUserRatedRide`)
  - Safety reports (`submitSafetyReport`)

### 🔌 `FirebaseDataConnectManager` (`com.example.data.remote.FirebaseDataConnectManager`)
- **Role:** Thread-safe singleton managing Firebase Data Connect (Cloud SQL / PostgreSQL) integration.
- **Configured Service Connector:** `us-south1/drigo-8b15c-service/default`
- **Exposed State:** `val isConnected: StateFlow<Boolean>`, `val lastSyncTimestamp: StateFlow<Long>`

### 🎨 `ThemeManager` (`com.example.util.ThemeManager`) & `ThemePreference` (`com.example.util.ThemePreference`)
- **Role:** Centralized theme mode management for `LIGHT`, `DARK`, and `SYSTEM` modes.
- **Persistence:** Jetpack DataStore Preferences (`drigo_theme_preferences`).
- **Reactive Stream:** Exposes `val themeMode: StateFlow<ThemeMode>` and `fun getThemeModeFlow(context): Flow<ThemeMode>`.
- **UI Observation:** Consumed via Kotlin Flow in `MainActivity.kt` to dynamically feed `DrigoTheme(themeMode = themeMode)`.
- **UI Controls:** Interactive appearance selector embedded in Navigation Drawer (`HomeScreen.kt`).

---

## 🔄 3. RIDE LIFECYCLE (4-PHASE MODEL)

1. `DRIVER_COMING` – Driver accepted offer and is traveling to passenger pickup location.
2. `DRIVER_ARRIVED` – Driver arrived at pickup spot; 4-digit PIN verification code displayed to passenger.
3. `IN_TRIP` – Trip underway following real-time map route.
4. `COMPLETED` / `CANCELLED` – Trip finished or terminated; automatically triggers rating and digital receipt modal on both Passenger and Driver screens.

---

## 🎯 4. CRITICAL STATE & COMPATIBILITY RULES

1. **Independent Locations (`FROM` and `TO`):**
   - `fromLocation` (pickup) and `toLocation` (destination) are strictly independent.
   - Selecting `TO` must NEVER alter `FROM`.
   - Selecting `FROM` must NEVER alter `TO`.
   - GPS/current location updates must NEVER overwrite explicit user choices.
2. **Device Compatibility:**
   - Designed for low-end phones (320dp–360dp available width).
   - Use `dp` for dimensions, `sp` for text.
   - Support font scaling without layout clipping.
   - Handle insets (`WindowInsets`), status bar, gesture navigation, and display cutouts.
3. **No Unnecessary Refactoring:**
   - Preserve existing visual hierarchy and feature functionality when fixing bugs.

---

## 🌿 5. GIT BRANCHES & COMMIT LOG

- **Active Branch:** `feature/firebase-dataconnect-sql`
- **Recent Commit History:**
  - `feat: add Firebase Data Connect SDK and manager for Cloud SQL integration (drigo-8b15c)`
  - `662f4f9` - `fix: resolve PlannedDeparture data model and FirebaseRepository compilation errors`
  - `374935b` - `feat: implement dynamic ThemeManager with DataStore, high-contrast themes, driver sheets theme adherence, and Drigo branding`
  - `5c21f7a` - `refactor: consume ThemeManager.themeMode StateFlow directly in MainActivity setContent for instant app-wide theme propagation`
  - `9b8d23e` - `fix: audit and align compose screen color tokens with MaterialTheme colorScheme and user theme preference`
  - `4e82b01` - `fix: dynamic MaterialTheme styling across driver and ride components with high-contrast support`
  - `3a7f82c` - `feat: implement ThemeManager singleton and ThemePreference DataStore with reactive dynamic MaterialTheme`
  - `b1a20f9` - `feat: add custom Drigo brand vector logo and configure adaptive app icon`
  - `7ba4d1b` - `feat: implement RideManager singleton for unified reactive active trip state model`
  - `c5fda74` - `fix: synchronize COMPLETED status real-time update between driver and passenger`
  - `a1eca48` - `feat: implement 4-phase active ride lifecycle flow across driver and passenger views`
  - `1d4f58d` - `fix: resolve potential NPE and race conditions during driver request feed initialization`
  - `e79999a` - `feat: implement inDrive-style passenger request feed and route inspection screen`

- **Git Rules:**
  - **NEVER push directly to `main`**.
  - Create logical feature branches (`feature/...`, `fix/...`).
  - Report status in standard format after each milestone:
    ```text
    BRANCH:
    COMMIT:
    CHANGED:
    FILES:
    TESTED:
    RESULT:
    ISSUES:
    NEXT:
    ```

---

## 📝 6. CHANGE PROTOCOL FOR ALL FUTURE AGENTS

Whenever you build a new feature or modify existing architecture:
1. Read `/AGENTS.md` and `/brainfile.md`.
2. Implement small, targeted changes.
3. Compile and verify with `compile_applet`.
4. Commit to the active feature branch with descriptive messages.
5. Update `/brainfile.md` under Section 5 with the latest commit details and new architectural additions.
