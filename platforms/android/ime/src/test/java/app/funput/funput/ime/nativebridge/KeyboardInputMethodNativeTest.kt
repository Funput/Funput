package app.funput.funput.ime.nativebridge

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardInputMethodNativeTest {
    @Test
    fun `input method wire values remain stable`() {
        assertEquals(0, KeyboardInputMethod.TELEX.nativeValue)
        assertEquals(1, KeyboardInputMethod.VNI.nativeValue)
        assertEquals(2, KeyboardInputMethod.TELEX_ADVANCED.nativeValue)
    }
}
