package by.alexy.witchersmedallion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import by.alexy.witchersmedallion.ui.screen.SwipeableTabs
import by.alexy.witchersmedallion.ui.theme.WitchersMedallionTheme
import by.alexy.witchersmedallion.viewmodel.CalibrationViewModel
import by.alexy.witchersmedallion.viewmodel.MacTrackingViewModel
import by.alexy.witchersmedallion.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val calibrationViewModel: CalibrationViewModel by viewModels()

    private val macTrackingViewModel: MacTrackingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WitchersMedallionTheme {
                SwipeableTabs(mainViewModel, calibrationViewModel, macTrackingViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mainViewModel.stopScan()
    }
}
