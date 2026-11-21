package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which endings of the text before a caret name a word to predict after. */
class DocumentWordsTest {
    @Test
    fun `a word and one space is a word to follow`() {
        assertEquals(DocumentWords("ơn", older = "cảm"), "Xin cảm ơn ".wordsBeforeCaret())
        assertEquals(DocumentWords("Chào", older = null), "Chào ".wordsBeforeCaret())
    }

    @Test
    fun `anything else before the caret is not`() {
        for (text in listOf("", " ", "cảm", "cảm  ", "cảm.", "cảm. ", "cảm\n", "cảm ơn\n ")) {
            assertNull("\"$text\"", text.wordsBeforeCaret())
        }
    }

    @Test
    fun `only words the trackers would learn count`() {
        assertNull("one letter is no word", "a ".wordsBeforeCaret())
        assertNull("digits are no word", "302 ".wordsBeforeCaret())
        assertEquals("a digit cuts the word there", DocumentWords("xyz", older = null), "12xyz ".wordsBeforeCaret())
        assertNull("too long to be one", ("a".repeat(MaximumTokenLength + 1) + " ").wordsBeforeCaret())
    }

    @Test
    fun `the older word needs a single space and a word of its own`() {
        assertEquals(DocumentWords("ơn", older = null), "cảm, ơn ".wordsBeforeCaret())
        assertEquals(DocumentWords("ơn", older = null), "cảm  ơn ".wordsBeforeCaret())
        assertEquals(DocumentWords("ơn", older = null), "a ơn ".wordsBeforeCaret())
    }

    @Test
    fun `a word cut by the reading window is not trusted`() {
        val cut = "x".repeat(ContextLookback - 4) + " ơn "
        assertEquals(ContextLookback, cut.length)
        assertEquals("the word before may be longer than what was read", DocumentWords("ơn", older = null), cut.wordsBeforeCaret())

        val whole = "x".repeat(8) + " ơn "
        assertEquals(DocumentWords("ơn", older = "xxxxxxxx"), whole.wordsBeforeCaret())
    }

    @Test
    fun `decomposed marks are part of the word`() {
        assertEquals(DocumentWords("ơn", older = null), "ơn ".wordsBeforeCaret())
    }
}
