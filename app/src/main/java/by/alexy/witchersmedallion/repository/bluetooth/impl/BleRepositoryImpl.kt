package by.alexy.witchersmedallion.repository.bluetooth.impl

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.annotation.RequiresPermission
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.BleScanConfig
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleRepository {
    private val _discoveredDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    override val discoveredDevices: Flow<List<BleDevice>> = _discoveredDevices
        .map { devices -> devices.values.toList() }

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: Flow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scanningInProgress = MutableStateFlow(false)
    override val scanningInProgress: Flow<Boolean> = _scanningInProgress.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var currentScanConfig: BleScanConfig? = null
    private var scanJob: Job? = null
    private val scanScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan(config: BleScanConfig) {
        currentScanConfig = config
        _discoveredDevices.value = emptyMap()
        bluetoothLeScanner?.startScan(scanCallback)
        _scanningInProgress.update { true }

        if (config.scanDurationMs > 0) {
            scanJob = scanScope.launch {
                delay(config.scanDurationMs)
                stopScan()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
        _scanningInProgress.update { false }
        currentScanConfig = null
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val config = currentScanConfig ?: return

            val device = result.device
            val address = device.address
            val name = device.name.takeIf { it?.isNotBlank() == true }

            if (result.rssi < config.minRssi) return

            _discoveredDevices.update { devices ->
                val existingDevice = devices[address]

                val updatedDevice = if (existingDevice != null) {
                    if (name != null && name != existingDevice.name) {
                        existingDevice.copy(name = name)
                    } else {
                        existingDevice
                    }
                } else {
                    BleDevice(address = address, name = name, rssi = result.rssi)
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