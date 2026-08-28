package app.funput.funput.ime.editing.caret

/**
 * The text on either side of the caret, plus where the caret sits in the host document.
 *
 * [caretPosition] is absolute because `setSelection` takes an absolute index. It is the caret's
 * real position when the host hands back an `ExtractedText`, and `before.length` when only the
 * windowed fallback is available.
 */
internal data class KeyboardCaretContext(
    val before: String,
    val after: String,
    val caretPosition: Int,
)
