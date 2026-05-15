---
name: architecture
description: Android (Kotlin) architecture guidelines for Witchers Medallion app. Covers MVVM, Clean Architecture layers, Hilt DI, Jetpack Compose UI, Coroutines/Flow state management, repository patterns, BLE infrastructure ownership, and code size constraints. Use when writing, reviewing, or refactoring Kotlin Android code.
origin: ECC
---

# Witchers Medallion Architecture Guidelines

Project-specific architecture for a Kotlin Android BLE application. Covers MVVM with Clean Architecture, Hilt DI, Jetpack Compose, Coroutines/Flow, repository patterns, BLE infrastructure ownership, and maintainable code structure.

## When to Use

- Writing new Kotlin/Android source files (UI, ViewModel, repository, domain, DI)
- Reviewing code for architectural compliance
- Refactoring to align with established patterns
- Adding new screens, ViewModels, repositories, or BLE flows

---

# Project Structure

```text
by.alexy.witchersmedallion/
  di/                 # Hilt modules (dependency injection)
  domain/             # Pure Kotlin business/domain models
  permissions/        # Permission handling utilities
  repository/
    bluetooth/        # BLE infrastructure + repositories
      datasource/     # Shared BLE infrastructure/services
      pairing/        # Pairing-specific repositories
      calibration/    # Calibration-specific repositories
      impl/           # Shared repository implementations
    impl/             # Local data repository implementations
  ui/
    model/            # UI-specific models/mappers
    screen/
      component/      # Reusable Compose components
    state/            # Immutable UI state classes
    theme/            # Compose theme definitions
  util/               # Utility helpers/extensions
  viewmodel/          # Android ViewModels
```

---

# Layer Responsibilities

## Domain Layer (`domain/`)

- Pure Kotlin only
- No Android or framework imports
- Represents business entities and business rules
- May contain validators, use cases, and domain-specific logic
- No Hilt annotations
- No Compose imports

### Allowed in Domain Layer

- Data classes
- Value objects
- Use cases/interactors
- Validators
- Pure business logic
- Pure mapping logic

```kotlin
data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class MedallionCalibrationSettings(
    val minDistance: Int,
    val maxDistance: Int,
    val rssiThreshold: Int
)
```

### DO

- Keep domain models immutable
- Keep business logic framework-independent
- Prefer pure functions

### DON'T

- Import `android.*`
- Import `androidx.*`
- Use ViewModel or Compose APIs
- Access Context or system services

---

## Repository Layer (`repository/`)

Repositories coordinate data access and infrastructure interaction.

- Define interfaces at the top level
- Place implementations in `impl/`
- Use repositories for business-oriented workflows
- No Compose dependencies
- No ViewModel dependencies
- All I/O operations must be `suspend` or `Flow`

### Repository Scope Rules

Repository scope depends on ownership of state.

### Use `@Singleton` for shared infrastructure

Shared infrastructure/services:

- BLE scanner
- GATT connection manager
- DataStore
- Database
- Shared caches
- System service wrappers

```kotlin
@Singleton
class BleScanner @Inject constructor(
    private val context: Context
)
```

### Use `@ViewModelScoped` or unscoped repositories

For screen-specific workflows/state:

- Pairing flows
- Calibration flows
- Discovery coordination
- UI-oriented repository logic

Repositories may depend on shared singleton infrastructure.

```kotlin
interface PairingRepository {
    val discoveredDevices: StateFlow<List<BleDevice>>

    suspend fun startScan()
    suspend fun stopScan()
}

class PairingRepositoryImpl @Inject constructor(
    private val bleScanner: BleScanner
) : PairingRepository
```

### DO

- Keep repositories focused on a single workflow/responsibility
- Expose state using Flow/StateFlow
- Use typed errors or Result wrappers
- Keep infrastructure reusable

### DON'T

- Put unrelated screen state into singleton repositories
- Access Compose APIs
- Store UI state in repositories
- Create BLE scanner instances per screen

---

## ViewModel Layer (`viewmodel/`)

ViewModels coordinate UI state and user actions.

- Extend `androidx.lifecycle.ViewModel`
- Use `@HiltViewModel`
- Inject repositories/use cases
- Use `viewModelScope`
- Expose immutable `StateFlow`
- Transform repository/domain state into UI state

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val pairingRepository: PairingRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        pairingRepository.discoveredDevices
    ) { devices ->
        MainUiState(
            availableDevices = devices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(3000),
        initialValue = MainUiState()
    )

    fun startScan() {
        viewModelScope.launch {
            pairingRepository.startScan()
        }
    }
}
```

### DO

- Keep ViewModels focused on state coordination
- Use immutable UI state
- Use `combine`, `map`, and Flow operators
- Expose read-only StateFlow
- Keep ViewModels lifecycle-aware

### DON'T

- Store Context references
- Put BLE implementation details in ViewModels
- Put heavy business logic in ViewModels
- Call Compose APIs
- Switch dispatchers unnecessarily

---

# One-Time Events

Use `SharedFlow` or `Channel` for transient UI events.

Use for:

- Navigation
- Snackbars/toasts
- Permission requests
- One-shot dialogs

Do not model transient events inside persistent `StateFlow`.

```kotlin
private val eventsFlow = MutableSharedFlow<MainEvent>()
val events = eventsFlow.asSharedFlow()
```

---

# UI Layer (`ui/`)

## Screens (`screen/`)

- Screens are Compose entry points
- Receive ViewModel as parameter
- Collect state using lifecycle-aware APIs
- Forward user actions to ViewModel
- Keep UI presentation-focused

```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // UI rendering
    }
}
```

### DO

- Use `collectAsStateWithLifecycle()`
- Split large composables into reusable components
- Keep composables stateless where possible
- Move business logic out of UI

### DON'T

- Call repositories directly
- Launch business coroutines from composables
- Put complex branching logic inside composables
- Create giant composables

---

## UI State (`ui/state/`)

- UI state must be immutable
- Use data classes
- Keep state screen-specific
- Use `copy()` for updates
- Avoid mutable collections

```kotlin
data class MainUiState(
    val isScanning: Boolean = false,
    val availableDevices: List<BleDevice> = emptyList(),
    val connectedDeviceName: String? = null
)
```

### Allowed Exceptions

UI model/state files may exceed size limits when they mainly contain:

- Immutable state declarations
- Sealed hierarchies
- Configuration definitions

---

## UI Models (`ui/model/`)

UI models are separate from domain models.

Use UI models when:

- Formatting is required
- UI-specific fields are needed
- Domain models require transformation

Prefer explicit mapping functions.

```kotlin
fun BleDevice.toUiModel(): BleDeviceUiModel
```

---

# DI Layer (`di/`)

- Use Hilt modules for dependency wiring
- Use `@Binds` for interface bindings
- Scope dependencies according to ownership/lifecycle

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPairingRepository(
        impl: PairingRepositoryImpl
    ): PairingRepository
}
```

---

# Error Handling

- Repositories should expose typed errors or Result wrappers
- ViewModels transform errors into UI state
- Avoid swallowing exceptions
- Prefer sealed interfaces/classes for errors

```kotlin
sealed interface ScanError {
    data object BluetoothDisabled : ScanError
    data object MissingPermissions : ScanError
    data class Unknown(val throwable: Throwable) : ScanError
}
```

### DON'T

- Use empty catch blocks
- Throw generic Exception unnecessarily
- Leak Throwable directly into Compose

---

# Coroutine Dispatcher Rules

- Repositories own dispatcher selection
- ViewModels should not switch dispatchers for repository calls
- Inject dispatchers when needed for testing
- Avoid unnecessary `withContext(Dispatchers.IO)`

```kotlin
class BleRepositoryImpl @Inject constructor(
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher
)
```

---

# Code Size Guidelines

Prefer small focused classes and methods.

## Class Size

- Target: <= 200 lines per class/file
- Split large responsibilities into collaborators
- Avoid god objects
- Avoid giant repositories and ViewModels

## Method Size

- Target: <= 20 lines per method
- Extract branching and transformations into helpers
- Keep composables readable

## Exceptions

The following may exceed limits when justified:

- Immutable state/model declarations
- Sealed hierarchies
- DI modules
- Generated code

---

# Architecture Rules

## DO

- Keep dependencies flowing inward
- Use immutable UI state
- Prefer Flow/StateFlow over LiveData
- Keep Compose presentation-only
- Use lifecycle-aware collection APIs
- Keep BLE infrastructure centralized
- Use explicit mappers between layers
- Keep business logic outside UI

## DON'T

- Put Android imports in domain layer
- Access repositories directly from Compose
- Create screen-specific BLE infrastructure
- Use mutable shared UI state
- Create giant composables or ViewModels
- Put unrelated responsibilities in one repository

---

# Dependency Flow

```text
Compose UI
  -> ViewModel
    -> Repository
      -> Shared Infrastructure/Data Sources
        -> Android BLE / DataStore / Database
```

Dependencies always flow inward.

---

# Key Technologies

| Technology | Usage |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | UI toolkit |
| Hilt | Dependency injection |
| Coroutines/Flow | Async + reactive state |
| DataStore | Local persistence |
| Accompanist Permissions | Runtime permissions |
| JUnit5 + MockK | Testing |

---

# Testing Guidelines

- Test ViewModels with `kotlinx-coroutines-test`
- Prefer fake repositories over deep mocks
- Test repositories independently from Compose
- Keep business logic testable without Android framework
- Test Flow emissions deterministically

---

# Architecture Goal

The architecture should optimize for:

- Predictable state flow
- Testability
- BLE stability
- Maintainability
- Small focused classes
- Scalable Compose UI
- Clear ownership of responsibilities