package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordKeyboardLayoutsTest {
    @Test
    fun `text password has ASCII qwerty plus a dedicated digit row`() {
        KeyboardInputMethod.entries.forEach { method ->
            val layout = resolve(method, KeyboardEditorMode.PASSWORD)

            assertEquals("1234567890", layout.rows.first().keys.joinToString("") { it.label })
            assertEquals(5, layout.rows.size)
            assertNull(layout.suggestionBar)
            assertFalse(layout.rows.flatMap { it.keys }.any { it.role == KeyRole.VNI_MODIFIER })
            assertNull(layout.rows.last().keys.first { it.role == KeyRole.SPACE }.horizontalSwipeAction)
        }
    }

    @Test
    fun `PIN layout contains only digits and editor commands`() {
        val layout = resolve(KeyboardInputMethod.VNI, KeyboardEditorMode.PIN)
        val textKeys = layout.rows.flatMap { it.keys }
            .filter { it.role == KeyRole.CHARACTER || it.role == KeyRole.PUNCTUATION }

        assertEquals("1234567890", textKeys.joinToString("") { it.label })
        assertEquals(KeyRole.BACKSPACE, layout.rows[0].keys[3].role)
        assertEquals(KeyRole.ENTER, layout.rows[1].keys[3].role)
        assertNull(layout.suggestionBar)
    }

    @Test
    fun `password symbol pages remain secure`() {
        listOf(KeyboardLayoutMode.SYMBOLS_PRIMARY, KeyboardLayoutMode.SYMBOLS_SECONDARY).forEach { mode ->
            val layout = KeyboardLayoutResolver.resolve(
                KeyboardInputMethod.VNI,
                mode,
                KeyboardEditorMode.PASSWORD,
            )

            assertNull(layout.suggestionBar)
            assertFalse(layout.rows.flatMap { it.keys }.any { it.role == KeyRole.EMOJI })
            assertNull(layout.rows.last().keys.first { it.role == KeyRole.SPACE }.horizontalSwipeAction)
        }
    }

    @Test
    fun `password modes disable composition and use purpose built heights`() {
        assertFalse(KeyboardEditorMode.PASSWORD.supportsVietnameseComposition)
        assertFalse(KeyboardEditorMode.PIN.supportsVietnameseComposition)
        assertTrue(KeyboardEditorMode.PASSWORD.isPassword)
        assertTrue(KeyboardEditorMode.PIN.isPassword)
        assertEquals(
            KeyboardDimensions.heightForRowCount(rowCount = 5, hasSuggestionBar = false),
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX, KeyboardEditorMode.PASSWORD),
        )
        assertEquals(
            KeyboardDimensions.heightForRowCount(rowCount = 4, hasSuggestionBar = false),
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.PIN),
        )
    }

    private fun resolve(method: KeyboardInputMethod, editorMode: KeyboardEditorMode) =
        KeyboardLayoutResolver.resolve(method, KeyboardLayoutMode.LETTERS, editorMode)
}
