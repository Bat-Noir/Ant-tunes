package com.ant.tunes.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🟢 1. Create the Local Provider to hold the active accent color
val LocalAccentColor = compositionLocalOf { Color(0xFF0EA5E9) }

@Composable
fun AntTunesTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    var accentColorInt by remember { mutableIntStateOf(prefs.getInt("accent_color", 0xFF0EA5E9.toInt())) }
    var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var isAmoled by remember { mutableStateOf(prefs.getBoolean("amoled_black", true)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "accent_color" -> accentColorInt = sharedPreferences.getInt(key, 0xFF0EA5E9.toInt())
                "dark_mode"    -> isDark = sharedPreferences.getBoolean(key, true)
                "amoled_black" -> isAmoled = sharedPreferences.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val primaryAccent = Color(accentColorInt)

    val darkBg = if (isAmoled) Color(0xFF000000) else Color(0xFF121212)
    val darkSurf1 = if (isAmoled) Color(0x0AFFFFFF) else Color(0xFF1E1E1E)
    val darkSurf2 = if (isAmoled) Color(0x12FFFFFF) else Color(0xFF2C2C2C)

    val AntDarkColors = darkColorScheme(
        primary         = primaryAccent,
        onPrimary       = darkBg,
        secondary       = primaryAccent.copy(alpha = 0.8f),
        onSecondary     = darkBg,
        tertiary        = primaryAccent.copy(alpha = 0.6f),
        background      = darkBg,
        onBackground    = Color.White,
        surface         = darkSurf1,
        onSurface       = Color.White,
        surfaceVariant  = darkSurf2,
        outline         = Color(0x17FFFFFF),
        outlineVariant  = primaryAccent.copy(alpha = 0.3f)
    )

    val AntBrightColors = lightColorScheme(
        primary         = primaryAccent,
        onPrimary       = Color.White,
        secondary       = primaryAccent.copy(alpha = 0.8f),
        onSecondary     = Color.White,
        tertiary        = primaryAccent.copy(alpha = 0.6f),
        background      = Color(0xFFF8FAFC),
        onBackground    = Color(0xFF0F172A),
        surface         = Color(0xFFF8FAFC),
        onSurface       = Color(0xFF0F172A),
        surfaceVariant  = Color(0x0A000000),
        outline         = Color(0x1A000000)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> AntDarkColors
        else   -> AntBrightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    // 🟢 2. Provide the live accent color to the rest of the app
    CompositionLocalProvider(LocalAccentColor provides primaryAccent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AntTypography,
            content     = content
        )
    }
}
