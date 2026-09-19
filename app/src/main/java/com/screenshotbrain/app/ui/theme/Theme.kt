package com.trove.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// primary is ink (black in light, near-white in dark), so Buttons, selected chips and FABs render
// as the high-contrast pills from the reference design. secondary carries the periwinkle accent.
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Ink,
    onPrimaryContainer = Color.White,
    secondary = Periwinkle,
    onSecondary = Color.White,
    secondaryContainer = PeriwinkleContainer,
    onSecondaryContainer = OnPeriwinkleContainer,
    tertiary = Peach,
    tertiaryContainer = PeachContainer,
    onTertiaryContainer = OnPeachContainer,
    background = PaperLight,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = PillLight,
    onSurfaceVariant = MutedLight,
    surfaceContainerLowest = CardLight,
    surfaceContainerLow = CardLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = CardLight,
    surfaceContainerHighest = PillLight,
    outline = OutlineLight,
    outlineVariant = HairlineLight,
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    primaryContainer = InkDark,
    onPrimaryContainer = PaperDark,
    secondary = PeriwinkleDark,
    onSecondary = PaperDark,
    secondaryContainer = PeriwinkleContainerDark,
    onSecondaryContainer = OnPeriwinkleContainerDark,
    tertiary = PeachDark,
    tertiaryContainer = PeachContainerDark,
    onTertiaryContainer = PeachDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = PillDark,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = CardDark,
    surfaceContainerLow = CardDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardDark,
    surfaceContainerHighest = PillDark,
    outline = OutlineDark,
    outlineVariant = HairlineDark,
)

@Composable
fun ScreenshotBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    // Status bar icons follow the app theme, not the system one, since users can override it.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
