package app.funput.funput.ime.shortcuts

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.ime.editing.composition.singleCodePointOrNull
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.shortcuts.model.ShortcutLibrary

/** Owns the shortcut snapshot installed in one active input session. */
internal class ImeShortcutSession(private val engine: VietnameseEngine) {
    private var library = DisabledLibrary
    private var pending: ShortcutLibrary? = null
    private var directWord = ""

    val runsInEnglish: Boolean
        get() = library.isEnabled && library.inEnglish && library.entries.isNotEmpty()

    fun beginActivation() {
        pending = null
        library = DisabledLibrary
        directWord = ""
        engine.clear()
        engine.clearShortcuts()
    }

    fun receive(value: ShortcutLibrary, wordIsActive: Boolean) {
        pending = value
        if (!wordIsActive) installPending()
    }

    /** Tracks English directly in the document; only a confirmed expansion rewrites it. */
    fun inputEnglish(connection: InputConnection, text: String): Boolean {
        val codePoint = text.singleCodePointOrNull()
            ?: return connection.commitText(text, CursorAfterText).also { finishWord() }
        if (!CompositionBoundary.isBoundary(codePoint)) {
            directWord = engine.process(codePoint)
            return connection.commitText(text, CursorAfterText).let { false }
        }
        val previous = directWord
        val replacement = engine.processBoundary(codePoint)
        directWord = ""
        val expanded = replacement != null && replaceSafely(connection, previous, replacement)
        if (!expanded) connection.commitText(text, CursorAfterText)
        installPending()
        return expanded
    }

    fun backspaceEnglish() {
        if (directWord.isNotEmpty()) directWord = engine.backspace()
        if (directWord.isEmpty()) installPending()
    }

    fun finishWord() {
        directWord = ""
        engine.clear()
        installPending()
    }

    private fun installPending() {
        val value = pending ?: return
        pending = null
        library = value
        engine.installShortcuts(value)
    }

    private fun replaceSafely(
        connection: InputConnection,
        previous: String,
        replacement: String,
    ): Boolean {
        if (previous.isEmpty() || !connection.getSelectedText(0).isNullOrEmpty()) return false
        val context = connection.getTextBeforeCursor(previous.length, 0)?.toString() ?: return false
        if (!context.endsWith(previous)) return false
        connection.beginBatchEdit()
        return try {
            if (!connection.deleteSurroundingText(previous.length, 0)) return false
            if (connection.commitText(replacement, CursorAfterText)) return true
            connection.commitText(previous, CursorAfterText)
            false
        } finally {
            connection.endBatchEdit()
        }
    }

    private companion object {
        const val CursorAfterText = 1
        val DisabledLibrary = ShortcutLibrary(isEnabled = false)
    }
}
