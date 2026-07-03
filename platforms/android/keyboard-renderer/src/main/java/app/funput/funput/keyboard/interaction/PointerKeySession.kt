package app.funput.funput.keyboard.interaction

/**
 * Owns the lifecycle of every active pointer independently from Android's event representation.
 *
 * Pointer IDs, rather than pointer indexes, are the stable identity. Indexes may be reordered by
 * Android whenever another finger enters or leaves the screen.
 */
internal class PointerKeySession(
    private val keyAt: (x: Float, y: Float) -> String?,
    private val onPressedStateChanged: () -> Unit,
) : PressedKeyState {
    private val pressedKeys = PressedKeyTracker()

    fun update(pointerId: Int, x: Float, y: Float): Boolean {
        val keyId = keyAt(x, y)
        if (pressedKeys.update(pointerId, keyId)) onPressedStateChanged()
        return keyId != null
    }

    /** Releases only [pointerId] and returns the key under its final coordinates. */
    fun release(pointerId: Int, x: Float, y: Float): String? {
        val wasPressed = pressedKeys.keyForPointer(pointerId) != null
        val releasedKeyId = if (wasPressed) keyAt(x, y) else null
        if (pressedKeys.release(pointerId)) onPressedStateChanged()
        return releasedKeyId
    }

    fun clear() {
        if (pressedKeys.clear()) onPressedStateChanged()
    }

    override fun isPressed(keyId: String): Boolean = pressedKeys.isPressed(keyId)
}
