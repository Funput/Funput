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
    fun allInputMethodsUseFiveRows() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val layout = KeyboardLayouts.forInputMethod(inputMethod)
            assertEquals(5, layout.rows.size)
        }
    }

    @Test
    fun telexTopRowUsesPlainDigits() {
        val topRow = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX).rows.first()

        assertEquals("1234567890", topRow.keys.joinToString("") { key -> key.label })
        assertTrue(topRow.keys.all { key -> key.role == KeyRole.CHARACTER })
        assertTrue(topRow.keys.all { key -> key.secondaryLabel == null })
    }

    @Test
    fun advancedTelexUsesItsOwnIdentityAndPlainDigits() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX_ADVANCED)

        assertEquals("qwerty-telex-advanced", layout.id)
        assertEquals(KeyboardInputMethod.TELEX_ADVANCED, layout.inputMethod)
        assertEquals("1234567890", layout.rows.first().keys.joinToString("") { key -> key.label })
        assertTrue(layout.rows.first().keys.all { key -> key.role == KeyRole.CHARACTER })
    }

    @Test
    fun vniTopRowUsesModifierHints() {
        val topRow = KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI).rows.first()

        assertEquals("1234567890", topRow.keys.joinToString("") { key -> key.label })
        assertTrue(topRow.keys.all { key -> key.role == KeyRole.VNI_MODIFIER })
        assertTrue(topRow.keys.all { key -> key.secondaryLabel != null })
    }

    @Test
    fun letterRowsStartBelowTopNumberRow() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX)

        assertEquals("qwertyuiop", layout.rows[1].keys.joinToString("") { key -> key.label })
    }

    @Test
    fun letterKeysDoNotShowSecondaryHints() {
        val keys = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX)
            .rows
            .flatMap { row -> row.keys }
            .filter { key -> key.role == KeyRole.CHARACTER && key.label.length == 1 && key.label[0].isLetter() }

        assertTrue(keys.none { key -> key.secondaryLabel != null })
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
    fun suggestionToolbarKeepsSettingsAndEmojiUtilities() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val bar = requireNotNull(KeyboardLayouts.forInputMethod(inputMethod).suggestionBar)
            assertEquals(KeyRole.SETTINGS, bar.settingsKey.role)
            assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
            assertTrue(bar.suggestionsEnabled)
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
