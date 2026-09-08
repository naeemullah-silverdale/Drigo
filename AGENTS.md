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
3. **Mass-Market Android Compatibility**:
   - `minSdk = 23` (Android 6.0).
   - Design primarily for compact budget devices (320dp–360dp available width, 2GB–4GB RAM).
   - Use `dp` for layout dimensions and `sp` for typography. Never hardcode fixed pixel coordinates.
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

