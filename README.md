# Flight Explorer (Roame takehome)

Shared-first Kotlin Multiplatform flight discovery: Compose (Android) + SwiftUI (iOS) over one `FlightExplorer` facade, local Python API, SQLDelight cache/saved, MapLibre (Android) + MapKit (iOS).

## Quick start

```bash
# 1) Local API (serves fixtures/flights.json)
python3 api/serve.py
# → http://127.0.0.1:8080/flights

# 2) Shared golden tests (need a JDK; Android Studio JBR works)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :shared:jvmTest

# 3) Android debug APK
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Open the folder in Android Studio and run `androidApp` on an emulator. On a Mac with full Xcode, open `iosApp/RoameStarter.xcodeproj` and run `RoameStarter` on an iPhone simulator.

## Base URLs

| Client | Base URL |
|--------|----------|
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://127.0.0.1:8080` |
| Physical device | `http://<your-LAN-IP>:8080` |

Android cleartext + `INTERNET` are enabled. iOS allows local networking via ATS (`NSAllowsLocalNetworking`).

## Architecture

```
Android Compose / iOS SwiftUI
        │
        ▼
  FlightExplorer facade  (250ms debounce, StateFlows)
        │
   ┌────┼────┐
   ▼    ▼    ▼
 Filter  MapAggregator  FlightRepository
                          │         │
                     FlightsApi   SQLDelight
                     (Ktor)     (cache + saved)
                          │
                    api/serve.py :8080
                          │
                  fixtures/flights.json
```

## Features

- Discovery filters: origin/dest (debounced 250ms), inclusive date range (default `2027-02-01`…`2027-04-01`), cabin, max points, multi-program OR
- Live result count, removable active-filter chips, one-tap reset
- List + map share one selection; filter-out clears selection
- Map markers show destination count + lowest points; selected flight draws origin→dest route
- Saved flights (SQLDelight), independent of discovery filters; bookmark marks on list rows
- Honest load UX: Loading / Ready / Empty / OfflineStale (cache + banner) / Error + retry
- Accessibility labels on filters, rows, markers, Save/Remove

## Offline demo

1. Start API, launch app, wait until flights load, save 1–2 flights.
2. Stop API (`Ctrl+C` on `serve.py`).
3. Pull to refresh / Retry — expect **Offline — showing cached flights** banner; search still works on cache; Saved tab still lists bookmarks.
4. Clear app data / fresh install with API down — expect Error + Retry (no cache).

## Tests

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :shared:jvmTest
```

Golden cases cover inclusive dates/points, program OR + cabin AND, city vs IATA, empty = all, map aggregation, cache replace, offline stale, saved CRUD honesty.

## iOS build note

This Mac mini may have Command Line Tools only (no full `Xcode.app`). SwiftUI sources under `iosApp/` are complete (MapKit, Discover/List/Map/Saved). Building requires Xcode 16+ so the Shared framework script can run. Set `JAVA_HOME` to Android Studio’s JBR before the Xcode Kotlin build phase if `/usr/libexec/java_home` is empty.

## Stack

Kotlin 2.x · Gradle wrapper · Ktor + kotlinx.serialization · SQLDelight · Jetpack Compose · SwiftUI · MapLibre GL JS (WebView) · MapKit · Python 3 `:8080`
