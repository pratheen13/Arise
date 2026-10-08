# Arise — full native Android app

No WebView, no bundled web assets. This is a real Kotlin + Jetpack Compose app that
reimplements the reference app's own UI and game logic natively — same screens (Hunter,
Quests, Connect, Rank), same rank table, same XP/level/quest-target formulas, same colour
palette — reverse-engineered directly out of your ARISE-Android-v2.apk bundle. `hero.png`
from that bundle is the character art here too.

## Open and build

Android Studio → **Open** → this `AriseNative` folder → sync → **Run** on a real phone.
Emulators have no real proximity sensor, accelerometer, or GPS.

## Game logic (matched to your original, not reinvented)

- Level = `min(100, 1 + floor(xp / 200))`
- Ranks: E Awakening(1) · D Rising hunter(10) · C Dungeon challenger(25) · B Elite hunter(45) · A Master hunter(70) · S Shadow monarch(100)
- Quest targets scale from day 0 → day 99: reps 5→100, distance 0.5→10 km, steps 1,000→10,000
- +40 XP per newly-cleared task, 5 tasks/day (push-ups, sit-ups, squats, distance, steps)
- Phone vs. Health Connect signals never add — the larger of the two always wins (`HunterState.withSignals`)

## Real sensors behind every screen

| Quest task | Native source |
|---|---|
| Push-ups | `TYPE_PROXIMITY` — phone flat on floor, face near→far = one rep |
| Sit-ups | Accelerometer tilt angle vs. a calibrated baseline |
| Squats | Calibrated accelerometer-magnitude dip→rise |
| Steps | `TYPE_STEP_COUNTER`, daily baseline persisted so it survives reboots |
| Distance | Fused location; haversine + accuracy/speed filtering ported from your own JS |
| Watch data | Health Connect aggregate read, with real contributing-app names |

## Files

- `HunterState.kt` — state, persistence, and every formula above
- `StepEngine.kt` / `RepEngine.kt` / `GpsEngine.kt` / `HealthBridge.kt` — the sensor layer
- `AriseViewModel.kt` — wires state + sensors together
- `MainActivity.kt` — permissions + root layout
- `ui/Theme.kt` — palette (`#08060E` / `#A944EF` / `#7CE9C8`) and the character stage
- `ui/Screens.kt` — Hunter, Quests, Connect, Rank, nav, session modal, toast
