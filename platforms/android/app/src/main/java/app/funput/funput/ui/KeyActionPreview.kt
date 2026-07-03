package app.funput.funput.ui

import app.funput.funput.keyboard.model.KeyAction

internal fun KeyAction?.previewLabel(): String = when (this) {
    is KeyAction.Input -> text
    is KeyAction.Shift -> "Shift: ${state.name}"
    KeyAction.Backspace -> "Backspace"
    KeyAction.Symbols -> "Symbols"
    KeyAction.Emoji -> "Emoji"
    KeyAction.Space -> "Space"
    KeyAction.Enter -> "Enter"
    KeyAction.ToggleLanguage -> "Language"
    null -> "—"
}
