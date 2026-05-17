package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.BleScanConfig
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bleRepository: BleRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        bleRepository.scanningInProgress,
        bleRepository.connectionState,
        bleRepository.discoveredDevices.map { devices ->
            devices.sortedWith(
                compareByDescending<BleDevice> { it.rssi }
                    .thenBy { it.name ?: "" },
            )
        },
        bleRepository.connectedDeviceName,
    ) { scanning, connectionState, sortedDevices, deviceName ->
        MainUiState(
            state = connectionState,
            isScanning = scanning,
            availableDevices = if (connectionState == BleConnectionState.CONNECTED) emptyList() else sortedDevices,
            connectedDeviceName = deviceName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(3000),
        initialValue = MainUiState(),
    )

    fun scanDevices() {
        val config = BleScanConfig(minRssi = -80, scanDurationMs = 20000)
        bleRepository.startScan(config)
    }

    fun stopScan() = bleRepository.stopScan()

    fun connectToDevice(device: BleDevice) {
        stopScan()
        bleRepository.connectToDevice(device)
    }

    fun disconnect() {
        bleRepository.disconnect()
    }
}
