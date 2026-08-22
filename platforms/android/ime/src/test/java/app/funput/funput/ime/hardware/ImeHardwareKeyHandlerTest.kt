package app.funput.funput.ime.hardware

import android.view.KeyEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeHardwareKeyHandlerTest {
    @Test
    fun `consumed down is matched by up without a second dispatch`() {
        val dispatched = mutableListOf<KeyAction>()
        val handler = handler(dispatched)

        val down = HardwareKeyStroke(KeyEvent.KEYCODE_A, 'a'.code)
        assertTrue(handler.onKeyDown(down))
        assertEquals(listOf(KeyAction.Input("hardware-${KeyEvent.KEYCODE_A}", "a")), dispatched)

        assertTrue(handler.onKeyUp(down))
        assertEquals(1, dispatched.size)
        assertFalse(handler.onKeyUp(down))
    }

    @Test
    fun `held backspace repeats on each down`() {
        val dispatched = mutableListOf<KeyAction>()
        val handler = handler(dispatched)
        val stroke = HardwareKeyStroke(KeyEvent.KEYCODE_DEL, repeatCount = 1)

        assertTrue(handler.onKeyDown(stroke))
        assertTrue(handler.onKeyDown(stroke.copy(repeatCount = 2)))
        assertEquals(listOf(KeyAction.Backspace, KeyAction.Backspace), dispatched)
        assertTrue(handler.onKeyUp(stroke))
    }

    @Test
    fun `auto-cap shift is applied before dispatch`() {
        val dispatched = mutableListOf<KeyAction>()
        val handler = handler(dispatched, shift = ShiftState.ON)

        assertTrue(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_A, 'a'.code)))
        assertEquals(listOf(KeyAction.Input("hardware-${KeyEvent.KEYCODE_A}", "A")), dispatched)
    }

    @Test
    fun `finish-and-pass finishes composition and returns false`() {
        var finished = 0
        val dispatched = mutableListOf<KeyAction>()
        val handler = ImeHardwareKeyHandler(
            currentShift = { ShiftState.OFF },
            dispatch = dispatched::add,
            finish = { finished += 1 },
        )

        assertFalse(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_DPAD_LEFT)))
        assertEquals(1, finished)
        assertEquals(emptyList<KeyAction>(), dispatched)
        assertFalse(handler.onKeyUp(HardwareKeyStroke(KeyEvent.KEYCODE_DPAD_LEFT)))
    }

    @Test
    fun `pass-through keys neither dispatch nor finish`() {
        var finished = 0
        val handler = ImeHardwareKeyHandler(
            currentShift = { ShiftState.OFF },
            dispatch = { error("should not dispatch") },
            finish = { finished += 1 },
        )

        assertFalse(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_BACK)))
        assertFalse(handler.onKeyDown(HardwareKeyStroke(KeyEvent.KEYCODE_SHIFT_LEFT)))
        assertEquals(0, finished)
    }

    private fun handler(
        dispatched: MutableList<KeyAction>,
        shift: ShiftState = ShiftState.OFF,
    ) = ImeHardwareKeyHandler(
        currentShift = { shift },
        dispatch = dispatched::add,
        finish = { error("should not finish") },
    )
}
