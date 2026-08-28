package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartGestureAccessibilityTest {
    @Test
    fun `space and backspace expose TalkBack actions when enabled`() {
        val snapshot = snapshot(enabled = true)

        assertEquals(
            listOf("Xóa cả từ"),
            snapshot.nodes.first { it.keyId == "backspace" }.customActions.map { it.label },
        )
        assertEquals(
            listOf("Con trỏ sang trái", "Con trỏ sang phải", "Con trỏ lên", "Con trỏ xuống"),
            snapshot.nodes.first { it.keyId == "space" }.customActions.map { it.label },
        )
    }

    @Test
    fun `actions vanish when smart gestures are off`() {
        val snapshot = snapshot(enabled = false)

        assertTrue(snapshot.nodes.first { it.keyId == "backspace" }.customActions.isEmpty())
        assertTrue(snapshot.nodes.first { it.keyId == "space" }.customActions.isEmpty())
        assertTrue(snapshot.nodes.none { it.customActions.isNotEmpty() })
    }

    @Test
    fun `action ids map to key actions`() {
        assertEquals(
            KeyAction.DeleteWord,
            SmartGestureAccessibility.keyAction(SmartGestureAccessibility.DeleteWord),
        )
        assertEquals(
            KeyAction.MoveCursor(-1),
            SmartGestureAccessibility.keyAction(SmartGestureAccessibility.CursorLeft),
        )
        assertEquals(
            KeyAction.MoveCursor(1),
            SmartGestureAccessibility.keyAction(SmartGestureAccessibility.CursorRight),
        )
        assertEquals(
            KeyAction.MoveCursor(columns = 0, lines = -1),
            SmartGestureAccessibility.keyAction(SmartGestureAccessibility.CursorUp),
        )
        assertEquals(
            KeyAction.MoveCursor(columns = 0, lines = 1),
            SmartGestureAccessibility.keyAction(SmartGestureAccessibility.CursorDown),
        )
    }

    private fun snapshot(enabled: Boolean): KeyboardAccessibilitySnapshot {
        val profile = KeyboardSizingProfile.Normal
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.VNI,
            mode = KeyboardLayoutMode.LETTERS,
            editorMode = KeyboardEditorMode.TEXT,
        )
        val keyboard = requireNotNull(layout.resolveGeometry(
            width = KeyboardDimensions.DefaultWidthDp.toInt(),
            height = KeyboardDimensions.recommendedHeightDp(
                KeyboardInputMethod.VNI, KeyboardEditorMode.TEXT, profile,
            ).toInt(),
            density = 1f,
            profile = profile,
        ))
        return KeyboardAccessibilitySnapshot(keyboard, ShiftState.OFF, smartGesturesEnabled = enabled)
    }
}
