package by.alexy.witchersmedallion.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import by.alexy.witchersmedallion.R
import by.alexy.witchersmedallion.viewmodel.CalibrationViewModel
import by.alexy.witchersmedallion.viewmodel.MacTrackingViewModel
import by.alexy.witchersmedallion.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SwipeableTabs(
    mainViewModel: MainViewModel,
    calibrationViewModel : CalibrationViewModel,
    macTrackingViewModel : MacTrackingViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.main_tab),
        stringResource(R.string.calibration_tab),
        stringResource(R.string.mac_settings_tab)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (pagerState.currentPage) {
                        0 -> MainScreen(mainViewModel)
                        1 -> CalibrationScreen(calibrationViewModel)
                        2 -> MacTrackingScreen(macTrackingViewModel)
                    }
                }
            }
        }
    }
}