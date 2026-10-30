package app.funput.funput.ime.editing.backspace

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.ImeKeyActionHandler
import java.lang.reflect.Proxy

/**
 * Editor whose caret can sit anywhere, unlike the append-only fakes: the user can tap inside the
 * text or select a word, and every edit lands at the caret the way Android's `EditText` applies it.
 *
 * [exposesText] false mimics hosts such as Reddit that withhold `getTextBeforeCursor`.
 */
internal class CaretDocument(private val exposesText: Boolean = true) {
    var text = ""
        private set
    private var selection = Span(0, 0)
    private var composing: Span? = null

    /** Sends the `onUpdateSelection` a host would post for the current state. */
    fun echoTo(handler: ImeKeyActionHandler) =
        handler.onSelectionChanged(selection.start, selection.end, composing?.end ?: -1)

    fun tap(position: Int) = select(position, position)

    fun select(start: Int, end: Int) {
        selection = Span(start, end)
        composing = null
    }

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        val first = arguments?.firstOrNull()
        when (method.name) {
            "getTextBeforeCursor" -> textBefore(first as Int)
            "getSelectedText" -> text.substring(selection.start, selection.end).ifEmpty { null }
            "commitText" -> true.also { write(first.toString(), keepComposing = false) }
            "setComposingText" -> true.also { write(first.toString(), keepComposing = true) }
            "finishComposingText" -> true.also { composing = null }
            "deleteSurroundingText", "deleteSurroundingTextInCodePoints" ->
                true.also { deleteBefore(first as Int) }
            "beginBatchEdit", "endBatchEdit" -> true
            "toString" -> "CaretDocument"
            else -> false
        }
    } as InputConnection

    /** The grapheme lookback answers empty: `BreakIterator` is not usable on the JVM. */
    private fun textBefore(count: Int): String? = when {
        !exposesText -> null
        count >= GraphemeLookback -> ""
        else -> text.substring(0, selection.start).takeLast(count)
    }

    /** Replaces the composing region, or else the selection, and leaves the caret after [value]. */
    private fun write(value: String, keepComposing: Boolean) {
        val target = composing ?: selection
        text = text.replaceRange(target.start, target.end, value)
        val end = target.start + value.length
        selection = Span(end, end)
        composing = if (keepComposing) Span(target.start, end) else null
    }

    /** Deletes before the selection, never inside it, as `deleteSurroundingText` specifies. */
    private fun deleteBefore(count: Int) {
        val start = (selection.start - count).coerceAtLeast(0)
        val removed = selection.start - start
        text = text.removeRange(start, selection.start)
        selection = Span(start, selection.end - removed)
        composing = composing?.takeIf { it.end <= start }
    }

    private data class Span(val start: Int, val end: Int)

    private companion object {
        const val GraphemeLookback = 128
    }
}
