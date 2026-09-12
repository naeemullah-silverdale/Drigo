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
  - Driver & Passenger order queries (`listenToPassengerOrders`, `listenToDriverActiveTrip`)
  - Live location streaming (`updateDriverLocation`, `observeLiveDriverLocation`)
  - Rating system (`saveRideRating`, `hasUserRatedRide`)
  - Safety reports (`submitSafetyReport`)

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

- **Active Branch:** `fix/driver-feed-radar-and-map-inspection`
- **Recent Commit History:**
  - `fix: implement full-screen animated radar search view for online empty state, 3-marker map preview with A and B badges for ride request inspection, and inDrive requests feed`
  - `fix: guarantee COMPLETED status updates across active_trips, ride_requests, passenger_orders, users active_ride_request and active_driver_trip when driver completes ride`
  - `fix: enforce authoritative ride status synchronization across all 5 active records (/active_trips, /ride_requests, /passenger_orders, /users/{passengerId}/active_ride_request, /users/{driverId}/active_driver_trip), RideManager, and driver_trip_history across all lifecycle states`
  - `fix: update ride status to COMPLETED across active_trips, ride_requests, passenger_orders, and users active_ride_request upon driver complete ride without premature node deletion`
  - `fix: synchronize all driver actions (accept, arrive, start, complete, cancel) immediately to Firebase RTDB, active_trips, passenger_orders, RideManager, and Firestore without network blocking lag`
  - `fix: synchronize passenger screen with driver ride acceptance, transition from searching state to active captain assigned card, and preserve driver details across observers`
  - `fix: simplify and synchronize passenger-driver active ride flow with /ride_requests/{requestId} as single authoritative source of truth, remove premature simulation overrides, and prevent spurious ride clearances`
  - `fix: resolve KSP 2.3.5 ApplicationManager.getApplication NullPointerException by pinning googleDevtoolsKsp to stable 2.3.4 and pruning unused moshi codegen processor`
  - `fix: immediately clear passenger active ride state and prompt review when driver completes ride via multi-channel synchronization (RideManager, RTDB, Firestore, live driver location)`
  - `feat: implement City to City passenger scheduled departures screen with interactive city/date filters, seat and full-car negotiation, driver profiles, and direct contact`
  - `fix: ensure clean driver active trip clearance by both ID and phone, accurately persist net earnings in driver trip history, and purge ghost sessions`
  - `fix: complete City to City passenger booking flow with Firebase persistence, real-time fare offers, theme-adaptive styling, and 320dp responsive layouts`
  - `fix: optimize ManageDepartureScreen responsiveness for all screen sizes (320dp-411dp+) with flexible row weights, constrained text truncation, and touch-friendly controls`
  - `feat: integrate ManageDepartureScreen for active scheduled departures with real-time Firebase passenger offers, schedule editing, and layout padding`
  - `fix: refactor City to City planned departures UI with fully responsive layouts, compact device scaling, and trip management`
  - `feat: add City to City entry in driver navigation drawer and connect scheduled ride posting and departures list`
  - `feat: implement City to City scheduled departures posting for drivers and seat/full-car booking flow for passengers`
  - `feat: implement unified Trip History view for drivers and passengers with Firestore streams, receipts, and ratings`
  - `fix: synchronize trip status across RideManager and listenToPassengerOrders to immediately clear passenger trip in progress and adapt active sheet theme to user settings`
  - `fix: ensure active ride bottom sheet UI adapts dynamically to user selected light or dark theme across passenger and driver views`
  - `fix: eliminate state lag on passenger screen and immediately show post-trip feedback review on driver ride completion`
  - `fix: synchronize ride completion and instant passenger review prompt between driver and passenger`
  - `fix: ensure InDriveFixedBottomBar adapts dynamically to theme and restore passenger booking flow`
  - `fix: make active ride bottom sheet, safety dialogs, and rating components respect dynamic light/dark MaterialTheme`
  - `9cbf2d3` - `fix: polish driver mode top bar, category filters, and remove duplicate map FAB`
  - `d14b144` - `feat: replace driver ride request bottom sheet with full-screen inDrive style feed list`
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

## 🛠️ 7. PENDING FEATURE / FIX SPECIFICATION: POST-RIDE RATING & REVIEW SKIP PERSISTENCE

### 🔍 Problem Diagnosis
- When a ride is completed, `PostRideRatingDialog` appears on screen.
- When the user taps **"Skip for Now"** (or dismisses the dialog), the skip action is recorded only in a local state / partial key, but:
  1. Multiple background flow observers in `HomeScreen.kt` (lines 414, 606, 712, 824, 1055) check only `repo.hasUserRatedRide(safeId, "PASSENGER")`.
  2. Because skipping a ride does not submit a rating to the database, `repo.hasUserRatedRide` evaluates to `false`.
  3. Consequently, whenever any active order state, Firebase listener (`listenToPassengerOrders`), or `RideManager` emits an update, the rating dialog is re-triggered and pops up again repeatedly.
  4. In `PostRideRatingAndSafety.kt`, dismissing or skipping does not uniformly persist the skipped status into the persistent `drigo_ratings` SharedPreferences and database cache across all ride ID variants (`id` and `requestId`).

### 📋 Planned Solution & Action Items
1. **Unified Skip & Rating Verification in `FirebaseRepository`**:
   - Add `markRideRatedOrSkipped(rideId: String, requestId: String = "", raterRole: String = "PASSENGER")` to persist skip status to `SharedPreferences("drigo_ratings")` and memory cache.
   - Add `isRideRatedOrSkipped(rideId: String, requestId: String = "", raterRole: String = "PASSENGER"): Boolean` which checks:
     - Local memory set of rated/skipped IDs
     - `SharedPreferences("drigo_ratings")` for `rated_or_skipped_<id>` and `rated_or_skipped_<requestId>`
     - Local SQLite database `safetyDao().hasRatedRide`
     - Remote Firebase `ride_ratings` collection/node
2. **Standardize All Rating Dialog Triggers in `HomeScreen.kt`**:
   - Update all 6 auto-prompt triggers (active order observer, `RideManager` state listener, cloud order stream listener, request status listener, recent order check) to strictly call `!repo.isRideRatedOrSkipped(orderId, requestId, "PASSENGER")` before setting `showPassengerRatingDialog = true`.
3. **Ensure Instant & Permanent Persistence on Skip in `PostRideRatingDialog`**:
   - In `PostRideRatingDialog` (`PostRideRatingAndSafety.kt`), update `onDismiss` / `Skip for Now` button click to immediately call `repo.markRideRatedOrSkipped(rideId, targetId, ...)` and persist `rated_or_skipped_<rideId>` to SharedPreferences before closing the modal.
## 8. DRIVER FEEDBACK DIALOG FIX SPECIFICATION (RATING PASSENGER VS RATING DRIVER)

### 🛑 Problem Diagnosis & Issue Verification
- **Issue Confirmed**: Yes, this issue was verified in the codebase.
- **Root Causes**:
  1. **`TripHistoryScreen.kt`**: In `TripHistoryScreen.kt` (lines 397–410), the rating dialog invocation is hardcoded with `isDriver = false`, `targetId = order.assignedDriverId`, and `targetName = order.driverName`. When a driver opens Trip History and taps "Rate" on a completed trip, it erroneously asks the driver to rate the driver captain (themselves).
  2. **Role Distinction in `DriverModeView.kt` & `PostRideRatingAndSafety.kt`**:
     - When the driver clicks "COMPLETE RIDE", the dialog must explicitly and prominently ask the driver to **"Rate Passenger: <Passenger Name>"**.
     - It must show the passenger avatar, passenger name, pickup/destination, and passenger rating.
     - It must offer passenger-focused feedback tags (e.g., "Polite Passenger", "On Time at Pickup", "Respectful", "Smooth Trip", or "Late to Pickup", "Rude Behavior", etc.).
     - It must NEVER show vehicle details, driver plate number, or driver tip options to the driver.
  3. **Passenger Name Fallback Resolution**: If `passengerName` is blank in `PassengerOrder` or `active_trips`, resolve it properly from `passengerEmail` or fallback to `"Passenger"`, ensuring it never falls back to the driver's own name or profile.

## 9. PASSENGER TRIP HISTORY DRIVER TAB VISIBILITY FIX SPECIFICATION

### 🛑 Problem Diagnosis & Issue Verification
- **Issue Confirmed**: Yes, verified in `TripHistoryScreen.kt` (lines 152–229).
- **Root Cause**:
  - In `TripHistoryScreen.kt`, the segmented role selector tab bar (`Surface` with `"Passenger Rides"` and `"Driver Trips"`) is rendered unconditionally for all users.
  - Even when a user is purely a passenger and has never registered, submitted verification documents, or been approved as a driver, the `"Driver Trips"` tab button is currently displayed and clickable.
  - When non-driver passengers open Trip History, showing a "Driver Trips" tab is confusing and violates the requirement that driver features/tabs must be hidden for unregistered users.

### Implementation Summary (Completed)
- **`TripHistoryScreen.kt`**:
  - Added realtime `driverVerification` flow observation from Firebase.
  - Computed `isRegisteredDriver` based on verification status, driver confirmation, driver trips presence, and initial mode.
  - Added safety `LaunchedEffect(isRegisteredDriver)` ensuring `selectedHistoryTab` automatically falls back to `TripHistoryTab.PASSENGER` if the user is not a registered driver.
  - Wrapped the segmented role selector tab bar inside `if (isRegisteredDriver) { ... }`.
  - Non-driver passengers now see only their clean Passenger Rides history without the Driver Trips tab or toggle.
  - Verified and compiled successfully with 0 errors.

---

## 10. HARDCODED DATA AUDIT & FIX SPECIFICATION FOR TRIP HISTORY TABS

### 🛑 Problem Diagnosis & Issue Verification
- **Issue Confirmed**: Yes, there are several hardcoded and static fallback values in the Trip History tab implementations across `TripHistoryScreen.kt`, `DriverTripHistoryScreen.kt`, `DriverModeView.kt`, and `FirebaseRepository.kt`.

### 🔍 Identified Hardcoded Data Points:
1. **Passenger Receipt Fare Breakdown (`PassengerReceiptDialog` in `TripHistoryScreen.kt`)**:
   - Lines 1340–1342: Hardcoded arbitrary percentage splits:
     - `val baseFare = (order.agreedFare * 0.40).toInt()` (40% hardcoded)
     - `val distanceFare = (order.agreedFare * 0.45).toInt()` (45% hardcoded)
     - `val taxesAndSurge = order.agreedFare - baseFare - distanceFare` (15% hardcoded)
   - Should derive breakdown dynamically from actual order tariff metadata or compute accurately using base fare + (distanceKm * perKmRate) based on category.

2. **Passenger Trip Card Hardcoded Fallbacks (`PassengerTripCard` in `TripHistoryScreen.kt`)**:
   - Line 707: Fallback ride category hardcoded to `"Ride A/C"` instead of dynamic `order.rideCategory.ifBlank { "Ride" }`.
   - Line 760: Hardcoded `"Cash"` payment method fallback.

3. **Driver Earnings & Commission Calculations (`TripHistoryScreen.kt` & `DriverTripHistoryScreen.kt`)**:
   - Lines 937, 1087, 1485, 1556: Platform commission rate is hardcoded to `10%` (`* 0.90` and `* 0.10`) across net earning formulas and text labels (`"After 10% platform commission"`, `"Drigo Platform Fee (10%)"`).
   - Line 1122: Fallback category hardcoded to `"Ride"`.

4. **Driver Trip Item Creation & Parsing Fallbacks (`DriverModeView.kt` & `FirebaseRepository.kt`)**:
   - `DriverModeView.kt` (lines 2560, 2568, 2583, 2587):
     - `passengerRating = 4.9` (hardcoded 4.9 rating instead of dynamic rating).
     - `calcDist` fallback is hardcoded to `8.5` km.
     - `durationMins` fallback is hardcoded to `18` mins.
     - `category` fallback is hardcoded to `"Ride Mini"`.
   - `DriverModeView.kt` (lines 4646, 4659, 4661, 4665):
     - `passengerRating = 4.9`, `distanceKm = 5.0`, `durationMins = 15`, `category = "Ride Mini"`.
   - `FirebaseRepository.kt` (lines 4473–4476, 5121):
     - Hardcoded fallbacks: `distanceKm = dist ?: 5.0`, `durationMins = dur ?: 15`, `passengerRating = 4.9`.

### 📋 Implementation Summary (Completed)
1. **Dynamic Receipt Fare Breakdown**:
   - `PassengerReceiptDialog` in `TripHistoryScreen.kt` calculates the fare breakdown dynamically from the order's actual ride category base fare and distance traveled instead of arbitrary 40/45/15 percentage splits.
   - `DriverTripReceiptDialog` in `TripHistoryScreen.kt` now reads the real `platformFeePkr` and `netEarningsPkr` directly from the trip object with graceful calculations.
2. **Eliminated Hardcoded Fallbacks in `FirebaseRepository.kt`**:
   - `observeDriverTripHistory` now parses actual `passengerRating` (with fallback to `userRating`), real `distanceKm`, real `durationMins`/`durationMinutes`, and actual `category`/`rideType`.
   - Removed arbitrary fallback values like `4.9`, `8.5` km, and `18` mins from `ordersListener` and `firestore` queries.
3. **Eliminated Hardcoded Data in `DriverModeView.kt`**:
   - `newHistoryEntry` and `cancelledHistoryEntry` now compute distance from coordinates using `calculateDistanceKm`, read real `passengerRating`, real duration, and actual `rideCategory`.
4. **Data Models Updated (`Models.kt`)**:
   - Added `passengerRating: Double = 5.0` to `PassengerOrder` data class to preserve passenger rating metadata seamlessly across order streams.
5. **Verified & Compiled**:
   - Successfully compiled with 0 errors via Gradle.

---

## 11. TRIP HISTORY UI POLISH (COMPLETED)

### 🎨 Key Enhancements
1. **Header & Navigation Bar**:
   - Status bar padding integrated with elevation and border.
   - Trip count indicator subtitle ("X rides recorded" / "X trips recorded").
   - Animated pill segmented tab switcher for Driver / Passenger modes with count badges.
2. **Search & Filter Controls**:
   - High-contrast search input with clear button and interactive icon tint.
   - Status filter chips ("All Trips", "Completed", "Cancelled") with custom colored icon badges and selected stroke highlights.
3. **Passenger Analytics Hero Banner**:
   - Brand gradient card (Magenta -> Purple -> Dark Purple) displaying:
     - Total Rides Taken
     - Total Distance Traveled (km)
     - Total Amount Spent (PKR) formatted with standard thousands separators.
4. **Trip Cards Design (Passenger & Driver)**:
   - Elevated cards with category badges (Car, Bike, Rickshaw icons).
   - Route timeline connectors with colored dot markers (Purple for pickup, Green for destination).
   - Driver & passenger profile chips with rating star indicators and vehicle make/model pill.
   - Fare badge with lime/green highlight.
   - Direct action buttons: **View Receipt**, **Rate Driver**, **Report Issue / SOS**, and **Book Again**.
5. **Digital Receipts & Empty States**:
   - Polished modal dialogs for passenger fare receipts and driver gross/net breakdowns.
   - One-tap "Copy Receipt Details" action with clipboard feedback.
   - Context-aware empty state illustrations with action guidance.

---

## 12. DRIVER MODE RIDE REQUESTS PULL-TO-REFRESH SYNC (COMPLETED)

### 🔄 Summary of Implementation
1. **On-Demand Firebase Realtime Database Query (`RideRequestRepository.kt`)**:
   - Added `suspend fun fetchRideRequestsOnce(): List<RideRequest>` which queries `ride_requests` node directly via `addListenerForSingleValueEvent` with timeout fallback.
   - Parses and filters only valid, active, eligible passenger ride requests, sorting descending by timestamp.
2. **ViewModel Sync Method (`MainViewModel.kt`)**:
   - Added `suspend fun refreshDriverRideRequests(): List<RideRequest>` to trigger on-demand snapshot query and update `_liveRideRequests` StateFlow.
   - Also syncs driver user profile and verification records from Firebase.
3. **Pull-to-Refresh Integration (`DriverModeView.kt`)**:
   - Integrated Material 3 `PullToRefreshBox` wrapping the passenger ride requests feed and empty states.
   - Added `isRequestsRefreshing` state with smooth pull gesture handling.
   - Made empty-state Box vertically scrollable (`Modifier.verticalScroll(rememberScrollState())`) to ensure swipe-down gesture works even when list is empty.
   - Added high-visibility **Sync Status Header Bar** displaying real-time sync indicator ("Pull down to refresh" / "Syncing with Firebase...") and a manual **Sync** shortcut button for drivers with dashboard mounts.
4. **State Pipeline Wiring (`HomeScreen.kt` & `MainActivity.kt`)**:
   - Passed `onRefreshDriverRideRequests` lambda through `MainActivity` -> `HomeScreen` -> `DriverModeView`.
5. **Verified**:
   - Full project compiled successfully via `compile_applet`.

---

## 13. DIAGNOSTIC & FIX SPECIFICATION: DRIVER SHOWN PASSENGER REVIEW SCREEN ON RIDE COMPLETION

### 🔍 Root Cause Verification (CONFIRMED)
The issue **definitely exists** in the codebase. Here is the exact chain of events causing it:
1. **Shared Parent Component (`HomeScreen.kt`)**:
   - `HomeScreen.kt` acts as the master container for both Passenger UI and Driver UI (`if (userMode == UserMode.DRIVER) DriverModeView(...) else ...`).
   - At line 583, `HomeScreen.kt` has a global `LaunchedEffect(Unit)` observing `RideManager.activeTrip`.
   - When a ride is completed (by the driver or remote update), `RideManager.activeTrip` emits `PassengerOrderStatus.COMPLETED`.
   - In `HomeScreen.kt` lines 616–636, this observer sets:
     ```kotlin
     completedOrderForRating = fullOrder
     showPassengerRatingDialog = true
     ```
     **Crucially, it does NOT check `if (userMode == UserMode.PASSENGER)`!**
2. **Top-Level Passenger Review Dialog Rendered Over Driver UI (`HomeScreen.kt`)**:
   - At lines 3712–3748 in `HomeScreen.kt`, `PostRideRatingDialog` is rendered at the root level of `HomeScreen`:
     ```kotlin
     if (showPassengerRatingDialog && completedOrderForRating != null) {
         PostRideRatingDialog(
             rideId = safeId,
             currentUserId = user?.uid ?: "passenger_user",
             currentUserName = user?.displayName ?: "Passenger",
             isDriver = false, // <--- HARDCODED PASSENGER RATING DRIVER
             targetId = order.driverId.ifBlank { "driver_captain" },
             targetName = order.driverName.ifBlank { "Driver Captain" },
             ...
         )
     }
     ```
   - Because `showPassengerRatingDialog` is true and `HomeScreen` does not gate this by `userMode == UserMode.PASSENGER`, it pops up the Passenger rating dialog (`isDriver = false`, targeting Driver Captain, showing vehicle summary and tip buttons) on top of the entire screen, even when the user is in DRIVER mode!
3. **Driver's Correct Dialog is Blocked/Overlaid**:
   - `DriverModeView.kt` already contains the correct driver rating dialog at line 4621:
     `PostRideRatingDialog(..., isDriver = true, targetId = trip.passengerId, targetName = trip.passengerName)`
   - But because `HomeScreen.kt`'s dialog is rendered at the top level with `isDriver = false`, the driver is presented with the prompt to rate the driver captain instead of the passenger.

### 🛠️ Fix Applied & Verified (September 12, 2026)
1. **In `HomeScreen.kt`**:
   - Gated root-level dialog rendering to `userMode == UserMode.PASSENGER`:
     `if (userMode == UserMode.PASSENGER && showPassengerRatingDialog && completedOrderForRating != null)`
   - Gated all 5 completion listeners / triggers to `userMode == UserMode.PASSENGER`:
     - Line 620: `RideManager.activeTrip` collector completion block.
     - Line 727: `passengerOrders` completed list check.
     - Line 837: Cloud orders listener completion check.
     - Line 901: `completedOrder` non-null check.
     - Line 1069: Direct ride request completed update handler.
   - Added mode-switch reset:
     ```kotlin
     LaunchedEffect(userMode) {
         if (userMode == UserMode.DRIVER) {
             showPassengerRatingDialog = false
             completedOrderForRating = null
         }
     }
     ```
2. **In `DriverModeView.kt`**:
   - Updated `PostRideRatingDialog` parameters at line 4624 to pass `targetPhone = trip.passengerPhone` and verified all passenger fields (`targetId = trip.passengerId`, `targetName = trip.passengerName`, `isDriver = true`) display properly for driver rating of the passenger.
3. **Compilation Verification**:
   - Verified successfully with `compile_applet`.

---

## 14. TRIP HISTORY REAL FIREBASE DATA VERIFICATION & FIX PLAN (SEPTEMBER 12, 2026)

### 🔍 Verification Finding: YES, Hardcoded Mock Data Exists and Masquerades as User History!
Upon inspecting the codebase, **YES, this issue definitely exists**:

1. **Hardcoded Fallback Seeds in `FirebaseRepository.listenToPassengerOrders` (lines 1329–1435)**:
   - In `FirebaseRepository.kt`:
     ```kotlin
     // Seed default historical past trips for the account so history is never blank
     val defaultOrders = listOf(
         PassengerOrder(
             id = "order_past_1",
             pickupTitle = "Zero Point, Islamabad",
             destinationTitle = "Thokar Niaz Baig, Lahore",
             driverName = "Captain Farhan",
             agreedFare = 1800,
             status = PassengerOrderStatus.COMPLETED,
             ...
         ),
         PassengerOrder(
             id = "order_past_2",
             pickupTitle = "Blue Area, Stock Exchange",
             destinationTitle = "F-10 Markaz, Islamabad",
             driverName = "Captain Tariq",
             agreedFare = 420,
             status = PassengerOrderStatus.COMPLETED,
             ...
         ),
         PassengerOrder(
             id = "order_past_3",
             pickupTitle = "University Town, Peshawar",
             destinationTitle = "Saddar Cantt, Peshawar",
             driverName = "Captain Zeeshan",
             agreedFare = 380,
             status = PassengerOrderStatus.COMPLETED,
             ...
         )
     )
     ```
   - At line 1435:
     ```kotlin
     defaultOrders.forEach { historyOrdersMap[it.id] = it }
     ```
   - When any user opens "Trip History" or "Past Trips" (in `TripHistoryScreen` or `MyOrdersScreen`), these 3 fake Islamabad/Lahore/Peshawar trips are hardcoded into `historyOrdersMap` and **always emitted** to the UI, even if the user never took these rides!

2. **Real Firebase Queries Miss Finished Active Rides & User Ride Nodes**:
   - In `listenToPassengerOrders()`:
     - It listens to `users/{userId}/ride_history` and Firestore `ride_requests`.
     - However, when rides are marked COMPLETED or CANCELLED in Drigo:
       - They are saved to `active_trips/{tripId}`, `passenger_orders/{orderId}`, `driver_trip_history/{driverId}/{tripId}`, and `users/{userId}/ride_history`.
       - If a user hasn't had `users/{userId}/ride_history` populated, but has completed rides in `active_trips` (where `passengerId == userId` or `passengerEmail == userEmail`) or `passenger_orders` (where `passengerId == userId`), those real completed orders are not queried by `listenToPassengerOrders()`.
     - Furthermore, because `defaultOrders` is pre-populated unconditionally, the screen never shows an empty state or purely real data—it always dumps those 3 hardcoded rides.

3. **Driver Mode Trip History (`DriverTripHistoryScreen` / `TripHistoryScreen`)**:
   - For drivers, `repo.observeDriverTripHistory(driverId, driverPhone)` queries `driver_trip_history/{driverId}`, `users/{driverId}/driver_trip_history`, `passenger_orders` where `assignedDriverId == driverId`, and Firestore `ride_requests`.
   - However, if `driverId` is passed as blank or fallback string, or if the driver views `TripHistoryScreen` when logged in with a phone/UID, we need to ensure both passenger and driver tabs pull exclusively authentic data for that user.

### 🛠️ Implemented Fix (Completed)
1. **Eliminated Hardcoded Mock Orders in `FirebaseRepository.kt`**:
   - Completely deleted `defaultOrders` (the fake `order_past_1`, `order_past_2`, `order_past_3` mock trips) from `listenToPassengerOrders()`.
   - Initialized `historyOrdersMap` as an empty map: `val historyOrdersMap = mutableMapOf<String, PassengerOrder>()`.
2. **Expanded Real Data Sources in `listenToPassengerOrders()`**:
   - Query Realtime Database nodes for the authenticated user:
     - `users/{safeUserId}/ride_history`
     - `passenger_orders` (filtered by `passengerId == safeUserId` or `passengerEmail == safeEmail`)
     - `active_trips` (filtered by `passengerId == safeUserId` or `passengerEmail == safeEmail` with status `COMPLETED` or `CANCELLED`)
   - Firestore `ride_requests` collection with real matching `passengerId` or `passengerEmail`.
3. **Clean Empty State Verified**:
   - For users with no prior ride history in Firebase, the UI cleanly renders `EmptyTripHistoryView` ("No rides yet") instead of displaying mock Islamabad/Lahore/Peshawar trips.
4. **Expanded Driver Trip History in `observeDriverTripHistory()`**:
   - Added listener for `active_trips` matching driver UID or phone.
   - Merged `historyFromActiveTrips` with `driver_trip_history`, `passenger_orders`, and Firestore requests. All mock data eliminated.

---

## 🚗 DRIVER MODE (ONLINE STATE) UI/UX AUDIT & PLANNED FIX (INDRIVE ARCHITECTURE)

### 🔍 Verification Against Codebase & Official inDrive Attachments

1. **Screen 1 — Searching for Requests (Empty State Radar)**:
   - **Current Code Status (Verified in `DriverModeView.kt:3010-3050`)**:
     - When online and `filteredRequests.isEmpty()`, it displays a flat white void with a static 56dp purple circle and `"Searching for passenger requests..."`.
   - **Required inDrive Architecture (Attachment 1)**:
     - Full-screen dark theme canvas (`#13151D` / `#0E1015`).
     - Animated full-screen rotating radar scanner:
       - Concentric green circular grid rings with a central anchor dot.
       - Continuously rotating translucent lime/neon green sweep beam (`#CCFF00` cone).
       - Pulsating blip dots scattered across radar rings.
       - Clean centered bold typography: **"Hold on, orders will appear here soon..."**.
     - Top App Bar: Hamburger menu, smooth animated pill toggle (`#CCFF00` neon green active thumb with black `"Online"` text on dark track), settings gear with notification indicator.
     - Bottom Navigation Bar: Docked tabs with `"Ride requests"` (list icon) and `"Performance"` (grid icon).

2. **Screen 2 — Passenger Requests Activity / Feed (ListView)**:
   - **Current Code Status (Verified in `DriverModeView.kt:3075-3090`)**:
     - Displays requests using generic cards with category chips crowding the top bar.
   - **Required inDrive Architecture (Attachment 2)**:
     - Pure requests ListView screen with dark mode styling matching inDrive.
     - Each request item card structured with:
       - Left column: Circular avatar with initial (e.g. purple `"j"`), passenger name (`"jawad"`), star rating (`"★ 4.73"`), review count (`"(44)"`), pickup ETA away (`"8 min."`).
       - Top right: Distance indicator (`"~8,4 km"`).
       - Fare: Bold prominent fare display (`"PKR371"`) with `"(^) Fair price"` badge.
       - Address points: Pickup location text and Destination location text.
       - Category badge: Light blue pill (`"Mini"`).
       - 3-dots overflow menu.
     - Bottom bar remains docked (`"Ride requests"` | `"Performance"`).

3. **Screen 3 — Request Clicked: Draggable Bottom Sheet + Map with Driver, A, B Markers**:
   - **Current Code Status (Verified in `DriverModeView.kt:1085-1100, 3435-3650` & `RealOsmMapView.kt:235-340`)**:
     - Opening a request currently uses `ModalBottomSheet` with a dark `0.6f` scrim that blinds and darkens the map.
     - `RealOsmMapView` only renders a red car (instead of the neon green car badge) and generic pins (instead of circular "A" and "B" badges).
     - Route calculation in line 1090 ONLY calculates Pickup -> Destination, completely omitting the route from Driver -> Pickup!
   - **Required inDrive Architecture (Attachment 3)**:
     - Background map is crisp and fully visible (no blocking modal scrim).
     - Top bar displays clean `"Ride request"` header with back/close.
     - Map displays the 3 verified markers:
       1. **Driver Location Marker**: Neon lime green circular badge with black car glyph (`#CCFF00` circle with `#13151D` car).
       2. **Marker A (Pickup Point)**: Royal blue circular badge with bold white letter **"A"** (`#2979FF`).
       3. **Marker B (Destination Point)**: Vibrant green circular badge with bold white letter **"B"** (`#00E676`).
     - Map Routes:
       - Route Segment 1: Driver Current Location → Pickup Point A (with floating ETA/distance pill: e.g. `"7 min \n 2,6 km"`).
       - Route Segment 2: Pickup Point A → Destination Point B (with floating ETA/distance pill: e.g. `"12 min \n 8,9 km"`).
     - Map Zoom Controls: Quick floating `+` and `-` buttons.
     - Draggable Bottom Sheet docked over map:
       - Passenger profile avatar, name, rating, arrival duration, distance, fare, and Fair price badge.
       - Route steps: Blue "A" icon for Pickup, Green "B" icon for Destination.
       - Category pill (`"Mini"`).
       - Three stacked action buttons:
         1. **Accept for PKR...** (Primary neon lime green `#CCFF00` with dark bold text).
         2. **Offer your fare** (Dark secondary button with counter-offer expansion).
         3. **Close** (Dark dismiss button returning to requests ListView).

---

### ✅ Completed Implementation & Theme Adherence:
- `app/src/main/java/com/example/ui/screens/DriverModeView.kt`:
  - Fixed ride inspection route pill overlays to use dynamic `MaterialTheme.colorScheme` tokens (`primaryContainer`, `onPrimaryContainer`, `secondaryContainer`, `onSecondaryContainer`).
  - Fixed dialog containers, text colors, radio buttons, and category vehicle selectors to strictly adhere to app theme settings configured via `ThemeManager`.
  - Refactored request inspection bottom sheet to use transparent scrim so the map with route and markers remains 100% visible and interactive.
  - Action buttons (`Accept`, `Offer your fare`, `Close`) updated to dynamically adapt to light/dark themes with high-contrast accessibility.
  - Maintained complete independence between pickup and destination coordinates.
- `app/src/main/java/com/example/ui/components/RealOsmMapView.kt`:
  - Dynamically updates dark tile color filters and map rendering in accordance with the user's active theme preference (`LIGHT` / `DARK` / `SYSTEM`).
  - Correctly renders driver car, pickup (A), and destination (B) pins.

---

### 📋 Verified Issue Analysis & Planned Fix (Awaiting User Command "apply the fix"):
- **Issue Status**: **VERIFIED** — Confirmed that the current codebase needs the following enhancements to match inDrive (Attachments 1, 2, and 3):
  1. **Attachment 1 — Full-screen Radar View**:
     - Animated radar search canvas (concentric circles, rotating sweep beam, pulsing green blips, `"Hold on, orders will appear here soon..."`) when driver goes online with no active requests.
  2. **Attachment 2 — Ride Requests Feed (ListView)**:
     - ListView showing passenger requests with fare, distance, pickup, destination, and category badge.
  3. **Attachment 3 — 3-Marker Map & Inspection View**:
     - **Driver Location Marker**: Green circular badge with car glyph (`#CCFF00`).
     - **Pickup Marker A**: Blue circular badge with letter **"A"** (`#2979FF`).
     - **Destination Marker B**: Green circular badge with letter **"B"** (`#00E676`).
     - **Dual Polylines**:
       - Leg 1: Driver → Pickup A (with floating badge e.g. `"7 min • 2,6 km"`).
       - Leg 2: Pickup A → Destination B (with floating badge e.g. `"12 min • 8,9 km"`).
     - **Camera Framing**: Auto-fits bounding box for Driver, A, and B.
     - **Theme Integration**: Respects the active theme settings configured in `ThemeManager`.










