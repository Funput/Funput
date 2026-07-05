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

class NumberKeyboardLayoutsTest {
    @Test
    fun `number layout matches the requested four by four grid`() {
        val layout = resolve(KeyboardEditorMode.NUMBER)

        assertEquals(
            listOf(
                listOf("1", "2", "3", ""),
                listOf("4", "5", "6", ""),
                listOf("7", "8", "9", "."),
                listOf("", "0", "", ","),
            ),
            layout.rows.map { row -> row.keys.map { it.label } },
        )
        assertEquals(KeyRole.BACKSPACE, layout.rows[0].keys[3].role)
        assertEquals(KeyRole.ENTER, layout.rows[1].keys[3].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[3].keys[0].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[3].keys[2].role)
        assertNull(layout.suggestionBar)
        assertEquals(4, layout.rows.size)
    }

    @Test
    fun `all numeric variants share the same keypad`() {
        val expected = resolve(KeyboardEditorMode.NUMBER).rows.map { row -> row.keys.map { it.label } }
        KeyboardEditorMode.entries.filter { it.isNumber }.forEach { mode ->
            assertEquals(expected, resolve(mode).rows.map { row -> row.keys.map { it.label } })
        }
    }

    @Test
    fun `numeric geometry gives rows the full area without hidden toolbar`() {
        val mode = KeyboardEditorMode.NUMBER_SIGNED_DECIMAL
        val layout = resolve(mode)
        val spec = KeyboardGeometrySpec.fromDensity(1f)
        val height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, mode)
        val keyboard = KeyboardGeometry.resolve(layout, 360f, height, spec)

        assertNull(keyboard.suggestionBar)
        assertEquals(spec.verticalPadding, keyboard.rows.first().first().bounds.top)
        assertEquals(height - spec.verticalPadding, keyboard.rows.last().first().bounds.bottom, 0.5f)
        assertFalse(keyboard.keys.any { it.spec.id == "emoji" })
    }

    @Test
    fun `all numeric modes disable composition and use four row keypad height`() {
        val expectedHeight = KeyboardDimensions.heightForRowCount(rowCount = 4, hasSuggestionBar = false)
        KeyboardEditorMode.entries.filter { it.isNumber }.forEach { mode ->
            assertFalse(mode.supportsVietnameseComposition)
            assertEquals(expectedHeight, KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, mode))
            assertTrue(resolve(mode).id.contains(mode.name.lowercase()))
        }
    }

    private fun resolve(mode: KeyboardEditorMode) = KeyboardLayoutResolver.resolve(
        KeyboardInputMethod.VNI,
        KeyboardLayoutMode.LETTERS,
        mode,
    )
}
