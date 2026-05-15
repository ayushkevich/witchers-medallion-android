package by.alexy.witchersmedallion.util

object MacAddressUtils {
    fun isValidMacAddress(mac: String): Boolean {
        val clean = mac.uppercase().replace(Regex("[^0-9A-F]"), "")
        return clean.length == 12 && clean.matches(Regex("[0-9A-F]{12}"))
    }

    fun isDynamicMac(address: String): Boolean {
        val clean = address.replace(Regex("[^0-9A-Fa-f]"), "").uppercase()
        if (clean.length < 2) return false

        val firstByte = clean.substring(0, 2).toIntOrNull(16) ?: 0
        val secondByte = if (clean.length > 2) clean.substring(2, 4).toIntOrNull(16) ?: 0 else 0
        val thirdByte = if (clean.length > 4) clean.substring(4, 6).toIntOrNull(16) ?: 0 else 0

        val macType = firstByte and 0x01
        val manufacturerSpecific = (firstByte and 0x02) == 0x02
        val macPrefix = firstByte shl 16 or (secondByte shl 8) or thirdByte

        val publicMacRanges = listOf(
            0x000000 to 0x000001,
            0x001A2D to 0x001A2D,
            0x0050C2 to 0x0050C2,
            0x006080 to 0x006080,
            0x00E018 to 0x00E018,
            0x0080F8 to 0x0080F8,
            0x00C09F to 0x00C09F,
            0x00D0B3 to 0x00D0B3,
            0x00D108 to 0x00D108,
            0x00D1B7 to 0x00D1B7,
            0x00D1F6 to 0x00D1F6,
            0x00D201 to 0x00D201,
            0x00D21E to 0x00D21E,
            0x00D262 to 0x00D262,
            0x00D274 to 0x00D274,
            0x00D277 to 0x00D277,
            0x00D278 to 0x00D278,
            0x020000 to 0x02FFFF,
        )

        val isInRange = publicMacRanges.any { (start, end) ->
            macPrefix in start..end
        }

        return macType == 1 || manufacturerSpecific || isInRange
    }
}
