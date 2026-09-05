package app.funput.funput.ime.editing.keyevent

/**
 * One write a Linux-sandbox host can receive. Text APIs stay off this path.
 *
 * [Delete] is KEYCODE_DEL. [Text] is an ACTION_MULTIPLE unicode payload — the only KeyEvent
 * that can carry Vietnamese after Telex/VNI has already composed it.
 */
internal sealed interface KeyEventStroke {
    data object Delete : KeyEventStroke
    data class Text(val characters: String) : KeyEventStroke
}
