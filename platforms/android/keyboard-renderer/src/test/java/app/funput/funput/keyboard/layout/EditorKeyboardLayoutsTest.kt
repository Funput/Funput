package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EditorKeyboardLayoutsTest {
    @Test
    fun `email layout exposes email punctuation without VNI modifier row`() {
        KeyboardInputMethod.entries.forEach { method ->
            val layout = resolve(method, KeyboardEditorMode.EMAIL)

            assertEquals(4, layout.rows.size)
            assertEquals(listOf("@", ".", ".com"), actionLabels(layout).filter { it in EmailLabels })
            assertNull(layout.rows.last().keys.first { it.id == "space" }.horizontalSwipeAction)
        }
    }

    @Test
    fun `URL layout exposes slash and domain punctuation`() {
        val layout = resolve(KeyboardInputMethod.VNI, KeyboardEditorMode.URL)

        assertEquals(4, layout.rows.size)
        assertEquals(listOf("/", ".", ".com"), actionLabels(layout).filter { it in UrlLabels })
    }

    @Test
    fun `ASCII layouts use compact height even when VNI is selected`() {
        assertEquals(
            268f,
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.EMAIL),
        )
        assertEquals(
            318f,
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.TEXT),
        )
    }

    @Test
    fun `disabled suggestion policy keeps emoji toolbar only`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.LETTERS,
            KeyboardEditorMode.TEXT,
            suggestionsEnabled = false,
        )
        val bar = requireNotNull(layout.suggestionBar)

        assertEquals(KeyRole.SETTINGS, bar.settingsKey.role)
        assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
        assertFalse(bar.suggestionsEnabled)
    }

    private fun resolve(method: KeyboardInputMethod, editorMode: KeyboardEditorMode) =
        KeyboardLayoutResolver.resolve(method, KeyboardLayoutMode.LETTERS, editorMode)

    private fun actionLabels(layout: app.funput.funput.keyboard.model.KeyboardLayout) =
        layout.rows.last().keys.map { it.label }

    private companion object {
        val EmailLabels = setOf("@", ".", ".com")
        val UrlLabels = setOf("/", ".", ".com")
    }
}
