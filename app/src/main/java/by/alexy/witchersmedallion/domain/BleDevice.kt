package by.alexy.witchersmedallion.domain

import java.time.Instant

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeenAt: Instant = Instant.now(),
)
