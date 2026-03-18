package by.alexy.witchersmedallion.ui.state

import by.alexy.witchersmedallion.domain.BleDevice

data class DialogState(
    val showDialog: Boolean = false,
    val selectedDevice: BleDevice? = null
)
