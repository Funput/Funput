package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberRowLayoutTest {
    @Test
    fun telexCompactLettersOmitNumberRow() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = false)

        assertEquals("qwerty-telex-compact", layout.id)
        assertEquals(4, layout.rows.size)
        assertFalse(layout.rows.first().keys.all { it.label.first().isDigit() })
    }

    @Test
    fun advancedTelexCompactMatchesTelexShape() {
        val layout = KeyboardLayouts.forInputMethod(
            KeyboardInputMethod.TELEX_ADVANCED,
            showsNumberRow = false,
        )

        assertEquals("qwerty-telex-advanced-compact", layout.id)
        assertEquals(4, layout.rows.size)
    }

    @Test
    fun vniAlwaysKeepsNumberRow() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI, showsNumberRow = false)

        assertEquals("qwerty-vni", layout.id)
        assertEquals(5, layout.rows.size)
        assertTrue(layout.rows.first().keys.all { it.label.first().isDigit() })
    }

    @Test
    fun resolverUsesCompactSymbolsForTelexTextWithoutNumberRow() {
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.TELEX,
            mode = KeyboardLayoutMode.SYMBOLS_PRIMARY,
            editorMode = KeyboardEditorMode.TEXT,
            showsNumberRow = false,
        )

        assertTrue(layout.id.contains("compact"))
        assertEquals(4, layout.rows.size)
    }

    @Test
    fun resolverKeepsFullSymbolsWhenNumberRowShown() {
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.TELEX,
            mode = KeyboardLayoutMode.SYMBOLS_PRIMARY,
            editorMode = KeyboardEditorMode.TEXT,
            showsNumberRow = true,
        )

        assertFalse(layout.id.contains("compact"))
        assertEquals(5, layout.rows.size)
    }

    @Test
    fun compactHeightIsShorterThanFullTelexHeight() {
        val compact = KeyboardDimensions.recommendedHeightDp(
            KeyboardInputMethod.TELEX,
            showsNumberRow = false,
        )
        val full = KeyboardDimensions.recommendedHeightDp(
            KeyboardInputMethod.TELEX,
            showsNumberRow = true,
        )

        assertTrue(compact < full)
    }

    @Test
    fun securePagesKeepFullHeightWhenNumberRowIsOff() {
        // A secure page has no number row to trade for long-press digits, so the preference
        // cannot shrink it. The QWERTY editors do follow it — WebEditorNumberRowLayoutTest.
        for (editorMode in listOf(KeyboardEditorMode.PASSWORD, KeyboardEditorMode.PIN)) {
            val rows = { showsNumberRow: Boolean ->
                KeyboardLayoutResolver.resolve(
                    inputMethod = KeyboardInputMethod.TELEX,
                    mode = KeyboardLayoutMode.LETTERS,
                    editorMode = editorMode,
                    showsNumberRow = showsNumberRow,
                ).rows.size
            }

            assertEquals("$editorMode keeps its own rows", rows(true), rows(false))
            assertEquals(
                "$editorMode height must not follow the preference",
                KeyboardDimensions.recommendedHeightDp(
                    KeyboardInputMethod.TELEX,
                    editorMode,
                    showsNumberRow = true,
                ),
                KeyboardDimensions.recommendedHeightDp(
                    KeyboardInputMethod.TELEX,
                    editorMode,
                    showsNumberRow = false,
                ),
                0.001f,
            )
        }
    }
}
