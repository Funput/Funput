package app.funput.funput.ime.editing.caret

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * Applies a two-axis caret pan to the live document.
 *
 * Reads the surrounding text once per step. Unlike iOS, whose `adjustTextPosition` is relative,
 * `setSelection` needs an absolute index, so even a purely horizontal step has to know where the
 * caret currently is — there is no read-free fast path to be had here.
 */
internal class CaretPanResolver {
    /**
     * Column an earlier step in the same pan was aiming for, paired with the caret position that
     * step left behind.
     *
     * Keying on the landing position is what makes the memory self-expiring: typing, deleting or
     * the user tapping elsewhere all move the caret away from it, so the next vertical step
     * recomputes from scratch without anyone having to call a reset.
     */
    private var pan: Pan? = null

    /** Returns whether the caret moved. */
    fun apply(connection: InputConnection, columns: Int, lines: Int): Boolean {
        if (columns == 0 && lines == 0) return false
        val context = read(connection) ?: return false
        val remembered = pan?.takeIf { it.position == context.caretPosition }?.column
        val resolution = CaretLineGeometry(context)
            .resolve(columns = columns, lines = lines, desiredColumn = remembered)
        if (resolution.offset == 0) return false
        val end = context.caretPosition + context.after.length
        val start = context.caretPosition - context.before.length
        val position = (context.caretPosition + resolution.offset).coerceIn(start, end)
        pan = resolution.column?.let { Pan(position, it) }
        return connection.setSelection(position, position)
    }

    private fun read(connection: InputConnection): KeyboardCaretContext? {
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null) {
            val text = extracted.text?.toString() ?: return null
            val caret = extracted.selectionStart.coerceIn(0, text.length)
            return KeyboardCaretContext(
                before = text.substring(0, caret),
                after = text.substring(caret),
                caretPosition = extracted.startOffset + caret,
            )
        }
        // No ExtractedText: fall back to a window around the caret.
        //
        // `before.length` is the caret's absolute position only while the window reaches the start
        // of the document. Once the request comes back full there is more text behind it than was
        // asked for, and the length is the size of the request rather than the position of the
        // caret — handing that to `setSelection` throws the caret back near the top of the field.
        // Refuse the step instead: a gesture that declines to move beats one that teleports.
        //
        // A document with exactly `Lookback` characters behind the caret is refused too. Telling
        // that apart from a clipped one costs another round-trip for a case worth nothing.
        val before = connection.getTextBeforeCursor(Lookback, 0)?.toString().orEmpty()
        if (before.length >= Lookback) return null
        // The window's far edges are not the document's, which can only shorten a jump to the end
        // of a line. Only the near edge, the caret itself, has to be exact.
        val after = connection.getTextAfterCursor(Lookback, 0)?.toString().orEmpty()
        return KeyboardCaretContext(before, after, caretPosition = before.length)
    }

    private data class Pan(val position: Int, val column: Int)

    private companion object {
        const val Lookback = 1024
    }
}
