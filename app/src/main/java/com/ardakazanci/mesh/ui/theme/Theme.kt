package com.ardakazanci.mesh.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MeshPrimary80,
    secondary = MeshSecondary80,
    tertiary = MeshTertiary80,
    background = Color(0xFF0B0F14),
    surface = Color(0xFF111317),
    onPrimary = Color(0xFF08111C),
    onSecondary = Color(0xFF251406),
    onTertiary = Color(0xFF1D1814),
    onBackground = Color(0xFFEAEFF5),
    onSurface = Color(0xFFEAEFF5)
)

private val LightColorScheme = lightColorScheme(
    primary = MeshPrimary40,
    secondary = MeshSecondary40,
    tertiary = MeshTertiary40,
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF10161D),
    onSurface = Color(0xFF10161D)
)

@Composable
fun MeshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
