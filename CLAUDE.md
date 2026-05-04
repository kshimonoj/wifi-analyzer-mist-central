# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Permissions
- You can run shell commands without asking for confirmation
- You can modify files under this project
- You can install dependencies if needed
- Do not ask for confirmation unless the action is destructive (delete, overwrite critical files)


## Commands

```bash
# Build
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device/emulator

# Test
./gradlew testDebug              # Run unit tests (JUnit4)
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)

# Clean
./gradlew clean
```

No linting or formatting tools are configured beyond the default Kotlin official style set in `gradle.properties`.

## Architecture

Android app (Kotlin + Jetpack Compose) that scans Wi-Fi BSSIDs in real time and resolves AP names via Juniper Mist and HPE Aruba Central cloud APIs. Targets network engineers doing field Wi-Fi surveys.

**Tech stack**: Kotlin 2.2.21 · Compose + Material3 · Hilt 2.56 · Room 2.7.1 · Ktor 3.1.3 · Kotlin Serialization · Vico 2.3.6 charts · DataStore preferences · Navigation Compose

### Layer structure

```
app/src/main/java/com/example/wifianalyzer/
├── data/
│   ├── wifi/        AndroidWifiScanner + ConnectedApMonitor (WifiManager integration)
│   ├── mist/        MistApiClient + MistRepository (Juniper REST API)
│   ├── aruba/       ArubaApiClient + ArubaRepository (HPE OAuth2 API)
│   ├── oui/         OuiVendorRepository (39K-entry IEEE CSV lookup from assets)
│   ├── db/          Room database — AppDatabase (v5), DAOs, entities
│   └── settings/    DataStore-backed user preferences
├── domain/
│   ├── model/       WifiObservation, BssidSummary, Snapshot
│   └── usecase/     ExportCsvUseCase
├── ui/
│   ├── scan/        ScanScreen + ScanViewModel (main scan tab)
│   ├── monitor/     MonitorScreen + MonitorViewModel (connected AP trends)
│   ├── compare/     CompareGraphScreen (multi-BSSID RSSI chart)
│   ├── snapshot/    SnapshotListScreen + SnapshotViewModel + SaveSnapshotDialog
│   ├── settings/    SettingsScreen + SettingsViewModel (API credentials)
│   ├── theme/       Material3 Color/Theme/Type
│   └── AppNavigation.kt   5-tab bottom nav (Scan, Snapshots, Monitor, Settings + Compare detail)
└── di/              Hilt modules: WifiModule, MistModule, ArubaModule, DatabaseModule, SettingsModule
```

### Key design points

- Single-activity app (`MainActivity`) — all UI is Compose; EdgeToEdge enabled.
- Hilt provides all dependencies; ViewModels are injected via `@HiltViewModel`.
- Room schema is at version 5 with explicit migrations (`v1→v5`) in `AppDatabase.kt`.
- Ktor handles both Mist (Bearer token) and Aruba (OAuth2 client-credentials) HTTP clients.
- `OuiVendorRepository` loads `assets/oui.csv` once into memory at startup for MAC vendor lookups.
- API credentials (Mist org ID/token, Aruba client ID/secret/cluster) are stored in DataStore and set by the user via SettingsScreen — not hardcoded.
- A `.env` file exists locally for developer convenience but must not be committed (it contains real tokens).
