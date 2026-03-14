package by.alexy.witchersmedallion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import by.alexy.witchersmedallion.repository.bluetooth.impl.BleRepositoryImpl
import by.alexy.witchersmedallion.ui.screen.SwipeableTabs
import by.alexy.witchersmedallion.ui.theme.WitchersMedallionTheme
import by.alexy.witchersmedallion.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WitchersMedallionTheme {
                SwipeableTabs(viewModel)
            }
        }
    }
}
