package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun telexShowsModifierHintsOnLetterKeys() {
        val keys = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX)
            .rows
            .flatMap { row -> row.keys }
            .filter { key -> key.role == KeyRole.CHARACTER }
            .associateBy { key -> key.label.single() }

        assertEquals("´", keys.getValue('s').secondaryLabel)
        assertEquals("`", keys.getValue('f').secondaryLabel)
        assertEquals("̉", keys.getValue('r').secondaryLabel)
        assertEquals("˜", keys.getValue('x').secondaryLabel)
        assertEquals("̣", keys.getValue('j').secondaryLabel)
        assertEquals("×", keys.getValue('z').secondaryLabel)
        assertEquals("đ", keys.getValue('d').secondaryLabel)
        assertEquals("˘+", keys.getValue('w').secondaryLabel)
        assertEquals("ˆ", keys.getValue('a').secondaryLabel)
        assertEquals("ˆ", keys.getValue('e').secondaryLabel)
        assertEquals("ˆ", keys.getValue('o').secondaryLabel)
        assertEquals(null, keys.getValue('q').secondaryLabel)
    }

    @Test
    fun vniLetterKeysDoNotShowTelexHints() {
        val keys = KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI)
            .rows
            .flatMap { row -> row.keys }
            .filter { key -> key.role == KeyRole.CHARACTER }

        assertTrue(keys.none { key -> key.secondaryLabel != null })
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
            val layout = KeyboardLayouts.forInputMethod(inputMethod)
            val ids = buildList {
                layout.suggestionBar?.settingsKey?.id?.let(::add)
                layout.suggestionBar?.emojiKey?.id?.let(::add)
                layout.rows.forEach { row -> addAll(row.keys.map { key -> key.id }) }
            }

            assertEquals(ids.size, ids.distinct().size)
        }
    }

    @Test
    fun emojiToolbarIsShownWithoutSuggestionStrip() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val bar = requireNotNull(KeyboardLayouts.forInputMethod(inputMethod).suggestionBar)
            assertEquals(KeyRole.SETTINGS, bar.settingsKey.role)
            assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
            assertFalse(bar.suggestionsEnabled)
        }
    }

    @Test
    fun actionRowGivesSpaceMoreWidth() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val layout = KeyboardLayouts.forInputMethod(inputMethod)
            val actionKeys = layout.rows.last().keys
            val space = actionKeys.first { key -> key.id == "space" }

            assertTrue(actionKeys.none { key -> key.role == KeyRole.EMOJI || key.role == KeyRole.SETTINGS })
            assertEquals(KeySwipeAction.TOGGLE_LANGUAGE, space.horizontalSwipeAction)
            assertEquals("Tiếng Việt", space.label)
            assertEquals(5.8f, space.widthWeight)
        }
    }
}
