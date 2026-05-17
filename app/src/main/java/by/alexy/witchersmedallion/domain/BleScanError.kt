package by.alexy.witchersmedallion.domain

sealed class BleScanError {
    object BluetoothDisabled : BleScanError()
    object ScannerUnavailable : BleScanError()
    data class ScanFailed(val errorCode: Int) : BleScanError()
    data class ConnectionFailed(val status: Int) : BleScanError()
    data class GattError(val code: Int) : BleScanError()
    data class Timeout(val operation: String) : BleScanError()
    data class Unknown(val message: String) : BleScanError()
}
