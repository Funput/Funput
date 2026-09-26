package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.popover.model.KeyAlternate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommaKeyTest {
    @Test
    fun `action-row comma offers placement then settings across layouts`() {
        val expected = listOf(KeyAlternate.Action.PLACEMENT, KeyAlternate.Action.SETTINGS)
        KeyboardInputMethod.entries.forEach { method ->
            listOf(true, false).forEach { numberRow ->
                listOf(KeyboardEditorMode.TEXT, KeyboardEditorMode.PASSWORD).forEach { editor ->
                    KeyboardLayoutMode.entries.forEach { mode ->
                        val layout = KeyboardLayoutResolver.resolve(method, mode, editor, showsNumberRow = numberRow)
                        val comma = layout.rows.last().keys.single { it.label == "," }
                        assertEquals(expected, comma.alternates)
                        assertEquals(2, comma.alternatePaletteColumns)
                    }
                }
            }
        }
    }

    @Test
    fun `decimal commas keep their numeric input policy`() {
        listOf(KeyboardEditorMode.NUMBER_DECIMAL, KeyboardEditorMode.NUMBER_SIGNED_DECIMAL).forEach { editor ->
            val layout = KeyboardLayoutResolver.resolve(KeyboardInputMethod.TELEX, KeyboardLayoutMode.LETTERS, editor)
            val comma = layout.rows.flatMap { it.keys }.single { it.id == "comma" }
            assertEquals(",", comma.label)
            assertTrue(comma.alternates.isEmpty())
        }
    }
}
