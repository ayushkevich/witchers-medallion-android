package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.screen.component.calibration.AutoCalibrationComponent
import by.alexy.witchersmedallion.ui.screen.component.calibration.ManualCalibrationComponent
import by.alexy.witchersmedallion.ui.state.AutoCalibrationStep
import by.alexy.witchersmedallion.viewmodel.CalibrationViewModel

@Composable
fun CalibrationScreen(viewModel: CalibrationViewModel, currentPage: Int = 0) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(currentPage) {
        viewModel.startRssiPolling()

        onDispose {
            viewModel.stopRssiPolling()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.current_rssi, uiState.currentRssi ?: 0),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.calibration_settings),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                if (uiState.isAutoCalibrationMode) {
                    AutoCalibrationComponent(
                        currentStep = uiState.autoCalibrationStep,
                        currentRssi = uiState.currentRssi,
                        hotRssi = uiState.hotRssi,
                        warmRssi = uiState.warmRssi,
                        coldRssi = uiState.coldRssi,
                        onCapture = { viewModel.captureRssiForCurrentStep() },
                    )
                } else {
                    ManualCalibrationComponent(
                        coldRssi = uiState.coldRssi,
                        warmRssi = uiState.warmRssi,
                        hotRssi = uiState.hotRssi,
                        onColdRssiChange = { viewModel.updateColdRssi(it) },
                        onWarmRssiChange = { viewModel.updateWarmRssi(it) },
                        onHotRssiChange = { viewModel.updateHotRssi(it) },
                    )
                }
            }
        }

        if (!uiState.isAutoCalibrationMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(onClick = { viewModel.startAutoCalibration() }) {
                    Text(stringResource(R.string.auto_calibration))
                }

                Button(
                    onClick = { viewModel.setDefaultValues() },
                    enabled = !uiState.isLoading,
                ) {
                    Text(stringResource(R.string.set_defaults))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveCalibrationSettings() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_to_esp32))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { viewModel.cancelAutoCalibration() },
                ) {
                    Text(stringResource(R.string.back_to_manual_calibration))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.saveCalibrationSettings() },
                    enabled = !uiState.isLoading && uiState.autoCalibrationStep == AutoCalibrationStep.COMPLETED,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save_to_esp32))
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(R.string.error)) },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
