package app.funput.funput.keyboard.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PressedKeyTrackerTest {
    @Test
    fun updatePressesKeyImmediately() {
        val tracker = PressedKeyTracker()

        assertTrue(tracker.update(pointerId = 7, keyId = "character-a"))
        assertTrue(tracker.isPressed("character-a"))
        assertEquals("character-a", tracker.keyForPointer(7))
    }

    @Test
    fun movingPointerTransfersPressedState() {
        val tracker = PressedKeyTracker()
        tracker.update(pointerId = 1, keyId = "character-a")

        assertTrue(tracker.update(pointerId = 1, keyId = "character-s"))
        assertFalse(tracker.isPressed("character-a"))
        assertTrue(tracker.isPressed("character-s"))
    }

    @Test
    fun releasingOneOfTwoPointersKeepsKeyPressed() {
        val tracker = PressedKeyTracker()
        tracker.update(pointerId = 1, keyId = "space")
        tracker.update(pointerId = 2, keyId = "space")

        assertTrue(tracker.release(1))
        assertTrue(tracker.isPressed("space"))
        assertNull(tracker.keyForPointer(1))

        assertTrue(tracker.release(2))
        assertFalse(tracker.isPressed("space"))
    }

    @Test
    fun cancelClearsEveryPointer() {
        val tracker = PressedKeyTracker()
        tracker.update(pointerId = 1, keyId = "character-a")
        tracker.update(pointerId = 2, keyId = "character-s")

        assertTrue(tracker.clear())
        assertFalse(tracker.isPressed("character-a"))
        assertFalse(tracker.isPressed("character-s"))
        assertFalse(tracker.clear())
    }
}
