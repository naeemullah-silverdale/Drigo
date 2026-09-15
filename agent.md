# AGENTS.md — Drigo Application Instructions & Brainfile Protocol

> [!IMPORTANT]
> **MANDATORY FOR ALL AGENTS**: At the start of every session or task, you MUST read `/brainfile.md` to understand the project architecture, recent commits, active singletons, git rules, and current state. Update `/brainfile.md` whenever you complete a major change or feature.

---

## 🛑 STEP 1: BEFORE DOING ANYTHING (READ CONTEXT FIRST)
Before inspecting files, writing code, executing commands, or answering technical questions:
1. **ALWAYS inspect `/brainfile.md` and `/AGENTS.md`** using the file viewing tool.
2. Review the existing singletons, state management flows, active branches, and recent changes to prevent:
   - Creating duplicate singletons, repositories, or services.
   - Breaking existing state systems (such as `RideManager`).
   - Overwriting user-selected state or locations.
   - Violating Git branching rules.

---

## ⚡ STEP 2: CORE ARCHITECTURAL RULES YOU MUST RESPECT
1. **Single Source of Truth for Active Rides**:
   - Use `RideManager` (`com.example.data.remote.RideManager`) for observing or modifying active trip states.
   - Both Passenger and Driver views MUST sync via `RideManager.activeTrip` (`active_trips/{tripId}`).
2. **Strict Location Independence**:
   - `fromLocation` (pickup) and `toLocation` (destination) are 100% independent.
   - Selecting or updating `TO` must **NEVER** overwrite `FROM`.
   - Selecting or updating `FROM` must **NEVER** overwrite `TO`.
   - GPS/current location updates must **NEVER** overwrite explicit user-selected locations.
3. **Mass-Market Android Compatibility & Universal Responsive UI**:
   - `minSdk = 23` (Android 6.0).
   - Design primarily for compact budget devices (320dp–360dp available width, 2GB–4GB RAM) while remaining clean on tablets and large screens.
   - Use `dp` for layout dimensions and `sp` for typography. Never hardcode fixed pixel coordinates or static offsets that overlap UI elements.
   - **Zero-Overlap Guarantee**: NEVER allow floating action buttons (e.g. Recenter FAB) to overlap bottom sheet headers, drag handles, or text badges.
   - **Adaptive Layouts**: Use `BoxWithConstraints`, flexible `weight()`, or `FlowRow` for action button rows so passenger names, titles, and fares are never truncated awkwardly or crowded by icon clusters.
   - Support device insets (`WindowInsets`), status bar, gesture navigation, and display cutouts.
4. **Git Branching Discipline**:
   - **NEVER commit or push directly to `main`**.
   - Always work on a clear feature or fix branch (`feature/...` or `fix/...`).

---

## 📝 STEP 3: AFTER COMPLETING A TASK (UPDATE BRAINFILE)
Immediately after implementing a feature, fixing a bug, or adding a new component:
1. **Update `/brainfile.md`** under the appropriate sections:
   - **Newly Created Files & Singletons**: List any new classes or repositories added.
   - **Commit Log & Active Branch**: Record the current branch name, commit hash, and summary message.
   - **Architecture Updates**: Document any state flow or API changes.
2. **Report Final Status** in the standard format:
   ```text
   BRANCH: <branch-name>
   COMMIT: <hash> - <commit-message>
   CHANGED: <summary of changes>
   FILES: <list of modified files>
   TESTED: <build and execution verification>
   RESULT: <outcome summary>
   ISSUES: <none or remaining concerns>
   NEXT: <suggested next step>
   ```

---

## 🚀 CITY-TO-CITY (INTERCITY) MASTER PLAN & BEST PRACTICES
1. **Single Source of Truth in Firebase:**
   - All departures (`/planned_departures`), offers (`/planned_departure_offers`), and bookings (`/planned_departure_bookings`) are persisted and observed via `FirebaseRepository`.
   - Never use fake or hardcoded mock data in UI components.
2. **Details Tab Stability in `ManageDepartureScreen`:**
   - Tab 0 (Details) is the primary anchor. Never auto-switch tabs without explicit user action.
   - Live route visualization, schedule details, capacity meters, quick adjustments, confirmed rider list, and schedule actions.
3. **Strict Settings Theme Adherence:**
   - Use `MaterialTheme.colorScheme` and `MaterialTheme.drigoColors` exclusively.
   - Support dynamic Light and Dark modes seamlessly without hardcoded light hex backgrounds or unreadable text.
4. **Responsive M3 Offers & Manifest:**
   - Responsive card layouts with Accept/Reject/Counter states.
   - Multi-stop optimized manifest with zero overlap on 320dp/360dp budget screens.


