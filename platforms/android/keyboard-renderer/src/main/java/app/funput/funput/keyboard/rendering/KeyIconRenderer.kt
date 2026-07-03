package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme

internal class KeyIconRenderer(metrics: RenderMetrics) {
    private val navigationIcons = NavigationKeyIconRenderer(metrics)
    private val utilityIcons = UtilityKeyIconRenderer(metrics)

    fun updateTheme(theme: KeyboardTheme) {
        navigationIcons.updateTheme(theme)
        utilityIcons.updateTheme(theme)
    }

    /** Returns true when the key uses an icon instead of text. */
    fun draw(canvas: Canvas, key: ResolvedKey, shiftState: ShiftState): Boolean {
        when (key.spec.role) {
            KeyRole.SHIFT -> navigationIcons.drawShift(canvas, key, shiftState)
            KeyRole.ENTER -> navigationIcons.drawEnter(canvas, key)
            KeyRole.BACKSPACE -> utilityIcons.drawBackspace(canvas, key)
            KeyRole.EMOJI -> utilityIcons.drawEmoji(canvas, key)
            else -> return false
        }
        return true
    }
}
