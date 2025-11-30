package com.example.veato.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VeatoCoral,
    onPrimary = VeatoSurface,
    primaryContainer = VeatoCoralLight,
    onPrimaryContainer = VeatoTextPrimary,

    secondary = VeatoGolden,
    onSecondary = VeatoTextPrimary,
    secondaryContainer = VeatoGoldenLight,
    onSecondaryContainer = VeatoTextPrimary,

    tertiary = VeatoPeach,
    onTertiary = VeatoSurface,

    error = VeatoError,
    onError = VeatoSurface,
    errorContainer = VeatoWarningLight,
    onErrorContainer = VeatoTextPrimary,

    background = VeatoBackground,
    onBackground = VeatoTextPrimary,

    surface = VeatoSurface,
    onSurface = VeatoTextPrimary,
    surfaceVariant = VeatoBackgroundLight,
    onSurfaceVariant = VeatoTextSecondary,

    outline = Gray300,
    outlineVariant = Gray200
)

private val DarkColorScheme = darkColorScheme(
    primary = VeatoCoralLight,
    onPrimary = Gray900,
    primaryContainer = VeatoCoralDark,
    onPrimaryContainer = Gray100,

    secondary = VeatoGolden,
    onSecondary = Gray900,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray100,

    tertiary = VeatoPeach,
    onTertiary = Gray900,

    error = VeatoError,
    onError = Gray900,

    background = Gray900,
    onBackground = Gray100,

    surface = Gray800,
    onSurface = Gray100,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,

    outline = Gray600,
    outlineVariant = Gray700
)

// Refined shapes with elegant corner radius
private val VeatoShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.cornerRadiusSmall),
    small = RoundedCornerShape(Dimensions.cornerRadiusMedium),
    medium = RoundedCornerShape(Dimensions.cornerRadiusLarge),
    large = RoundedCornerShape(Dimensions.cornerRadiusExtraLarge),
    extraLarge = RoundedCornerShape(Dimensions.cornerRadiusExtraLarge)
)

@Composable
fun VeatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VeatoTypography,
        shapes = VeatoShapes,
        content = content
    )
}
