# 🚖 Drigo Admin Panel & Dispatch Control Center (Pakistan Operational Market)

A modern, high-performance real-time web administration dashboard and dispatch management system engineered for the **Drigo** ride-sharing ecosystem in Pakistan (Lahore, Islamabad/Rawalpindi, Karachi, Faisalabad, Multan).

---

## 🇵🇰 Market Localization & Currency
* **Primary Currency**: Pakistani Rupee (`PKR` / `Rs.`)
* **Local Payment Channels**: JazzCash, EasyPaisa, Cash on Delivery, Bank Transfers & Cards
* **Pakistani Vehicle Fleet**:
  * 🏍️ **Bike**: Motorcycle Quick Ride & Delivery
  * 🛺 **Rickshaw**: Traditional 3-Wheeler Auto Rickshaw
  * 🚗 **Mini / AC Car**: Affordable Hatchback & Compact Rides
  * 🚘 **Ride Go**: Standard Sedan (Corolla / City / Yaris)
  * 🚘 **Executive / Comfort**: Premium Sedan & SUV

---

## 🌟 Key Features

### 1. 📍 Live Driver Tracking (`live_driver_locations`)
* **Real-time Map Stream**: Directly subscribes to `live_driver_locations` in Firebase Realtime Database and Firestore.
* **Car Markers on Map**: Displays drivers as custom car markers on Google Maps with real-time speed (`km/h`), heading rotation angle (`deg`), vehicle class badge, and active operational status (`online`, `on_trip`).
* **Pakistan Regional Presets**: Fast testing presets centered on major Pakistani hubs (Liberty Market Gulberg, Allama Iqbal Airport Lahore, Centaurus Mall Islamabad, Emporium Mall Johar Town, Dolmen Mall Clifton Karachi, F-7 Markaz Islamabad).

### 2. 🚖 User Management & Driver KYC Verification
* **Strict Role Separation**: Dedicated management interfaces for **Drivers** and **Passengers**.
* **Unified Account & Verification State Engine**:
  * **Driver Verification Status**: `PENDING` | `APPROVED` | `REJECTED` (tied to CNIC, Driving License, and Vehicle Registration document submission).
  * **Driver Account Operational Status**: `PENDING_REVIEW` | `ACTIVE` | `ONLINE` | `ON_TRIP` | `SUSPENDED` | `FLAGGED`.
  * **Passenger Account Status**: `ACTIVE` | `ON_TRIP` | `SUSPENDED` | `FLAGGED` | `INACTIVE` | `DEACTIVATED`.
* **CNIC & Document Verification Inspector**: Review CNIC front/back, driving licenses, excise registration certificates, and selfie verification with approval or rejection reasons.
* **Non-Destructive Suspension**: Suspending or flagging a user updates operational permissions without wiping verified documents.

### 3. 🗺️ Real-Time Fleet & Route Dispatch Radar
* Live interactive dispatch map showing driver availability and active ride requests.
* Independent **FROM** (Pickup) and **TO** (Drop-off) marker selection with guaranteed state preservation.
* Google Maps route polyline rendering with camera fitting.

### 4. 📊 Real-Time Analytics & Financial Reporting
* Revenue breakdown in PKR across JazzCash, EasyPaisa, Cash, and Card transactions.
* Driver earnings, platform commission metrics, and peak hour ride analytics.

### 5. 🚨 Safety & SOS Emergency Center
* Real-time monitoring of live SOS emergency alerts triggered by passengers or drivers.
* One-touch emergency response team dispatch and trip audit trails.

### 6. 💰 Fare & Surge Pricing Configuration
* Configurable base fares, per-kilometer rates, per-minute rates, and surge multipliers in PKR.

### 7. 🔥 Firebase Realtime Data Synchronization
* Bi-directional synchronization with **Firebase Realtime Database (RTDB)** and **Firestore** (`live_driver_locations`, `users`, `drivers`, `riders`, `trips`, `driver_verifications`).
* Automatic local fallback for offline/demo operation.

---

## 🛠️ Tech Stack

* **Framework**: React 18 with TypeScript
* **Build Tool**: Vite
* **Styling**: Tailwind CSS
* **Icons**: Lucide React
* **Maps**: `@vis.gl/react-google-maps`
* **Database & Auth**: Firebase Realtime Database & Firestore (`firebase/app`, `firebase/database`, `firebase/firestore`)

---

## 🚀 Getting Started

### Prerequisites
* Node.js (v18 or higher recommended)
* npm or yarn

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/naeemullah-silverdale/adminpaneldrigo.git
   cd adminpaneldrigo
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure Environment Variables (Optional):
   Set your Firebase and Google Maps credentials in `.env` or use the in-app **Firebase Config** modal:
   ```env
   VITE_FIREBASE_API_KEY=your_api_key
   VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain
   VITE_FIREBASE_DATABASE_URL=your_database_url
   VITE_FIREBASE_PROJECT_ID=your_project_id
   VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket
   VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
   VITE_FIREBASE_APP_ID=your_app_id
   VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key
   ```

4. Run Development Server:
   ```bash
   npm run dev
   ```

5. Build for Production:
   ```bash
   npm run build
   ```

---

## 📂 Project Structure

```text
src/
├── components/
│   ├── DashboardOverview.tsx    # Executive summary & quick PKR metrics
│   ├── FleetDispatchMap.tsx     # Live map & live_driver_locations car markers
│   ├── UserManagement.tsx       # Driver/Passenger table, CNIC verification & wallet controls
│   ├── RealtimeAnalytics.tsx    # JazzCash/EasyPaisa revenue charts & operational insights
│   ├── SafetySOSCenter.tsx      # Emergency alert monitoring
│   ├── PricingControls.tsx      # PKR Fare configuration & surge rules
│   ├── Header.tsx               # Top navigation & system status
│   └── Sidebar.tsx              # Application navigation
├── firebase.ts                  # Firebase RTDB & Firestore integration layer
├── mockData.ts                  # Local fallback data for Pakistan market
├── types.ts                     # TypeScript interfaces (LiveDriverLocation, Driver, etc.)
└── App.tsx                      # Root component & real-time subscription router
```

---

## 📄 License

This project is proprietary software for the Drigo Ride-Sharing Platform (Pakistan). All rights reserved.

