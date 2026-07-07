package app.funput.funput.ui.theme.custom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal data class AccentPreset(
    val label: String,
    val color: Color,
) {
    val argb: Int = color.toArgb()
}

internal val AccentPresets = listOf(
    AccentPreset("Vàng Funput", Color(0xFFC8A951)),
    AccentPreset("Cam nắng", Color(0xFFFF8A2A)),
    AccentPreset("Tím mơ", Color(0xFF9F5CFF)),
    AccentPreset("Xanh biển", Color(0xFF2F9BFF)),
    AccentPreset("Hồng đào", Color(0xFFFF5C8A)),
)
