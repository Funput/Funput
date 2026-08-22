package app.funput.funput.ime.hardware

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareInputCasingTest {
    @Test
    fun `active shift title-cases a lowercase letter`() {
        val action = KeyAction.Input("hardware-29", "a").applyHardwareCasing(ShiftState.ON)
        assertEquals(KeyAction.Input("hardware-29", "A"), action)
    }

    @Test
    fun `caps lock title-cases like one-shot shift`() {
        val action = KeyAction.Input("hardware-29", "a").applyHardwareCasing(ShiftState.CAPS_LOCK)
        assertEquals(KeyAction.Input("hardware-29", "A"), action)
    }

    @Test
    fun `already uppercase input is left unchanged`() {
        val action = KeyAction.Input("hardware-29", "A").applyHardwareCasing(ShiftState.ON)
        assertEquals(KeyAction.Input("hardware-29", "A"), action)
    }

    @Test
    fun `inactive shift leaves lowercase input unchanged`() {
        val action = KeyAction.Input("hardware-29", "a").applyHardwareCasing(ShiftState.OFF)
        assertEquals(KeyAction.Input("hardware-29", "a"), action)
    }

    @Test
    fun `non-input actions ignore shift`() {
        assertEquals(KeyAction.Space, KeyAction.Space.applyHardwareCasing(ShiftState.ON))
        assertEquals(KeyAction.Enter, KeyAction.Enter.applyHardwareCasing(ShiftState.ON))
        assertEquals(KeyAction.Backspace, KeyAction.Backspace.applyHardwareCasing(ShiftState.ON))
    }
}
