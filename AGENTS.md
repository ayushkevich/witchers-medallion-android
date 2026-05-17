```md
# AGENTS.md — Witcher's Medallion

## Project Overview

**Witcher's Medallion** is an Android application built with Jetpack Compose, Material3, MVVM, and Repository architecture for tracking BLE devices, including:
- ESP32-based Witcher medallion
- Nearby BLE "monster" devices

The project uses:
- Kotlin
- Jetpack Compose
- Hilt DI
- BLE scanning + GATT communication
- DataStore Preferences
- Coroutines + Flow

---

# Tech Stack

| Component | Value |
|---|---|
| Language | Kotlin 2.3.10 |
| Android Gradle Plugin | 8.12.3 |
| minSdk | 24 |
| compileSdk | 36 |
| UI | Jetpack Compose + Material3 |
| Navigation | HorizontalPager + TabRow |
| Dependency Injection | Hilt (KSP only, no kapt) |
| Persistence | DataStore Preferences |
| Concurrency | Kotlin Coroutines + Flow |
| Testing | JUnit5 + MockK + kotlinx-coroutines-test |
| Formatting | Spotless + ktlint |
| Static Analysis | SpotBugs |

---

# Architecture

The project follows:

```text
UI (Compose)
    ↓
ViewModel
    ↓
Repository
    ↓
BLE API / DataStore
```

Architecture principles:
- MVVM + Repository
- Unidirectional data flow
- Immutable UI state
- Shared BLE repository singleton
- Flow-based reactive state
- Compose-only UI

---

# Project Structure

```text
app/src/main/java/by/alexy/witchersmedallion/
├── config/
├── di/
├── domain/
├── permissions/
├── repository/
├── ui/
├── util/
├── viewmodel/
```

Important modules:

| Module | Responsibility |
|---|---|
| `repository/bluetooth` | BLE scan + GATT communication |
| `repository/impl` | DataStore persistence |
| `ui/screen` | Compose screens |
| `ui/state` | Immutable UI state classes |
| `viewmodel` | Screen business logic |
| `domain` | Shared models |
| `util` | Validation and helper utilities |

---

# Important Architectural Rules

## 1. BLE Repository Is Shared

`BleRepositoryImpl` is a singleton shared between multiple screens.

The following ViewModels use the same repository instance:
- `MainViewModel`
- `MacTrackingViewModel`

This means:
- BLE scan lifecycle must be centralized
- Scanning state is shared
- Multiple tabs may request scanning simultaneously

Do NOT:
- Create separate BLE repositories per screen
- Assume only one screen can scan
- Stop global scanning without ownership checks

---

## 2. No Hardcoded UI Strings

All user-visible text MUST use `stringResource()` and `strings.xml`.

Never write:
```kotlin
Text("Error")
```

Always use:
```kotlin
Text(stringResource(R.string.error))
```

Localization files:
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`

Requirements:
- Every new string must exist in BOTH files
- Use formatted placeholders (`%1$s`, `%1$d`) when needed

---

## 3. UI State Must Be Immutable

All UI state classes:
- use `@Immutable`
- are Kotlin `data class`
- are exposed via `StateFlow`

Example:
```kotlin
@Immutable
data class MainUiState(
    val isLoading: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
)
```

Do NOT:
- expose mutable collections
- mutate state objects directly
- use mutable shared state inside composables

---

## 4. Flow-Based State Management

Use:
- `StateFlow`
- `SharedFlow`

Do NOT use:
- `LiveData`
- callback-based UI state
- mutable singleton UI state

State updates should use:
```kotlin
_uiState.update { current ->
    current.copy(...)
}
```

---

## 5. Dialog State Belongs To UI Layer

Ephemeral UI state should stay inside composables whenever possible.

Examples:
- dialogs
- dropdown expansion
- temporary selection
- bottom sheet visibility

ViewModels should contain:
- business state
- persistent screen state
- repository coordination

---

## 6. Repository Responsibilities

Repositories:
- encapsulate BLE/DataStore APIs
- expose Flow-based state
- manage synchronization
- handle threading

ViewModels:
- should not directly manage BLE callbacks
- should not own BluetoothGatt
- should not launch long-running BLE loops

---

# Current Known Pitfalls

## BLE Scan Lifecycle

BLE scanning currently has shared lifecycle complexity because multiple screens use one repository instance.

Important considerations:
- scan ownership
- duplicate scan requests
- cleanup timing
- scan stop coordination
- stale device cleanup

Be careful when modifying:
- `BleRepositoryImpl.startScan()`
- `BleRepositoryImpl.stopScan()`

---

## Localization Consistency

The project previously contained:
- mixed Russian/English error strings
- hardcoded dialog text
- hardcoded RSSI labels

Do not introduce new hardcoded strings.

---

## Compose Component Reusability

Reusable UI components should:
- accept composable parameters when appropriate
- avoid raw String parameters for localized text
- avoid embedding formatting logic internally

Preferred:
```kotlin
label = { Text(stringResource(...)) }
```

instead of:
```kotlin
label: String
```

---

# Anti-Patterns

Do NOT:
- use `LiveData`
- use `kapt`
- hardcode UI strings
- create multiple BLE repositories
- launch long-running BLE jobs in `viewModelScope`
- directly mutate UI state
- store Android Context inside ViewModels
- expose mutable flows publicly
- use blocking BLE operations on Main thread

---

# Dependency Injection Rules

Use:
- `@HiltViewModel`
- constructor injection
- `@Singleton` repositories
- `@Binds` modules

The project uses:
- KSP
- NOT kapt

Correct:
```kotlin
ksp(libs.hilt.compiler)
```

Incorrect:
```kotlin
kapt(libs.hilt.compiler)
```

---

# Coroutines & Threading

Preferred dispatchers:
- `Dispatchers.IO` for BLE/DataStore operations
- `Dispatchers.Default` for computation
- Main thread only for UI updates

Avoid long-running BLE work on:
```kotlin
Dispatchers.Main
```

Repositories may use:
```kotlin
SupervisorJob()
```

for independent coroutine failure handling.

---

# Compose Conventions

## State Collection

Use:
```kotlin
collectAsState()
```

## Component Organization

Reusable components belong in:
```text
ui/screen/component/
```

## Dimensions

Use:
- `dp`
- Compose modifiers

Avoid hardcoded pixel values.

---

# Naming Conventions

| Type | Convention |
|---|---|
| Classes | PascalCase |
| Files | PascalCase |
| Functions | camelCase |
| Constants | SCREAMING_SNAKE_CASE |
| ViewModels | `*ViewModel` |
| UI State | `*UiState` |
| Repository interfaces | no suffix |
| Repository implementations | `*Impl` |

Examples:
- `MainViewModel`
- `MacTrackingUiState`
- `BleRepository`
- `BleRepositoryImpl`

---

# Import Order

Preferred import order:

1. Android / androidx
2. Kotlin standard library
3. Project imports
4. Third-party libraries

---

# Testing Strategy

| Layer | Preferred Testing |
|---|---|
| Utilities | Pure unit tests |
| ViewModels | Coroutine tests + MockK |
| Repositories | Fake implementations |
| BLE integration | Manual testing |

Important:
- BLE hardware behavior should not be heavily mocked
- Core logic should remain testable without Android framework

---

# Definition of Done

A task/refactor is NOT complete unless:

- `./gradlew test` passes
- `./gradlew spotlessCheck` passes
- localization files are updated
- no hardcoded strings are introduced
- no new warnings are introduced
- code follows existing architecture rules

---

# Useful Commands

## Run Unit Tests

```bash
./gradlew test
```

## Run Full Validation

```bash
./gradlew testAll
```

## Build Debug APK

```bash
./gradlew assembleDebug
```

## Spotless Check

```bash
./gradlew spotlessCheck
```

## Auto-format

```bash
./gradlew spotlessApply
```

---

# Useful Search Commands

## Find Hardcoded Compose Strings

```bash
rg 'Text\(\s*"' app/src/main/java --type kt
```

## Find Hardcoded Error Messages

```bash
rg 'errorMessage\s*=\s*"' app/src/main/java --type kt
```

## Compare Localization Files

```bash
diff app/src/main/res/values/strings.xml app/src/main/res/values-ru/strings.xml
```

---

# Preferred Development Approach

When implementing features:
1. Understand existing architecture first
2. Prefer incremental refactoring
3. Avoid rewriting unrelated systems
4. Keep BLE lifecycle stable
5. Validate localization immediately
6. Run formatting and tests frequently

Large architectural changes should be split into small isolated phases.

---

# Important Files

| File | Responsibility |
|---|---|
| `BleRepositoryImpl.kt` | BLE scanning + GATT |
| `MainViewModel.kt` | Main screen logic |
| `MacTrackingViewModel.kt` | Monster tracking logic |
| `CalibrationViewModel.kt` | RSSI calibration |
| `BleConfig.kt` | UUID configuration |
| `BlePermissionUtils.kt` | Runtime BLE permissions |
| `MacAddressUtils.kt` | MAC validation helpers |

---

# BLE Design Notes

The ESP32 medallion:
- is discovered through BLE scanning
- communicates through GATT
- exposes custom service UUIDs
- may later provide RSSI/calibration data directly

The application should:
- tolerate reconnects
- tolerate temporary scan interruptions
- clean up stale devices safely
- avoid duplicate connection attempts

---

# Final Principles

Prefer:
- simplicity
- incremental refactoring
- explicit state
- immutable models
- localized UI
- lifecycle-safe BLE handling

Avoid:
- overengineering
- unnecessary abstractions
- premature optimization
- hidden mutable state
- UI logic inside repositories
```