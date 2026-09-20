package fr.corentin.biblioscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1B4332)
private val PrimaryDark = Color(0xFF74C69D)

private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Color(0xFF2D6A4F)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    secondary = Color(0xFF95D5B2)
)

@Composable
fun BiblioScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
