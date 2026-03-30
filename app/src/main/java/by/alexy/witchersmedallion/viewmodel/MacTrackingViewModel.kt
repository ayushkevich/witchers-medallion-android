package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.ui.model.MacDevice
import by.alexy.witchersmedallion.ui.state.DialogState
import by.alexy.witchersmedallion.ui.state.MacTrackingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAC_ADDRESS_PATTERN = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})"
private const val SCAN_DURATION_MS = 10_000L

@HiltViewModel
class MacTrackingViewModel @Inject constructor(
    private val medallionRepository: MedallionRepository,
    private val bleRepository: BleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MacTrackingUiState())
    val uiState = _uiState.asStateFlow()

    private val _dialogState = MutableStateFlow(DialogState())
    val dialogState = _dialogState.asStateFlow()


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
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Ошибка загрузки MAC-адресов: ${e.message}"
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
                    errorMessage = "Неверный формат MAC-адреса"
                )
                return
            }
        }

        val cleanMac = mac.uppercase().replace(Regex("[^0-9A-F]"), "")
        if (!isValidMacAddress(cleanMac)) {
            _uiState.update {
                it.copy(
                    errorMessage = "Invalid MAC format"
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
                        }
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
                        trackedDevices = it.trackedDevices.filter { it.mac != mac }
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

    fun onDeviceClick(device: BleDevice) {
        _dialogState.update {
            DialogState(
                showDialog = true,
                selectedDevice = device
            )
        }
    }

    fun onConfirmAddDevice() {
        val device = _dialogState.value.selectedDevice ?: return

        viewModelScope.launch {
            val macAddresses = medallionRepository.getRegisteredMacAddresses().toMutableList()
            if (!macAddresses.contains(device.address)) {
                macAddresses.add(device.address)
                medallionRepository.updateRegisteredMacAddresses(macAddresses)

                _uiState.update {
                    it.copy(
                        trackedDevices = it.trackedDevices.toMutableList().apply {
                            add(MacDevice(device.address, device.rssi))
                        }
                    )
                }
            }

            _dialogState.update { it.copy(showDialog = false, selectedDevice = null) }
        }
    }

    fun onCancelAddDevice() {
        _dialogState.update { it.copy(showDialog = false, selectedDevice = null) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun isDynamicMac(address: String): Boolean {
        val bytes = address.replace(Regex("[^0-9A-Fa-f]"), "").toByteArray()
        if (bytes.size < 1) return false

        val firstByte = bytes[0].toInt() and 0xFF
        val secondByte = if (bytes.size > 1) bytes[1].toInt() and 0xFF else 0

        val macType = firstByte and 0x01
        val manufacturerSpecific = (firstByte and 0x02) == 0x02

        val publicMacRanges = listOf(
            "00:00:00" to "00:00:01",
            "00:1A:2D" to "00:1A:2D",
            "00:50:C2" to "00:50:C2",
            "00:60:80" to "00:60:80",
            "00:E0:18" to "00:E0:18",
            "00:80:F8" to "00:80:F8",
            "00:C0:9F" to "00:C0:9F",
            "00:D0:B3" to "00:D0:B3",
            "00:D1:08" to "00:D1:08",
            "00:D1:B7" to "00:D1:B7",
            "00:D1:F6" to "00:D1:F6",
            "00:D2:01" to "00:D2:01",
            "00:D2:1E" to "00:D2:1E",
            "00:D2:62" to "00:D2:62",
            "00:D2:74" to "00:D2:74",
            "00:D2:77" to "00:D2:77",
            "00:D2:78" to "00:D2:78",
            "02:00:00" to "02:FF:FF"
        )

        val isInRange = publicMacRanges.any { (start, end) ->
            address.uppercase() >= start && address.uppercase() <= end
        }

        return macType == 1 || manufacturerSpecific || isInRange
    }

    private fun isValidMacAddress(mac: String): Boolean =
        mac.length == 12 && mac.matches(Regex("[0-9A-F]{12}"))
}