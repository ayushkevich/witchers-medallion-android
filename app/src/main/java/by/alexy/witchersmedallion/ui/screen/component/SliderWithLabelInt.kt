package by.alexy.witchersmedallion.ui.screen.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import by.alexy.witchersmedallion.R

@Composable
fun SliderWithLabelInt(label: @Composable () -> Unit, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text(stringResource(R.string.slider_label_format, label().toString(), value))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = -100f..0f,
            steps = 100,
        )
    }
}
