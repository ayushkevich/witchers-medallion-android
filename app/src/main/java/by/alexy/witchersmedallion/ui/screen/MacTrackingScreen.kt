package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.model.MacDevice

@Composable
fun MacTrackingScreen() {
    var macInput by remember { mutableStateOf("") }
    val trackedDevices = remember { mutableStateListOf<MacDevice>() }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            TextField(
                value = macInput,
                onValueChange = { macInput = it },
                label = { Text(stringResource(R.string.mac_address)) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (macInput.isNotBlank()) {
                    trackedDevices.add(MacDevice(macInput, -60))
                    macInput = ""
                }
            }) {
                Text(stringResource(R.string.add_mac))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(trackedDevices, key = { it.mac }) { device ->
                Text("${device.mac} -> RSSI: ${device.rssi}")
            }
        }
    }
}