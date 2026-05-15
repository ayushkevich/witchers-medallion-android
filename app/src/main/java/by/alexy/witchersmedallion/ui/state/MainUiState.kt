package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice

@Immutable
data class MainUiState(
    val state: BleConnectionState = BleConnectionState.DISCONNECTED,
    val isScanning: Boolean = false,
    val availableDevices: List<BleDevice> = emptyList(),
    val errorMessage: String? = null,
    val connectedDeviceName: String? = null,
)
