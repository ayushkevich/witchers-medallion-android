package by.alexy.witchersmedallion.repository.impl

import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings
import by.alexy.witchersmedallion.repository.MedallionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedallionRepositoryStub @Inject constructor() : MedallionRepository {

    private val registeredMacAddresses = mutableListOf<String>()

    private var currentRssi: Int? = -50

    private var calibrationSettings: MedallionCalibrationSettings? = null

    override suspend fun getRegisteredMacAddresses(): List<String> {
        return registeredMacAddresses.toList()
    }

    override suspend fun updateRegisteredMacAddresses(macAddresses: List<String>) {
        registeredMacAddresses.clear()
        registeredMacAddresses.addAll(macAddresses)
    }

    override suspend fun getMedallionRssi(): Int? {
        return currentRssi
    }

    override suspend fun getCalibrationSettings(): MedallionCalibrationSettings? {
        return calibrationSettings
    }

    override suspend fun setCalibrationSettings(settings: MedallionCalibrationSettings) {
        calibrationSettings = settings
    }
}
