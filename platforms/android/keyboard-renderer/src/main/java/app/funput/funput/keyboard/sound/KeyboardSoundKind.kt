package app.funput.funput.keyboard.sound

import app.funput.funput.keyboard.KeyboardHapticType

/** The three sounds each bundled style has, split the way the system keyboard's are. */
internal enum class KeyboardSoundKind {
    KEY,
    DELETE,
    MODIFIER,
    ;

    companion object {
        /** The sound for a key press of [type]: space, return and the other controls share the modifier. */
        fun of(type: KeyboardHapticType): KeyboardSoundKind = when (type) {
            KeyboardHapticType.KEY_PRESS -> KEY
            KeyboardHapticType.DELETE,
            KeyboardHapticType.DELETE_REPEAT,
            -> DELETE
            KeyboardHapticType.SPACE,
            KeyboardHapticType.CONTROL,
            KeyboardHapticType.SUBMIT,
            -> MODIFIER
        }
    }
}
