package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice

@Immutable
data class MainUiState (
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val rssi: Int = 0,
    val state: BleConnectionState = BleConnectionState.DISCONNECTED,
    val isScanning: Boolean = false,
    val availableDevices: List<BleDevice> = emptyList(),
    val needsPermission: Boolean = false,
    val errorMessage: String? = null
)
