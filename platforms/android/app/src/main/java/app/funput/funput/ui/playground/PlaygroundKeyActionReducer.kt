package app.funput.funput.ui.playground

import app.funput.funput.keyboard.model.KeyAction

/** Applies semantic keyboard output to the playground without coupling UI to key roles. */
internal object PlaygroundKeyActionReducer {
    fun reduce(buffer: PlaygroundTextBuffer, action: KeyAction): PlaygroundTextBuffer = when (action) {
        is KeyAction.Input -> buffer.insert(action.text)
        KeyAction.Space -> buffer.insert(" ")
        KeyAction.Enter -> buffer.insert("\n")
        KeyAction.Backspace -> buffer.backspace()
        is KeyAction.Shift,
        is KeyAction.ToggleLanguage,
        KeyAction.Symbols,
        KeyAction.MoreSymbols,
        KeyAction.Letters,
        -> buffer
    }
}
