package dev.patryk.score66.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = NearBlack,
    surface = Surface,
    primary = Accent,
    onPrimary = OnSurface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    secondary = AccentDim,
    onSecondary = OnSurface,
)

@Composable
fun Score66Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
