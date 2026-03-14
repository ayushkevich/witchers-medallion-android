package by.alexy.witchersmedallion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BootstrapColorScheme = darkColorScheme(
    primary = BootstrapPrimary,
    secondary = BootstrapSecondary,
    background = BootstrapDark,
    surface = BootstrapDark,
    onPrimary = BootstrapLight,
    onSecondary = BootstrapLight,
    onBackground = BootstrapLight,
    onSurface = BootstrapLight
)

@Composable
fun WitchersMedallionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BootstrapColorScheme,
        typography = Typography,
        content = content
    )
}