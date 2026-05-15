package by.alexy.witchersmedallion.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MacAddressUtilsTest {

    @ParameterizedTest
    @MethodSource("validMacAddresses")
    fun `isValidMacAddress returns true for valid MAC addresses`(mac: String) {
        assertTrue(MacAddressUtils.isValidMacAddress(mac))
    }

    @ParameterizedTest
    @MethodSource("invalidMacAddresses")
    fun `isValidMacAddress returns false for invalid MAC addresses`(mac: String) {
        assertFalse(MacAddressUtils.isValidMacAddress(mac))
    }

    @Test
    fun `isValidMacAddress returns true for valid MAC with colons`() {
        assertTrue(MacAddressUtils.isValidMacAddress("00:1A:2D:4E:5F:6A"))
    }

    @Test
    fun `isValidMacAddress returns true for valid MAC without separators`() {
        assertTrue(MacAddressUtils.isValidMacAddress("001A2D4E5F6A"))
    }

    @Test
    fun `isValidMacAddress returns true for valid MAC with dashes`() {
        assertTrue(MacAddressUtils.isValidMacAddress("00-1A-2D-4E-5F-6A"))
    }

    @Test
    fun `isValidMacAddress returns false for invalid MAC too short`() {
        assertFalse(MacAddressUtils.isValidMacAddress("00:1A:2D"))
    }

    @Test
    fun `isValidMacAddress returns false for invalid MAC with wrong characters`() {
        assertFalse(MacAddressUtils.isValidMacAddress("GG:HH:II:JJ:KK:LL"))
    }

    @Test
    fun `isValidMacAddress handles lowercase`() {
        assertTrue(MacAddressUtils.isValidMacAddress("00:1a:2d:4e:5f:6a"))
    }

    @ParameterizedTest
    @MethodSource("dynamicMacAddresses")
    fun `isDynamicMac returns true for dynamic MAC addresses`(mac: String) {
        assertTrue(MacAddressUtils.isDynamicMac(mac))
    }

    @ParameterizedTest
    @MethodSource("staticMacAddresses")
    fun `isDynamicMac returns false for static MAC addresses`(mac: String) {
        assertFalse(MacAddressUtils.isDynamicMac(mac))
    }

    companion object {
        @JvmStatic
        fun validMacAddresses() = listOf(
            "00:1A:2D:4E:5F:6A",
            "001A2D4E5F6A",
            "00-1A-2D-4E-5F-6A",
            "00:1a:2d:4e:5f:6a",
        )

        @JvmStatic
        fun invalidMacAddresses() = listOf(
            "00:1A:2D",
            "GG:HH:II:JJ:KK:LL",
        )

        @JvmStatic
        fun dynamicMacAddresses() = listOf(
            "02:00:00:00:00:01",
            "00:D2:01:00:00:00",
        )

        @JvmStatic
        fun staticMacAddresses() = listOf(
            "00:11:22:33:44:55",
            "",
        )
    }
}
