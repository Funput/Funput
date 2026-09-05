package app.funput.funput.ime.editing.keyevent

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * Performs a [KeyEventTextPlan] against hosts that ignore composing and commitText.
 *
 * [send] is injectable so JVM tests never construct KeyEvent (android.jar getters throw).
 */
internal class KeyEventBufferWriter(
    private val send: (InputConnection, KeyEventStroke) -> Boolean = ::sendAndroidKeyEventStroke,
) {
    fun replace(
        connection: InputConnection,
        previous: String,
        replacement: String,
    ): Boolean {
        connection.beginBatchEdit()
        return try {
            KeyEventTextPlan.replace(previous, replacement).strokes()
                .all { send(connection, it) }
        } finally {
            connection.endBatchEdit()
        }
    }
}

internal fun sendAndroidKeyEventStroke(
    connection: InputConnection,
    stroke: KeyEventStroke,
): Boolean = when (stroke) {
    KeyEventStroke.Delete -> {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) &&
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }
    is KeyEventStroke.Text -> connection.sendKeyEvent(
        KeyEvent(
            SystemClock.uptimeMillis(),
            stroke.characters,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
        ),
    )
}
