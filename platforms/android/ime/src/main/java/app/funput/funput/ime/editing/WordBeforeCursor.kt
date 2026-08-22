package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.composition.CompositionBoundary

/** Longest Vietnamese syllable is 7 characters (`nghiêng`); this leaves slack. */
private const val WordLookback = 12

/**
 * The unbroken run of non-boundary characters immediately before the caret, or null
 * when the caret sits on a boundary (or the editor returns nothing).
 *
 * Reads at most [WordLookback] characters: enough for any Vietnamese syllable, and
 * small enough to stay cheap on the one keystroke that asks for it.
 */
internal fun InputConnection.wordBeforeCursor(): String? =
    getTextBeforeCursor(WordLookback, 0)?.wordAtEnd()

internal fun CharSequence.wordAtEnd(): String? {
    var start = length
    while (start > 0) {
        val codePoint = Character.codePointBefore(this, start)
        if (CompositionBoundary.isBoundary(codePoint)) break
        start -= Character.charCount(codePoint)
    }
    return if (start == length) null else subSequence(start, length).toString()
}
