package by.alexy.witchersmedallion.ui.screen

import android.Manifest
import android.os.Build
import android.widget.Spinner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.screen.component.ValueWithLabel
import by.alexy.witchersmedallion.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = blePermissions()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ValueWithLabel(
            label = stringResource(R.string.connection_status),
            value = if (uiState.isConnected) {
                stringResource(R.string.connected)
            } else {
                stringResource(R.string.disconnected)
            }
        )
        ValueWithLabel(
            label = stringResource(R.string.rssi),
            value = if (uiState.isConnected) {
                uiState.rssi.toString()
            } else {
                ""
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            if (uiState.isScanning) {
                Button(onClick = {
                    viewModel.stopScan()
                }) {
                    Text(stringResource(R.string.stop_scan))
                }
            } else {
                Button(onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        viewModel.scanDevices()
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }) {
                    Text(stringResource(R.string.scan_devices))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {}, enabled = uiState.isConnected) {
                Text(stringResource(R.string.disconnect))
            }
        }

        if (uiState.isScanning) {
            Row {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.scan_in_progress),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        if (uiState.availableDevices.isNotEmpty()) {
            LazyColumn {
                items(
                    items = uiState.availableDevices,
                    key = { it.address }
                ) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
//                            .clickable { viewModel.connectToDevice(device.address) }
                            .padding(4.dp)
                    ) {
                        Text("${device.name ?: device.address} (${device.rssi} dBm)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun blePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
