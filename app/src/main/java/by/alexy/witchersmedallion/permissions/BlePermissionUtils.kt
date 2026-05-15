package by.alexy.witchersmedallion.permissions

import android.Manifest
import android.os.Build

fun getBlePermissions(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
} else {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
}
