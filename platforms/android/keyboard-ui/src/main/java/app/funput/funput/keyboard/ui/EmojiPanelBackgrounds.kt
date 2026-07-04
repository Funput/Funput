package app.funput.funput.keyboard.ui

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import app.funput.funput.theme.KeyboardTheme

internal object EmojiPanelBackgrounds {
    fun panel(theme: KeyboardTheme): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(theme.backgroundStartColor, theme.backgroundEndColor),
    )

    fun button(theme: KeyboardTheme, density: Float): StateListDrawable = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_pressed),
            rounded(theme.pressedKeyColor, theme.pressedKeyBorderColor, theme, density),
        )
        addState(
            intArrayOf(),
            rounded(theme.specialKeyColor, theme.keyBorderColor, theme, density),
        )
    }

    private fun rounded(
        color: Int,
        borderColor: Int,
        theme: KeyboardTheme,
        density: Float,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = theme.keyCornerRadiusDp * density
        setStroke((theme.keyBorderWidthDp * density).toInt().coerceAtLeast(1), borderColor)
    }
}
