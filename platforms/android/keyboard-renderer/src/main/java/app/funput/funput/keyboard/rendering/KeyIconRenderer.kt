package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme

internal class KeyIconRenderer(metrics: RenderMetrics) {
    private val navigationIcons = NavigationKeyIconRenderer(metrics)
    private val utilityIcons = UtilityKeyIconRenderer(metrics)
    private val clipboardIcon = ClipboardKeyIconRenderer(metrics)
    private val enterContent = EnterKeyContentRenderer(metrics)

    fun updateTheme(theme: KeyboardTheme) {
        navigationIcons.updateTheme(theme)
        utilityIcons.updateTheme(theme)
        clipboardIcon.updateTheme(theme)
        enterContent.updateTheme(theme)
    }

    /** Returns true when the key uses an icon instead of text. */
    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        shiftState: ShiftState,
        enterAction: KeyboardEnterAction,
    ): Boolean {
        when (key.spec.role) {
            KeyRole.SHIFT -> navigationIcons.drawShift(canvas, key, shiftState)
            KeyRole.ENTER -> enterContent.draw(canvas, key, enterAction)
            KeyRole.BACKSPACE -> utilityIcons.drawBackspace(canvas, key)
            KeyRole.SYSTEM_INPUT_METHOD -> utilityIcons.drawSystemInputMethod(canvas, key)
            KeyRole.CLIPBOARD -> clipboardIcon.draw(canvas, key)
            KeyRole.EMOJI -> utilityIcons.drawEmoji(canvas, key)
            else -> return false
        }
        return true
    }
}
