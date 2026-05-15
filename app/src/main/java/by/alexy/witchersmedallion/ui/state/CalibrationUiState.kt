package by.alexy.witchersmedallion.ui.state

import androidx.compose.runtime.Immutable
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings

@Immutable
data class CalibrationUiState(
    val currentRssi: Int? = null,
    val calibrationSettings: MedallionCalibrationSettings? = null,
    val coldRssi: Int = -70,
    val warmRssi: Int = -60,
    val hotRssi: Int = -45,
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
