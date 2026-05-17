package by.alexy.witchersmedallion.repository.bluetooth.impl

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
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
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val DEVICE_TIMEOUT_SECONDS = 30L
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val CLEANUP_INTERVAL_MS = 15_000L

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

    private val deviceMutex = Mutex()
    private val connectionMutex = Mutex()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
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
        _scanError.value = null
        repositoryScope.cancel()
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
                    delay(CLEANUP_INTERVAL_MS)
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

                    connectionTimeoutJob = repositoryScope.launch {
                        delay(CONNECT_TIMEOUT_MS)
                        if (_connectionState.value == BleConnectionState.CONNECTING) {
                            disconnectInternal()
                        }
                    }
                } catch (_: Exception) {
                    _connectionState.update { BleConnectionState.DISCONNECTED }
                    connectedDevice = null
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        disconnectInternal()
    }

    private fun cleanupStaleDevices() {
        val cutoff = Instant.now().minus(DEVICE_TIMEOUT_SECONDS, ChronoUnit.SECONDS)
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

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
                        }
                    }
                }
            }
        }
    }
}
