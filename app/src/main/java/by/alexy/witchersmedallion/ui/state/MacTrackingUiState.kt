package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.domain.UiText
import by.alexy.witchersmedallion.ui.model.MacDevice

@Immutable
data class MacTrackingUiState(
    val trackedDevices: List<MacDevice> = emptyList(),
    val macInput: String = "",
    val isScanning: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)
