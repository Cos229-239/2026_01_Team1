package edu.fullsail.anchor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = DarkBrandPrimary,
    onPrimary = DarkTextPrimary,

    secondary = DarkSecondaryBlue,
    onSecondary = DarkTextPrimary,

    background = DarkNeutralBackground,
    onBackground = DarkTextPrimary,

    surface = DarkNeutralSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkNeutralElevated,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkNeutralBorder,

    error = DarkError,
    onError = DarkTextPrimary
)

private val LightColors = lightColorScheme(
    primary = LightBrandPrimary,
    onPrimary = LightTextPrimary,

    secondary = LightSecondaryBlue,
    onSecondary = LightTextPrimary,

    background = LightNeutralBackground,
    onBackground = LightTextPrimary,

    surface = LightNeutralSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightNeutralElevated,
    onSurfaceVariant = LightTextSecondary,

    outline = LightNeutralBorder,

    error = LightError,
    onError = LightTextPrimary
)

@Composable
fun AnchorTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}