package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolLayoutsTest {
    @Test
    fun `primary page follows Gboard order before Funput-only symbols`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.TELEX,
            KeyboardLayoutMode.SYMBOLS_PRIMARY,
        )

        assertEquals(5, layout.rows.size)
        assertEquals("1234567890", labels(layout.rows[0].keys))
        assertTrue(layout.rows[0].keys.all { key -> key.role == KeyRole.PUNCTUATION })
        assertTrue(layout.rows[0].keys.all { key -> key.secondaryLabel == null })
        assertEquals(listOf("@", "#", "₫", "_", "&", "-", "+", "(", ")", "/"), keyLabels(layout, 1))
        assertEquals(listOf("*", "\"", "'", ":", ";", "!", "?", "…", "<", ">"), keyLabels(layout, 2))
        assertEquals(listOf("=\\<", "¥", "¶", "·", "≠", "±", "≈", "≤", ""), keyLabels(layout, 3))
        assertTrue(layout.rows.flattenedKeys().any { it.label == "₫" })
        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.MORE_SYMBOLS })
        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.LETTERS })
    }

    @Test
    fun `secondary page follows Gboard order before Funput-only symbols`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.SYMBOLS_SECONDARY,
        )

        assertEquals(5, layout.rows.size)
        assertEquals(listOf("~", "`", "|", "•", "√", "÷", "×", "§", "£", "€"), keyLabels(layout, 1))
        assertEquals(listOf("$", "¢", "^", "°", "=", "{", "}", "\\", "%", "©"), keyLabels(layout, 2))
        assertEquals(listOf("?123", "®", "™", "✓", "[", "]", "≥", "∞", ""), keyLabels(layout, 3))
        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.SYMBOLS && it.label == "?123" })
        assertTrue(layout.rows.flattenedKeys().any { it.label == "€" })
        assertTrue(layout.rows.flattenedKeys().none { it.role == KeyRole.PLACEHOLDER })
    }

    @Test
    fun `symbol layouts have stable unique ids`() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            listOf(KeyboardLayoutMode.SYMBOLS_PRIMARY, KeyboardLayoutMode.SYMBOLS_SECONDARY).forEach { mode ->
                val layout = KeyboardLayoutResolver.resolve(inputMethod, mode)
                val ids = buildList {
                    layout.suggestionBar?.emojiKey?.id?.let(::add)
                    addAll(layout.rows.flattenedKeys().map { it.id })
                }
                assertEquals(ids.size, ids.distinct().size)
            }
        }
    }

    @Test
    fun `no suggestions keeps emoji toolbar without disabling language swipe`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.SYMBOLS_PRIMARY,
            KeyboardEditorMode.TEXT,
            suggestionsEnabled = false,
        )

        val bar = requireNotNull(layout.suggestionBar)
        assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
        assertFalse(bar.suggestionsEnabled)
        val space = layout.rows.last().keys.first { it.role == KeyRole.SPACE }
        assertEquals(KeySwipeAction.TOGGLE_LANGUAGE, space.horizontalSwipeAction)
    }

    @Test
    fun `no symbol glyph is repeated across the two pages`() {
        val glyphs = with(SymbolPageContent) {
            primaryRow1 + primaryRow2 + primaryRow3 +
                secondaryRow1 + secondaryRow2 + secondaryRow3
        }

        assertEquals(glyphs.size, glyphs.distinct().size)
    }

    @Test
    fun `compact pages follow Gboard order before Funput-only symbols`() {
        val primary = compact(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        val secondary = compact(KeyboardLayoutMode.SYMBOLS_SECONDARY)

        assertEquals(listOf("@", "#", "₫", "_", "&", "-", "+", "(", ")", "/"), keyLabels(primary, 1))
        assertEquals(listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", ""), keyLabels(primary, 2))
        assertEquals(listOf("~", "`", "|", "•", "÷", "×", "£", "€", "$", "^"), keyLabels(secondary, 0))
        assertEquals(listOf("°", "=", "{", "}", "\\", "%", "©", "[", "]", "…"), keyLabels(secondary, 1))
        assertEquals(listOf("?123", "<", ">", "¥", "≠", "±", "≤", "≥", ""), keyLabels(secondary, 2))
    }

    private fun compact(mode: KeyboardLayoutMode) = KeyboardLayoutResolver.resolve(
        inputMethod = KeyboardInputMethod.TELEX,
        mode = mode,
        editorMode = KeyboardEditorMode.SEARCH,
        showsNumberRow = false,
    )

    private fun keyLabels(layout: KeyboardLayout, row: Int) =
        layout.rows[row].keys.map { it.label }

    private fun labels(keys: List<KeySpec>) = keys.joinToString("") { it.label }

    private fun List<KeyboardRow>.flattenedKeys() = flatMap { it.keys }
}
