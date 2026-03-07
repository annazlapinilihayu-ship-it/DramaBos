package com.sonzaix.shortxrama.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sonzaix.shortxrama.data.AppSettings

@Composable
fun DramaTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val accentColor = parseColorHex(settings.accentColor, Color(0xFFFF2965))
    val isAmoled = settings.amoledMode
    val bgColor = if (isAmoled) Color.Black else Color(0xFF121212)
    val surfaceColor = if (isAmoled) Color(0xFF0A0A0A) else SurfaceDark
    val surfaceVariantColor = if (isAmoled) Color(0xFF111111) else SurfaceVariantDark

    val darkColors = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        background = bgColor,
        surface = surfaceColor,
        surfaceVariant = surfaceVariantColor,
        onBackground = TextWhite,
        onSurface = TextWhite,
        onSurfaceVariant = TextGray,
        error = Color(0xFFCF6679),
        onError = Color.Black
    )

    MaterialTheme(
        colorScheme = darkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
