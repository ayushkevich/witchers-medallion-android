package by.alexy.witchersmedallion.repository.impl

import by.alexy.witchersmedallion.config.BleConfig
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings
import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleMedallionRepository @Inject constructor(
    private val bleRepository: BleRepository,
) : MedallionRepository {

    override suspend fun getRegisteredMacAddresses(): List<String> = emptyList()

    override suspend fun updateRegisteredMacAddresses(macAddresses: List<String>) {
    }

    override suspend fun getMedallionRssi(): Int? = withContext(Dispatchers.IO) {
        val bytes = bleRepository.readCharacteristic(BleConfig.STATUS_UUID) ?: return@withContext null
        if (bytes.size < 2) return@withContext null
        bytes[1].toInt()
    }

    override suspend fun getCalibrationSettings(): MedallionCalibrationSettings? = withContext(Dispatchers.IO) {
        val versionBytes = bleRepository.readCharacteristic(BleConfig.PROTOCOL_VERSION_UUID) ?: return@withContext null
        if (versionBytes.size < 2) return@withContext null
        val version = readUint16LE(versionBytes, 0)
        if (version != 1) return@withContext null

        val bytes = bleRepository.readCharacteristic(BleConfig.CALIBRATION_UUID) ?: return@withContext null
        return@withContext parseCalibrationBytes(bytes)
    }

    override suspend fun setCalibrationSettings(settings: MedallionCalibrationSettings) = withContext(Dispatchers.IO) {
        val bytes = serializeCalibrationSettings(settings)
        val success = bleRepository.writeCharacteristic(BleConfig.CALIBRATION_UUID, bytes)
        if (!success) {
            throw RuntimeException("Failed to write calibration to device")
        }
    }

    companion object {
        private fun readInt16LE(bytes: ByteArray, offset: Int): Int {
            val low = bytes[offset].toInt() and 0xFF
            val high = bytes[offset + 1].toInt() and 0xFF
            return ((low or (high shl 8)).toShort()).toInt()
        }

        private fun readUint16LE(bytes: ByteArray, offset: Int): Int {
            val low = bytes[offset].toInt() and 0xFF
            val high = bytes[offset + 1].toInt() and 0xFF
            return low or (high shl 8)
        }

        private fun readUint32LE(bytes: ByteArray, offset: Int): Int {
            val b0 = bytes[offset].toInt() and 0xFF
            val b1 = bytes[offset + 1].toInt() and 0xFF
            val b2 = bytes[offset + 2].toInt() and 0xFF
            val b3 = bytes[offset + 3].toInt() and 0xFF
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        private fun serializeCalibrationSettings(settings: MedallionCalibrationSettings): ByteArray {
            val version: Int = 1
            val coldWarm: Int = settings.coldRssi
            val warmHot: Int = settings.warmRssi
            val hotFound: Int = settings.hotRssi

            val buf = ByteArray(12)
            buf[0] = (version and 0xFF).toByte()
            buf[1] = ((version shr 8) and 0xFF).toByte()
            buf[2] = (coldWarm and 0xFF).toByte()
            buf[3] = ((coldWarm ushr 8) and 0xFF).toByte()
            buf[4] = (warmHot and 0xFF).toByte()
            buf[5] = ((warmHot ushr 8) and 0xFF).toByte()
            buf[6] = (hotFound and 0xFF).toByte()
            buf[7] = ((hotFound ushr 8) and 0xFF).toByte()

            val crc = CRC32().also { it.update(buf, 0, 8) }.value.toInt()
            buf[8] = (crc and 0xFF).toByte()
            buf[9] = ((crc shr 8) and 0xFF).toByte()
            buf[10] = ((crc shr 16) and 0xFF).toByte()
            buf[11] = ((crc shr 24) and 0xFF).toByte()

            return buf
        }

        private fun parseCalibrationBytes(bytes: ByteArray): MedallionCalibrationSettings? {
            if (bytes.size != 12) return null

            val version = readUint16LE(bytes, 0)
            if (version != 1) return null

            val coldWarm = readInt16LE(bytes, 2)
            val warmHot = readInt16LE(bytes, 4)
            val hotFound = readInt16LE(bytes, 6)

            if (warmHot !in coldWarm..hotFound) return null

            val storedCrc = readUint32LE(bytes, 8)
            val computedCrc = CRC32().also { it.update(bytes, 0, 8) }.value.toInt()
            if (storedCrc != computedCrc) return null

            return MedallionCalibrationSettings(coldWarm, warmHot, hotFound)
        }
    }
}
