package by.alexy.witchersmedallion.repository.bluetooth

import android.bluetooth.BluetoothGattDescriptor
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.BleScanConfig
import by.alexy.witchersmedallion.domain.BleScanError
import by.alexy.witchersmedallion.domain.StatusUpdate
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BleRepository {
    val discoveredDevices: Flow<List<BleDevice>>
    val connectionState: Flow<BleConnectionState>
    val scanningInProgress: Flow<Boolean>
    val connectedDeviceName: Flow<String?>
    val scanError: Flow<BleScanError?>
    val statusUpdates: Flow<StatusUpdate>

    fun startScan(config: BleScanConfig = BleScanConfig())
    fun stopScan()
    fun connectToDevice(device: BleDevice)
    fun disconnect()
    fun clear()
    suspend fun readCharacteristic(uuid: UUID): ByteArray?
    suspend fun writeCharacteristic(uuid: UUID, value: ByteArray): Boolean
    suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean
    suspend fun requestMtu(mtu: Int): Int
}
