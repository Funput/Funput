package app.funput.funput.keyboard.ui.emoji.search

import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.localtext.LocalTextComposing
import app.funput.funput.keyboard.ui.localtext.LocalTextEdit
import app.funput.funput.keyboard.ui.localtext.PlainLocalTextComposer

internal enum class EmojiSearchMode {
    BROWSING,
    EDITING,
    SHOWING_RESULTS,
}

/** [inputMethod] is the one the query composes with, which shapes the search keys. */
internal data class EmojiSearchState(
    val mode: EmojiSearchMode = EmojiSearchMode.BROWSING,
    val query: String = "",
    val inputMethod: KeyboardInputMethod? = null,
)

/**
 * Drives the search panel; the query itself lives in [field], so the IME can make it
 * compose Vietnamese without this class knowing about the engine.
 */
internal class EmojiSearchController(private val changed: (EmojiSearchState) -> Unit) {
    var field: () -> LocalTextComposing = PlainLocalTextComposer().let { plain -> { plain } }
    var state = EmojiSearchState()
        private set

    /** A search opened from the browser starts from a fresh field with the current settings. */
    fun begin() {
        val field = field()
        if (state.mode == EmojiSearchMode.BROWSING) field.reset()
        update(state.copy(mode = EmojiSearchMode.EDITING, query = field.text, inputMethod = field.inputMethod))
    }

    fun input(text: String) = edit(LocalTextEdit.Text(text))
    fun space() = edit(LocalTextEdit.Space)
    fun backspace() = edit(LocalTextEdit.DeleteBackward)
    fun clear() {
        field().reset()
        update(state.copy(query = ""))
    }
    fun done() = update(state.copy(mode = EmojiSearchMode.SHOWING_RESULTS))
    fun cancel() = reset()
    fun reset() {
        field().reset()
        update(EmojiSearchState())
    }

    private fun edit(edit: LocalTextEdit) {
        val field = field()
        field.apply(edit)
        update(state.copy(query = field.text))
    }

    private fun update(value: EmojiSearchState) {
        if (state == value) return
        state = value
        changed(value)
    }
}
