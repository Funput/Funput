package app.funput.funput.ime.editing.backspace

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.testSession
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy

/** Types into [document] through a handler wired the way the input method service wires it. */
internal class CaretTypist(
    val document: CaretDocument,
    adoptable: Set<String>,
    mode: CompositionRenderMode,
    allowComposition: Boolean = true,
) {
    val session = testSession(RetoneAdoptingEngine(adoptable))
    val handler = ImeKeyActionHandler(
        composition = session,
        editor = InputConnectionEditor(),
        connection = { document.proxy },
        enterCommand = { ImeEditCommand.CommitText("\n") },
    ).apply { start(allowComposition = allowComposition, renderMode = mode) }

    /** Taps each key of [text]; with [echo], the host reports the caret after every one. */
    fun type(text: String, echo: Boolean = true) = text.forEach { char ->
        press(if (char == ' ') KeyAction.Space else KeyAction.Input(keyId = "character-$char", text = "$char"))
        if (echo) document.echoTo(handler)
    }

    fun press(action: KeyAction) = handler.onKeyAction(action)
}

/** One `onUpdateSelection` as the host posts it; delivering it later models a late echo. */
internal data class SelectionReport(val start: Int, val end: Int, val composingStart: Int, val composingEnd: Int) {
    fun deliverTo(handler: ImeKeyActionHandler) =
        handler.onSelectionChanged(start, end, composingStart, composingEnd)
}

/**
 * Editor whose caret can sit anywhere, unlike the append-only fakes: the user can tap inside the
 * text or select a word, and every edit lands at the caret the way Android's `EditText` applies it.
 *
 * [exposesText] false mimics hosts such as Reddit that withhold `getTextBeforeCursor`.
 * [staleInsideBatch] mimics hosts such as Chrome that keep answering with the text as it was
 * when the outer batch edit began.
 */
internal class CaretDocument(
    private val exposesText: Boolean = true,
    private val staleInsideBatch: Boolean = false,
) {
    var text = ""
        private set
    private var selection = Span(0, 0)
    private var composing: Span? = null
    private var batchDepth = 0
    private var batchStart: Pair<String, Span>? = null

    fun report() = SelectionReport(selection.start, selection.end, composing?.start ?: -1, composing?.end ?: -1)

    /** Sends the `onUpdateSelection` a host would post for the current state. */
    fun echoTo(handler: ImeKeyActionHandler) = report().deliverTo(handler)

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
            "beginBatchEdit" -> true.also { if (batchDepth++ == 0) batchStart = text to selection }
            "endBatchEdit" -> true.also { if (--batchDepth == 0) batchStart = null }
            "toString" -> "CaretDocument"
            else -> false
        }
    } as InputConnection

    /** The grapheme lookback answers empty: `BreakIterator` is not usable on the JVM. */
    private fun textBefore(count: Int): String? {
        if (!exposesText) return null
        if (count >= GraphemeLookback) return ""
        val (shown, caret) = batchStart?.takeIf { staleInsideBatch } ?: (text to selection)
        return shown.substring(0, caret.start).takeLast(count)
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
