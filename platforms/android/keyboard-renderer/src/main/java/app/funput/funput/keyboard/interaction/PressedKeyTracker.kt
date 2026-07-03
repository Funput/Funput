package app.funput.funput.keyboard.interaction

/**
 * Tracks visual key presses independently from Android touch events and rendering.
 *
 * Multiple pointers may hold the same key. Releasing one pointer keeps that key pressed until the
 * final pointer leaves it.
 */
internal class PressedKeyTracker {
    private val keyByPointerId = mutableMapOf<Int, String>()
    private val pointerCountByKeyId = mutableMapOf<String, Int>()

    fun update(pointerId: Int, keyId: String?): Boolean {
        val previousKeyId = keyByPointerId[pointerId]
        if (previousKeyId == keyId) return false

        if (previousKeyId != null) {
            decrement(previousKeyId)
            keyByPointerId.remove(pointerId)
        }
        if (keyId != null) {
            keyByPointerId[pointerId] = keyId
            pointerCountByKeyId[keyId] = pointerCountByKeyId.getOrDefault(keyId, 0) + 1
        }
        return true
    }

    fun release(pointerId: Int): Boolean = update(pointerId, null)

    fun clear(): Boolean {
        if (keyByPointerId.isEmpty()) return false
        keyByPointerId.clear()
        pointerCountByKeyId.clear()
        return true
    }

    fun keyForPointer(pointerId: Int): String? = keyByPointerId[pointerId]

    fun isPressed(keyId: String): Boolean = pointerCountByKeyId.containsKey(keyId)

    private fun decrement(keyId: String) {
        val remainingPointers = pointerCountByKeyId.getValue(keyId) - 1
        if (remainingPointers == 0) {
            pointerCountByKeyId.remove(keyId)
        } else {
            pointerCountByKeyId[keyId] = remainingPointers
        }
    }
}
