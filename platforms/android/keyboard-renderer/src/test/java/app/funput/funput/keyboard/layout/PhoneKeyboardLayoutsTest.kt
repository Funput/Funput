package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class PhoneKeyboardLayoutsTest {
    @Test
    fun `phone layout is a four by four dial pad`() {
        val layout = resolve()

        assertEquals(
            listOf(
                listOf("1", "2", "3", ""),
                listOf("4", "5", "6", ""),
                listOf("7", "8", "9", "+"),
                listOf("*", "0", "#", ""),
            ),
            layout.rows.map { row -> row.keys.map { it.label } },
        )
        assertEquals(KeyRole.BACKSPACE, layout.rows[0].keys[3].role)
        assertEquals(KeyRole.ENTER, layout.rows[1].keys[3].role)
        assertEquals(KeyRole.PLACEHOLDER, layout.rows[3].keys[3].role)
        assertNotNull(layout.suggestionBar)
    }

    @Test
    fun `phone geometry includes the suggestion toolbar`() {
        val layout = resolve()
        val spec = KeyboardGeometrySpec.fromDensity(1f)
        val height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.PHONE)
        val keyboard = KeyboardGeometry.resolve(layout, 360f, height, spec)

        assertNotNull(keyboard.suggestionBar)
        assertEquals(height - spec.verticalPadding, keyboard.rows.last().first().bounds.bottom, 0.5f)
    }

    @Test
    fun `phone mode disables composition and uses four row keypad height`() {
        assertFalse(KeyboardEditorMode.PHONE.supportsVietnameseComposition)
        assertEquals(
            KeyboardDimensions.heightForRowCount(rowCount = 4, hasSuggestionBar = true),
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.PHONE),
        )
    }

    private fun resolve() = KeyboardLayoutResolver.resolve(
        KeyboardInputMethod.VNI,
        KeyboardLayoutMode.LETTERS,
        KeyboardEditorMode.PHONE,
    )
}
