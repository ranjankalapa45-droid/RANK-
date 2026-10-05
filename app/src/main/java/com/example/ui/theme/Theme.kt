package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = FreshBlue80,
    secondary = FreshGreen80,
    tertiary = FreshBluePrimary,
    background = FreshDarkSurface,
    surface = Color(0xFF1E293B),
    onPrimary = Color(0xFF00354E),
    onSurface = Color(0xFFF1F5F9),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = FreshBluePrimary,
    secondary = FreshBlueDark,
    tertiary = FreshGreenAccent,
    background = FreshBackgroundLight,
    surface = FreshSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = FreshTextPrimary,
    onSurface = FreshTextPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our signature FreshCare branding by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
