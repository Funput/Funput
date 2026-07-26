package app.funput.funput.keyboard.ui.emoji

internal enum class EmojiSearchMode {
    BROWSING,
    EDITING,
    SHOWING_RESULTS,
}

internal data class EmojiSearchState(
    val mode: EmojiSearchMode = EmojiSearchMode.BROWSING,
    val query: String = "",
)

internal class EmojiSearchController(private val changed: (EmojiSearchState) -> Unit) {
    var state = EmojiSearchState()
        private set

    fun begin() = update(state.copy(mode = EmojiSearchMode.EDITING))
    fun input(text: String) = update(state.copy(query = state.query + text))
    fun space() {
        if (state.query.isNotEmpty() && !state.query.endsWith(' ')) input(" ")
    }
    fun backspace() = update(state.copy(query = state.query.dropLast(1)))
    fun clear() = update(state.copy(query = ""))
    fun done() = update(state.copy(mode = EmojiSearchMode.SHOWING_RESULTS))
    fun cancel() = update(EmojiSearchState())
    fun reset() = update(EmojiSearchState())

    private fun update(value: EmojiSearchState) {
        if (state == value) return
        state = value
        changed(value)
    }
}
