package by.alexy.witchersmedallion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.alexy.witchersmedallion.config.BleConfig
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings
import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.ui.state.AutoCalibrationStep
import by.alexy.witchersmedallion.ui.state.CalibrationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val medallionRepository: MedallionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadCalibrationSettings()
    }

    override fun onCleared() {
        stopRssiPolling()
    }

    fun startRssiPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val currentRssi = medallionRepository.getMedallionRssi()
                    _uiState.update { it.copy(currentRssi = currentRssi) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
                delay(BleConfig.DEFAULT_POLLING_INTERVAL_MS.milliseconds)
            }
        }
    }

    fun stopRssiPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun updateColdRssi(value: Int) {
        _uiState.update { it.copy(coldRssi = value) }
    }

    fun updateWarmRssi(value: Int) {
        _uiState.update { it.copy(warmRssi = value) }
    }

    fun updateHotRssi(value: Int) {
        _uiState.update { it.copy(hotRssi = value) }
    }

    fun setDefaultValues() {
        _uiState.update {
            it.copy(
                coldRssi = BleConfig.DEFAULT_COLD_RSSI,
                warmRssi = BleConfig.DEFAULT_WARM_RSSI,
                hotRssi = BleConfig.DEFAULT_HOT_RSSI,
            )
        }
    }

    fun saveCalibrationSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val settings = MedallionCalibrationSettings(
                    coldRssi = _uiState.value.coldRssi,
                    warmRssi = _uiState.value.warmRssi,
                    hotRssi = _uiState.value.hotRssi,
                )
                medallionRepository.setCalibrationSettings(settings)
                _uiState.update {
                    it.copy(
                        calibrationSettings = settings,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message,
                    )
                }
            }
        }
    }

    fun startAutoCalibration() {
        _uiState.update {
            it.copy(
                isAutoCalibrationMode = true,
                autoCalibrationStep = AutoCalibrationStep.MOVE_TO_HOT,
            )
        }
    }

    fun cancelAutoCalibration() {
        _uiState.update {
            it.copy(
                isAutoCalibrationMode = false,
                autoCalibrationStep = AutoCalibrationStep.NONE,
            )
        }
    }

    fun captureRssiForCurrentStep() {
        viewModelScope.launch {
            try {
                val currentRssi = medallionRepository.getMedallionRssi() ?: return@launch

                val currentState = _uiState.value
                val nextStep = when (currentState.autoCalibrationStep) {
                    AutoCalibrationStep.MOVE_TO_HOT -> {
                        _uiState.update { it.copy(hotRssi = currentRssi) }
                        AutoCalibrationStep.MOVE_TO_WARM
                    }
                    AutoCalibrationStep.MOVE_TO_WARM -> {
                        _uiState.update { it.copy(warmRssi = currentRssi) }
                        AutoCalibrationStep.MOVE_TO_COLD
                    }
                    AutoCalibrationStep.MOVE_TO_COLD -> {
                        _uiState.update { it.copy(coldRssi = currentRssi) }
                        AutoCalibrationStep.COMPLETED
                    }
                    else -> AutoCalibrationStep.NONE
                }

                _uiState.update {
                    it.copy(
                        autoCalibrationStep = nextStep,
                        currentRssi = currentRssi,
                    )
                }

                if (nextStep == AutoCalibrationStep.COMPLETED) {
                    _uiState.update {
                        it.copy(
                            isAutoCalibrationMode = false,
                            autoCalibrationStep = AutoCalibrationStep.NONE,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadCalibrationSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val settings = medallionRepository.getCalibrationSettings()

                _uiState.update { state ->
                    state.copy(
                        calibrationSettings = settings,
                        coldRssi = settings?.coldRssi ?: state.coldRssi,
                        warmRssi = settings?.warmRssi ?: state.warmRssi,
                        hotRssi = settings?.hotRssi ?: state.hotRssi,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message,
                    )
                }
            }
        }
    }
}
