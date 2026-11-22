package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.suggestion.ClipboardOfferChip

internal object KeyHapticTypeMapper {
    /** [targetId] names a band target that is neither a key nor a candidate: the paste chip. */
    fun forTarget(key: KeySpec?, isSuggestion: Boolean, targetId: String? = null): KeyboardHapticType? = when {
        key != null -> forRole(key.role)
        isSuggestion || targetId == ClipboardOfferChip.TargetId -> KeyboardHapticType.CONTROL
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
        KeyRole.PLACEMENT,
        KeyRole.CLIPBOARD,
        KeyRole.EMOJI,
        KeyRole.MICROPHONE,
        -> KeyboardHapticType.CONTROL

        KeyRole.BACKSPACE -> KeyboardHapticType.DELETE
        KeyRole.ENTER -> KeyboardHapticType.SUBMIT
    }
}
