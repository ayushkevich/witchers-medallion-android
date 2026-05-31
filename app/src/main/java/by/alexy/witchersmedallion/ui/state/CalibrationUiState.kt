package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.config.BleConfig
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings

@Immutable
data class CalibrationUiState(
    val currentRssi: Int? = null,
    val calibrationSettings: MedallionCalibrationSettings? = null,
    val coldRssi: Int = BleConfig.DEFAULT_COLD_RSSI,
    val warmRssi: Int = BleConfig.DEFAULT_WARM_RSSI,
    val hotRssi: Int = BleConfig.DEFAULT_HOT_RSSI,
    val isAutoCalibrationMode: Boolean = false,
    val autoCalibrationStep: AutoCalibrationStep = AutoCalibrationStep.NONE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

enum class AutoCalibrationStep {
    NONE,
    MOVE_TO_HOT,
    MOVE_TO_WARM,
    MOVE_TO_COLD,
    COMPLETED,
}
