package app.funput.funput.ime.hardware

import android.view.KeyEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One-shot Shift must end with the typed character, not when the editor reports the caret. */
class ImeHardwareShiftTest {
    @Test
    fun `one-shot shift is turned off right after the first typed character`() {
        val shift = ShiftHolder(ShiftState.ON)
        val handler = handler(shift)

        // No onUpdateSelection arrives between these keys, as with fast typing or `adb input text`.
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_V, 'v'.code)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_I, 'i'.code)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_E, 'e'.code)))

        assertEquals(listOf("V", "i", "e"), shift.typed())
        assertEquals(ShiftState.OFF, shift.value)
        assertEquals(listOf(ShiftState.OFF), shift.writes)
    }

    @Test
    fun `one-shot shift is turned off even when the first letter is already upper case`() {
        val shift = ShiftHolder(ShiftState.ON)
        val handler = handler(shift)

        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_V, 'V'.code, isShift = true)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_I, 'i'.code)))

        assertEquals(listOf("V", "i"), shift.typed())
        assertEquals(listOf(ShiftState.OFF), shift.writes)
    }

    @Test
    fun `caps lock survives typing`() {
        val shift = ShiftHolder(ShiftState.CAPS_LOCK)
        val handler = handler(shift)

        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_A, 'a'.code)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_B, 'b'.code)))

        assertEquals(listOf("A", "B"), shift.typed())
        assertEquals(ShiftState.CAPS_LOCK, shift.value)
        assertEquals(emptyList<ShiftState>(), shift.writes)
    }

    @Test
    fun `keys that type no character leave one-shot shift alone`() {
        val shift = ShiftHolder(ShiftState.ON)
        val handler = handler(shift)

        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_SPACE, ' '.code)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_DEL)))
        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_ENTER)))

        assertEquals(listOf(KeyAction.Space, KeyAction.Backspace, KeyAction.Enter), shift.dispatched)
        assertEquals(ShiftState.ON, shift.value)
        assertEquals(emptyList<ShiftState>(), shift.writes)
    }

    private class ShiftHolder(var value: ShiftState) {
        val dispatched = mutableListOf<KeyAction>()
        val writes = mutableListOf<ShiftState>()

        fun set(state: ShiftState) {
            writes += state
            value = state
        }

        fun typed(): List<String> = dispatched.map { (it as KeyAction.Input).text }
    }

    private fun handler(shift: ShiftHolder) = ImeHardwareKeyHandler(
        currentShift = { shift.value },
        dispatch = shift.dispatched::add,
        finish = { error("should not finish") },
        setShift = shift::set,
    )
}
