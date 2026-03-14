package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor (
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeScanningState()
        observeConnectionState()
        observeRssiUpdate()
        observeDiscoveredDevices()
    }

    fun scanDevices() = bleRepository.startScan()

    fun stopScan() = bleRepository.stopScan()

    private fun observeScanningState() {
        viewModelScope.launch {
            bleRepository.scanningInProgress.collect { it ->
                val scanningInProgress = it
                _uiState.update { it.copy(isScanning = scanningInProgress) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleRepository.connectionState.collect { it ->
                if (it == BleConnectionState.CONNECTING) {
                    _uiState.update { it.copy(isConnecting = true) }
                    _uiState.update { it.copy(isConnected = false) }
                }  else if (it == BleConnectionState.CONNECTED) {
                    _uiState.update { it.copy(isConnecting = false) }
                    _uiState.update { it.copy(isConnected = true) }
                } else {
                    _uiState.update { it.copy(isConnecting = false) }
                    _uiState.update { it.copy(isConnected = false) }
                }
            }
        }
    }

    private fun observeRssiUpdate() {

    }

    private fun observeDiscoveredDevices() {
        viewModelScope.launch {
            bleRepository.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(availableDevices = devices) }
            }
        }
    }
}