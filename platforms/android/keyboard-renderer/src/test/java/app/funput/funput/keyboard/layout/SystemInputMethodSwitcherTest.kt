package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
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
        assertEquals(null, layout.suggestionBar?.systemInputMethodKey)
    }

    @Test
    fun `visible switcher joins toolbar without changing the action row`() {
        val original = resolve(visible = false)
        val switched = resolve(visible = true)
        val bar = requireNotNull(switched.suggestionBar)

        assertEquals(original.rows.last(), switched.rows.last())
        assertEquals(KeyRole.SYSTEM_INPUT_METHOD, bar.systemInputMethodKey?.role)
        assertEquals(KeyRole.SETTINGS, bar.settingsKey.role)
        assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
        assertTrue(switched.id.endsWith("-system-switcher"))
    }

    @Test
    fun `keypad without toolbar does not add low priority switcher`() {
        val switched = resolve(visible = true, editorMode = KeyboardEditorMode.PHONE)

        assertEquals(null, switched.suggestionBar)
        assertFalse(switched.rows.flatMap { it.keys }.any { it.role == KeyRole.SYSTEM_INPUT_METHOD })
    }

    @Test
    fun `switcher remains available on symbol pages`() {
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.VNI,
            mode = KeyboardLayoutMode.SYMBOLS_PRIMARY,
            systemInputMethodSwitcherVisible = true,
        )

        assertEquals(KeyRole.SYSTEM_INPUT_METHOD, layout.suggestionBar?.systemInputMethodKey?.role)
    }

    @Test
    fun `toolbar keeps settings and emoji ahead of switcher`() {
        val layout = resolve(visible = true)
        val height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI)
        val keyboard = KeyboardGeometry.resolve(
            layout,
            KeyboardDimensions.DefaultWidthDp,
            height,
            KeyboardGeometrySpec.fromDensity(1f),
        )
        val bar = requireNotNull(keyboard.suggestionBar)
        val system = requireNotNull(bar.systemInputMethodKey)

        assertTrue(system.bounds.right < requireNotNull(bar.settingsKey).bounds.left)
        assertTrue(requireNotNull(bar.settingsKey).bounds.right < bar.emojiKey.bounds.left)
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
}
