package app.funput.funput.keyboard.model

/** Semantic keyboard output emitted after a pointer is released over a key. */
sealed interface KeyAction {
    data class Input(
        val keyId: String,
        val text: String,
    ) : KeyAction {
        init {
            require(keyId.isNotBlank()) { "Input key id must not be blank" }
            require(text.isNotEmpty()) { "Input text must not be empty" }
        }
    }

    data class Shift(val state: ShiftState) : KeyAction
    data object Backspace : KeyAction
    data object Symbols : KeyAction
    data object MoreSymbols : KeyAction
    data object Letters : KeyAction
    data object Space : KeyAction
    data object Enter : KeyAction
    data class ToggleLanguage(val language: KeyboardLanguage) : KeyAction
}
