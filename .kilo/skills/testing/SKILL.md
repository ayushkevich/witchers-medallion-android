---
name: testing
description: Unit testing guidelines for the Witchers Medallion Android app. Covers JUnit 5, MockK, Coroutines Test, Flow testing, fake implementations, and deterministic testing practices. Use when writing or reviewing test files.
origin: ECC
---

# Unit Testing Guidelines

Guidelines for writing deterministic, maintainable, and production-grade unit tests in the Witchers Medallion project.

Primary technologies:

- JUnit 5
- MockK
- kotlinx-coroutines-test
- Turbine (for Flow testing)

---

# When to Use

- Writing unit tests for ViewModels, repositories, or utilities
- Reviewing test quality and architecture
- Refactoring flaky or over-mocked tests
- Adding coroutine or Flow-based tests
- Creating reusable testing infrastructure

---

# Test Dependencies

From `app/build.gradle.kts`:

```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation("app.cash.turbine:turbine:<version>")
```

---

# Test Location Convention

Unit tests go under:

```text
app/src/test/java/by/alexy/witchersmedallion/
```

Mirror the source package structure:

```text
app/src/test/java/by/alexy/witchersmedallion/
  permissions/
    GetBlePermissionsTest.kt
  util/
    MacAddressUtilsTest.kt
  viewmodel/
    MainViewModelTest.kt
```

---

# Naming Conventions

## Test Files

Use:

```text
{Subject}Test.kt
```

Examples:

- `MainViewModelTest.kt`
- `BleScannerTest.kt`
- `MacAddressUtilsTest.kt`

---

## Test Methods

Use descriptive backtick names:

```kotlin
@Test
fun `scanDevices starts BLE scan when permissions are granted`() {
}
```

Prefer:

```text
should_doSomething_when_condition
```

or readable sentence-style naming.

---

# Test Structure

Use Arrange-Act-Assert (AAA):

```kotlin
@Test
fun `normalizeMacAddress returns uppercase without separators`() {
    // Arrange
    val input = "aa:bb:cc:dd:ee:ff"

    // Act
    val result = MacAddressUtils.normalizeMacAddress(input)

    // Assert
    assertEquals("AABBCCDDEEFF", result)
}
```

---

# Main Dispatcher Rule

Use a reusable `MainDispatcherRule` for coroutine/ViewModel tests.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

Usage:

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()
```

---

# Coroutines Testing

Use deterministic coroutine testing APIs.

```kotlin
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState emits connected state after connect`() = runTest {
        // Arrange
        val device = testBleDevice()

        // Act
        viewModel.connectToDevice(device)
        advanceUntilIdle()

        // Assert
        assertEquals(
            BleConnectionState.CONNECTED,
            viewModel.uiState.value.state
        )
    }
}
```

---

# Coroutines Test Rules

## DO

- Use `runTest`
- Use `advanceUntilIdle()`
- Use `advanceTimeBy()` for delay testing
- Keep tests deterministic
- Control coroutine execution explicitly

## DON'T

- Use `Thread.sleep()`
- Depend on real timing
- Launch uncontrolled background coroutines

---

# MockK Usage

MockK is the mocking framework.

```kotlin
class MainViewModelTest {

    private val bleRepository = mockk<BleRepository>()

    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setUp() {
        viewModel = MainViewModel(bleRepository)
    }

    @Test
    fun `scanDevices calls repository startScan`() = runTest {
        // Arrange
        coEvery {
            bleRepository.startScan(any())
        } returns Unit

        // Act
        viewModel.scanDevices()

        // Assert
        coVerify {
            bleRepository.startScan(any())
        }
    }
}
```

---

# Mocking Guidelines

Prefer real objects over mocks when possible.

## DO mock

- repositories
- network/data sources
- BLE infrastructure
- external collaborators
- Android framework wrappers

## DON'T mock

- data classes
- state objects
- collections
- value objects
- pure utility classes

---

# Relaxed Mocks

Prefer strict mocks by default.

Use:

```kotlin
mockk<Type>(relaxed = true)
```

only when:

- interactions are irrelevant
- default values are acceptable
- logging-style collaborators are used

Avoid excessive relaxed mocks.

---

# MockK Patterns

| Pattern | Usage |
|---|---|
| `mockk<Type>()` | Create strict mock |
| `mockk<Type>(relaxed = true)` | Create relaxed mock |
| `coEvery { ... } returns value` | Stub suspend function |
| `coVerify { ... }` | Verify suspend function |
| `every { ... } returns value` | Stub regular function |
| `verify { ... }` | Verify regular function |

---

# Static Mocking

Avoid `mockkStatic()` unless absolutely necessary.

Prefer:

- dependency injection
- instance-based collaborators
- pure functions

Static mocking is usually a design smell.

---

# Flow Testing

Prefer Turbine for testing Flow emissions.

```kotlin
@Test
fun `events emits navigation event`() = runTest {
    viewModel.events.test {

        viewModel.onBackClicked()

        assertEquals(
            MainEvent.NavigateBack,
            awaitItem()
        )
    }
}
```

---

# Parameterized Tests

Use parameterized tests for multiple input/output cases.

Prefer strongly typed arguments.

```kotlin
@ParameterizedTest
@MethodSource("macAddressTestCases")
fun `normalizeMacAddress formats correctly`(
    input: String,
    expected: String
) {
    assertEquals(
        expected,
        MacAddressUtils.normalizeMacAddress(input)
    )
}

companion object {

    @JvmStatic
    fun macAddressTestCases() = Stream.of(
        Arguments.of(
            "aa:bb:cc:dd:ee:ff",
            "AABBCCDDEEFF"
        ),
        Arguments.of(
            "AA-BB-CC-DD-EE-FF",
            "AABBCCDDEEFF"
        ),
        Arguments.of(
            "aabbccddeeff",
            "AABBCCDDEEFF"
        )
    )
}
```

---

# Test Data Builders

Prefer reusable builders/factories for test objects.

```kotlin
fun testBleDevice(
    address: String = "AA:BB:CC:DD:EE:FF",
    name: String = "Test Device",
    rssi: Int = -50
) = BleDevice(
    address = address,
    name = name,
    rssi = rssi
)
```

Avoid large inline object construction in tests.

---

# Testing ViewModels

When testing ViewModels:

- mock repositories/use cases
- assert observable state
- test success and failure paths
- test one-shot events
- use deterministic dispatchers

```kotlin
class MainViewModelTest {

    private val bleRepository = mockk<BleRepository>()

    private lateinit var viewModel: MainViewModel

    @BeforeEach
    fun setUp() {
        viewModel = MainViewModel(bleRepository)
    }

    @Test
    fun `onDeviceClick shows dialog with selected device`() {
        // Arrange
        val device = testBleDevice()

        // Act
        viewModel.onDeviceClick(device)

        // Assert
        assertEquals(
            device,
            viewModel.dialogState.value.selectedDevice
        )

        assertTrue(
            viewModel.dialogState.value.showDialog
        )
    }

    @Test
    fun `onConfirmConnect clears dialog and connects`() = runTest {
        // Arrange
        val device = testBleDevice()

        viewModel.onDeviceClick(device)

        coEvery {
            bleRepository.connectToDevice(device)
        } returns Unit

        // Act
        viewModel.onConfirmConnect()

        advanceUntilIdle()

        // Assert
        assertFalse(
            viewModel.dialogState.value.showDialog
        )

        coVerify {
            bleRepository.connectToDevice(device)
        }
    }
}
```

---

# Testing Repositories

Repository tests should verify:

- contract behavior
- Flow emissions
- error handling
- edge cases
- async behavior

```kotlin
class MedallionRepositoryLocalTest {

    private lateinit var repository: MedallionRepositoryLocal

    @BeforeEach
    fun setUp() {
        // Initialize repository with test dependencies
    }

    @Test
    fun `getCalibrationSettings returns null when no settings stored`() = runTest {
        val result = repository.getCalibrationSettings()

        assertNull(result)
    }

    @Test
    fun `setCalibrationSettings stores and retrieves settings`() = runTest {
        val settings = MedallionCalibrationSettings(
            minDistance = 1,
            maxDistance = 10,
            rssiThreshold = -70
        )

        repository.setCalibrationSettings(settings)

        assertEquals(
            settings,
            repository.getCalibrationSettings()
        )
    }
}
```

---

# Testing Utility Functions

Pure utility functions should:

- avoid mocking
- use parameterized tests
- test malformed/edge input
- remain deterministic

```kotlin
class MacAddressUtilsTest {

    @ParameterizedTest
    @MethodSource("normalizeTestCases")
    fun `normalizeMacAddress handles various formats`(
        input: String,
        expected: String
    ) {
        assertEquals(
            expected,
            MacAddressUtils.normalizeMacAddress(input)
        )
    }

    @Test
    fun `normalizeMacAddress handles null`() {
        assertNull(
            MacAddressUtils.normalizeMacAddress(null)
        )
    }

    @Test
    fun `normalizeMacAddress handles empty string`() {
        assertEquals(
            "",
            MacAddressUtils.normalizeMacAddress("")
        )
    }

    companion object {

        @JvmStatic
        fun normalizeTestCases() = Stream.of(
            Arguments.of(
                "aa:bb:cc:dd:ee:ff",
                "AABBCCDDEEFF"
            ),
            Arguments.of(
                "AA-BB-CC-DD-EE-FF",
                "AABBCCDDEEFF"
            ),
            Arguments.of(
                "aabbccddeeff",
                "AABBCCDDEEFF"
            )
        )
    }
}
```

---

# Unit Test Boundaries

Unit tests should:

- avoid Android framework dependencies
- avoid real databases/network
- execute quickly
- isolate a single behavior
- remain deterministic

Use instrumentation tests for:

- Compose UI testing
- Android integration behavior
- real Bluetooth integration
- database integration

Compose UI tests belong in:

```text
androidTest/
```

---

# Test Size Guidelines

- Prefer focused tests
- Avoid giant setup blocks
- Extract reusable builders/helpers
- Keep test methods concise
- Prefer readability over clever abstractions

---

# Testing Philosophy

Good tests should:

- validate observable behavior
- remain stable during refactoring
- avoid implementation coupling
- execute quickly
- clearly communicate intent

Prefer:

- state assertions
- deterministic execution
- fake implementations

Over:

- excessive mocking
- verify-heavy tests
- timing-based assertions

---

# Testing Rules

## DO

- Use `@BeforeEach` for shared setup
- Use descriptive test names
- Use `runTest`
- Use `coEvery` / `coVerify`
- Use Turbine for Flow testing
- Test edge cases
- Keep tests isolated
- Prefer fake implementations where appropriate

## DON'T

- Use `Thread.sleep()`
- Mock data classes
- Depend on Android framework internals
- Mix instrumentation and unit tests
- Assert implementation details
- Overuse relaxed mocks
- Create flaky timing-dependent tests