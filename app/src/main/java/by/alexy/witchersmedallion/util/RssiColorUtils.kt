package by.alexy.witchersmedallion.util

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

object RssiColorUtils {
    fun getRssiColor(rssi: Int, colors: ColorScheme): Color = when {
        rssi >= -50 -> colors.primary
        rssi >= -65 -> Color(0xFFFFB300)
        rssi >= -80 -> Color(0xFFFF7043)
        else -> Color.Red
    }
}
