package com.example.raksha.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = PinkPrimary,
    secondary = PinkSecondary,
    tertiary = PinkAccent,

    background = AppBackground,
    surface = CardSurface,
    surfaceVariant = CardSurfaceSoft,

    onPrimary = OnPink,
    onSecondary = TextMain,
    onTertiary = TextMain,

    onBackground = TextMain,
    onSurface = TextMain,
    onSurfaceVariant = TextMain,
)

private val DarkColors = darkColorScheme(
    primary = PinkSecondary,
    secondary = PinkAccent,
    tertiary = PinkPrimary,

    onPrimary = TextMain,
)

@Composable
fun RakshaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep false so your pink brand stays consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
