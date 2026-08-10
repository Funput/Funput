package app.funput.funput.ui.theme.custom.metrics

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme

/**
 * The shape of a key, as the four answers people actually have, instead of a radius in dp.
 *
 * A slider labelled "Bo góc 8dp" asks the user to think in the renderer's units about a decision
 * they hold as a picture. The slider is still there under fine-tuning for anyone who wants 9.
 */
internal enum class KeyShapePreset(@param:StringRes val labelRes: Int, val cornerRadiusDp: Float) {
    Square(R.string.custom_theme_shape_square, 2f),
    Soft(R.string.custom_theme_shape_soft, 8f),
    Round(R.string.custom_theme_shape_round, 16f),
    Pill(R.string.custom_theme_shape_pill, 28f),
    ;

    fun applyTo(theme: KeyboardTheme): KeyboardTheme = theme.copy(keyCornerRadiusDp = cornerRadiusDp)

    companion object {
        /** Whichever preset the current radius is nearest, so the row can show where it stands. */
        fun nearest(theme: KeyboardTheme): KeyShapePreset =
            entries.minBy { preset -> kotlin.math.abs(preset.cornerRadiusDp - theme.keyCornerRadiusDp) }
    }
}

/**
 * How much a key stands off the surface.
 *
 * Inset and shadow were two sliders, and nobody moves one without the other: together they are one
 * idea, which is whether the keys look printed on or stacked on top.
 */
internal enum class KeyReliefPreset(
    @param:StringRes val labelRes: Int,
    val keycapInsetDp: Float,
    val shadowOffsetDp: Float,
) {
    Flat(R.string.custom_theme_relief_flat, 0f, 0f),
    Raised(R.string.custom_theme_relief_raised, 2f, 1f),
    Deep(R.string.custom_theme_relief_deep, 4f, 3f),
    ;

    fun applyTo(theme: KeyboardTheme): KeyboardTheme =
        theme.copy(keycapInsetDp = keycapInsetDp, keyShadowOffsetDp = shadowOffsetDp)

    companion object {
        fun nearest(theme: KeyboardTheme): KeyReliefPreset = entries.minBy { preset ->
            kotlin.math.abs(preset.keycapInsetDp - theme.keycapInsetDp) +
                kotlin.math.abs(preset.shadowOffsetDp - theme.keyShadowOffsetDp)
        }
    }
}
