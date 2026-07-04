package app.funput.funput.ui.playground

import app.funput.funput.keyboard.model.SuggestionSelection

/** Applies non-keyboard callbacks that commit text into the playground. */
internal object PlaygroundCallbackReducer {
    fun emojiSelected(buffer: PlaygroundTextBuffer, emoji: String): PlaygroundTextBuffer =
        buffer.insert(emoji)

    fun suggestionSelected(
        buffer: PlaygroundTextBuffer,
        selection: SuggestionSelection,
    ): PlaygroundTextBuffer {
        val range = PlaygroundTokenBoundary.rangeAt(buffer.text, buffer.cursor)
            ?: return buffer.insert(selection.text)
        return buffer.replace(range.first, range.last + 1, selection.text)
    }
}
