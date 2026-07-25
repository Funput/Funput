package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection

/** Longest Vietnamese syllable is 7 characters (`nghiêng`); this leaves slack. */
private const val WordLookback = 12

/**
 * The unbroken run of non-boundary characters immediately before the caret, or null
 * when the caret sits on a boundary (or the editor returns nothing).
 *
 * Reads at most [WordLookback] characters: enough for any Vietnamese syllable, and
 * small enough to stay cheap on the one keystroke that asks for it.
 */
internal fun InputConnection.wordBeforeCursor(): String? {
    val tail = getTextBeforeCursor(WordLookback, 0)?.toString() ?: return null
    var start = tail.length
    while (start > 0) {
        val codePoint = tail.codePointBefore(start)
        if (CompositionBoundary.isBoundary(codePoint)) break
        start -= Character.charCount(codePoint)
    }
    return tail.substring(start).ifEmpty { null }
}
