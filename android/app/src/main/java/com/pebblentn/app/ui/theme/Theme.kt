package com.pebblentn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 with the system's dynamic colours (spec/400-ui). minSdk is 31, so dynamic colour is
 * always available; the app deliberately has no brand accent of its own (#28: the unused orange
 * fallback was dropped).
 */
@Composable
fun PebbleNtnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        content = content,
    )
}
