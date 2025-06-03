package personal.aaron212.foodguide.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (useDarkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    val customColorsPalette =
        if (useDarkTheme) DarkCustomColorsPalette
        else LightCustomColorsPalette

    CompositionLocalProvider(
        LocalCustomColorsPalette provides customColorsPalette
    ) {
        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }
}
