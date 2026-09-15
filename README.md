# 🚗 Drigo — Smart Ridesharing & Mobility Platform

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-7B1FA2.svg?style=flat)](https://m3.material.io/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28.svg?style=flat&logo=firebase)](https://firebase.google.com/)
[![Room Database](https://img.shields.io/badge/Local%20Storage-Room-3DDC84.svg?style=flat)](https://developer.android.com/training/data-storage/room)

**Drigo** is a modern, real-time Android ridesharing and urban mobility application built natively using **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Firebase**, **OpenStreetMap (OsmDroid)**, and **Room Database**. It delivers seamless passenger-driver matchmaking, live routing, transparent fare estimation, real-time telemetry streaming, and in-ride chat communication.

---

## 📱 Key Features

### 1. 🗺️ Interactive OpenStreetMap & Smart Routing
- **Independent Location Selection**: Precise, independent selection for **FROM (Pickup)** and **TO (Destination)** locations with custom interactive map markers.
- **Real-Time Turn-by-Turn Route Engine**: Powered by **OSRM (Open Source Routing Machine)** for realistic road geometries, live distance calculations, and estimated arrival times (ETA).
- **Auto Camera Fitting**: Dynamic bounding box calculations that smoothly adjust the map viewport to encompass pickup, destination, and the complete route geometry.
- **Search & Popular Locations**: Fast location search with quick-access preset hotspots (Universities, Airports, Metro Stations, Commercial Hubs).

### 2. 👥 Dual Mode: Passenger & Driver Switcher
- **Passenger Mode**:
  - Ride category selection: **Share Ride**, **Book Car**, and **Send Parcel**.
  - Dynamic transparent fare breakdown based on route distance and traffic duration.
  - Live driver tracking once a ride is confirmed.
  - Instant direct call and in-ride live messaging.
- **Driver Mode**:
  - One-tap online/offline toggle for receiving rides.
  - Live broadcast of driver GPS coordinates and heading telemetry.
  - Incoming passenger ride requests with real-time pickup & dropoff details.
  - Direct communication interface with passengers.

### 3. 💬 Real-Time In-Ride Chat (Driver ↔ Passenger)
- **Live WebSocket/Firebase Streaming**: Instant message delivery powered by Firebase Realtime Database and Cloud Firestore.
- **Role-Aware Quick Replies**: Pre-configured rapid responses for drivers (e.g., *"I have arrived at pickup 📍"*, *"Stuck in 2 min traffic ⏳"*) and passengers (e.g., *"Coming down in 1 minute 🚶"*, *"Which car color?"*).
- **Trip Summary & Direct Dialing**: Floating chat access from the live map, status indicators, and integrated phone call dialer.
- **Offline Fallback**: Automatic caching and syncing via Room Database (`ChatDao`).

### 4. 🔒 Authentication & Account Management
- **Firebase Authentication**: Email/Password login, Sign Up, and Google Sign-In with Jetpack Credential Manager.
- **Guest / Fast Exploration**: Seamless entry with instant profile personalization.
- **Profile & Preference Customization**: Manage saved locations, favorite payment methods, and notification preferences.

### 5. 💾 Offline-First Local Data Persistence
- **Room Database**: Complete local persistence for ride history, active bookings, recent chats, and user preferences.
- **Reactive UI Flow**: Uses Kotlin StateFlow and Jetpack Compose state primitives for glitch-free UI updates.

---

## 🏗️ Architecture & Technology Stack

```
com.example/
├── data/
│   ├── local/            # Room Database (AppDatabase, DAOs, Converters, SampleDataProvider)
│   ├── model/            # Domain data entities (LocationPoint, RideRequest, ChatMessageEntity, etc.)
│   ├── remote/           # Firebase Repository & Network Services (Realtime DB, Firestore, Auth)
│   ├── LocationHelper.kt # GPS & Telemetry manager
│   └── RouteService.kt   # OSRM Polyline routing and distance engine
├── ui/
│   ├── components/       # Reusable Compose widgets (RealOsmMapView, RideChatSheet, BottomCards)
│   ├── screens/          # Main UI screens (HomeScreen, SignInScreen, SignUpScreen, WelcomeScreen)
│   └── theme/            # Material Design 3 theme, typography, shapes, and Drigo color system
├── viewmodel/            # MainViewModel (State management, intent handlers, business logic)
└── MainActivity.kt       # Application entry point & edge-to-edge Compose container
```

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2+ with Coroutines & StateFlow |
| **UI Framework** | Jetpack Compose with Material 3 (M3) Design System |
| **Architecture** | MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF) |
| **Map & Navigation** | OsmDroid (OpenStreetMap) + OSRM Routing API |
| **Cloud Backend** | Firebase Realtime Database, Cloud Firestore, Firebase Authentication |
| **Local Database** | Room Database (SQLite with KSP codegen) |
| **Image Loading** | Coil Compose |
| **Networking** | OkHttp3 & Retrofit |
| **Testing** | JUnit 4, Robolectric, Roborazzi UI screenshot verification |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or latest version
- **JDK 17** or **JDK 21**
- **Android SDK API 35** (Minimum SDK: API 24)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/naeemullah-silverdale/Drigo.git

# Navigate into the project directory
cd Drigo

# Build debug APK
gradle assembleDebug
```

---

## ⚙️ Configuration & Firebase Setup

1. **Firebase Configuration**:
   - Place your `google-services.json` file inside the `/app` module directory.
   - Ensure Firebase Realtime Database and Cloud Firestore rules are configured according to your authentication requirements.

2. **OpenStreetMap User-Agent**:
   - Drigo automatically initializes the OsmDroid user-agent configuration in `MainActivity.kt` using standard Android application context.

---

## 🧪 Testing

Run standard unit tests and local JVM verification:

```bash
# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Verify UI Screenshot tests (Roborazzi)
gradle :app:verifyRoborazziDebug
```

---

## 📄 License

This project is licensed under the Apache 2.0 License.

