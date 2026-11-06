# Flight Explorer — submission notes

## Approach

Shared-first KMP core owns every product rule the PDF cares about:

- **Networking** — Ktor client `GET /flights`
- **Persistence** — SQLDelight `flight_cache` + `saved_flights` (upsert by `id`), serialized behind a mutex + background dispatcher
- **Search / filters** — `SearchFilterEngine` (trim/casefold; inclusive date + max points; program OR; dimension AND)
- **Map aggregation** — `MapAggregator` (destination → count + lowest points)
- **UI parity** — `FlightExplorer` facade exposes `StateFlow`s both native UIs observe

```text
Compose / SwiftUI
        │
        ▼
 FlightExplorer facade  (debounce 250ms, cancel superseded filter jobs, async DB)
   ├── SearchFilterEngine
   ├── MapAggregator
   └── FlightRepository → FlightsApi (Ktor) + FlightLocalStore (SQLDelight)
                              │
                              ▼
                     python3 api/serve.py :8080
                              │
                              ▼
                     fixtures/flights.json
```

Native UIs are intentionally thin: discovery chrome, list/map, detail sheet, saved tab, status banners.

## Key decisions

| Choice | Why |
|--------|-----|
| Python `api/serve.py` | One command, zero deps, serves the exact fixture envelope |
| SQLDelight | Multiplatform, testable with in-memory JDBC driver, honest upsert semantics |
| Ktor | Shared HTTP client with platform engines (OkHttp / Darwin / CIO) |
| MapLibre GL JS in Android WebView | MapLibre without a Google Maps API key; markers show count + min points; route polyline on selection |
| Lightweight Compose `RoutePreview` in Android detail sheet | Avoids spinning a second WebView just for the sheet map |
| MapKit on iOS | Native gestures, annotations, polyline — matches “SwiftUI preferred” |
| Facade + StateFlows | One selection model, one filtered set, list/map parity without duplicating rules |
| 250ms search debounce | Keeps typing smooth against 1k rows; latest query wins |
| Detail sheet vs selection | Dismissing the sheet keeps selection so the map route stays visible |

## Assumptions

1. **Route connector** = map polyline from origin coordinates → destination coordinates when a flight is selected (list or map).
2. **Android map** uses MapLibre **GL JS** (OpenFreeMap / demotiles / Esri fallback) embedded in a WebView rather than the native MapLibre Android SDK — still MapLibre, still pan/zoom/markers/route, no Google key.
3. **Default date range** = fixture full span `2027-02-01`…`2027-04-01` so results appear immediately.
4. **Points filter** is opt-in via an on/off control; when on, the slider value is an inclusive max.
5. **Empty filtered results** with a successful cache are UI “No flights match these filters”, not network `Empty` (network `Empty` means API returned zero flights).
6. Cross-device sync and offline map tiles are out of scope per PDF.

## Tradeoffs

**List / filter performance (~1,000 flights)**
- Rendering and diffing every filtered row on every selection or save is too expensive once the list is large.
- **Windowed list** (page size 24, scroll-driven load-more) on both platforms — UI only materializes what’s on screen; the facade still holds the full filtered set for count + map.
- **250ms debounce** on origin/destination text + cancel superseded filter jobs on `FlightExplorer` so typing doesn’t queue N full scans of the catalog.
- **Generation gating:** Android and iOS only rebuild the bridged/Compose list when `filteredGeneration` changes — selection and save update IDs without rewriting ~1k rows.

**Map aggregation**
- Markers are destination clusters (`count` + min points), not one annotation per flight — keeps marker count closer to airports than to 1k rows.
- Nested flight lists stay unloaded until a cluster is focused/opened so every filter change doesn’t rebuild heavy payloads.
- Route drawing uses a short-path polyline (not a polar great-circle) — cheaper geometry, good enough for takehome framing.

**Android map: MapLibre GL JS in a WebView**
- Avoids a Google Maps key and fights less with native annotation plugins for labeled markers + polylines.
- Cost: tiles still need network (offline tiles out of scope per PDF); WebView is heavier than a pure native map SDK.
- Detail sheet uses a lightweight Compose `RoutePreview` instead of a second WebView so we don’t pay that cost twice.

**Persistence / offline**
- **Cache the whole catalog** after a successful fetch (not only saved rows) so offline search/filters hit the same engine as online — more disk than a saved-only store, but required for the PDF offline-search behavior.
- SQLDelight writes are serialized behind a mutex on a background dispatcher so network/DB never block the UI thread.

**iOS bridge**
- `FlightExplorerIos` Main-thread snapshots instead of SKIE Flow adapters — less bridge machinery for a takehome; generation gating above keeps snapshot churn low when selection/save fire often.

**Testing**
- Golden tests on **JVM** (`:shared:jvmTest`) against the real fixture cover filter rules, aggregation, cache/saved, and facade debounce — fast and shared. Not a substitute for emulator/simulator checks of scroll, map, and offline UX.

## Tests run

```bash
./gradlew :shared:jvmTest
# BUILD SUCCESSFUL — SearchFilterEngineTest, MapAggregatorTest, FlightRepositoryTest, FlightExplorerTest
```

Covered: inclusive date bounds, inclusive max points (including exact boundary present in fixture), program OR + cabin AND, city/IATA trim/casefold, empty filters = all 1000, map count/min points, cache replace, offline-stale on failed refresh, error with no cache, saved upsert without dupes, saved survives empty API response, facade debounce (latest query wins), selection clear-on-filter-out, reset, offline stale via facade, save/remove ids.

```bash
./gradlew :androidApp:assembleDebug
# BUILD SUCCESSFUL
```

```bash
# iOS (requires Xcode.app)
xcodebuild -project iosApp/RoameStarter.xcodeproj -scheme RoameStarter \
  -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' build
```

```bash
python3 api/serve.py &  curl localhost:8080/flights  # HTTP 200, 1000 flights
```

## Manual checklist

| Check | Android | iOS |
|-------|---------|-----|
| API one-command + load flights | Ready | Ready |
| Debounced origin/dest search | Implemented | Implemented |
| Filter chips + reset + result count | Implemented | Implemented (incl. date chips) |
| List/map selection parity | Implemented | Implemented |
| Destination markers count + min pts | Implemented (MapLibre JS) | Implemented (MapKit) |
| Route connector + detail Save/Remove | Implemented | Implemented |
| Dismiss detail keeps map route | Implemented | Implemented |
| Saved tab ignores discovery filters | Implemented | Implemented |
| Offline stale banner + cached search | Implemented | Implemented |
| Empty vs filter-miss / error + retry | Implemented | Implemented |
| A11y labels on filters/rows/save | Implemented | Implemented |

### Offline demo steps (both apps)

1. `python3 api/serve.py`
2. Launch app; confirm flights + result count.
3. Search `BOS`, enable Business cabin, save a flight.
4. Switch List ↔ Map; confirm same selection + route after closing the detail sheet.
5. Open Saved tab; confirm flight present.
6. Stop API; force-stop app; relaunch.
7. Confirm stale/offline banner, filters still work, Saved still works, Remove works.
8. (Optional) clear app data, launch with API down → error + Retry.

## AI use

I treated AI as a force multiplier, not a substitute for owning the design:

1. **Plan + architecture** — I wrote the product/engineering plan from the PDF, then worked with Claude to turn that into a detailed PR-level architecture (shared facade, filters, cache/saved semantics, dual native UIs, local API).
2. **First implementation** — a Cursor cloud/worker agent built the first end-to-end implementation against that plan.
3. **My pass** — I personally drove polish and product choices beyond the bare spec (Roame branding, full-screen search morph, stacked map + list, expand-in-place selection, filter chips, opt-in points, map focus / dimming, Save color parity), helped structure and extend the shared tests, and did UI/interaction touch-ups on both platforms until the demo felt right.

I can explain and modify every layer that shipped. Verification wasn’t “trust the agent”: golden `:shared:jvmTest`, Android emulator installs, iOS simulator builds, and manual offline/cache walks on both clients.

## Recording

Capture once both simulators are handy:

1. Start `python3 api/serve.py` (+ `adb reverse tcp:8080 tcp:8080` for Android emulator).
2. Record Android: search → chips → list select → dismiss sheet (route remains) → map → save → Saved tab → kill API → relaunch stale banner.
3. Record iOS with the same script.
4. Drop files under the project Agent Store `media/` (e.g. `media/flight-explorer-android.mp4`, `media/flight-explorer-ios.mp4`).

## How to build / test (short)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
python3 api/serve.py          # terminal 1
adb reverse tcp:8080 tcp:8080 # Android emulator
./gradlew :shared:jvmTest :androidApp:installDebug
open iosApp/RoameStarter.xcodeproj
```
