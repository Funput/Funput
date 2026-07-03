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

    data object Shift : KeyAction
    data object Backspace : KeyAction
    data object Symbols : KeyAction
    data object Emoji : KeyAction
    data object Space : KeyAction
    data object Enter : KeyAction
    data object ToggleLanguage : KeyAction
}
