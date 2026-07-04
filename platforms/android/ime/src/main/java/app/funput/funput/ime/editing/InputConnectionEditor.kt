package app.funput.funput.ime.editing

import android.icu.text.BreakIterator
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

    private companion object {
        const val CursorAfterText = 1
        const val GraphemeLookback = 128
    }
}
