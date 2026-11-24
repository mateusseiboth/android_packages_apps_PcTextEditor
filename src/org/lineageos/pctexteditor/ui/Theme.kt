package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.lineageos.pctexteditor.ThemeMode

/** Material 3 with the system's dynamic colors, like the other desktop apps. */
@Composable
fun TextEditorTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mapOf(
        ThemeMode.SYSTEM to isSystemInDarkTheme(),
        ThemeMode.LIGHT to false,
        ThemeMode.DARK to true,
    ).getValue(mode)
    val context = LocalContext.current
    val colors = dynamicDarkColorScheme(context).takeIf { dark } ?: dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colors, content = content)
}
