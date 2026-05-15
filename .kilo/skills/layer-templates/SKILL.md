---
name: layer-templates
description: Project-specific templates for creating new files in Witchers Medallion app. Covers UI screens, ViewModels, repositories, domain classes, DI bindings, and Compose components. Use when scaffolding new code.
origin: ECC
---

# Layer Templates

Ready-to-use templates for scaffolding new code in the Witchers Medallion project. Use these when adding new features to maintain consistency with the existing codebase.

## When to Use

- Adding a new screen, ViewModel, or repository
- Creating new domain entities or value objects
- Setting up dependency injection bindings
- Adding reusable Compose components
- Extending existing layers with new functionality

## Domain Entity Template

Location: `app/src/main/java/by/alexy/witchersmedallion/domain/{EntityName}.kt`

```kotlin
package by.alexy.witchersmedallion.domain

data class {EntityName}(
    val id: String,
    val name: String?,
    // additional fields...
)
```

Rules:
- Plain Kotlin data class, no Android/framework imports
- Use nullable types for optional fields
- Use default values where applicable
- No annotations (Hilt, etc.)

## Repository Interface Template

Location: `app/src/main/java/by/alexy/witchersmedallion/repository/{Feature}Repository.kt`

```kotlin
package by.alexy.witchersmedallion.repository

interface {Feature}Repository {

    val {feature}State: StateFlow<{StateType}>

    suspend fun {operation}(param: {ParamType})

    suspend fun {queryOperation}(): {ReturnType}?
}
```

Rules:
- Interface defines the contract, no implementation details
- I/O operations are `suspend` functions
- State is exposed via `StateFlow`
- No Android imports in the interface

## Repository Implementation Template

Location: `app/src/main/java/by/alexy/witchersmedallion/repository/{feature}/impl/{Feature}RepositoryImpl.kt`

```kotlin
package by.alexy.witchersmedallion.repository.{feature}.impl

import by.alexy.witchersmedallion.repository.{Feature}Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class {Feature}RepositoryImpl @Inject constructor(
    // dependencies: data store, context, other services
) : {Feature}Repository {

    private val _{feature}State = MutableStateFlow({DefaultState})
    override val {feature}State: StateFlow<{StateType}> = _{feature}State.asStateFlow()

    override suspend fun {operation}(param: {ParamType}) {
        // implementation
    }
}
```

Rules:
- Implementation in `impl/` subdirectory under `repository/` or `repository/{feature}/impl/`
- Use `MutableStateFlow` internally, expose as `StateFlow`
- Constructor uses `@Inject` for Hilt injection
- Keep implementation details hidden behind the interface

## ViewModel Template

Location: `app/src/main/java/by/alexy/witchersmedallion/viewmodel/{Feature}ViewModel.kt`

```kotlin
package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.repository.{Feature}Repository
import by.alexy.witchersmedallion.ui.state.{Feature}UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class {Feature}ViewModel @Inject constructor(
    private val {feature}Repository: {Feature}Repository
) : ViewModel() {

    private val _{feature}State = MutableStateFlow({Feature}UiState())
    val {feature}State: StateFlow<{Feature}UiState> = _{feature}State.asStateFlow()

    fun {action}(param: {ParamType}) {
        viewModelScope.launch {
            try {
                {feature}Repository.{operation}(param)
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
```

Rules:
- Must have `@HiltViewModel` and `@Inject` constructor
- Use `viewModelScope` for coroutines
- State exposed via `StateFlow`
- No Compose imports
- Inject repositories, not individual data sources

## UI Screen Template

Location: `app/src/main/java/by/alexy/witchersmedallion/ui/screen/{Feature}Screen.kt`

```kotlin
package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.viewmodel.{Feature}ViewModel

@Composable
fun {Feature}Screen(viewModel: {Feature}ViewModel) {
    val uiState by viewModel.{feature}State.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // UI elements based on uiState
        // Actions call viewModel.{action}()
    }
}
```

Rules:
- Receives ViewModel as the only parameter
- Collects state with `collectAsState()`
- No business logic; delegate to ViewModel
- Use `stringResource` for all user-facing strings
- Padding of 16.dp as standard

## UI State Template

Location: `app/src/main/java/by/alexy/witchersmedallion/ui/state/{Feature}UiState.kt`

```kotlin
package by.alexy.witchersmedallion.ui.state

data class {Feature}UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: {DataType}? = null
)
```

Rules:
- Plain data class with default values
- Include loading, error, and data states
- Keep screen-specific; don't share between screens

## DI Binding Template

Location: `app/src/main/java/by/alexy/witchersmedallion/di/{ModuleName}.kt`

```kotlin
package by.alexy.witchersmedallion.di

import by.alexy.witchersmedallion.repository.{Feature}Repository
import by.alexy.witchersmedallion.repository.{feature}.impl.{Feature}RepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class {ModuleName} {
    @Binds
    abstract fun bind{Feature}Repository(
        impl: {Feature}RepositoryImpl
    ): {Feature}Repository
}
```

Rules:
- Co-locate related bindings in one module
- Use `@InstallIn(ViewModelComponent::class)`
- Use `@Binds` abstract methods for interface bindings
- Follow naming convention: if binding is the only one, name the module `{Feature}Module`; otherwise use a logical grouping

## Compose Component Template

Location: `app/src/main/java/by/alexy/witchersmedallion/ui/screen/component/{ComponentName}.kt`

```kotlin
package by.alexy.witchersmedallion.ui.screen.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun {ComponentName}(
    modifier: Modifier = Modifier,
    // parameters...
) {
    // reusable composable logic
}
```

Rules:
- Place in `ui/screen/component/` directory
- Accept `Modifier` as first parameter with default
- Keep components focused on a single visual concern
- No ViewModel dependencies (pass data via parameters)

## Adding a New Feature: Checklist

When adding a new feature (e.g., "Notifications"), follow this order:

1. [ ] Create domain entity: `domain/NotificationEntity.kt`
2. [ ] Create repository interface: `repository/NotificationRepository.kt`
3. [ ] Create repository implementation: `repository/notifications/impl/NotificationRepositoryImpl.kt`
4. [ ] Create UI state: `ui/state/NotificationUiState.kt`
5. [ ] Create ViewModel: `viewmodel/NotificationViewModel.kt`
6. [ ] Add DI binding in `di/ViewModelModule.kt` or new module
7. [ ] Create UI screen: `ui/screen/NotificationScreen.kt`
8. [ ] Add navigation to screen (if applicable)
9. [ ] Write unit tests: `app/src/test/java/by/alexy/witchersmedallion/{feature}/*Test.kt`

## File Naming Conventions

| Layer | Convention | Example |
|-------|-----------|---------|
| Domain | PascalCase data class | `BleDevice`, `MedallionCalibrationSettings` |
| Repository interface | PascalCase + `Repository` suffix | `BleRepository`, `MedallionRepository` |
| Repository impl | PascalCase + `Impl` suffix | `BleRepositoryImpl`, `MedallionRepositoryLocal` |
| ViewModel | PascalCase + `ViewModel` suffix | `MainViewModel`, `CalibrationViewModel` |
| UI screen | PascalCase + `Screen` suffix | `MainScreen`, `CalibrationScreen` |
| UI state | PascalCase + `UiState` suffix | `MainUiState`, `CalibrationUiState` |
| Compose component | PascalCase | `ValueWithLabel`, `MacDeviceCardComponent` |
| Test | PascalCase + `Test` suffix | `MainViewModelTest`, `MacAddressUtilsTest` |
| DI module | PascalCase + `Module` suffix | `ViewModelModule` |
