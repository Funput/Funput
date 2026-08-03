package app.funput.funput.ime.editing.composition

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionBoundaryTest {
    @Test
    fun `whitespace and ascii punctuation are boundaries`() {
        listOf(' ', '\n', '\t', ',', '.', '!', '?', '-', '"').forEach { character ->
            assertTrue(CompositionBoundary.isBoundary(character.code, KeyboardInputMethod.TELEX))
        }
    }

    @Test
    fun `letters digits emoji and unicode punctuation are not boundaries`() {
        listOf('a'.code, '1'.code, '9'.code, 'á'.code, '—'.code, 0x1F600).forEach { codePoint ->
            assertFalse(CompositionBoundary.isBoundary(codePoint, KeyboardInputMethod.TELEX))
        }
    }

    @Test
    fun `brackets compose only in advanced Telex`() {
        listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.VNI).forEach { method ->
            assertTrue(CompositionBoundary.isBoundary('['.code, method))
            assertTrue(CompositionBoundary.isBoundary(']'.code, method))
        }
        assertFalse(CompositionBoundary.isBoundary('['.code, KeyboardInputMethod.TELEX_ADVANCED))
        assertFalse(CompositionBoundary.isBoundary(']'.code, KeyboardInputMethod.TELEX_ADVANCED))
        assertTrue(CompositionBoundary.isBoundary('{'.code, KeyboardInputMethod.TELEX_ADVANCED))
        assertTrue(CompositionBoundary.isBoundary('}'.code, KeyboardInputMethod.TELEX_ADVANCED))
    }
}
