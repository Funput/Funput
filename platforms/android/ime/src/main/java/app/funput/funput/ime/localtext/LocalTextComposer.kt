package app.funput.funput.ime.localtext

import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.ime.editing.wordAtEnd
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.localtext.LocalTextComposing
import app.funput.funput.keyboard.ui.localtext.LocalTextEdit
import app.funput.funput.keyboard.ui.localtext.LocalTextSpacing
import app.funput.funput.keyboard.ui.localtext.dropLastCodePoint

/** What a panel field copies from the document each time it starts fresh. */
internal data class LocalTextSettings(
    val configuration: EngineConfiguration?,
    val composesVietnamese: Boolean,
)

/**
 * A [LocalTextComposing] field that composes Vietnamese with an engine of its own.
 *
 * It follows `AndroidCompositionSession` onto a string instead of an InputConnection:
 * [committed] is text the engine has let go of, [composing] its live buffer. A separate
 * engine means picking a search result — which finishes the document's composition —
 * never disturbs the query, and the query never disturbs the document.
 */
internal class LocalTextComposer(
    private val engine: VietnameseEngine,
    private val settings: () -> LocalTextSettings,
) : LocalTextComposing, AutoCloseable {
    private var committed = ""
    private var composing = ""
    private var composesVietnamese = false
    private var closed = false

    override val text: String get() = committed + composing
    override val inputMethod: KeyboardInputMethod?
        get() = engine.inputMethod.takeIf { composesVietnamese }

    override fun apply(edit: LocalTextEdit) {
        if (closed) return
        when (edit) {
            is LocalTextEdit.Text -> edit.value.forEachCodePoint(::input)
            // A refused space still ends the word, so the next letter starts a new one.
            LocalTextEdit.Space -> if (LocalTextSpacing.accepts(text)) input(' '.code) else endWord()
            LocalTextEdit.DeleteBackward -> deleteBackward()
        }
    }

    /** Copies the document's options and language switch; shortcuts are never installed. */
    override fun reset() {
        if (closed) return
        val current = settings()
        current.configuration?.let(engine::configure)
        composesVietnamese = current.composesVietnamese
        engine.setEnabled(composesVietnamese)
        committed = ""
        composing = ""
        engine.clear()
    }

    /** Idempotent, and leaves the field inert: a panel may still reset it while its view goes away. */
    override fun close() {
        if (closed) return
        closed = true
        engine.close()
    }

    private fun input(codePoint: Int) {
        val raw = String(Character.toChars(codePoint))
        when {
            !composesVietnamese -> committed += raw
            CompositionBoundary.isBoundary(codePoint, engine.inputMethod) -> {
                // A replacement (a restored word) already ends with the boundary itself.
                committed += engine.processBoundary(codePoint) ?: (composing + raw)
                composing = ""
            }
            else -> {
                val buffer = engine.process(codePoint)
                if (buffer.isNotEmpty()) composing = buffer else {
                    // Nothing composes this key: keep the word and the key as they are.
                    composing += raw
                    endWord()
                }
            }
        }
    }

    private fun endWord() {
        committed += composing
        composing = ""
        engine.clear()
    }

    /** The document's Backspace: step the engine back, or reopen the word before it for a new tone. */
    private fun deleteBackward() {
        if (composing.isNotEmpty()) {
            composing = engine.backspace()
            return
        }
        committed = committed.dropLastCodePoint()
        if (!composesVietnamese) return
        val word = committed.wordAtEnd() ?: return
        if (engine.adopt(word)) {
            committed = committed.dropLast(word.length)
            composing = word
        }
    }
}

private inline fun String.forEachCodePoint(action: (Int) -> Unit) {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        action(codePoint)
        index += Character.charCount(codePoint)
    }
}
