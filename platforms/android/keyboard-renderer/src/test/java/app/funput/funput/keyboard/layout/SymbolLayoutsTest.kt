package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolLayoutsTest {
    @Test
    fun `primary page contains digits and navigation keys`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.TELEX,
            KeyboardLayoutMode.SYMBOLS_PRIMARY,
        )

        assertEquals("1234567890", labels(layout.rows.first().keys))
        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.MORE_SYMBOLS })
        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.LETTERS })
    }

    @Test
    fun `secondary page returns to primary page`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.SYMBOLS_SECONDARY,
        )

        assertTrue(layout.rows.flattenedKeys().any { it.role == KeyRole.SYMBOLS && it.label == "?123" })
        assertTrue(layout.rows.flattenedKeys().any { it.label == "€" })
    }

    @Test
    fun `symbol layouts have stable unique ids`() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            listOf(KeyboardLayoutMode.SYMBOLS_PRIMARY, KeyboardLayoutMode.SYMBOLS_SECONDARY).forEach { mode ->
                val layout = KeyboardLayoutResolver.resolve(inputMethod, mode)
                val emojiId = requireNotNull(layout.suggestionBar).emojiKey.id
                val ids = listOf(emojiId) + layout.rows.flattenedKeys().map { it.id }
                assertEquals(ids.size, ids.distinct().size)
            }
        }
    }

    @Test
    fun `no suggestions removes toolbar without disabling language swipe`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.SYMBOLS_PRIMARY,
            KeyboardEditorMode.TEXT,
            suggestionBarEnabled = false,
        )

        assertNull(layout.suggestionBar)
        val space = layout.rows.last().keys.first { it.role == KeyRole.SPACE }
        assertEquals(KeySwipeAction.TOGGLE_LANGUAGE, space.horizontalSwipeAction)
    }

    private fun labels(keys: List<KeySpec>) = keys.joinToString("") { it.label }

    private fun List<KeyboardRow>.flattenedKeys() = flatMap { it.keys }
}
