package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.domain.BleDevice

@Immutable
data class DialogState(
    val showDialog: Boolean = false,
    val selectedDevice: BleDevice? = null,
)
