package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemInputMethodSwitcherTest {
    @Test
    fun `hidden switcher leaves the layout unchanged`() {
        val layout = resolve(visible = false)

        assertFalse(layout.id.endsWith("-system-switcher"))
        assertFalse(layout.keys().any { it.role == KeyRole.SYSTEM_INPUT_METHOD })
    }

    @Test
    fun `visible switcher joins the action row without changing its weight`() {
        val original = resolve(visible = false)
        val switched = resolve(visible = true)
        val originalWeight = original.rows.last().keys.sumOf { it.widthWeight.toDouble() }
        val switchedWeight = switched.rows.last().keys.sumOf { it.widthWeight.toDouble() }

        assertEquals(originalWeight, switchedWeight, 0.001)
        assertEquals(1, switched.keys().count { it.role == KeyRole.SYSTEM_INPUT_METHOD })
        assertTrue(switched.id.endsWith("-system-switcher"))
    }

    @Test
    fun `keypad switcher replaces an empty position`() {
        val original = resolve(visible = false, editorMode = KeyboardEditorMode.PHONE)
        val switched = resolve(visible = true, editorMode = KeyboardEditorMode.PHONE)

        assertEquals(original.keys().size, switched.keys().size)
        assertEquals(1, switched.keys().count { it.role == KeyRole.SYSTEM_INPUT_METHOD })
        assertEquals(
            original.keys().count { it.role == KeyRole.PLACEHOLDER } - 1,
            switched.keys().count { it.role == KeyRole.PLACEHOLDER },
        )
    }

    @Test
    fun `switcher remains available on symbol pages`() {
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.VNI,
            mode = KeyboardLayoutMode.SYMBOLS_PRIMARY,
            systemInputMethodSwitcherVisible = true,
        )

        assertTrue(layout.keys().any { it.role == KeyRole.SYSTEM_INPUT_METHOD })
    }

    private fun resolve(
        visible: Boolean,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
    ) = KeyboardLayoutResolver.resolve(
        inputMethod = KeyboardInputMethod.VNI,
        mode = KeyboardLayoutMode.LETTERS,
        editorMode = editorMode,
        systemInputMethodSwitcherVisible = visible,
    )

    private fun app.funput.funput.keyboard.model.KeyboardLayout.keys() = rows.flatMap { it.keys }
}
