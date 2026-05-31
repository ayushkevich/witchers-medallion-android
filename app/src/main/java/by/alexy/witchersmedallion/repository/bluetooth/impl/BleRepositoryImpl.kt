package by.alexy.witchersmedallion.repository.bluetooth.impl

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import by.alexy.witchersmedallion.config.BleConfig
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.BleScanConfig
import by.alexy.witchersmedallion.domain.BleScanError
import by.alexy.witchersmedallion.domain.StatusUpdate
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BleRepository {
    private val _discoveredDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    override val discoveredDevices: Flow<List<BleDevice>> = _discoveredDevices
        .map { devices -> devices.values.toList() }

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: Flow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scanningInProgress = MutableStateFlow(false)
    override val scanningInProgress: Flow<Boolean> = _scanningInProgress.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: Flow<String?> = _connectedDeviceName.asStateFlow()

    private val _scanError = MutableStateFlow<BleScanError?>(null)
    override val scanError: Flow<BleScanError?> = _scanError.asStateFlow()

    private val _statusUpdates = MutableSharedFlow<StatusUpdate>(replay = 0)
    override val statusUpdates: Flow<StatusUpdate> = _statusUpdates

    private val bluetoothAdapter: BluetoothAdapter?
    private val bluetoothLeScanner: BluetoothLeScanner?

    private var activeScanClients: Int = 0
    private var currentScanConfig: BleScanConfig? = null
    private var connectedDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private var cleanupJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var gattOpConsumerJob: Job? = null

    private val deviceMutex = Mutex()
    private val connectionMutex = Mutex()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private sealed class GattOperation {
        data class Read(val uuid: UUID, val deferred: kotlinx.coroutines.CompletableDeferred<ByteArray?>) : GattOperation()
        data class Write(val uuid: UUID, val value: ByteArray, val deferred: kotlinx.coroutines.CompletableDeferred<Boolean>) : GattOperation()
        data class WriteDescriptor(val descriptor: BluetoothGattDescriptor, val value: ByteArray, val deferred: kotlinx.coroutines.CompletableDeferred<Boolean>) : GattOperation()
        data class RequestMtu(val mtu: Int, val deferred: kotlinx.coroutines.CompletableDeferred<Int>) : GattOperation()
    }

    private val gattOps = Channel<GattOperation>(Channel.UNLIMITED)

    @Volatile
    private var activeReadOp: GattOperation.Read? = null

    @Volatile
    private var activeWriteOp: GattOperation.Write? = null

    @Volatile
    private var activeDescriptorOp: GattOperation.WriteDescriptor? = null

    @Volatile
    private var activeMtuOp: GattOperation.RequestMtu? = null

    @Volatile
    private var gattOpsClosed: Boolean = false

    init {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        startGattOpConsumer()
    }

    private fun startGattOpConsumer() {
        gattOpConsumerJob = repositoryScope.launch {
            for (op in gattOps) {
                try {
                    withTimeout(BleConfig.GATT_OP_TIMEOUT_MS) {
                        when (op) {
                            is GattOperation.Read -> executeRead(op)
                            is GattOperation.Write -> executeWrite(op)
                            is GattOperation.WriteDescriptor -> executeWriteDescriptor(op)
                            is GattOperation.RequestMtu -> executeRequestMtu(op)
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
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

    @Suppress("MissingPermission")
    private fun executeRead(op: GattOperation.Read) {
        activeReadOp = op
        val gatt = bluetoothGatt ?: return completeRead(null)
        val service = gatt.getService(BleConfig.SERVICE_UUID) ?: return completeRead(null)
        val char = service.getCharacteristic(op.uuid) ?: return completeRead(null)

        val success = gatt.readCharacteristic(char)
        if (!success) {
            completeRead(null)
        }
    }

    private fun completeRead(result: ByteArray?) {
        val op = activeReadOp ?: return
        activeReadOp = null
        op.deferred.complete(result)
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun executeWrite(op: GattOperation.Write) {
        activeWriteOp = op
        val gatt = bluetoothGatt ?: return completeWrite(false)
        val service = gatt.getService(BleConfig.SERVICE_UUID) ?: return completeWrite(false)
        val char = service.getCharacteristic(op.uuid) ?: return completeWrite(false)

        char.value = op.value
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val success = gatt.writeCharacteristic(char)
        if (!success) {
            completeWrite(false)
        }
    }

    private fun completeWrite(success: Boolean) {
        val op = activeWriteOp ?: return
        activeWriteOp = null
        op.deferred.complete(success)
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun executeWriteDescriptor(op: GattOperation.WriteDescriptor) {
        activeDescriptorOp = op
        val gatt = bluetoothGatt ?: return completeDescriptor(false)
        op.descriptor.value = op.value
        val success = gatt.writeDescriptor(op.descriptor)
        if (!success) {
            completeDescriptor(false)
        }
    }

    private fun completeDescriptor(success: Boolean) {
        val op = activeDescriptorOp ?: return
        activeDescriptorOp = null
        op.deferred.complete(success)
    }

    @Suppress("MissingPermission")
    private fun executeRequestMtu(op: GattOperation.RequestMtu) {
        activeMtuOp = op
        val gatt = bluetoothGatt ?: return completeMtu(0)
        val success = gatt.requestMtu(op.mtu)
        if (!success) {
            completeMtu(0)
        }
    }

    private fun completeMtu(mtu: Int) {
        val op = activeMtuOp ?: return
        activeMtuOp = null
        op.deferred.complete(mtu)
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        ],
    )
    override fun clear() {
        stopScan()
        disconnectInternal()
        cleanupJob?.cancel()
        connectionTimeoutJob?.cancel()
        gattOpConsumerJob?.cancel()
        gattOps.close()
        _scanError.value = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan(config: BleScanConfig) {
        if (bluetoothLeScanner == null) {
            _scanError.value = BleScanError.ScannerUnavailable
            return
        }

        activeScanClients++
        currentScanConfig = config
        _scanError.value = null

        val isFirstClient = activeScanClients == 1

        if (isFirstClient) {
            _scanningInProgress.update { true }

            val scanFilters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BleConfig.SERVICE_UUID))
                    .build(),
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bluetoothLeScanner?.startScan(scanFilters, settings, scanCallback)

            cleanupJob?.cancel()
            cleanupJob = repositoryScope.launch {
                while (activeScanClients > 0 && _scanningInProgress.value) {
                    delay(BleConfig.CLEANUP_INTERVAL_MS)
                    cleanupStaleDevices()
                }
            }

            if (config.scanDurationMs > 0) {
                scanJob = repositoryScope.launch {
                    delay(config.scanDurationMs)
                    stopScan()
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        if (activeScanClients <= 0) return

        activeScanClients--

        if (activeScanClients <= 0) {
            bluetoothLeScanner?.stopScan(scanCallback)
            _scanningInProgress.update { false }
            scanJob?.cancel()
            scanJob = null
            cleanupJob?.cancel()
            cleanupJob = null
            currentScanConfig = null
            activeScanClients = 0
        }
        _scanError.value = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connectToDevice(device: BleDevice) {
        if (bluetoothAdapter == null || bluetoothAdapter?.isEnabled == false) {
            _connectionState.update { BleConnectionState.DISCONNECTED }
            return
        }

        connectionJob?.cancel()
        connectionTimeoutJob?.cancel()
        _connectionState.update { BleConnectionState.CONNECTING }

        val bleMac = device.address
        val bleDevice = bluetoothAdapter?.getRemoteDevice(bleMac)

        if (bleDevice == null) {
            _connectionState.update { BleConnectionState.DISCONNECTED }
            return
        }

        connectedDevice = bleDevice
        _connectedDeviceName.update { device.name ?: device.address }

        connectionJob = repositoryScope.launch {
            connectionMutex.withLock {
                try {
                    val gatt = bleDevice.connectGatt(
                        context,
                        false,
                        connectCallback,
                    )

                    if (gatt == null) {
                        _connectionState.update { BleConnectionState.DISCONNECTED }
                        connectedDevice = null
                        return@withLock
                    }

                    bluetoothGatt = gatt
                } catch (_: Exception) {
                    _connectionState.update { BleConnectionState.DISCONNECTED }
                    connectedDevice = null
                }
            }

            connectionTimeoutJob = repositoryScope.launch {
                delay(BleConfig.CONNECT_TIMEOUT_MS)
                if (_connectionState.value == BleConnectionState.CONNECTING) {
                    disconnectInternal()
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        disconnectInternal()
    }

    @Suppress("NewApi")
    private fun cleanupStaleDevices() {
        val cutoff = Instant.now().minus(BleConfig.DEVICE_TIMEOUT_SECONDS, ChronoUnit.SECONDS)
        _discoveredDevices.update { devices ->
            if (devices.isEmpty()) return@update emptyMap()
            devices.filterValues { it.lastSeenAt.isAfter(cutoff) }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectInternal() {
        connectionJob?.cancel()
        connectionJob = null
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null

        gattOpConsumerJob?.cancel()
        gattOpConsumerJob = null

        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (_: Exception) {
            }
        }

        bluetoothGatt = null
        connectedDevice = null
        _connectedDeviceName.update { null }
        _connectionState.update { BleConnectionState.DISCONNECTED }
    }

    override suspend fun readCharacteristic(uuid: UUID): ByteArray? {
        val deferred = kotlinx.coroutines.CompletableDeferred<ByteArray?>()
        val op = GattOperation.Read(uuid, deferred)
        gattOps.send(op)
        return deferred.await()
    }

    override suspend fun writeCharacteristic(uuid: UUID, value: ByteArray): Boolean {
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val op = GattOperation.Write(uuid, value, deferred)
        gattOps.send(op)
        return deferred.await()
    }

    override suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val op = GattOperation.WriteDescriptor(descriptor, value, deferred)
        gattOps.send(op)
        return deferred.await()
    }

    override suspend fun requestMtu(mtu: Int): Int {
        val deferred = kotlinx.coroutines.CompletableDeferred<Int>()
        val op = GattOperation.RequestMtu(mtu, deferred)
        gattOps.send(op)
        return deferred.await()
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Suppress("NewApi")
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
                            } else {
                                existingDevice
                            }
                        } else {
                            BleDevice(address = address, name = name, rssi = result.rssi, lastSeenAt = Instant.now())
                        }

                        if (updatedDevice == existingDevice) {
                            devices
                        } else {
                            devices + (address to updatedDevice)
                        }
                    }
                }
            }
        }
    }

    private val connectCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            repositoryScope.launch {
                connectionMutex.withLock {
                    when (newState) {
                        STATE_CONNECTED -> {
                            connectionTimeoutJob?.cancel()
                            connectionTimeoutJob = null
                            _connectionState.update { BleConnectionState.CONNECTED }
                            gatt?.discoverServices()
                        }
                        STATE_DISCONNECTED -> {
                            connectionTimeoutJob?.cancel()
                            connectionTimeoutJob = null
                            _connectedDeviceName.update { null }
                            _connectionState.update { BleConnectionState.DISCONNECTED }
                            connectedDevice = null
                            bluetoothGatt = null
                            drainPendingGattOps()
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            repositoryScope.launch {
                val deferred = kotlinx.coroutines.CompletableDeferred<Int>()
                val op = GattOperation.RequestMtu(BleConfig.DEFAULT_MTU, deferred)
                deferred.invokeOnCompletion {
                    if (it == null) {
                        repositoryScope.launch {
                            subscribeToStatusNotifications()
                        }
                    }
                }
                if (!gattOps.isClosedForSend) {
                    gattOps.send(op)
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            repositoryScope.launch {
                val result = if (status == BluetoothGatt.GATT_SUCCESS) characteristic.value else null
                completeRead(result)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            repositoryScope.launch {
                val success = status == BluetoothGatt.GATT_SUCCESS
                completeWrite(success)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            repositoryScope.launch {
                val success = status == BluetoothGatt.GATT_SUCCESS
                completeDescriptor(success)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            repositoryScope.launch {
                val actualMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else 0
                completeMtu(actualMtu)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (bluetoothGatt != gatt) return
            if (characteristic.uuid == BleConfig.STATUS_UUID && characteristic.value?.size == 2) {
                val state = characteristic.value[0].toInt() and 0xFF
                val rssi = characteristic.value[1].toInt()
                repositoryScope.launch {
                    _statusUpdates.emit(StatusUpdate(state, rssi))
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun subscribeToStatusNotifications() {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(BleConfig.SERVICE_UUID) ?: return
        val statusChar = service.getCharacteristic(BleConfig.STATUS_UUID) ?: return

        gatt.setCharacteristicNotification(statusChar, true)
        val cccd = statusChar.getDescriptor(BleConfig.CCCD_DESCRIPTOR_UUID) ?: return

        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val op = GattOperation.WriteDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, deferred)
        if (!gattOps.isClosedForSend) {
            gattOps.send(op)
        }
    }

    private fun drainPendingGattOps() {
        if (gattOpsClosed) return
        gattOpsClosed = true
        gattOps.close()
        activeReadOp?.deferred?.complete(null)
        activeReadOp = null
        activeWriteOp?.deferred?.complete(false)
        activeWriteOp = null
        activeDescriptorOp?.deferred?.complete(false)
        activeDescriptorOp = null
        activeMtuOp?.deferred?.complete(0)
        activeMtuOp = null
    }
}
