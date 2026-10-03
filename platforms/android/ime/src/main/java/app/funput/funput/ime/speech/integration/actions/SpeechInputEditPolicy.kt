package app.funput.funput.ime.speech.integration.actions

import app.funput.funput.keyboard.model.KeyAction

/** Only editor commands require a new caret confirmation; cancellation alone never does. */
internal object SpeechInputEditPolicy {
    fun expectsSelection(action: KeyAction): Boolean = when (action) {
        is KeyAction.Input, KeyAction.Space, KeyAction.Backspace, KeyAction.Enter,
        is KeyAction.MoveCursor, KeyAction.DeleteWord, is KeyAction.ToggleLanguage -> true
        is KeyAction.Shift, KeyAction.Symbols, KeyAction.MoreSymbols,
        KeyAction.Letters, KeyAction.SwitchInputMethod -> false
    }
}
