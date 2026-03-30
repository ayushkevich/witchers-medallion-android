package by.alexy.witchersmedallion.ui.screen.component.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.state.AutoCalibrationStep

@Composable
fun AutoCalibrationComponent(
    currentStep: AutoCalibrationStep,
    currentRssi: Int?,
    hotRssi: Int,
    warmRssi: Int,
    coldRssi: Int,
    onCapture: () -> Unit
) {
    val instructionText = when (currentStep) {
        AutoCalibrationStep.MOVE_TO_HOT -> stringResource(R.string.move_to_hot_zone)
        AutoCalibrationStep.MOVE_TO_WARM -> stringResource(R.string.move_to_warm_zone)
        AutoCalibrationStep.MOVE_TO_COLD -> stringResource(R.string.move_to_cold_zone)
        AutoCalibrationStep.COMPLETED -> stringResource(R.string.calibration_completed)
        else -> ""
    }

    Text(
        text = instructionText,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.hot_zone_rssi))
                Text("$hotRssi dBm", modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.warm_zone_rssi))
                Text("$warmRssi dBm", modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.cold_zone_rssi))
                Text("$coldRssi dBm", modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Button(
                onClick = { onCapture() },
                enabled = currentStep != AutoCalibrationStep.COMPLETED
            ) {
                Text(stringResource(R.string.capture))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}