package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection

internal enum class CompositionRenderMode {
    COMPOSING,
    COMMITTED,
}

/**
 * Applies the engine buffer without exposing host-specific behavior to the engine session.
 *
 * Most editors get Android composing text. Gecko editors get committed replacements because they
 * decorate every composing region themselves, even when the supplied text has no underline span.
 */
internal class CompositionDocumentEditor(
    private val composingTextFactory: (String) -> CharSequence,
) {
    private var committedSelection: Int? = null
    private var expectedCommittedSelection: Int? = null

    fun update(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        current: String,
    ): Boolean = when (mode) {
        CompositionRenderMode.COMPOSING ->
            connection.setComposingText(composingTextFactory(current), CursorAfterText)
        CompositionRenderMode.COMMITTED -> replaceBeforeCursor(connection, previous, current)
    }

    fun finish(connection: InputConnection?, mode: CompositionRenderMode, active: Boolean) {
        if (active && mode == CompositionRenderMode.COMPOSING) connection?.finishComposingText()
    }

    fun commitBoundary(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        replacement: String?,
        boundary: String,
    ): Boolean = when (mode) {
        CompositionRenderMode.COMPOSING -> {
            if (replacement != null) connection.commitText(replacement, CursorAfterText)
            else {
                connection.finishComposingText()
                connection.commitText(boundary, CursorAfterText)
            }
        }
        CompositionRenderMode.COMMITTED -> {
            if (replacement != null) replaceBeforeCursor(connection, previous, replacement)
            else {
                expectCommittedSelection(previousLength = 0, currentLength = boundary.length)
                connection.commitText(boundary, CursorAfterText)
            }
        }
    }

    fun acceptSuggestion(
        connection: InputConnection,
        mode: CompositionRenderMode,
        prefix: String,
        candidate: String,
    ): Boolean = when (mode) {
        CompositionRenderMode.COMPOSING ->
            connection.commitText("$candidate ", CursorAfterText)
        CompositionRenderMode.COMMITTED ->
            replaceBeforeCursor(connection, prefix, "$candidate ")
    }

    fun reopenWord(
        connection: InputConnection,
        mode: CompositionRenderMode,
        word: String,
    ): Boolean = when (mode) {
        CompositionRenderMode.COMPOSING ->
            connection.deleteSurroundingText(word.length, 0) &&
                connection.setComposingText(composingTextFactory(word), CursorAfterText)
        CompositionRenderMode.COMMITTED -> true.also {
            committedSelection = null
            expectedCommittedSelection = null
        }
    }

    fun ownsSelection(
        connection: InputConnection?,
        mode: CompositionRenderMode,
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        composingEnd: Int,
    ): Boolean = when (mode) {
        CompositionRenderMode.COMPOSING ->
            selectionStart == composingEnd && selectionEnd == composingEnd
        CompositionRenderMode.COMMITTED -> ownsCommittedSelection(
            connection,
            text,
            selectionStart,
            selectionEnd,
        )
    }

    private fun replaceBeforeCursor(
        connection: InputConnection,
        previous: String,
        replacement: String,
    ): Boolean {
        expectCommittedSelection(previous.length, replacement.length)
        connection.beginBatchEdit()
        return try {
            val deleted = previous.isEmpty() || connection.deleteSurroundingText(previous.length, 0)
            deleted && (replacement.isEmpty() || connection.commitText(replacement, CursorAfterText))
        } finally {
            connection.endBatchEdit()
        }
    }

    private fun ownsCommittedSelection(
        connection: InputConnection?,
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): Boolean {
        if (selectionStart != selectionEnd) return false
        val beforeCursor = connection?.getTextBeforeCursor(text.length, 0)?.toString()
        val editorConfirmsText = beforeCursor?.endsWith(text) == true
        val editorWithholdsText = beforeCursor.isNullOrEmpty() && selectionEnd >= text.length
        val positionMatches = expectedCommittedSelection?.let { it == selectionEnd } ?: true
        val owned = editorConfirmsText || (editorWithholdsText && positionMatches)
        if (owned) {
            committedSelection = selectionEnd
            expectedCommittedSelection = null
        }
        return owned
    }

    private fun expectCommittedSelection(previousLength: Int, currentLength: Int) {
        val base = expectedCommittedSelection ?: committedSelection
        expectedCommittedSelection = base?.minus(previousLength)?.plus(currentLength)
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
