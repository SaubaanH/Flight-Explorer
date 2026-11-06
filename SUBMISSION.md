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

**List rendering over ~1,000 flights**
- Spec asks for a smooth list against the full fixture. Rendering every row eagerly is too heavy once selection, saves, and filters update.
- **Choice:** windowed / paged list (page size 24) with scroll-driven load-more on both platforms; 250ms search debounce; cancel superseded filter jobs on the facade.
- **iOS specifically:** bridge filtered flights into Swift `FlightCard` value types and only re-bridge when `filteredGeneration` bumps — selection/save don’t rebuild 1k rows.

**Map performance & framing**
- Destination markers show count + min points for the filtered set; selecting a flight draws a short-path route (not a polar great-circle).
- **Android:** MapLibre GL JS in a WebView (no Google Maps key) — faster to ship labeled markers + polylines than fighting native annotation plugins; tiles still need network (offline tiles out of scope).
- **iOS Discover:** MapKit for native gestures; camera fit resets before applying a new region so selection actually reframes.
- **Discover UX:** stacked map + list (not List/Map tabs) so selection parity is obvious; city tap filters to/from that airport; selected route dims other markers instead of hiding them.
- Clusters keep nested flight lists unloaded until opened / focused — cheaper recompute on every filter change.

**Other**
- **Cache whole catalog** after a successful fetch so offline search works on the same filter engine — not just saved rows.
- **Swift bridge** uses `FlightExplorerIos` Main snapshots rather than SKIE Flow adapters — simpler for a takehome; generation gating above keeps it fast.
- Golden tests on **JVM** (`:shared:jvmTest`) with the real fixture — same shared code both apps use; not a substitute for simulator/emulator checks.
- Material3 `Button` remaps brand pink — Save uses a flat custom pill so Android matches iOS `#E83A6F`.

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
