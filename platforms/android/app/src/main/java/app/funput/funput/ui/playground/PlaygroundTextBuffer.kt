package app.funput.funput.ui.playground

/** Immutable playground text state. Cursor positions use Android-compatible UTF-16 offsets. */
@ConsistentCopyVisibility
internal data class PlaygroundTextBuffer private constructor(
    val text: String,
    val cursor: Int,
) {
    fun insert(value: String): PlaygroundTextBuffer {
        if (value.isEmpty()) return this
        val updatedText = text.substring(0, cursor) + value + text.substring(cursor)
        return PlaygroundTextBuffer(updatedText, cursor + value.length)
    }

    fun backspace(): PlaygroundTextBuffer {
        if (cursor == 0) return this
        val deleteFrom = PlaygroundGraphemeNavigator.previous(text, cursor)
        return PlaygroundTextBuffer(text.removeRange(deleteFrom, cursor), deleteFrom)
    }

    fun moveLeft(): PlaygroundTextBuffer =
        PlaygroundTextBuffer(text, PlaygroundGraphemeNavigator.previous(text, cursor))

    fun moveRight(): PlaygroundTextBuffer =
        PlaygroundTextBuffer(text, PlaygroundGraphemeNavigator.next(text, cursor))

    fun moveCursorTo(offset: Int): PlaygroundTextBuffer =
        from(text, PlaygroundGraphemeNavigator.floor(text, offset))

    fun clear(): PlaygroundTextBuffer = if (text.isEmpty()) this else Empty

    companion object {
        val Empty = PlaygroundTextBuffer(text = "", cursor = 0)

        fun from(text: String, cursor: Int = text.length): PlaygroundTextBuffer = PlaygroundTextBuffer(
            text = text,
            cursor = PlaygroundGraphemeNavigator.floor(text, cursor),
        )
    }
}
