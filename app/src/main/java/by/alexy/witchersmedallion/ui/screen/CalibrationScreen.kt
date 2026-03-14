package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.ui.screen.component.SliderWithLabel

@Composable
fun CalibrationScreen() {
    var coldWarm by remember { mutableStateOf(-70f) }
    var warmHot by remember { mutableStateOf(-60f) }
    var hotFound by remember { mutableStateOf(-45f) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.manual_calibration))

        SliderWithLabel(stringResource(R.string.cold_warm), coldWarm) { coldWarm = it }
        SliderWithLabel(stringResource(R.string.warm_hot), warmHot) { warmHot = it }
        SliderWithLabel(stringResource(R.string.hot_found), hotFound) { hotFound = it }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {}) {
                Text(stringResource(R.string.auto_calibration))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {}) {
                Text(stringResource(R.string.save_to_esp32))
            }
        }
    }
}