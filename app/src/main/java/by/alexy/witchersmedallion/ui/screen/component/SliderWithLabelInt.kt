package by.alexy.witchersmedallion.ui.screen.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SliderWithLabelInt(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text("$label: $value dBm")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = -100f..0f,
            steps = 100,
        )
    }
}
