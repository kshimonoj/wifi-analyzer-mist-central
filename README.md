# Wi-Fi Analyzer with Mist & Aruba Central Integration

An Android Wi-Fi analyzer app for network engineers, featuring real-time BSSID scanning,
AP name resolution via Juniper Mist and HPE Aruba Central APIs, and snapshot-based field survey tools.

## Features

### Wi-Fi Scanning
- Real-time BSSID scanning with RSSI, Channel, Channel Width, Security, Band
- Vendor lookup via IEEE OUI database (39,000+ entries)
- Filter by SSID, Band (2.4/5/6 GHz), Security type
- Sort by RSSI, SSID, or Channel
- Auto-scan with configurable interval (5s / 10s / 30s)
- OS throttle detection with cached result display

### AP Name Resolution
- **Juniper Mist**: Resolves AP names via Mist API using radio MAC prefix matching
  - Supports Org-wide or Site-specific sync
  - Cloud region selection (Global 01-04, EMEA 01-03, APAC 01)
- **HPE Aruba Central**: Resolves AP names via BSSID exact match
  - Supports Site filtering using OData filter syntax
  - Multi-cluster support (Internal, US, EU, APAC, Canada, UAE, China)
  - OAuth2 token auto-refresh

### Monitor Screen
- Real-time graphs for connected AP: RSSI, TX/RX Speed, Channel
- Auto-updates every second

### RSSI Comparison
- Long-press to select multiple BSSIDs
- Multi-series RSSI trend graph with color-coded legend

### Snapshot & Export
- Save scan results as named snapshots with location/floor metadata
- CSV export with full BSSID details including AP names
- Share via Android Share Sheet

## Requirements

- Android 10 (API 29) or higher
- Location permission (required for Wi-Fi scanning)

## Tested Devices

| Device | Android Version | Notes |
|--------|----------------|-------|
| Pixel 9 | Android 16 | Primary test device |

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM
- **DI**: Hilt
- **Database**: Room
- **HTTP**: Ktor Client
- **Charts**: Vico
- **Async**: Coroutines + Flow
- **Settings**: DataStore Preferences

## Setup

### Mist API
1. Open Settings screen in the app
2. Select Cloud Region
3. Enter your Mist API Token
4. Tap "Test Connection"
5. Select Org and Site
6. Tap "Sync APs"

### Aruba Central API
1. Open Settings screen in the app
2. Select Cluster
3. Enter Client ID and Client Secret (from HPE GreenLake)
4. Tap "Test Connection"
5. Select Site (optional)
6. Tap "Sync APs"

## Repository Structure

This repository contains two components:

- Root directory + `app/` — Android Wi-Fi Analyzer app
- `analyzer/` — Web-based survey analyzer (Streamlit + Docker)

The Android project files (build.gradle.kts, settings.gradle.kts, etc.)
live in the root. The analyzer is a standalone tool in its own subdirectory.

```
analyzer/
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
└── app/
    ├── app.py          # Streamlit main app
    ├── analysis.py     # Analysis logic
    └── map_plot.py     # Floor map visualization
```

## Architecture

```
app/
├── data/
│   ├── wifi/          # WifiScanner, ScanResultMapper, ConnectedApMonitor
│   ├── mist/          # MistApiClient, MistRepository
│   ├── aruba/         # ArubaApiClient, ArubaRepository
│   ├── oui/           # OuiVendorRepository
│   ├── db/            # Room Database, DAOs, Entities
│   └── settings/      # DataStore SettingsRepository
├── domain/
│   ├── model/         # WifiObservation, BssidSummary, Snapshot
│   └── usecase/       # ExportCsvUseCase
└── ui/
    ├── scan/          # ScanScreen, ScanViewModel
    ├── monitor/       # MonitorScreen, MonitorViewModel
    ├── compare/       # CompareGraphScreen
    ├── snapshot/      # SnapshotListScreen, SnapshotViewModel
    └── settings/      # SettingsScreen, SettingsViewModel
```

## Survey Analyzer (Web Tool)

A Streamlit-based analysis tool for survey data exported from this app.

```bash
cd analyzer
docker-compose up
# Open http://localhost:8501
```

See [analyzer/README.md](analyzer/README.md) for details.

## Known Limitations

### Wi-Fi Scan Throttling (Android 9+)

Android 9 and later enforce OS-level limits on Wi-Fi scan frequency:

- **Foreground**: up to 4 scans per 2 minutes
- **Background**: up to 1 scan per 30 minutes

When the limit is reached, the app displays cached results from the previous scan and shows the warning banner **"Scan throttled by OS – showing cached results"** on the scan screen.

Additional notes:
- Other apps performing Wi-Fi scans contribute to the same quota, making throttling more likely in busy environments.
- Using Auto-scan with a short interval (e.g. 5 s) increases the chance of hitting the limit quickly.
- Throttling strictness may vary by device manufacturer and Android version.

## Screenshots

*Coming soon*

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgements

- [IEEE OUI Database](https://standards-oui.ieee.org/) for vendor lookup
- [Vico](https://github.com/patrykandpatrick/vico) for chart rendering
- [Juniper Mist API](https://www.mist.com/documentation/)
- [HPE Aruba Central API](https://developer.arubanetworks.com/new-central/)
