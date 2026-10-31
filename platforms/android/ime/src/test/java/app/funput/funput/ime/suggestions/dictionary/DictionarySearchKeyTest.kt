package app.funput.funput.ime.suggestions.dictionary

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionarySearchKeyTest {
    @Test
    fun `marks, đ and case are folded away`() {
        assertTrue(DictionarySearchKey("Được").contains(DictionarySearchKey("duoc")))
        assertTrue(DictionarySearchKey("đm").contains(DictionarySearchKey("DM")))
        assertTrue(DictionarySearchKey("Nguyễn").contains(DictionarySearchKey("guyê")))
    }

    @Test
    fun `it matches anywhere in the word, and an empty search matches all`() {
        assertTrue(DictionarySearchKey("Funput").contains(DictionarySearchKey("npu")))
        assertTrue(DictionarySearchKey("").isEmpty)
        assertTrue(DictionarySearchKey("Funput").contains(DictionarySearchKey("")))
        assertFalse(DictionarySearchKey("Funput").contains(DictionarySearchKey("fx")))
    }
}
