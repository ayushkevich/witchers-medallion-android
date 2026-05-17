package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.domain.BleConnectionState
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.permissions.getBlePermissions
import by.alexy.witchersmedallion.ui.screen.component.ConnectionConfirmationDialog
import by.alexy.witchersmedallion.ui.screen.component.ValueWithLabel
import by.alexy.witchersmedallion.util.RssiColorUtils
import by.alexy.witchersmedallion.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isConnected = uiState.state == BleConnectionState.CONNECTED
    val view = LocalView.current

    var showConnectDialog by remember { mutableStateOf(false) }
    var selectedDeviceForConnect by remember { mutableStateOf<BleDevice?>(null) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = getBlePermissions(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ValueWithLabel(
            label = stringResource(R.string.connection_status),
            value = if (isConnected) stringResource(R.string.connected) else stringResource(R.string.disconnected),
        )
        if (isConnected && uiState.connectedDeviceName != null) {
            Text(
                text = stringResource(R.string.connected_device, uiState.connectedDeviceName!!),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            if (!isConnected) {
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
            }
            Button(onClick = { viewModel.disconnect() }, enabled = isConnected) {
                Text(stringResource(R.string.disconnect))
            }
        }

        if (uiState.isScanning) {
            Row {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.scan_in_progress),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (uiState.availableDevices.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.device_name),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            stringResource(R.string.signal_strength),
                            Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                items(
                    items = uiState.availableDevices,
                    key = { it.address },
                ) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                if (!view.isInEditMode) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                }
                                selectedDeviceForConnect = device
                                showConnectDialog = true
                            },
                    ) {
                        Text(
                            text = device.name ?: device.address,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.rssi_unit, device.rssi),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = RssiColorUtils.getRssiColor(device.rssi, MaterialTheme.colorScheme),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showConnectDialog && selectedDeviceForConnect != null) {
            ConnectionConfirmationDialog(
                device = selectedDeviceForConnect!!,
                onConfirm = {
                    viewModel.connectToDevice(selectedDeviceForConnect!!)
                    showConnectDialog = false
                    selectedDeviceForConnect = null
                },
                onDismiss = {
                    showConnectDialog = false
                    selectedDeviceForConnect = null
                },
            )
        }
    }
}
