---
name: ble
description: BLE scanning, GATT communication, connection lifecycle, scan management, and operation serialization patterns for Witchers Medallion app. Use when writing, reviewing, or refactoring BLE-related code in repository layer.
origin: AI-generated
---

# BLE Communication Patterns

Project-specific BLE conventions for Witchers Medallion. Covers scanning lifecycle, GATT operation queue, connection management, device discovery, and BLE error handling.

## When to Use

- Writing or modifying BLE repository code
- Adding new GATT operations (read, write, notifications)
- Modifying scan configuration or filters
- Handling BLE connection lifecycle events
- Implementing BLE protocol parsing/serialization

## When NOT to Use

- Compose UI code (use `android-composable` skill)
- BLE permission handling (use `android-kotlin` skill)
- High-level BLE business logic in ViewModels

---

# Architecture Overview

```text
ViewModel (MainViewModel, MacTrackingViewModel)
  -> BleRepository (interface)
    -> BleRepositoryImpl (singleton, shared)
      -> Android BLE API (BluetoothAdapter, BluetoothLeScanner, BluetoothGatt)
```

### Key Principles

- **Singleton**: `BleRepositoryImpl` is a `@Singleton` shared between all ViewModels
- **Shared scan lifecycle**: Multiple screens can request scanning simultaneously
- **GATT operation serialization**: All GATT operations go through a Channel-based queue
- **Flow-based state**: All BLE state exposed as `Flow` / `StateFlow`
- **Mutex-protected connections**: Connection/disconnection is serialized

---

# BLE Repository Interface

```kotlin
interface BleRepository {
    // State flows
    val discoveredDevices: Flow<List<BleDevice>>
    val connectionState: Flow<BleConnectionState>
    val scanningInProgress: Flow<Boolean>
    val connectedDeviceName: Flow<String?>
    val scanError: Flow<BleScanError?>
    val statusUpdates: Flow<StatusUpdate>

    // Lifecycle
    fun startScan(config: BleScanConfig = BleScanConfig())
    fun stopScan()
    fun connectToDevice(device: BleDevice)
    fun disconnect()
    fun clear()

    // GATT operations (suspend)
    suspend fun readCharacteristic(uuid: UUID): ByteArray?
    suspend fun writeCharacteristic(uuid: UUID, value: ByteArray): Boolean
    suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean
    suspend fun requestMtu(mtu: Int): Int
}
```

### Rules

- All state is exposed as `Flow` (read-only)
- All GATT operations are `suspend` functions
- Lifecycle methods (`startScan`, `connectToDevice`) are **not** suspend -- they launch internally
- Interface has no Android imports beyond what's necessary for GATT types

---

# Scan Management

### Multi-Client Scan Ownership

```kotlin
// Client counting pattern
private var activeScanClients: Int = 0
private var currentScanConfig: BleScanConfig? = null

override fun startScan(config: BleScanConfig) {
    activeScanClients++
    currentScanConfig = config

    val isFirstClient = activeScanClients == 1

    if (isFirstClient) {
        _scanningInProgress.update { true }
        // Actually start BLE scan
        bluetoothLeScanner?.startScan(scanFilters, settings, scanCallback)
        // Start stale device cleanup
        startCleanupJob()
        // Auto-stop after duration
        if (config.scanDurationMs > 0) {
            scanJob = repositoryScope.launch {
                delay(config.scanDurationMs)
                stopScan()
            }
        }
    }
}

override fun stopScan() {
    if (activeScanClients <= 0) return
    activeScanClients--

    if (activeScanClients <= 0) {
        bluetoothLeScanner?.stopScan(scanCallback)
        _scanningInProgress.update { false }
        scanJob?.cancel()
        cleanupJob?.cancel()
        currentScanConfig = null
        activeScanClients = 0
    }
}
```

### Scan Configuration

```kotlin
data class BleScanConfig(
    val minRssi: Int = Int.MIN_VALUE,
    val scanDurationMs: Long = 30_000L,
)
```

### Scan Filters

```kotlin
val scanFilters = listOf(
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(BleConfig.SERVICE_UUID))
        .build(),
)

val settings = ScanSettings.Builder()
    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
    .build()
```

### DO

- Use client counting for shared scan ownership
- Always check `activeScanClients` before stopping scan
- Set `SCAN_MODE_LOW_LATENCY` for discovery
- Use service UUID filters to reduce noise
- Clean up stale devices periodically (every 15s, timeout 30s)

### DON'T

- Stop scan unconditionally (breaks multi-client pattern)
- Create separate BLE scanners per screen
- Scan without UUID filters (battery drain)
- Assume only one screen requests scanning

---

# GATT Operation Queue

### Architecture

All GATT operations are serialized through a `Channel<GattOperation>` with a single consumer coroutine:

```kotlin
private sealed class GattOperation {
    data class Read(val uuid: UUID, val deferred: CompletableDeferred<ByteArray?>) : GattOperation()
    data class Write(val uuid: UUID, val value: ByteArray, val deferred: CompletableDeferred<Boolean>) : GattOperation()
    data class WriteDescriptor(val descriptor: BluetoothGattDescriptor, val value: ByteArray, val deferred: CompletableDeferred<Boolean>) : GattOperation()
    data class RequestMtu(val mtu: Int, val deferred: CompletableDeferred<Int>) : GattOperation()
}

private val gattOps = Channel<GattOperation>(Channel.UNLIMITED)
```

### Operation Execution

```kotlin
private fun startGattOpConsumer() {
    gattOpConsumerJob = repositoryScope.launch {
        for (op in gattOps) {
            try {
                withTimeout(GATT_OP_TIMEOUT_MS) { // 5000ms
                    when (op) {
                        is GattOperation.Read -> executeRead(op)
                        is GattOperation.Write -> executeWrite(op)
                        is GattOperation.WriteDescriptor -> executeWriteDescriptor(op)
                        is GattOperation.RequestMtu -> executeRequestMtu(op)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // Complete deferred with failure values on timeout
                when (op) {
                    is GattOperation.Read -> op.deferred.complete(null)
                    is GattOperation.Write -> op.deferred.complete(false)
                    is GattOperation.WriteDescriptor -> op.deferred.complete(false)
                    is GattOperation.RequestMtu -> op.deferred.complete(0)
                }
            }
        }
    }
}
```

### Suspend API

```kotlin
override suspend fun readCharacteristic(uuid: UUID): ByteArray? {
    val deferred = CompletableDeferred<ByteArray?>()
    gattOps.send(GattOperation.Read(uuid, deferred))
    return deferred.await()
}
```

### Constants

```kotlin
private const val GATT_OP_TIMEOUT_MS = 5_000L
```

### DO

- Route all GATT operations through the Channel queue
- Use `CompletableDeferred` for async result delivery
- Handle timeout with specific failure values per operation type
- Use `withTimeout()` around operation execution

### DON'T

- Call `gatt.readCharacteristic()` directly from ViewModels
- Execute multiple GATT operations concurrently (race conditions)
- Forget to handle timeout cases
- Use blocking BLE calls on Main thread

---

# Connection Management

### Connection Flow

```
CONNECTING -> (30s timeout) -> CONNECTED -> DISCONNECTED
                  |                              ^
                  v                              |
            DISCONNECTED (auto-timeout)          |
```

### Mutex Protection

```kotlin
private val connectionMutex = Mutex()

override fun connectToDevice(device: BleDevice) {
    connectionJob?.cancel()
    _connectionState.update { BleConnectionState.CONNECTING }

    connectionJob = repositoryScope.launch {
        connectionMutex.withLock {
            val gatt = bleDevice.connectGatt(context, false, connectCallback)
            bluetoothGatt = gatt
            // Start timeout
            connectionTimeoutJob = repositoryScope.launch {
                delay(CONNECT_TIMEOUT_MS) // 30s
                if (_connectionState.value == BleConnectionState.CONNECTING) {
                    disconnectInternal()
                }
            }
        }
    }
}
```

### GATT Callback

```kotlin
private val connectCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        repositoryScope.launch {
            connectionMutex.withLock {
                when (newState) {
                    STATE_CONNECTED -> {
                        _connectionState.update { BleConnectionState.CONNECTED }
                        gatt?.discoverServices()
                    }
                    STATE_DISCONNECTED -> {
                        _connectionState.update { BleConnectionState.DISCONNECTED }
                        drainPendingGattOps()
                    }
                }
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) return
        // Request MTU 64, then subscribe to status notifications
    }
}
```

### Disconnect

```kotlin
private fun disconnectInternal() {
    connectionJob?.cancel()
    connectionTimeoutJob?.cancel()
    gattOpConsumerJob?.cancel()
    gattOps.close()

    bluetoothGatt?.let { gatt ->
        try {
            gatt.disconnect()
            gatt.close()
        } catch (_: Exception) {}
    }

    bluetoothGatt = null
    connectedDevice = null
    _connectedDeviceName.update { null }
    _connectionState.update { BleConnectionState.DISCONNECTED }
}
```

### DO

- Use `connectionMutex` to serialize connect/disconnect
- Set 30s connection timeout
- Call `discoverServices()` after successful connection
- Drain pending GATT ops on disconnect
- Cancel all jobs before disconnect

### DON'T

- Connect to multiple devices simultaneously
- Skip `discoverServices()` after connection
- Forget to close `BluetoothGatt` on disconnect
- Store `BluetoothGatt` references in ViewModels

---

# Device Discovery

### Scan Callback

```kotlin
private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val config = currentScanConfig ?: return
        val device = result.device
        val address = device.address
        val name = device.name.takeIf { it?.isNotBlank() == true }

        if (result.rssi < config.minRssi) return

        repositoryScope.launch {
            deviceMutex.withLock {
                _discoveredDevices.update { devices ->
                    val existingDevice = devices[address]
                    val updatedDevice = if (existingDevice != null) {
                        if (name != null && name != existingDevice.name) {
                            existingDevice.copy(name = name)
                        } else existingDevice
                    } else {
                        BleDevice(address, name, result.rssi, Instant.now())
                    }
                    if (updatedDevice == existingDevice) devices
                    else devices + (address to updatedDevice)
                }
            }
        }
    }
}
```

### Stale Device Cleanup

```kotlin
private const val DEVICE_TIMEOUT_SECONDS = 30L
private const val CLEANUP_INTERVAL_MS = 15_000L

private fun cleanupStaleDevices() {
    val cutoff = Instant.now().minus(DEVICE_TIMEOUT_SECONDS, ChronoUnit.SECONDS)
    _discoveredDevices.update { devices ->
        devices.filterValues { it.lastSeenAt.isAfter(cutoff) }
    }
}
```

### DO

- Use `deviceMutex` to protect device map mutations
- Filter by `minRssi` from scan config
- Update device name only if non-blank and changed
- Clean up devices not seen in 30 seconds

---

# BLE Thread Model

```kotlin
private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

All BLE callbacks run on `Dispatchers.Main` via `repositoryScope`.

### DO

- Use `repositoryScope.launch` in all BLE callbacks
- Use `SupervisorJob()` for independent coroutine failure handling
- Use `Mutex.withLock` for shared state protection

---

# BLE Protocol: Calibration

### 12-Byte Calibration Frame

```
[Version: 2B LE] [Cold: 2B LE] [Warm: 2B LE] [Hot: 2B LE] [CRC32: 4B]
```

### Byte Parsing (Little-Endian)

```kotlin
private fun readUint16LE(bytes: ByteArray, offset: Int): Int {
    val low = bytes[offset].toInt() and 0xFF
    val high = bytes[offset + 1].toInt() and 0xFF
    return low or (high shl 8)
}

private fun readInt16LE(bytes: ByteArray, offset: Int): Int {
    val low = bytes[offset].toInt() and 0xFF
    val high = bytes[offset + 1].toInt() and 0xFF
    return ((low or (high shl 8)).toShort()).toInt()
}

private fun readUint32LE(bytes: ByteArray, offset: Int): Int {
    val b0 = bytes[offset].toInt() and 0xFF
    val b1 = bytes[offset + 1].toInt() and 0xFF
    val b2 = bytes[offset + 2].toInt() and 0xFF
    val b3 = bytes[offset + 3].toInt() and 0xFF
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}
```

### CRC32 ISO 3309

```kotlin
private fun computeCrc32Iso3309(data: ByteArray, len: Int): Int {
    var crc = -1
    val polynomial = 0xEDB88320.toInt()
    for (i in 0 until len) {
        crc = crc xor (data[i].toInt() and 0xFF)
        for (j in 0 until 8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor polynomial else crc ushr 1
        }
    }
    return crc xor -1
}
```

### Validation Rules

1. Version must equal 1
2. Threshold ordering: cold <= warm <= hot
3. CRC32 must match computed value
4. Total frame size must be 12 bytes

### DO

- Validate version before parsing data
- Check threshold ordering (cold <= warm <= hot)
- Verify CRC32 before accepting calibration data
- Use `withContext(Dispatchers.IO)` for BLE read/write in repository layer

---

# BLE UUID Configuration

```kotlin
object BleConfig {
    val SERVICE_UUID: UUID = UUID.fromString("9e5b4cb7-6ccc-4027-a496-ac7c01bbf706")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("d3481eab-e4cf-49f7-9d3e-36a2b7dff88e")
    val CALIBRATION_UUID: UUID = UUID.fromString("a1b2c3d4-aaaa-bbbb-cccc-ddddeeeeffff")
    val STATUS_UUID: UUID = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556666")
    val PROTOCOL_VERSION_UUID: UUID = UUID.fromString("a1b2c3d4-7777-8888-9999-aaaaaaaaaaaa")
    val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
```

### DO

- Define all UUIDs in `BleConfig` object
- Reference UUIDs via `BleConfig.*` throughout codebase
- Use `BleConfig.CCCD_DESCRIPTOR_UUID` for notification subscription

---

# BLE Error Handling

### Sealed Error Hierarchy

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

### DO

- Use typed errors via sealed hierarchy
- Map GATT status codes to specific error types
- Include operation context in timeout errors

---

# BLE Permissions

```kotlin
fun getBlePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETO_CONNECT,
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```

### DO

- Check SDK version for correct permission set
- Use Accompanist `rememberMultiplePermissionsState` in Compose
- Gate scan/connection behind permission check

---

# Automatic Notification Subscription

After services are discovered, automatically:

1. Request MTU 64
2. Subscribe to `STATUS_UUID` notifications

```kotlin
override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    if (status != BluetoothGatt.GATT_SUCCESS) return

    repositoryScope.launch {
        val deferred = CompletableDeferred<Int>()
        gattOps.send(GattOperation.RequestMtu(64, deferred))
        deferred.invokeOnCompletion {
            if (it == null) {
                repositoryScope.launch { subscribeToStatusNotifications() }
            }
        }
    }
}

private suspend fun subscribeToStatusNotifications() {
    val gatt = bluetoothGatt ?: return
    val service = gatt.getService(BleConfig.SERVICE_UUID) ?: return
    val statusChar = service.getCharacteristic(BleConfig.STATUS_UUID) ?: return

    gatt.setCharacteristicNotification(statusChar, true)
    val cccd = statusChar.getDescriptor(BleConfig.CCCD_DESCRIPTOR_UUID) ?: return

    val deferred = CompletableDeferred<Boolean>()
    gattOps.send(GattOperation.WriteDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, deferred))
}
```

---

# Status Update Parsing

```kotlin
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
    if (characteristic.uuid == BleConfig.STATUS_UUID && characteristic.value?.size == 2) {
        val state = characteristic.value[0].toInt() and 0xFF
        val rssi = characteristic.value[1].toInt().toByte().toInt()
        repositoryScope.launch {
            _statusUpdates.emit(StatusUpdate(state, rssi))
        }
    }
}
```

---

# Detection Heuristics

- Check for direct `BluetoothGatt` usage outside `BleRepositoryImpl`
- Check for multiple BLE scanner instances
- Check for missing `connectionMutex` usage in connect/disconnect
- Check for hardcoded UUIDs outside `BleConfig`
- Check for BLE operations on Main thread without `repositoryScope`
- Check for missing timeout handling on GATT operations
- Check for scan without service UUID filter
- Check for `gatt.close()` missing on disconnect

---

# Related Skills

- `architecture` -- repository layer responsibilities
- `android-kotlin` -- Flow, Coroutines, sealed hierarchies
- `android-composable` -- BLE permission handling in UI
