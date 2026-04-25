package com.smartr.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.compose.ui.platform.LocalContext
import com.smartr.data.ThemePreference

val DarkBrandColorScheme = ColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = ErrorRed,
    onError = DarkOnTertiary, // reusing tertiary text color for simplicity or define ErrorOn
    errorContainer = ErrorContainerRed,
    onErrorContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surfaceContainer = DarkSurfaceContainer,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

val LightBrandColorScheme = ColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = ErrorRed,
    onError = LightOnTertiary,
    errorContainer = ErrorContainerRed,
    onErrorContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surfaceContainer = LightSurfaceContainer,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun SmartRTheme(
    themePreference: ThemePreference = ThemePreference.AUTO,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themePreference) {
        ThemePreference.AUTO -> dynamicColorScheme(context) ?: DarkBrandColorScheme
        ThemePreference.LIGHT -> LightBrandColorScheme
        ThemePreference.DARK -> DarkBrandColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
