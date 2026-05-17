package by.alexy.witchersmedallion.ui.screen.component.calibration

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.screen.component.SliderWithLabelInt

@Composable
fun ManualCalibrationComponent(
    coldRssi: Int,
    warmRssi: Int,
    hotRssi: Int,
    onColdRssiChange: (Int) -> Unit,
    onWarmRssiChange: (Int) -> Unit,
    onHotRssiChange: (Int) -> Unit,
) {
    SliderWithLabelInt(
        label = { Text(stringResource(R.string.cold_warm)) },
        value = coldRssi,
        onValueChange = { onColdRssiChange(it) },
    )

    Spacer(modifier = Modifier.height(16.dp))

    SliderWithLabelInt(
        label = { Text(stringResource(R.string.warm_hot)) },
        value = warmRssi,
        onValueChange = { onWarmRssiChange(it) },
    )

    Spacer(modifier = Modifier.height(16.dp))

    SliderWithLabelInt(
        label = { Text(stringResource(R.string.hot_found)) },
        value = hotRssi,
        onValueChange = { onHotRssiChange(it) },
    )
}
