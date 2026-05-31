---
name: android-kotlin
description: Kotlin coding conventions, naming, architecture patterns, Hilt DI, Coroutines/Flow, error handling, and utility patterns for Witchers Medallion app. Use when writing, reviewing, or refactoring Kotlin source files outside of Compose UI.
origin: AI-generated
---

# Android Kotlin Conventions

Project-specific Kotlin conventions for Witchers Medallion. Covers naming, DI with Hilt (KSP), Coroutines/Flow, sealed hierarchies, utility patterns, and code organization.

## When to Use

- Writing new Kotlin source files (ViewModels, repositories, domain models, utilities, config)
- Reviewing Kotlin code for convention compliance
- Refactoring existing Kotlin code
- Adding new error types, domain models, or repository methods

## When NOT to Use

- Compose UI code (use `android-composable` skill)
- XML/layout code (not used in this project -- Compose-only)

---

# Naming Conventions

| Type | Convention | Examples |
|---|---|---|
| Classes | `PascalCase` | `BleRepositoryImpl`, `MainViewModel` |
| Files | `PascalCase` | `MainViewModel.kt`, `BleDevice.kt` |
| Functions | `camelCase` | `startScan()`, `connectToDevice()` |
| Properties | `camelCase` | `discoveredDevices`, `connectionState` |
| Constants | `SCREAMING_SNAKE_CASE` | `DEVICE_TIMEOUT_SECONDS`, `GATT_OP_TIMEOUT_MS` |
| ViewModels | `*ViewModel` suffix | `MainViewModel`, `CalibrationViewModel` |
| UI State | `*UiState` suffix | `MainUiState`, `MacTrackingUiState` |
| Repository impls | `*Impl` suffix | `BleRepositoryImpl`, `BleMedallionRepository` |
| Repository interfaces | no suffix | `BleRepository`, `MedallionRepository` |
| Domain models | domain noun | `BleDevice`, `BleConnectionState`, `BleScanError` |
| Utilities | `*Utils` object | `MacAddressUtils`, `RssiColorUtils` |
| Config | `*Config` object | `BleConfig` |

### DO

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val bleRepository: BleRepository,
) : ViewModel() {
    companion object {
        private const val DEFAULT_SCAN_DURATION_MS = 20_000L
    }
}
```

### DON'T

```kotlin
// Wrong: class named with Impl suffix when it's an interface
interface BleRepositoryImpl { ... }

// Wrong: camelCase constant
private const val deviceTimeoutSeconds = 30

// Wrong: ViewModel without suffix
class MainScreenLogic @Inject constructor(...) : ViewModel()
```

---

# Package Structure

```text
by.alexy.witchersmedallion/
  config/              # Singleton objects with constants (BleConfig)
  di/                  # Hilt modules
  domain/              # Pure Kotlin models, sealed hierarchies, UiText
  permissions/         # Permission utilities
  repository/          # Repository interfaces (top-level)
    impl/              # Repository implementations
    bluetooth/         # BLE-specific repositories
      impl/            # BLE repository implementations
  ui/
    model/             # UI-specific models
    screen/            # Compose screens + component/
    state/             # Immutable UI state classes
    theme/             # Compose theme
  util/                # Utility objects
  viewmodel/           # Android ViewModels
```

Rules:
- Repository interfaces live at the top level of their package
- Repository implementations go in `impl/` subdirectory
- DI modules live in `di/`
- Domain models have zero Android imports

---

# Hilt Dependency Injection

Project uses **KSP** (not kapt) for Hilt annotation processing.

### Repository Bindings

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindBleRepository(impl: BleRepositoryImpl): BleRepository

    @Binds
    abstract fun bindMedallionRepository(impl: BleMedallionRepository): MedallionRepository
}
```

### ViewModel Injection

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val bleRepository: BleRepository,
) : ViewModel()
```

### Activity Injection

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
}
```

### DO

- Use `@Binds` abstract methods for interface bindings
- Use `@Inject constructor` for concrete class injection
- Use `@Singleton` for shared infrastructure (BLE repo, DataStore)
- Use `@ApplicationContext` qualifier for Context in singletons
- Keep DI modules minimal -- only `@Binds`, no `@Provides`

### DON'T

- Use `kapt` -- use `ksp` only
- Use `@Provides` methods when `@Binds` suffices
- Inject `ActivityContext` into ViewModels or repositories
- Create multiple instances of BLE infrastructure

---

# Coroutines & Flow

### StateFlow for Persistent State

```kotlin
private val _uiState = MutableStateFlow(ScreenUiState())
val uiState: StateFlow<ScreenUiState> = _uiState.asStateFlow()
```

### Update Pattern

```kotlin
_uiState.update { current ->
    current.copy(isLoading = true, errorMessage = null)
}
```

### Flow Composition (preferred when merging multiple flows)

```kotlin
val uiState: StateFlow<MainUiState> = combine(
    bleRepository.scanningInProgress,
    bleRepository.connectionState,
    bleRepository.discoveredDevices.map { devices ->
        devices.sortedWith(compareByDescending { it.rssi })
    },
    bleRepository.connectedDeviceName,
) { scanning, connectionState, sortedDevices, deviceName ->
    MainUiState(
        state = connectionState,
        isScanning = scanning,
        availableDevices = sortedDevices,
        connectedDeviceName = deviceName,
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(3000),
    initialValue = MainUiState(),
)
```

### SharedFlow for One-Time Events

```kotlin
private val _statusUpdates = MutableSharedFlow<StatusUpdate>(replay = 0)
override val statusUpdates: Flow<StatusUpdate> = _statusUpdates
```

### DO

- Expose read-only `Flow` / `StateFlow` publicly
- Use `viewModelScope.launch` for async ViewModel work
- Use `combine` + `stateIn` for Flow-based state composition
- Use `.update` pattern for `MutableStateFlow` mutations
- Use `SharingStarted.WhileSubscribed(3000)` for state sharing

### DON'T

- Expose `MutableStateFlow` publicly
- Use `LiveData`
- Switch dispatchers in ViewModels for repository calls
- Launch long-running BLE jobs in `viewModelScope`
- Use blocking operations on Main thread

---

# Sealed Hierarchies

Use sealed classes for typed error handling and state representation.

### Error Types

```kotlin
sealed class BleScanError {
    object BluetoothDisabled : BleScanError()
    object ScannerUnavailable : BleScanError()
    data class ScanFailed(val errorCode: Int) : BleScanError()
    data class ConnectionFailed(val status: Int) : BleScanError()
    data class GattError(val code: Int) : BleScanError()
    data class Timeout(val operation: String) : BleScanError()
    data class Unknown(val message: String) : BleScanError()
}
```

### State Enums

```kotlin
enum class BleConnectionState {
    CONNECTING, CONNECTED, DISCONNECTED
}

enum class AutoCalibrationStep {
    NONE, MOVE_TO_HOT, MOVE_TO_WARM, MOVE_TO_COLD, COMPLETED
}
```

### DO

- Use sealed classes for error hierarchies
- Use `object` for stateless variants, `data class` for parameterized ones
- Use `enum class` for ordered/finite state sets

---

# Domain Models

### Immutable Data Classes

```kotlin
data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeenAt: Instant = Instant.now(),
)
```

### UiText Abstraction

```kotlin
sealed class UiText {
    data class StringResource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText()
    data class DynamicString(val value: String) : UiText()

    fun getString(context: Context): String = when (this) {
        is StringResource -> context.getString(id, *args.toTypedArray())
        is DynamicString -> value
    }

    companion object {
        fun fromStringResource(@StringRes id: Int, vararg args: Any): UiText = StringResource(id, args.toList())
        fun fromDynamicString(value: String): UiText = DynamicString(value)
    }
}
```

### DO

- Make all domain models immutable `data class`
- Use `UiText` for localized text passing across layers
- Keep domain layer free of Android imports

---

# Utility Objects

```kotlin
object MacAddressUtils {
    fun isValidMacAddress(mac: String): Boolean {
        val clean = mac.uppercase().replace(Regex("[^0-9A-F]"), "")
        return clean.length == 12 && clean.matches(Regex("[0-9A-F]{12}"))
    }

    fun isDynamicMac(address: String): Boolean { ... }
}
```

### DO

- Use `object` for stateless utility functions
- Keep utility functions pure and testable
- Name utilities `*Utils`

---

# Config Objects

```kotlin
object BleConfig {
    val SERVICE_UUID: UUID = UUID.fromString("9e5b4cb7-6ccc-4027-a496-ac7c01bbf706")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("d3481eab-e4cf-49f7-9d3e-36a2b7dff88e")
    // ...
}
```

### DO

- Use `object` for singleton config
- Use `SCREAMING_SNAKE_CASE` for constants
- Group related constants together

---

# Error Handling

### Repository Layer

```kotlin
override suspend fun setCalibrationSettings(settings: MedallionCalibrationSettings) =
    withContext(Dispatchers.IO) {
        val bytes = serializeCalibrationSettings(settings)
        val success = bleRepository.writeCharacteristic(BleConfig.CALIBRATION_UUID, bytes)
        if (!success) {
            throw RuntimeException("Failed to write calibration to device")
        }
    }
```

### ViewModel Layer

```kotlin
try {
    val registeredMacs = medallionRepository.getRegisteredMacAddresses()
    _uiState.update { it.copy(trackedDevices = registeredMacs.map { mac -> MacDevice(mac, -60) }) }
} catch (e: Exception) {
    _uiState.update {
        it.copy(errorMessage = UiText.fromStringResource(R.string.error_loading_macs, e.message ?: ""))
    }
} finally {
    _uiState.update { it.copy(isLoading = false) }
}
```

### DO

- Use `UiText` for localized error messages in ViewModels
- Wrap exceptions in UI state, don't let them leak to Compose
- Use `try/finally` for loading state management

### DON'T

- Pass raw `Throwable` to UI
- Use hardcoded error strings in ViewModels
- Swallow exceptions without logging or state update

---

# Testing Conventions

### Pure Unit Tests (JUnit 5 + Parameterized)

```kotlin
class MacAddressUtilsTest {
    @ParameterizedTest
    @MethodSource("validMacAddresses")
    fun `isValidMacAddress returns true for valid MAC addresses`(mac: String) {
        assertTrue(MacAddressUtils.isValidMacAddress(mac))
    }

    companion object {
        @JvmStatic
        fun validMacAddresses() = listOf(
            "00:1A:2D:4E:5F:6A",
            "001A2D4E5F6A",
        )
    }
}
```

### DO

- Use JUnit 5 (`org.junit.jupiter.api.Test`)
- Use `@ParameterizedTest` + `@MethodSource` for data-driven tests
- Use descriptive backtick test names: `` `function returns true for valid input` ``
- Keep test files in parallel structure under `app/src/test/`

---

# Code Size Guidelines

| Type | Target |
|---|---|
| Class / file | <= 200 lines |
| Method | <= 20 lines |

Exceptions: sealed hierarchies, DI modules, immutable state declarations may exceed limits.

---

# Detection Heuristics

- Check for `kapt` usage -- should be `ksp`
- Check for `LiveData` usage -- should be `StateFlow`
- Check for hardcoded strings in non-UI Kotlin files -- should use `UiText` or resource references
- Check for public `MutableStateFlow` -- should be private with public read-only `StateFlow`
- Check for missing `*ViewModel` suffix on ViewModel classes
- Check for Android imports in `domain/` package

---

# Related Skills

- `architecture` -- MVVM, Clean Architecture, layer responsibilities
- `testing` -- JUnit 5, MockK, coroutine testing patterns
- `android-composable` -- Jetpack Compose conventions
