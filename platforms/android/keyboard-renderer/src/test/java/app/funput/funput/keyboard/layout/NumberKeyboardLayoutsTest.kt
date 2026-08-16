package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
                listOf("7", "8", "9", ""),
                listOf("", "0", "", ""),
            ),
            layout.rows.map { row -> row.keys.map { it.label } },
        )
        assertEquals(KeyRole.BACKSPACE, layout.rows[0].keys[3].role)
        assertEquals(KeyRole.ENTER, layout.rows[1].keys[3].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[2].keys[3].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[3].keys[0].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[3].keys[2].role)
        assertNotNull(layout.suggestionBar)
        assertEquals(4, layout.rows.size)
    }

    @Test
    fun `numeric flags control sign and decimal keys`() {
        val expectedOptionalKeys = mapOf(
            KeyboardEditorMode.NUMBER to emptyList(),
            KeyboardEditorMode.NUMBER_DECIMAL to listOf(".", ","),
            KeyboardEditorMode.NUMBER_SIGNED to listOf("-"),
            KeyboardEditorMode.NUMBER_SIGNED_DECIMAL to listOf(".", "-", ","),
        )

        expectedOptionalKeys.forEach { (mode, expected) ->
            val optional = resolve(mode).rows
                .flatMap { it.keys }
                .filter { it.role == KeyRole.PUNCTUATION }
                .map { it.label }
            assertEquals(expected, optional)
        }
    }

    @Test
    fun `numeric geometry includes the suggestion toolbar`() {
        val mode = KeyboardEditorMode.NUMBER_SIGNED_DECIMAL
        val layout = resolve(mode)
        val spec = KeyboardGeometrySpec.fromDensity(1f)
        val height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, mode)
        val keyboard = KeyboardGeometry.resolve(layout, 360f, height, spec)

        assertNotNull(keyboard.suggestionBar)
        assertEquals(height - spec.verticalPadding, keyboard.rows.last().first().bounds.bottom, 0.5f)
        assertTrue(keyboard.keys.any { it.spec.id == "emoji" })
    }

    @Test
    fun `all numeric modes disable composition and use four row keypad height`() {
        val expectedHeight = KeyboardDimensions.heightForRowCount(rowCount = 4, hasSuggestionBar = true)
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
