package app.funput.funput.ime.hardware

import android.view.KeyEvent
import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareKeyActionMapperTest {
    @Test
    fun `letters and VNI digits are consumed as input`() {
        assertInput(KeyEvent.KEYCODE_A, 'a'.code, "a")
        assertInput(KeyEvent.KEYCODE_6, '6'.code, "6")
        assertInput(KeyEvent.KEYCODE_W, 'w'.code, "w")
    }

    @Test
    fun `space enter and delete are consumed as editing actions`() {
        assertEquals(KeyAction.Space, consume(HardwareKeyStroke(KeyEvent.KEYCODE_SPACE)))
        assertEquals(KeyAction.Enter, consume(HardwareKeyStroke(KeyEvent.KEYCODE_ENTER)))
        assertEquals(KeyAction.Enter, consume(HardwareKeyStroke(KeyEvent.KEYCODE_NUMPAD_ENTER)))
        assertEquals(KeyAction.Backspace, consume(HardwareKeyStroke(KeyEvent.KEYCODE_DEL)))
    }

    @Test
    fun `arrows and ctrl shortcuts finish then pass`() {
        assertEquals(
            HardwareKeyDecision.FinishAndPass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_DPAD_LEFT)),
        )
        assertEquals(
            HardwareKeyDecision.FinishAndPass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_C, isCtrl = true)),
        )
        assertEquals(
            HardwareKeyDecision.FinishAndPass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_MOVE_HOME)),
        )
    }

    @Test
    fun `back tab and shift pass without finishing`() {
        assertEquals(HardwareKeyDecision.Pass, HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_BACK)))
        assertEquals(HardwareKeyDecision.Pass, HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_TAB)))
        assertEquals(
            HardwareKeyDecision.Pass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_SHIFT_LEFT)),
        )
    }

    @Test
    fun `canceled strokes and combining accents pass`() {
        assertEquals(
            HardwareKeyDecision.Pass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_A, isCanceled = true)),
        )
        assertEquals(
            HardwareKeyDecision.Pass,
            HardwareKeyActionMapper.map(HardwareKeyStroke(KeyEvent.KEYCODE_E, codePoint = Int.MIN_VALUE)),
        )
    }

    private fun assertInput(keyCode: Int, codePoint: Int, text: String) {
        val action = consume(HardwareKeyStroke(keyCode, codePoint))
        assertTrue(action is KeyAction.Input)
        action as KeyAction.Input
        assertEquals("hardware-$keyCode", action.keyId)
        assertEquals(text, action.text)
    }

    private fun consume(stroke: HardwareKeyStroke): KeyAction {
        val decision = HardwareKeyActionMapper.map(stroke)
        assertTrue(decision is HardwareKeyDecision.Consume)
        return (decision as HardwareKeyDecision.Consume).action
    }
}
