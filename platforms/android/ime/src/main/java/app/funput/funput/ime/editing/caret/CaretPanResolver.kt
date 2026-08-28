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

    /**
     * Caret position as the host last reported it through `onUpdateSelection`, or [NoCaret].
     *
     * This is the only absolute position available when the editor hands back no `ExtractedText`
     * and the readable window does not reach the start of the document. It stays current across
     * the start of a pan because `finishComposingText` does not move the caret, and because the
     * hold that arms the gesture gives the report ample time to arrive.
     */
    private var reportedCaret = NoCaret

    fun onSelectionChanged(position: Int) {
        reportedCaret = position
    }

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
        reportedCaret = position
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
        // No ExtractedText: fall back to a window around the caret. Its far edges are not the
        // document's, which can only shorten a jump to the end of a line. Only the near edge, the
        // caret itself, has to be exact.
        val before = connection.getTextBeforeCursor(Lookback, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(Lookback, 0)?.toString().orEmpty()
        // A window that did not come back full reaches the start of the document, so its length is
        // the caret. A full one only proves there is at least that much text behind the caret —
        // reading its length as a position is what used to throw the caret back near the top of a
        // long field, so fall back to what the host reported instead.
        if (before.length < Lookback) {
            return KeyboardCaretContext(before, after, caretPosition = before.length)
        }
        // A report from before the text we can see is stale past rescuing; sitting still beats
        // moving to a guess.
        if (reportedCaret < before.length) return null
        return KeyboardCaretContext(before, after, caretPosition = reportedCaret)
    }

    private data class Pan(val position: Int, val column: Int)

    private companion object {
        const val Lookback = 1024
        const val NoCaret = -1
    }
}
