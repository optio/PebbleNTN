package com.pebblentn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 with the system's dynamic colours (spec/400-ui), or, when the user opts in, the Pebble
 * monochrome theme: black and white like a Pebble screen. Error red is kept in both, so destructive
 * actions still stand out. minSdk is 31, so dynamic colour is always available.
 */
@Composable
fun PebbleNtnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    monochrome: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        monochrome -> if (darkTheme) MonochromeDark else MonochromeLight
        darkTheme -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private val MonochromeLight: ColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6E6),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF3D3D3D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF3D3D3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6E6E6),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF474747),
    outline = Color(0xFF7A7A7A),
)

private val MonochromeDark: ColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2E2E2E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFCFCFCF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFCFCFCF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2E2E2E),
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF8A8A8A),
)
