# Screen recording script (both apps)

Prerequisite: Xcode.app installed for iOS; Android emulator running.

## Setup

```bash
cd ~/roame-mobile
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
python3 api/serve.py   # leave running
```

## Script (≈2–3 minutes per platform)

1. **Cold load** — open app; wait for flights; show result count (~1000).
2. **Search** — type `bos` in Origin; confirm debounce + filtered count.
3. **Filters** — cabin Business; enable max points; select Avios + Aeroplan; show chips + count.
4. **List select** — tap a row; selection card appears; bookmark icon if saved later.
5. **Map** — switch to Map; markers show `CODE · count · minPts`; tap marker; pick a flight.
6. **Route** — confirm origin→dest polyline + card fields.
7. **Save** — Save on card; open Saved tab; confirm flight.
8. **Parity** — back to Discover; switch List/Map; same selection preserved.
9. **Offline** — stop API (`Ctrl+C`); force-quit app; relaunch; show stale banner; search still works; remove a saved flight.
10. **Error path (optional)** — clear app storage; launch with API down; show error + Retry.

Save recordings to Agent Store media:

- `/Users/saubaanhasan/Library/Application Support/Cursor/AgentStores/cursor_agent_stores/bc-d50e4264-3558-4df8-bd9c-9f85714cddda/files/media/flight-explorer-android.mp4`
- `/Users/saubaanhasan/Library/Application Support/Cursor/AgentStores/cursor_agent_stores/bc-d50e4264-3558-4df8-bd9c-9f85714cddda/files/media/flight-explorer-ios.mp4`
