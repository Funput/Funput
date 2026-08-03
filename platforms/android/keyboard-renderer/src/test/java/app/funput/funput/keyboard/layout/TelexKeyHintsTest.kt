package app.funput.funput.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelexKeyHintsTest {
    @Test
    fun toneKeysExposeStableGlyphsAndAccessibility() {
        val expected = mapOf(
            's' to ("´" to "S, dấu sắc"),
            'f' to ("`" to "F, dấu huyền"),
            'r' to ("̉" to "R, dấu hỏi"),
            'x' to ("˜" to "X, dấu ngã"),
            'j' to ("̣" to "J, dấu nặng"),
            'z' to ("×" to "Z, xóa dấu"),
        )

        expected.forEach { (character, glyphAndLabel) ->
            val (glyph, label) = glyphAndLabel
            assertEquals(glyph, TelexKeyHints.hint(character)?.glyph)
            assertEquals(label, TelexKeyHints.accessibilityLabel(character))
        }
    }

    @Test
    fun nonToneLettersHaveNoHint() {
        assertNull(TelexKeyHints.hint('a'))
        assertNull(TelexKeyHints.hint('w'))
        assertNull(TelexKeyHints.accessibilityLabel('d'))
    }
}
