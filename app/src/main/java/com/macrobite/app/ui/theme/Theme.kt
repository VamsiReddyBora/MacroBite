package com.macrobite.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppThemePreset = staticCompositionLocalOf { AppThemePreset.AMBER }
val LocalAppPrimaryColor = staticCompositionLocalOf { Color(0xFFF59E0B) }

private val DarkColorScheme = darkColorScheme(
    primary = CalorieAmber,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = ProteinBlue,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = CarbsGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    surfaceTint = Color.Transparent
)

private val LightColorScheme = lightColorScheme(
    primary = CalorieAmber,
    onPrimary = Color.Black,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    secondary = ProteinBlue,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = CarbsGreen,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MacroBiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: AppThemePreset = AppThemePreset.AMBER,
    themeId: String = themePreset.id,
    content: @Composable () -> Unit
) {
    val resolvedTheme = AppThemePreset.resolveTheme(themeId)
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = baseScheme.copy(
        primary = resolvedTheme.primaryColor,
        onPrimary = resolvedTheme.onPrimaryColor,
        primaryContainer = if (darkTheme) resolvedTheme.primaryColor.copy(alpha = 0.20f) else resolvedTheme.primaryColor.copy(alpha = 0.12f),
        onPrimaryContainer = if (darkTheme) Color.White else Color.Black,
        surfaceTint = Color.Transparent
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            } catch (_: Throwable) {
                // Ignore gracefully on devices where status bar / insets controller is not supported
            }
        }
    }

    CompositionLocalProvider(
        LocalAppThemePreset provides AppThemePreset.fromId(resolvedTheme.id),
        LocalAppPrimaryColor provides resolvedTheme.primaryColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
