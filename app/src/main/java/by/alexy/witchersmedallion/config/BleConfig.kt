package by.alexy.witchersmedallion.config

import java.util.UUID

object BleConfig {
    const val DEVICE_TIMEOUT_SECONDS = 30L
    const val CONNECT_TIMEOUT_MS = 30_000L
    const val CLEANUP_INTERVAL_MS = 15_000L
    const val GATT_OP_TIMEOUT_MS = 5_000L
    const val DEFAULT_POLLING_INTERVAL_MS = 1000L
    const val DEFAULT_COLD_RSSI = -75
    const val DEFAULT_WARM_RSSI = -60
    const val DEFAULT_HOT_RSSI = -45
    const val DEFAULT_INITIAL_RSSI = -60
    const val DEFAULT_MTU = 64

    val SERVICE_UUID: UUID = UUID.fromString("9e5b4cb7-6ccc-4027-a496-ac7c01bbf706")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("d3481eab-e4cf-49f7-9d3e-36a2b7dff88e")
    val CALIBRATION_UUID: UUID = UUID.fromString("a1b2c3d4-aaaa-bbbb-cccc-ddddeeeeffff")
    val STATUS_UUID: UUID = UUID.fromString("a1b2c3d4-1111-2222-3333-444455556666")
    val PROTOCOL_VERSION_UUID: UUID = UUID.fromString("a1b2c3d4-7777-8888-9999-aaaaaaaaaaaa")
    val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
