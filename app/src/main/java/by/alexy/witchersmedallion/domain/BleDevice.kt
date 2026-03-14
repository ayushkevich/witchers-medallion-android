package by.alexy.witchersmedallion.domain

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)
