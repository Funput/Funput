package app.funput.funput.keyboard.model

internal fun KeySpec.toKeyAction(): KeyAction = when (role) {
    KeyRole.CHARACTER,
    KeyRole.VNI_MODIFIER,
    KeyRole.PUNCTUATION,
    -> KeyAction.Input(keyId = id, text = label)

    KeyRole.SHIFT -> KeyAction.Shift
    KeyRole.BACKSPACE -> KeyAction.Backspace
    KeyRole.SYMBOLS -> KeyAction.Symbols
    KeyRole.EMOJI -> KeyAction.Emoji
    KeyRole.SPACE -> KeyAction.Space
    KeyRole.ENTER -> KeyAction.Enter
}
