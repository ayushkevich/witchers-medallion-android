package by.alexy.witchersmedallion.ui.screen.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.domain.BleDevice

@Composable
fun ConnectionConfirmationDialog(
    device: BleDevice,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.confirm_connection))
        },
        text = {
            Text(
                stringResource(
                    R.string.confirm_connection_message,
                    device.name ?: device.address,
                ),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        },
    )
}
