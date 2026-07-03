package app.funput.funput.keyboard.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerKeySessionTest {
    private var stateChangeCount = 0
    private val session = PointerKeySession(
        keyAt = { x, _ ->
            when {
                x < 0f -> null
                x < 100f -> "a"
                else -> "s"
            }
        },
        onPressedStateChanged = { stateChangeCount++ },
    )

    @Test
    fun nonSequentialPointerIdsPressDifferentKeys() {
        session.update(pointerId = 3, x = 50f, y = 0f)
        session.update(pointerId = 11, x = 150f, y = 0f)

        assertTrue(session.isPressed("a"))
        assertTrue(session.isPressed("s"))
    }

    @Test
    fun movingOnePointerDoesNotAffectAnother() {
        session.update(pointerId = 3, x = 50f, y = 0f)
        session.update(pointerId = 11, x = 50f, y = 0f)

        session.update(pointerId = 3, x = 150f, y = 0f)

        assertTrue(session.isPressed("a"))
        assertTrue(session.isPressed("s"))
    }

    @Test
    fun releasingOnePointerKeepsOtherPointersActive() {
        session.update(pointerId = 3, x = 50f, y = 0f)
        session.update(pointerId = 11, x = 150f, y = 0f)

        assertTrue(session.release(pointerId = 3))
        assertFalse(session.isPressed("a"))
        assertTrue(session.isPressed("s"))
    }

    @Test
    fun movingOutsideReleasesOnlyThatPointer() {
        session.update(pointerId = 3, x = 50f, y = 0f)
        session.update(pointerId = 11, x = 150f, y = 0f)

        assertFalse(session.update(pointerId = 3, x = -1f, y = 0f))
        assertFalse(session.isPressed("a"))
        assertTrue(session.isPressed("s"))
    }

    @Test
    fun cancelClearsAllPointersWithOneStateChange() {
        session.update(pointerId = 3, x = 50f, y = 0f)
        session.update(pointerId = 11, x = 150f, y = 0f)
        val changesBeforeCancel = stateChangeCount

        session.clear()

        assertFalse(session.isPressed("a"))
        assertFalse(session.isPressed("s"))
        assertEquals(changesBeforeCancel + 1, stateChangeCount)
    }
}
