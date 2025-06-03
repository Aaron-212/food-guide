package personal.aaron212.foodguide.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomColorsPalette(
    val vegGreen: Color = Color.Unspecified,
    val meatRed: Color = Color.Unspecified,
    val mainYellow: Color = Color.Unspecified,
    val toolCyan: Color = Color.Unspecified,
)

val LightCustomColorsPalette = CustomColorsPalette(
    vegGreen = Color(color = 0xFF35C759),
    meatRed = Color(color = 0xFFFF3B2F),
    mainYellow = Color(color = 0xFFFFCC02),
    toolCyan = Color(color = 0xFF31ADE6)
)

val DarkCustomColorsPalette = CustomColorsPalette(
    vegGreen = Color(color = 0xFF31D158),
    meatRed = Color(color = 0xFFFF453A),
    mainYellow = Color(color = 0xFFFED709),
    toolCyan = Color(color = 0xFF64D3FF)
)

val LocalCustomColorsPalette = staticCompositionLocalOf { CustomColorsPalette() }
