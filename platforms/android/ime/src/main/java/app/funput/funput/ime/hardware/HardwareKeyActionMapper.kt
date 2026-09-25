package app.funput.funput.ime.hardware

import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.funput.funput.keyboard.model.KeyAction

/**
 * Verdict for one hardware stroke: consume into Funput, show or hide the soft keyboard, commit
 * then pass, or ignore.
 */
internal sealed interface HardwareKeyDecision {
    data class Consume(val action: KeyAction) : HardwareKeyDecision
    data object ToggleSoftKeyboard : HardwareKeyDecision
    data object FinishAndPass : HardwareKeyDecision
    data object Pass : HardwareKeyDecision
}

/** Maps a hardware stroke onto a [HardwareKeyDecision] without touching the editor. */
internal object HardwareKeyActionMapper {
    fun map(stroke: HardwareKeyStroke, hotkeyEnabled: Boolean = false): HardwareKeyDecision {
        if (stroke.isCanceled || stroke.keyCode in PassThroughKeys) return HardwareKeyDecision.Pass
        if (stroke.isCtrl || stroke.isMeta) return HardwareKeyDecision.FinishAndPass
        // Ahead of specialAction, which would otherwise type Alt+Space as a plain space.
        if (hotkeyEnabled && stroke.isSoftKeyboardHotkey) return HardwareKeyDecision.ToggleSoftKeyboard
        specialAction(stroke.keyCode)?.let { return HardwareKeyDecision.Consume(it) }
        if (stroke.keyCode in NavigationKeys) return HardwareKeyDecision.FinishAndPass
        if (stroke.isAlt && printableText(stroke.codePoint) == null) {
            return HardwareKeyDecision.FinishAndPass
        }
        val text = printableText(stroke.codePoint) ?: return HardwareKeyDecision.Pass
        return HardwareKeyDecision.Consume(KeyAction.Input("hardware-${stroke.keyCode}", text))
    }
}

/**
 * Alt+Space. Android keeps every Meta chord and Ctrl+Space for itself, and Shift+Space is too
 * easy to hit while typing capitals; Alt+Space is none of those.
 */
private val HardwareKeyStroke.isSoftKeyboardHotkey: Boolean
    get() = keyCode == KeyEvent.KEYCODE_SPACE && isAlt && !isShift

private fun specialAction(keyCode: Int): KeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_DEL -> KeyAction.Backspace
    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyAction.Enter
    KeyEvent.KEYCODE_SPACE -> KeyAction.Space
    else -> null
}

private fun printableText(codePoint: Int): String? {
    if (codePoint == 0 || codePoint and KeyCharacterMap.COMBINING_ACCENT != 0) return null
    if (Character.isISOControl(codePoint)) return null
    return String(Character.toChars(codePoint))
}

private val PassThroughKeys = setOf(
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_TAB,
    KeyEvent.KEYCODE_SHIFT_LEFT,
    KeyEvent.KEYCODE_SHIFT_RIGHT,
    KeyEvent.KEYCODE_CTRL_LEFT,
    KeyEvent.KEYCODE_CTRL_RIGHT,
    KeyEvent.KEYCODE_ALT_LEFT,
    KeyEvent.KEYCODE_ALT_RIGHT,
    KeyEvent.KEYCODE_META_LEFT,
    KeyEvent.KEYCODE_META_RIGHT,
    KeyEvent.KEYCODE_CAPS_LOCK,
    KeyEvent.KEYCODE_NUM_LOCK,
    KeyEvent.KEYCODE_FUNCTION,
)

private val NavigationKeys = setOf(
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_MOVE_HOME,
    KeyEvent.KEYCODE_MOVE_END,
    KeyEvent.KEYCODE_PAGE_UP,
    KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_FORWARD_DEL,
)
