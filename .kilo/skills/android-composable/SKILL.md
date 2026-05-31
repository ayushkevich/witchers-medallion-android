---
name: android-composable
description: Jetpack Compose conventions for Witchers Medallion app. Covers screen structure, reusable components, theming, dialog state, navigation, and localization in Compose UI. Use when writing, reviewing, or refactoring Compose UI code.
origin: AI-generated
---

# Jetpack Compose Conventions

Project-specific Compose conventions for Witchers Medallion. Covers screen structure, reusable components, theming, dialog state management, navigation, and localization.

## When to Use

- Writing new Compose screens or reusable components
- Reviewing Compose UI code for convention compliance
- Refactoring existing Compose code
- Adding new dialogs, navigation, or UI state

## When NOT to Use

- Kotlin business logic (use `android-kotlin` skill)
- Repository or ViewModel code

---

# Screen Structure

### Entry Point Pattern

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // UI rendering
    }
}
```

### Rules

- Screens receive ViewModel as a parameter
- Screens collect state using `collectAsState()`
- Screens forward user actions to ViewModel methods
- Screens contain no business logic
- Screens are located in `ui/screen/`

### DO

```kotlin
@Composable
fun CalibrationScreen(viewModel: CalibrationViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Present uiState
    }
}
```

### DON'T

```kotlin
// Wrong: calling repository directly from Compose
@Composable
fun BadScreen() {
    val repository: BleRepository = inject() // Never do this
    // ...
}

// Wrong: business logic inside composable
@Composable
fun BadScreen(viewModel: MainViewModel) {
    val devices = viewModel.uiState.collectAsState()
    val sorted = devices.value.availableDevices.sortedBy { it.rssi } // Logic belongs in ViewModel
}
```

---

# Navigation

Project uses `HorizontalPager` + `TabRow` -- **no Navigation Compose**.

```kotlin
@Composable
fun SwipeableTabs(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    macTrackingViewModel: MacTrackingViewModel,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) },
                    )
                }
            }

            HorizontalPager(state = pagerState) { currentPage ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentPage) {
                        0 -> MainScreen(mainViewModel)
                        1 -> CalibrationScreen(calibrationViewModel)
                        2 -> MacTrackingScreen(macTrackingViewModel)
                    }
                }
            }
        }
    }
}
```

### Rules

- Use `rememberPagerState(pageCount = { N })` for tab count
- Use `animateScrollToPage` for smooth transitions
- Pass ViewModels from `MainActivity` through the navigation container
- Use `Scaffold` with `WindowInsets.systemBars` for proper inset handling

---

# Dialog State Management

Ephemeral dialog state belongs in the **UI layer**, not in ViewModels.

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showConnectDialog by remember { mutableStateOf(false) }
    var selectedDeviceForConnect by remember { mutableStateOf<BleDevice?>(null) }

    // ...

    if (showConnectDialog && selectedDeviceForConnect != null) {
        ConnectionConfirmationDialog(
            device = selectedDeviceForConnect!!,
            onConfirm = {
                viewModel.connectToDevice(selectedDeviceForConnect!!)
                showConnectDialog = false
                selectedDeviceForConnect = null
            },
            onDismiss = {
                showConnectDialog = false
                selectedDeviceForConnect = null
            },
        )
    }
}
```

### DO

- Use `remember { mutableStateOf(...) }` for dialog visibility
- Store dialog-related data in local `remember` variables
- Clear dialog state in both `onConfirm` and `onDismiss`
- Use `AlertDialog` for error dialogs (tied to ViewModel error state)

### DON'T

```kotlin
// Wrong: storing dialog state in ViewModel
// In MacTrackingUiState: val showAddDialog: Boolean = false // Never do this

// Wrong: using DialogState data class that is never consumed
// ui/state/DialogState.kt exists but is unused
```

---

# Reusable Components

Components go in `ui/screen/component/`, organized by feature subdirectory.

### Component Signature Pattern

```kotlin
@Composable
fun MacDeviceCardComponent(
    device: MacDevice,
    isDynamic: Boolean,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        // Component content
    }
}
```

### Localization in Components

**Preferred**: Accept composable label lambdas for localized text

```kotlin
// Good: component accepts composable for label
@Composable
fun SliderWithLabelInt(
    label: @Composable () -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Text(stringResource(R.string.slider_label_format, label().toString(), value))
        Slider(value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = -100f..0f, steps = 100)
    }
}
```

**Usage**:

```kotlin
SliderWithLabelInt(
    label = { Text(stringResource(R.string.cold_warm)) },
    value = coldRssi,
    onValueChange = { viewModel.updateColdRssi(it) },
)
```

### Known Anti-Pattern (should be fixed)

```kotlin
// Current: ValueWithLabel takes raw String parameters
// This limits reusability since caller must localize before passing
@Composable
fun ValueWithLabel(label: String, value: String) { ... }

// Better: accept composable labels
// @Composable
// fun ValueWithLabel(label: @Composable () -> Unit, value: @Composable () -> Unit) { ... }
```

### Component Organization

```
ui/screen/component/
  ValueWithLabel.kt              # Generic label-value row
  SliderWithLabelInt.kt          # Slider with localized label
  ConnectionConfirmationDialog.kt # Reusable dialog
  mac/
    MacDeviceCardComponent.kt    # MAC device card
  calibration/
    AutoCalibrationComponent.kt  # Auto-calibration UI
    ManualCalibrationComponent.kt # Manual calibration sliders
```

### DO

- Accept data objects (not raw strings) as parameters
- Accept composable callbacks for actions (`onConfirm`, `onDismiss`, `onRemove`)
- Keep components focused on single visual element
- Use `stringResource()` inside components for their own text

### DON'T

- Pass raw String parameters for text that needs localization
- Embed `stringResource()` calls inside component logic (formatting)
- Create components larger than 100 lines
- Put business logic inside components

---

# UI State in Compose

### Collecting State

```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val isConnected = uiState.state == BleConnectionState.CONNECTED
    val devices = uiState.availableDevices
}
```

### Error Display

```kotlin
// For UiText-based errors (MacTrackingScreen)
uiState.errorMessage?.let { error ->
    AlertDialog(
        onDismissRequest = { viewModel.clearError() },
        title = { Text(stringResource(R.string.error)) },
        text = { Text(error.getString(LocalContext.current)) },
        confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text(stringResource(R.string.ok)) } },
    )
}

// For String? errors (CalibrationScreen)
uiState.errorMessage?.let { error ->
    AlertDialog(
        onDismissRequest = { viewModel.clearError() },
        title = { Text(stringResource(R.string.error)) },
        text = { Text(error) },
        confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text(stringResource(R.string.ok)) } },
    )
}
```

### Loading State

```kotlin
if (uiState.isLoading) {
    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
}
```

---

# Theming

### Theme Entry Point

```kotlin
@Composable
fun WitchersMedallionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BootstrapColorScheme,
        typography = Typography,
        content = content,
    )
}
```

### Dark Theme Colors (Bootstrap-inspired)

| Token | Value |
|---|---|
| Primary | `#0D6EFD` |
| Secondary | `#6C757D` |
| Background | `#212529` |
| Surface | `#212529` |
| On Primary/Secondary/Background/Surface | `#F8F9FA` |

### Using Theme Colors

```kotlin
// Prefer MaterialTheme.colorScheme tokens
color = MaterialTheme.colorScheme.primary
color = MaterialTheme.colorScheme.surfaceVariant

// For dynamic colors (RSSI, etc.) use utility objects
color = RssiColorUtils.getRssiColor(device.rssi, MaterialTheme.colorScheme)
```

### Typography

```kotlin
Text(text = stringResource(R.string.device_name), style = MaterialTheme.typography.labelMedium)
Text(text = device.mac, style = MaterialTheme.typography.bodyLarge)
```

---

# Localization

### Rules

- **Every** user-visible string MUST use `stringResource(R.string.xxx)`
- Never write hardcoded strings in Compose: `Text("Error")` is forbidden
- All strings must exist in BOTH `values/strings.xml` and `values-ru/strings.xml`
- Use format placeholders: `%1$s`, `%1$d`, `%1$d dBm`

### Format Strings

```xml
<!-- strings.xml -->
<string name="connected_device">Device is connected: %1$s</string>
<string name="rssi_unit">%1$d dBm</string>
<string name="slider_label_format">%1$s: %2$d dBm</string>
```

```kotlin
Text(text = stringResource(R.string.connected_device, uiState.connectedDeviceName!!))
Text(text = stringResource(R.string.rssi_unit, device.rssi))
```

### UiText for Cross-Layer Localization

```kotlin
// ViewModel sets UiText error
_uiState.update { it.copy(errorMessage = UiText.fromStringResource(R.string.error_loading_macs, e.message ?: "")) }

// Compose resolves it
Text(uiState.errorMessage!!.getString(LocalContext.current))
```

---

# Layout Conventions

### Common Modifiers

```kotlin
Modifier.fillMaxSize()
Modifier.fillMaxWidth()
Modifier.padding(16.dp)
Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
Modifier.weight(1f)
```

### Common Layouts

| Use Case | Layout |
|---|---|
| Vertical stack | `Column` |
| Horizontal row | `Row` |
| List of items | `LazyColumn` + `items` |
| Tab navigation | `Scaffold` + `TabRow` + `HorizontalPager` |
| Card content | `Card` + `Column` |
| Centered content | `Box` + `align(Alignment.CenterHorizontally)` |

### Spacing

- Use `8.dp` increments: `4.dp`, `8.dp`, `16.dp`, `24.dp`
- Use `Spacer(modifier = Modifier.height(16.dp))` between sections
- Use `Arrangement.SpaceBetween` for aligned rows

---

# Permissions in Compose

```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = getBlePermissions(),
    )

    Button(onClick = {
        if (permissionsState.allPermissionsGranted) {
            viewModel.scanDevices()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }) {
        Text(stringResource(R.string.scan_devices))
    }
}
```

---

# Haptic Feedback

```kotlin
val view = LocalView.current
Row(modifier = Modifier.clickable {
    if (!view.isInEditMode) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    // action
})
```

---

# Detection Heuristics

- Check for hardcoded strings in `Text(...)`, `Button(...)`, `AlertDialog(...)`
- Check for `LiveData` usage -- should be `StateFlow` + `collectAsState()`
- Check for business logic inside composables (sorting, validation, API calls)
- Check for dialog state stored in ViewModel UI state classes
- Check for missing `stringResource()` calls
- Check for reusable components that accept raw String instead of `@Composable () -> Unit`
- Check for giant composables (>150 lines) -- should extract sub-components

---

# Related Skills

- `architecture` -- layer boundaries, ViewModel responsibilities
- `android-kotlin` -- naming, Flow, DI, error handling
- `BLE` -- BLE communication patterns used in UI
