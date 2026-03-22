package by.alexy.witchersmedallion.repository

import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings

interface MedallionRepository {

    suspend fun getRegisteredMacAddresses(): List<String>

    suspend fun updateRegisteredMacAddresses(macAddresses: List<String>)

    suspend fun getMedallionRssi(): Int?

    suspend fun getCalibrationSettings(): MedallionCalibrationSettings?

    suspend fun setCalibrationSettings(settings: MedallionCalibrationSettings)
}
