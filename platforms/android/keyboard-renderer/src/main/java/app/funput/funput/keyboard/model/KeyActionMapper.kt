package app.funput.funput.keyboard.model

internal fun KeySpec.toKeyAction(shiftState: ShiftState): KeyAction? = when (role) {
    KeyRole.CHARACTER -> KeyAction.Input(
        keyId = id,
        text = if (shiftState.isActive) shiftedLabel ?: label else label,
    )

    KeyRole.VNI_MODIFIER,
    KeyRole.PUNCTUATION,
    -> KeyAction.Input(keyId = id, text = label)

    KeyRole.SHIFT -> KeyAction.Shift(shiftState)
    KeyRole.BACKSPACE -> KeyAction.Backspace
    KeyRole.SYMBOLS -> KeyAction.Symbols
    KeyRole.MORE_SYMBOLS -> KeyAction.MoreSymbols
    KeyRole.LETTERS -> KeyAction.Letters
    KeyRole.EMOJI -> null
    KeyRole.SPACE -> KeyAction.Space
    KeyRole.ENTER -> KeyAction.Enter
}
