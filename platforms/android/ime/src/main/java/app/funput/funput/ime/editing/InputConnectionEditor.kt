package app.funput.funput.ime.editing

import android.icu.text.BreakIterator
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/** Executes editor commands against the connection that currently owns focus. */
internal class InputConnectionEditor {
    fun execute(
        connection: InputConnection?,
        command: ImeEditCommand,
    ): Boolean {
        if (connection == null) return false

        return when (command) {
            is ImeEditCommand.CommitText -> connection.commitText(command.text, CursorAfterText)
            ImeEditCommand.DeleteBackward -> deleteBackward(connection)
            is ImeEditCommand.DeleteSurrounding -> deleteSurrounding(connection, command.beforeLength)
            is ImeEditCommand.MoveCursor -> moveCursor(connection, command.offset)
            is ImeEditCommand.PerformEditorAction -> connection.performEditorAction(command.actionId)
        }
    }

    private fun deleteBackward(connection: InputConnection): Boolean {
        if (!connection.getSelectedText(0).isNullOrEmpty()) {
            return connection.commitText("", CursorAfterText)
        }

        val textBeforeCursor = connection
            .getTextBeforeCursor(GraphemeLookback, 0)
            ?.toString()
            .orEmpty()
        if (textBeforeCursor.isEmpty()) {
            return connection.deleteSurroundingTextInCodePoints(1, 0)
        }

        val boundary = BreakIterator.getCharacterInstance().run {
            setText(textBeforeCursor)
            preceding(textBeforeCursor.length)
        }
        if (boundary == BreakIterator.DONE) {
            return connection.deleteSurroundingTextInCodePoints(1, 0)
        }

        return connection.deleteSurroundingText(textBeforeCursor.length - boundary, 0)
    }

    private fun deleteSurrounding(connection: InputConnection, beforeLength: Int): Boolean {
        if (!connection.getSelectedText(0).isNullOrEmpty()) {
            return connection.commitText("", CursorAfterText)
        }
        return connection.deleteSurroundingText(beforeLength, 0)
    }

    private fun moveCursor(connection: InputConnection, offset: Int): Boolean {
        if (offset == 0) return false
        val extracted = connection.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null) {
            val start = extracted.startOffset + extracted.selectionStart
            val end = extracted.startOffset + extracted.text.length
            val position = (start + offset).coerceIn(extracted.startOffset, end)
            return connection.setSelection(position, position)
        }
        val before = connection.getTextBeforeCursor(Lookback, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(Lookback, 0)?.toString().orEmpty()
        val position = (before.length + offset).coerceIn(0, before.length + after.length)
        return connection.setSelection(position, position)
    }

    private companion object {
        const val CursorAfterText = 1
        const val GraphemeLookback = 128
        const val Lookback = 1024
    }
}
