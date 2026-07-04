package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyAction

/** Maps keyboard semantics to operations supported by the active text editor. */
internal fun KeyAction.toImeEditCommand(
    enterCommand: ImeEditCommand = ImeEditCommand.CommitText("\n"),
): ImeEditCommand? = when (this) {
    is KeyAction.Input -> ImeEditCommand.CommitText(text)
    KeyAction.Space -> ImeEditCommand.CommitText(" ")
    KeyAction.Enter -> enterCommand
    KeyAction.Backspace -> ImeEditCommand.DeleteBackward
    is KeyAction.Shift,
    is KeyAction.ToggleLanguage,
    KeyAction.Symbols,
    KeyAction.MoreSymbols,
    KeyAction.Letters,
    -> null
}
