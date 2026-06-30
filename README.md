# android-plane-tracker

A native Android app that tracks aircraft flying within a configurable radius of **your phone's current location**, using the free, key-less [adsb.lol](https://adsb.lol) ADS-B feed. It's a port of the [atom-plane-tracker](../atom-plane-tracker) M5Stack Atom S3R firmware, with two deliberate platform swaps:

- **Live GPS** replaces the firmware's fixed coordinate — the scan radius follows you as you move.
- **Native Android notifications** (posted locally from a background polling service) replace the firmware's ntfy push server — no backend, no account, fully standalone.

---

## What it does

- A foreground service polls adsb.lol for aircraft within your radius, centered on your live location, at a configurable interval (minimum 10 s).
- Each aircraft is classified and color-coded exactly as on the device:

  | Class | Color | Basis |
  |---|---|---|
  | Military | Red | API `mil` flag or `dbFlags` bit 0 |
  | Medevac | Blue | FAA LIFEGUARD/MEDVAC/REACH/LIFEFLT callsign prefix, or known EMS operator |
  | Commercial | Green | ADS-B category A3/A4/A5 or ICAO airline-code pattern (e.g. `UAL123`) |
  | Private / Other | Yellow | Everything else |

- The **Live** screen shows callsign, type, altitude, distance + compass bearing, ETA until the aircraft leaves your radius (counted down using last position, speed, and track), and squawk. Emergency squawks (7500 hijacking, 7600 radio failure, 7700 general emergency) flash red.
- The **Radar** tab has two sub-tabs:
  - **Radar** — a top-down scope with overhead aircraft as triangles pointing along their track.
  - **Map** — an OpenStreetMap view centered on your live position, auto-zoomed so the scan-radius circle fills the screen, with each overhead aircraft as a class-colored marker. Tapping a marker opens its live flight path on ADS-B Exchange.
- **History** lists the last 5 detected aircraft; **Summary** shows session totals by class.
- **Tap any callsign** (on the Live cards or in History) to open that flight's live path on [ADS-B Exchange](https://globe.adsbexchange.com).
- Notifications fire when aircraft enter your radius, with per-class priority (Military = urgent, Medevac = high, Commercial = default, Private = low) and a dedicated urgent channel for emergency squawks. Each notification has a **Track Flight** action that opens the same live flight view.

---

## Settings

Configured in-app (persisted via Jetpack DataStore — the equivalent of the firmware's NVS):

| Setting | Notes |
|---|---|
| Search radius (NM) | 1–50 NM |
| Scan interval (s) | Minimum 10 s |
| POI aircraft types | Comma-separated ICAO type codes (e.g. `B737,F16,C172`) |
| Enable POI filter | Show only POI types on Live/Radar |
| Enable notifications | Master toggle |
| Per-class toggles | Military / Medevac / Commercial / Private |
| POI aircraft | Notify for any POI-matched aircraft, overriding the class filter |
| Emergency squawk | Notify on 7500 / 7600 / 7700 (on by default) |
| Send test notification | Fires a test push immediately |

---

## Mapping from the firmware

| Firmware (C++) | Android (Kotlin) |
|---|---|
| `Aircraft.cpp` classify / ETA / distance / bearing | `model/AircraftClass.kt`, `model/Aircraft.kt` |
| `AdsbLolSource.cpp` (HTTPClient) | `data/AdsbLolSource.kt` (OkHttp) |
| `AircraftStore.cpp` | `data/AircraftStore.kt` |
| `Config.h/.cpp` + NVS | `data/Settings.kt` + `data/SettingsRepository.kt` (DataStore) |
| `Notifier.cpp` (ntfy) | `notify/Notifier.kt` + `notify/NotificationChannels.kt` (local notifications) |
| `main.cpp` setup/loop | `service/TrackingService.kt` (foreground service) |
| `Display.cpp` + WebUI screens | `ui/` Jetpack Compose screens |
| Fixed query coordinate | `location/LocationProvider.kt` (FusedLocation) |
| OTA updater / web UI / web password / analytics | _dropped — handled by Play Store / not applicable_ |

---

## Build & run

This is a standard Android Studio project (Kotlin + Jetpack Compose).

**Requirements**
- Android Studio (Koala / 2024.1+)
- JDK 17
- Android SDK with API 34; minimum device API 26 (Android 8.0)

**Steps**
1. Open the `android-plane-tracker` folder in Android Studio.
2. Let it sync — Android Studio reads `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9) and downloads dependencies. _(If you build from the command line instead, generate the wrapper jar once with a local Gradle install: `gradle wrapper`. The wrapper jar is intentionally not committed.)_
3. Connect a device (or start an emulator) and click **Run**.
4. On first launch, grant **Location** and **Notifications** permissions. Tracking starts automatically; use the ▶/⏹ button in the top bar to start/stop the background service.

> **Note:** Location and notifications require a physical setup or an emulator with mock location + Google Play services. The aircraft feed needs internet access.

---

## Dependencies (all permissive licenses)

- AndroidX Core / Lifecycle / Activity / Compose (Apache-2.0)
- Jetpack Compose Material 3 (Apache-2.0)
- Jetpack DataStore (Apache-2.0)
- osmdroid — OpenStreetMap map view (Apache-2.0, no API key)
- Google Play Services Location (proprietary-but-redistributable Google API)
- OkHttp (Apache-2.0)
- kotlinx-coroutines (Apache-2.0)

JSON is parsed with the platform's built-in `org.json`.
