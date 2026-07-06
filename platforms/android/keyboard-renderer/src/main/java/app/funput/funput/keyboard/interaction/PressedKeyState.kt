package app.funput.funput.keyboard.interaction

/** Read-only pressed state consumed by the renderer without per-frame callback allocation. */
internal fun interface PressedKeyState {
    fun isPressed(keyId: String): Boolean
}
