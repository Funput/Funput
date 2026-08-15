package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec

internal object KeyHapticTypeMapper {
    fun forTarget(key: KeySpec?, isSuggestion: Boolean): KeyboardHapticType? = when {
        key != null -> forRole(key.role)
        isSuggestion -> KeyboardHapticType.CONTROL
        else -> null
    }

    private fun forRole(role: KeyRole): KeyboardHapticType? = when (role) {
        KeyRole.PLACEHOLDER -> null
        KeyRole.CHARACTER,
        KeyRole.VNI_MODIFIER,
        KeyRole.PUNCTUATION,
        -> KeyboardHapticType.KEY_PRESS

        KeyRole.SPACE -> KeyboardHapticType.SPACE

        KeyRole.SHIFT,
        KeyRole.SYMBOLS,
        KeyRole.MORE_SYMBOLS,
        KeyRole.LETTERS,
        KeyRole.SYSTEM_INPUT_METHOD,
        KeyRole.CLIPBOARD,
        KeyRole.EMOJI,
        -> KeyboardHapticType.CONTROL

        KeyRole.BACKSPACE -> KeyboardHapticType.DELETE
        KeyRole.ENTER -> KeyboardHapticType.SUBMIT
    }
}
