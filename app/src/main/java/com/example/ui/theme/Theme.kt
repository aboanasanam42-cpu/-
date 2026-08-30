package com.example.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlue,
    secondary = MedicalTeal,
    tertiary = MedicalGold,
    background = Color(0xFF101622),
    surface = Color(0xFF182234),
    surfaceVariant = Color(0xFF223048),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF0F4F8),
    onSurface = Color(0xFFF0F4F8)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalNavy,
    secondary = MedicalTeal,
    tertiary = MedicalGold,
    background = SurfaceBackground,
    surface = SurfaceCard,
    surfaceVariant = Color(0xFFEEF2F6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = MedicalNavyDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = SurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branded medical look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
