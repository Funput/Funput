package app.funput.funput.ime.suggestions.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The codes are fixed by crates/funput-jni/src/suggestion/dictionary/codes.rs; these pin both ends. */
class DictionaryCodesTest {
    @Test
    fun `edit kinds and sorts keep the native numbering`() {
        assertEquals(listOf(0, 1, 2, 3), DictionaryEditKind.entries.map(DictionaryCodes::of))
        assertEquals(listOf(0, 1, 2), DictionarySort.entries.map(DictionaryCodes::of))
    }

    @Test
    fun `the blocked list is not one of the engine's filters`() {
        assertEquals(1, DictionaryCodes.of(DictionaryFilter.LEARNED))
        assertEquals(2, DictionaryCodes.of(DictionaryFilter.ADDED))
        assertEquals(DictionaryCodes.FILTER_ALL, DictionaryCodes.of(DictionaryFilter.BLOCKED))
    }

    @Test
    fun `an unknown outcome is a refusal, never a success`() {
        assertEquals(DictionaryEditOutcome.APPLIED, DictionaryCodes.outcome(0))
        assertEquals(DictionaryEditOutcome.LIMIT_REACHED, DictionaryCodes.outcome(3))
        assertEquals(DictionaryEditOutcome.INVALID, DictionaryCodes.outcome(-1))
        assertEquals(DictionaryEditOutcome.INVALID, DictionaryCodes.outcome(42))
    }

    @Test
    fun `a listing decodes word by word`() {
        val words = DictionaryCodes.words(arrayOf("được", "Funput"), longArrayOf(12, 7, 0, 0, 9, 1))

        assertEquals(
            listOf(
                DictionaryWord("được", uses = 12, lastUsed = 7, origin = DictionaryOrigin.LEARNED),
                DictionaryWord("Funput", uses = 0, lastUsed = 9, origin = DictionaryOrigin.ADDED),
            ),
            words,
        )
    }

    @Test
    fun `arrays that disagree read as an empty listing`() {
        assertTrue(DictionaryCodes.words(arrayOf("được"), longArrayOf(1, 2)).isEmpty())
        assertTrue(DictionaryCodes.words(null, longArrayOf()).isEmpty())
        assertTrue(DictionaryCodes.words(arrayOf("được"), null).isEmpty())
    }

    @Test
    fun `a use count past Int range is clamped rather than wrapped`() {
        val word = DictionaryCodes.words(arrayOf("a"), longArrayOf(Long.MAX_VALUE, 0, 0)).single()
        assertEquals(Int.MAX_VALUE, word.uses)
    }
}
