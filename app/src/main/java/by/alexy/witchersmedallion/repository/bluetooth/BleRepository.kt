package by.alexy.witchersmedallion.repository.bluetooth

import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.BleScanConfig
import by.alexy.witchersmedallion.domain.BleScanError
import kotlinx.coroutines.flow.Flow

interface BleRepository {
    val discoveredDevices: Flow<List<BleDevice>>
    val connectionState: Flow<BleConnectionState>
    val scanningInProgress: Flow<Boolean>
    val connectedDeviceName: Flow<String?>
    val scanError: Flow<BleScanError?>

    fun startScan(config: BleScanConfig = BleScanConfig())
    fun stopScan()
    fun connectToDevice(device: BleDevice)
    fun disconnect()
    fun clear()
}
