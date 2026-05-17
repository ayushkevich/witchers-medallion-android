# Witcher's Medallion

An Android application that turns the Witcher universe into an interactive experience. Paired with a wearable ESP32-based "medallion" (a BLE peripheral), the app detects nearby "monsters" (BLE-emitting devices), vibrates the medallion when they approach, and displays direction and distance estimated from RSSI signal strength.

## What It Does

The app has three main screens:

- **Device Scanner** — Scans for BLE devices, lets you connect to your medallion, and shows the connection status.
- **Calibration** — Defines three proximity zones (cold/warm/hot) by setting RSSI thresholds, either manually with sliders or through a guided auto-calibration wizard.
- **Monster Tracker** — Registers "monster" devices by MAC address, tracks them, and identifies devices with dynamic addresses.

## Architecture

The project follows MVVM with Clean Architecture principles:

- **Domain layer** — Plain Kotlin data classes and value objects, no Android dependencies
- **Repository layer** — Interfaces and implementations for data sources (BLE API, local storage)
- **ViewModel layer** — Android ViewModels with Hilt injection, state exposed via StateFlow
- **UI layer** — Jetpack Compose screens, reusable components, and UI state classes
- **DI layer** — Hilt modules for dependency injection

```
UI Screen (Compose)
  -> ViewModel (StateFlow, actions)
    -> Repository interface
      -> RepositoryImpl (suspend, data sources)
        -> Local storage / BLE API
```

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| Kotlin 2.3.10 | Primary language, JVM 21 |
| Min SDK | 24 (Android 7.0) |
| Jetpack Compose + Material3 | UI toolkit |
| Hilt + KSP | Dependency injection |
| Coroutines + Flow | Async operations and state management |
| Hilt Navigation Compose | Navigation between screens |
| DataStore Preferences | Local key-value storage |
| Accompanist Permissions | Runtime permission handling |
| JUnit 5 + MockK | Unit testing |
| PMD, SpotBugs, Checkstyle, Spotless | Static code analysis |

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Run all checks (tests + static analysis)
./gradlew testAll

# Run unit tests only
./gradlew test

# Run Spotless formatting check
./gradlew spotlessCheck

# Apply Spotless formatting
./gradlew spotlessApply
```

## Project Structure

```
app/src/main/java/by/alexy/witchersmedallion/
  di/                # Hilt DI modules
  domain/            # Plain Kotlin domain entities
  permissions/       # Permission handling utilities
  repository/        # Repository interfaces and implementations
    bluetooth/       # BLE-specific repository
    impl/            # Local repository implementations
  ui/
    model/           # UI-specific models
    screen/          # Compose screens
      component/     # Reusable Compose components
    state/           # UI state data classes
    theme/           # Compose theme definitions
  util/              # Utility functions
  viewmodel/         # Android ViewModels
  MainActivity.kt
  WitchersMedallionApp.kt
```

## Static Analysis Configuration

- **Spotless** (ktlint 1.5.0) — Code formatting with Apache 2.0 license header
- **SpotBugs** (4.9.3) — Bug detection with exclusion filter for generated Hilt code

## Testing

- Unit tests located in `app/src/test/java/` mirroring source structure
- Uses JUnit 5 with parameterized tests and MockK
- Coroutines tested with `StandardTestDispatcher` and `StandardTestScheduler`
- Run `./gradlew test` for unit tests or `./gradlew testAll` for full verification

## License

Apache License 2.0

## Bugs
- Hardcoded values and magic numbers
- Search by service should be applicable for the main tab only, the search by mac should be applied to any device with static mac(static + dynamic macs should be visible). If user attemts to add dynamic mac - error message should be displayed.
- Mac Address input should not be visible near the scan devices button, only nead Add mac
- Calibration. Save to medallion should be located under the back to manual calibration, now it's located in the bottom for the auto calibration and on the right side for manual calibration
- Current RSSI should not be visible if device is not connected.
- On the autocalibration screen user should see message that informs that device is not connected. If user clicks on the connection button, user should be moved to the main screen and search should be started.
- Main view model is in imperative style, other screens are in the reactive style. The single style should be followed.
