# Flight Explorer (Roame takehome)

Kotlin Multiplatform flight discovery app: **SwiftUI (iOS)** + **Jetpack Compose (Android)** over a shared data layer (Ktor + SQLDelight + filter/map logic). Local API serves the fixture at `fixtures/flights.json`.

## Demo & submission notes

Walkthrough of search, map/list selection, saves, and offline cache on both platforms:

https://github.com/user-attachments/assets/4ee2c96f-1fd0-4055-a502-7bb55b87f280

**Submission report:** [SUBMISSION.md](./SUBMISSION.md) — approach, assumptions, performance tradeoffs, tests, and AI use.

## One-command local API

```bash
python3 api/serve.py
```

Serves `GET http://127.0.0.1:8080/flights` (HTTP 200, fixture envelope). Smoke check:

```bash
curl -s http://127.0.0.1:8080/flights | python3 -c "import sys,json; print(len(json.load(sys.stdin)['flights']))"
```

## Prerequisites

- **JDK** via Android Studio JBR (or any JDK 17+). Example:

  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export ANDROID_HOME="$HOME/Library/Android/sdk"
  ```

- **Android:** Android Studio / SDK 37, emulator or device.
- **iOS:** macOS with **Xcode** (full app, not only Command Line Tools). Open `iosApp/RoameStarter.xcodeproj`.
- Python 3 (stdlib only) for the API.

## Run Android

1. Start the API: `python3 api/serve.py`
2. Start an emulator (or device; for a physical device set the machine LAN IP — see below).
3. Forward the host API into the emulator (needed with the baked-in `127.0.0.1` base URL):

   ```bash
   adb reverse tcp:8080 tcp:8080
   ```

4. From repo root:

   ```bash
   ./gradlew :androidApp:installDebug
   # or Run `androidApp` from Android Studio
   ```

Emulator base URL (baked in): `http://127.0.0.1:8080` via `adb reverse`. Re-run the reverse command after cold-booting the AVD. (`ApiBaseUrls.ANDROID_EMULATOR_GATEWAY` still has the classic `10.0.2.2` alias if you prefer that.)

## Run iOS

1. Start the API: `python3 api/serve.py`
2. Open `iosApp/RoameStarter.xcodeproj` in Xcode 16+/26+.
3. Select the `RoameStarter` scheme + iPhone simulator → Run.
4. First build runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (needs `JAVA_HOME`). An editor “No such module Shared” warning clears after the first successful build.

Simulator base URL (baked in): `http://127.0.0.1:8080`.

### Physical devices

Point both apps at `http://<your-lan-ip>:8080` (edit `ApiBaseUrls` usage in `MainActivity` / `FlightExplorerModel`). Ensure the phone and Mac are on the same network and the API binds `0.0.0.0:8080` (default).

## Tests

```bash
./gradlew :shared:jvmTest
```

Golden fixture tests cover search/filter boundaries, map aggregation, persistence, offline-stale refresh, and saved upsert/remove.

## Offline demo

1. Start API + app; wait until flights load.
2. Save at least one flight.
3. Stop the API (`Ctrl+C` in the API terminal).
4. Kill and reopen the app (or pull to refresh via Retry).
5. Expect: **Offline — showing cached flights** banner, search/filters still work on cache, Saved tab still lists/removes saves.
6. Clear app storage and launch with API down → **error + Retry** (no cache).

## Project layout

```
api/serve.py          Local GET /flights
fixtures/flights.json 1,000 fictional flights
shared/               KMP models, filters, map aggregation, Ktor, SQLDelight, FlightExplorer facade
androidApp/           Compose UI + MapLibre GL JS map
iosApp/               SwiftUI UI + MapKit
media/demo.mp4        Screen recording demo
SUBMISSION.md         Architecture, tradeoffs, AI disclosure, checklist
```

## Stack

Kotlin 2.4.21 · Gradle 9.7 · AGP 9.3 · Compose BOM 2026.09 · SQLDelight 2.1 · Ktor 3.1 · SwiftUI · MapKit · MapLibre GL JS (Android WebView)
