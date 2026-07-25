package app.funput.funput.ui.theme.custom.color

import android.graphics.Color as AndroidColor

/**
 * HSV plus alpha, kept apart from the packed ARGB value.
 *
 * Round-tripping through ARGB on every gesture would make the picker drift: a fully black or
 * fully unsaturated color has no meaningful hue to convert back to, so the hue slider would jump
 * to zero as soon as the user dragged value to the bottom. Holding the components lets the field
 * stay where the finger left it.
 */
internal data class ColorPickerState(
    val hue: Float,
    val saturation: Float,
    val value: Float,
    val alpha: Float,
) {
    val argb: Int
        get() = AndroidColor.HSVToColor(
            (alpha * MaxAlpha).toInt().coerceIn(0, MaxAlpha),
            floatArrayOf(hue, saturation, value),
        )

    /** The same color at full alpha, for the alpha bar's own gradient. */
    val opaqueArgb: Int
        get() = AndroidColor.HSVToColor(MaxAlpha, floatArrayOf(hue, saturation, value))

    val hex: String get() = String.format("#%06X", opaqueArgb and RgbMask)

    internal companion object {
        fun from(argb: Int): ColorPickerState {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(argb, hsv)
            return ColorPickerState(
                hue = hsv[0],
                saturation = hsv[1],
                value = hsv[2],
                alpha = (argb ushr AlphaShift) / MaxAlpha.toFloat(),
            )
        }

        /** Accepts `#RRGGBB` or `RRGGBB`; anything else leaves the current color alone. */
        fun fromHexOrNull(text: String, alpha: Float): ColorPickerState? {
            val digits = text.removePrefix("#")
            if (digits.length != HexDigits || digits.any { it.digitToIntOrNull(16) == null }) {
                return null
            }
            val rgb = digits.toInt(16)
            return from(rgb or (MaxAlpha shl AlphaShift)).copy(alpha = alpha)
        }

        private const val MaxAlpha = 255
        private const val AlphaShift = 24
        private const val RgbMask = 0x00FFFFFF
        private const val HexDigits = 6
    }
}
