package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.domain.BleDevice
import by.alexy.witchersmedallion.ui.screen.component.ConnectionConfirmationDialog
import by.alexy.witchersmedallion.ui.screen.component.ValueWithLabel
import by.alexy.witchersmedallion.ui.screen.component.mac.MacDeviceCardComponent
import by.alexy.witchersmedallion.viewmodel.MacTrackingViewModel

private const val MAX_TRACKED_MAC_DEVICES = 50

@Composable
fun MacTrackingScreen(viewModel: MacTrackingViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDeviceForAdd by remember { mutableStateOf<BleDevice?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ValueWithLabel(
            label = stringResource(R.string.mac_tracked_devices),
            value = "${uiState.trackedDevices.size}",
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextField(
                value = uiState.macInput,
                onValueChange = { viewModel.onMacInputChange(it) },
                label = { Text(stringResource(R.string.mac_address)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (uiState.isScanning) {
                Button(
                    onClick = { viewModel.stopScan() },
                    enabled = !uiState.isLoading,
                ) {
                    Text(stringResource(R.string.mac_stop_scan))
                }
            } else {
                Button(
                    onClick = { viewModel.startScan() },
                    enabled = !uiState.isLoading,
                ) {
                    Text(stringResource(R.string.mac_scan_devices))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isScanning && uiState.trackedDevices.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scan_in_progress),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.mac_add_mac_address),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    TextField(
                        value = uiState.macInput,
                        onValueChange = { viewModel.onMacInputChange(it) },
                        label = { Text(stringResource(R.string.mac_address)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (uiState.macInput.isNotBlank()) {
                                viewModel.addMacAddress(uiState.macInput)
                                viewModel.onMacInputChange("")
                            }
                        },
                        enabled = uiState.macInput.isNotBlank() && !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.add_mac))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(R.string.error)) },
                text = { Text(uiState.errorMessage!!.getString(androidx.compose.ui.platform.LocalContext.current)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.mac_available_devices),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = uiState.trackedDevices,
                key = { it.mac },
            ) { device ->
                MacDeviceCardComponent(
                    device = device,
                    isDynamic = viewModel.isDynamicMac(device.mac),
                    onRemove = { viewModel.removeMacAddress(device.mac) },
                )
            }
        }

        if (showAddDialog && selectedDeviceForAdd != null) {
            ConnectionConfirmationDialog(
                device = selectedDeviceForAdd!!,
                onConfirm = {
                    viewModel.confirmAddDevice(selectedDeviceForAdd!!)
                    showAddDialog = false
                    selectedDeviceForAdd = null
                },
                onDismiss = {
                    showAddDialog = false
                    selectedDeviceForAdd = null
                },
            )
        }
    }
}
