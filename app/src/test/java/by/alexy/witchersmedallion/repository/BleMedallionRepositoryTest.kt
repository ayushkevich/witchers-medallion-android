package by.alexy.witchersmedallion.repository

import by.alexy.witchersmedallion.config.BleConfig
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.zip.CRC32

class BleMedallionRepositoryTest {

    @Test
    fun `parse calibration correctly reads negative int16 LE`() {
        val bytes = byteArrayOf(
            0x01.toByte(), 0x00.toByte(),
            0xB5.toByte(), 0xFF.toByte(),
            0xC4.toByte(), 0xFF.toByte(),
            0xD1.toByte(), 0xFF.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        )

        val crc = CRC32().also { it.update(bytes, 0, 8) }.value.toInt()
        bytes[8] = (crc and 0xFF).toByte()
        bytes[9] = ((crc ushr 8) and 0xFF).toByte()
        bytes[10] = ((crc ushr 16) and 0xFF).toByte()
        bytes[11] = ((crc ushr 24) and 0xFF).toByte()

        val result = parseCalibrationBytes(bytes)

        assertEquals(BleConfig.DEFAULT_COLD_RSSI, result?.coldRssi)
        assertEquals(BleConfig.DEFAULT_WARM_RSSI, result?.warmRssi)
        assertEquals(BleConfig.DEFAULT_HOT_RSSI, result?.hotRssi)
    }

    @Test
    fun `parse calibration rejects wrong version`() {
        val bytes = byteArrayOf(
            0x63.toByte(), 0x00.toByte(),
            0xB5.toByte(), 0xFF.toByte(), 0xC4.toByte(), 0xFF.toByte(), 0xD1.toByte(), 0xFF.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        )

        val result = parseCalibrationBytes(bytes)

        assertNull(result)
    }

    @Test
    fun `parse calibration rejects wrong CRC`() {
        val bytes = byteArrayOf(
            0x01.toByte(), 0x00.toByte(),
            0xB5.toByte(), 0xFF.toByte(), 0xC4.toByte(), 0xFF.toByte(), 0xD1.toByte(), 0xFF.toByte(),
            0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte(),
        )

        val result = parseCalibrationBytes(bytes)

        assertNull(result)
    }

    @Test
    fun `parse calibration rejects wrong length`() {
        val bytes = byteArrayOf(0x01.toByte(), 0x00.toByte())

        val result = parseCalibrationBytes(bytes)

        assertNull(result)
    }

    @Test
    fun `parse calibration rejects invalid threshold order`() {
        val bytes = byteArrayOf(
            0x01.toByte(), 0x00.toByte(),
            0x00.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        )

        val result = parseCalibrationBytes(bytes)

        assertNull(result)
    }

    @Test
    fun `serialize calibration produces correct byte layout`() {
        val settings = MedallionCalibrationSettings(BleConfig.DEFAULT_COLD_RSSI, BleConfig.DEFAULT_WARM_RSSI, BleConfig.DEFAULT_HOT_RSSI)
        val bytes = serializeCalibrationSettings(settings)

        assertEquals(12, bytes.size)
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x00.toByte(), bytes[1])
        assertEquals(0xB5.toByte(), bytes[2])
        assertEquals(0xFF.toByte(), bytes[3])
        assertEquals(0xC4.toByte(), bytes[4])
        assertEquals(0xFF.toByte(), bytes[5])
        assertEquals(0xD1.toByte(), bytes[6])
        assertEquals(0xFF.toByte(), bytes[7])
    }

    @Test
    fun `serialize and parse calibration roundtrip`() {
        val original = MedallionCalibrationSettings(BleConfig.DEFAULT_COLD_RSSI, BleConfig.DEFAULT_WARM_RSSI, BleConfig.DEFAULT_HOT_RSSI)
        val bytes = serializeCalibrationSettings(original)
        val result = parseCalibrationBytes(bytes)

        assertEquals(original.coldRssi, result?.coldRssi)
        assertEquals(original.warmRssi, result?.warmRssi)
        assertEquals(original.hotRssi, result?.hotRssi)
    }

    @Test
    fun `read int16LE reads default cold rssi correctly`() {
        val bytes = byteArrayOf(0xB5.toByte(), 0xFF.toByte())
        val result = readInt16LE(bytes, 0)
        assertEquals(BleConfig.DEFAULT_COLD_RSSI, result)
    }

    @Test
    fun `read int16LE reads -1 correctly`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        val result = readInt16LE(bytes, 0)
        assertEquals(-1, result)
    }

    @Test
    fun `read int16LE reads 0 correctly`() {
        val bytes = byteArrayOf(0x00.toByte(), 0x00.toByte())
        val result = readInt16LE(bytes, 0)
        assertEquals(0, result)
    }

    @Test
    fun `read int16LE reads 1 correctly`() {
        val bytes = byteArrayOf(0x01.toByte(), 0x00.toByte())
        val result = readInt16LE(bytes, 0)
        assertEquals(1, result)
    }

    @Test
    fun `read uint16LE reads 1 correctly`() {
        val bytes = byteArrayOf(0x01.toByte(), 0x00.toByte())
        val result = readUint16LE(bytes, 0)
        assertEquals(1, result)
    }

    @Test
    fun `read uint32LE reads CRC correctly`() {
        val bytes = byteArrayOf(0xC8.toByte(), 0xD6.toByte(), 0xB2.toByte(), 0xE2.toByte())
        val result = readUint32LE(bytes, 0)
        assertEquals(-305419896, result)
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

            if (coldWarm > warmHot || warmHot > hotFound) return null

            val storedCrc = readUint32LE(bytes, 8)
            val computedCrc = CRC32().also { it.update(bytes, 0, 8) }.value.toInt()
            if (storedCrc != computedCrc) return null

            return MedallionCalibrationSettings(coldWarm, warmHot, hotFound)
        }
    }
}
