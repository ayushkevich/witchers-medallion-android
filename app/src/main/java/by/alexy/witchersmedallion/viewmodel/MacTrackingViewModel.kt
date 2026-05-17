package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.domain.UiText
import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.ui.model.MacDevice
import by.alexy.witchersmedallion.ui.state.MacTrackingUiState
import by.alexy.witchersmedallion.util.MacAddressUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SCAN_DURATION_MS = 10_000L

@HiltViewModel
class MacTrackingViewModel @Inject constructor(
    private val medallionRepository: MedallionRepository,
    private val bleRepository: BleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MacTrackingUiState())
    val uiState: StateFlow<MacTrackingUiState> = _uiState.asStateFlow()

    init {
        loadRegisteredMacs()
    }

    private fun loadRegisteredMacs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val registeredMacs = medallionRepository.getRegisteredMacAddresses()
                _uiState.update {
                    it.copy(
                        trackedDevices = registeredMacs.map { mac ->
                            MacDevice(mac, -60)
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = UiText.fromStringResource(
                            R.string.error_loading_macs,
                            e.message ?: "",
                        ),
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addMacAddress(mac: String) {
        if (mac.isBlank() || mac.length < 17) {
            _uiState.update {
                it.copy(
                    errorMessage = UiText.fromStringResource(R.string.error_invalid_mac_format),
                )
                return
            }
        }

        val cleanMac = mac.uppercase().replace(Regex("[^0-9A-F]"), "")
        if (!MacAddressUtils.isValidMacAddress(cleanMac)) {
            _uiState.update {
                it.copy(
                    errorMessage = UiText.fromStringResource(R.string.error_invalid_mac_characters),
                )
                return
            }
        }

        viewModelScope.launch {
            val macAddresses = medallionRepository.getRegisteredMacAddresses().toMutableList()
            if (!macAddresses.contains(cleanMac)) {
                macAddresses.add(cleanMac)
                medallionRepository.updateRegisteredMacAddresses(macAddresses)

                _uiState.update {
                    it.copy(
                        trackedDevices = it.trackedDevices.toMutableList().apply {
                            add(MacDevice(cleanMac, -60))
                        },
                    )
                }
            }
        }
    }

    fun removeMacAddress(mac: String) {
        viewModelScope.launch {
            val macAddresses = medallionRepository.getRegisteredMacAddresses().toMutableList()
            if (macAddresses.contains(mac)) {
                macAddresses.remove(mac)
                medallionRepository.updateRegisteredMacAddresses(macAddresses)

                _uiState.update {
                    it.copy(
                        trackedDevices = it.trackedDevices.filter { it.mac != mac },
                    )
                }
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            bleRepository.startScan()
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            bleRepository.stopScan()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun confirmAddDevice(device: BleDevice) {
        viewModelScope.launch {
            val macAddresses = medallionRepository.getRegisteredMacAddresses().toMutableList()
            if (!macAddresses.contains(device.address)) {
                macAddresses.add(device.address)
                medallionRepository.updateRegisteredMacAddresses(macAddresses)

                _uiState.update {
                    it.copy(
                        trackedDevices = it.trackedDevices.toMutableList().apply {
                            add(MacDevice(device.address, device.rssi))
                        },
                    )
                }
            }
        }
    }

    fun onMacInputChange(value: String) {
        _uiState.update { it.copy(macInput = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isDynamicMac(address: String): Boolean = MacAddressUtils.isDynamicMac(address)
}
