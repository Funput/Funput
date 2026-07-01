package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {
    @Test
    fun telexUsesFourRows() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX)

        assertEquals(4, layout.rows.size)
        assertEquals("qwertyuiop", layout.rows.first().keys.joinToString("") { key -> key.label })
    }

    @Test
    fun vniAddsDirectModifierRow() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI)
        val modifierRow = layout.rows.first()

        assertEquals(5, layout.rows.size)
        assertEquals("1234567890", modifierRow.keys.joinToString("") { key -> key.label })
        assertTrue(modifierRow.keys.all { key -> key.role == KeyRole.VNI_MODIFIER })
        assertTrue(modifierRow.keys.all { key -> key.secondaryLabel != null })
    }

    @Test
    fun everyLayoutUsesStableUniqueKeyIds() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val ids = KeyboardLayouts.forInputMethod(inputMethod).rows.flatMap { row -> row.keys.map { key -> key.id } }

            assertEquals(ids.size, ids.distinct().size)
        }
    }

    @Test
    fun actionRowProvidesEmojiAndLanguageToggleOnSpace() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val actionKeys = KeyboardLayouts.forInputMethod(inputMethod).rows.last().keys
            val emoji = actionKeys.first { key -> key.id == "emoji" }
            val space = actionKeys.first { key -> key.id == "space" }

            assertEquals(KeyRole.EMOJI, emoji.role)
            assertEquals(KeySwipeAction.TOGGLE_LANGUAGE, space.horizontalSwipeAction)
            assertEquals("VI ⇄ EN", space.label)
        }
    }
}
