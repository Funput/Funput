package app.funput.funput.ime.editing

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * True when replacing the committed buffer has to delete before it commits, so the
 * document passes through a state that holds neither buffer.
 *
 * Mirrors the branch both writers take — [CommittedBufferWriter.replace] and the
 * KeyEvent plan behind it — so a new branch in either has to be reflected here.
 */
internal fun deletesBeforeCommit(previous: String, replacement: String): Boolean =
    previous.isNotEmpty() && replacement.isNotEmpty() &&
        !replacement.startsWith(previous) && !previous.startsWith(replacement)

/**
 * Writes the engine buffer into editors that cannot host composing spans.
 *
 * Prefer appending/shrinking the committed prefix so broken hosts that ignore
 * [InputConnection.deleteSurroundingText] do not duplicate characters. Full
 * replacements (tone transforms) still delete then commit.
 */
internal class CommittedBufferWriter {
    fun replace(
        connection: InputConnection,
        previous: String,
        replacement: String,
        deleteWithKeyEvents: Boolean,
    ): Boolean {
        connection.beginBatchEdit()
        return try {
            when {
                previous == replacement -> true
                previous.isEmpty() -> commit(connection, replacement)
                replacement.startsWith(previous) ->
                    commit(connection, replacement.substring(previous.length))
                previous.startsWith(replacement) ->
                    deleteBefore(connection, previous.length - replacement.length, deleteWithKeyEvents)
                else -> {
                    deleteBefore(connection, previous.length, deleteWithKeyEvents) &&
                        commit(connection, replacement)
                }
            }
        } finally {
            connection.endBatchEdit()
        }
    }

    private fun commit(connection: InputConnection, text: String): Boolean =
        text.isEmpty() || connection.commitText(text, CursorAfterText)

    private fun deleteBefore(
        connection: InputConnection,
        count: Int,
        deleteWithKeyEvents: Boolean,
    ): Boolean {
        if (count <= 0) return true
        if (deleteWithKeyEvents) {
            repeat(count) {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
            return true
        }
        return connection.deleteSurroundingText(count, 0)
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
