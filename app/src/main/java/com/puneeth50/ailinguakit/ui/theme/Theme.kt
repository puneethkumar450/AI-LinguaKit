package com.puneeth50.ailinguakit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    secondary = AccentGoldDark,
    background = BrandSurfaceDark,
    surface = BrandPanelDark,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    primaryContainer = BrandBlue.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.72f),
    surfaceVariant = BrandPanelDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineDark.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    secondary = AccentGold,
    background = BrandSurface,
    surface = BrandPanel,
    onPrimary = Color.White,
    onSecondary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    primaryContainer = BrandBlue.copy(alpha = 0.14f),
    onPrimaryContainer = TextPrimaryLight,
    onSurfaceVariant = TextPrimaryLight.copy(alpha = 0.72f),
    surfaceVariant = Color.White,
    outline = BrandOutline,
    outlineVariant = BrandOutline.copy(alpha = 0.58f)
)

@Composable
fun TranslateAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
