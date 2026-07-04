package app.funput.funput.ime.editing

/** Editor operation independent from Android's input-connection lifecycle. */
internal sealed interface ImeEditCommand {
    data class CommitText(val text: String) : ImeEditCommand {
        init {
            require(text.isNotEmpty()) { "Committed text must not be empty" }
        }
    }

    data object DeleteBackward : ImeEditCommand
    data class PerformEditorAction(val actionId: Int) : ImeEditCommand
}
