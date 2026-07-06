package app.funput.funput.keyboard.model

/** Visual meaning exposed by the keyboard's context-sensitive Enter key. */
sealed interface KeyboardEnterAction {
    enum class Standard : KeyboardEnterAction {
        NEW_LINE,
        GO,
        SEARCH,
        SEND,
        NEXT,
        DONE,
        PREVIOUS,
    }

    data class Custom(val label: String) : KeyboardEnterAction {
        init {
            require(label.isNotBlank()) { "Custom action label must not be blank" }
        }
    }
}
