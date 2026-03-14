package by.alexy.witchersmedallion.repository.bluetooth

import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import kotlinx.coroutines.flow.Flow

interface BleRepository {
    val discoveredDevices: Flow<List<BleDevice>>
    val connectionState: Flow<BleConnectionState>

    val scanningInProgress: Flow<Boolean>
    fun startScan()
    fun stopScan()
}