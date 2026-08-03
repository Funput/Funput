package app.funput.funput.keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardInputMethodTest {
    @Test
    fun `methods keep stable settings order`() {
        assertEquals(
            listOf(
                KeyboardInputMethod.TELEX,
                KeyboardInputMethod.TELEX_ADVANCED,
                KeyboardInputMethod.VNI,
            ),
            KeyboardInputMethod.entries,
        )
    }

    @Test
    fun `advanced and standard Telex share a family`() {
        assertTrue(KeyboardInputMethod.TELEX.isTelexFamily)
        assertTrue(KeyboardInputMethod.TELEX_ADVANCED.isTelexFamily)
        assertFalse(KeyboardInputMethod.VNI.isTelexFamily)
    }
}
