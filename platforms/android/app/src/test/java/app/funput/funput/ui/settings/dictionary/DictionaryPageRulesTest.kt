package app.funput.funput.ui.settings.dictionary

import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ui.settings.dictionary.model.DictionaryWordDraft
import app.funput.funput.ui.settings.dictionary.model.addMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryPageRulesTest {
    @Test fun `a draft is one word of at most 32 letters`() {
        assertTrue(DictionaryWordDraft("  Funput ").canSave)
        assertEquals("funput", DictionaryWordDraft("Funput").suggestionPreview)
        assertEquals(R.string.dictionary_add_spaced, DictionaryWordDraft("hai từ").problem)
        // Counted in code points, as the engine counts: thirty-two marked letters still fit.
        assertTrue(DictionaryWordDraft("ệ".repeat(32)).canSave)
        assertEquals(R.string.dictionary_add_too_long, DictionaryWordDraft("ệ".repeat(33)).problem)
        assertFalse(DictionaryWordDraft("   ").canSave)
        assertNull(DictionaryWordDraft("").problem)
    }

    @Test fun `only an applied add closes the sheet`() {
        assertNull(DictionaryEditOutcome.APPLIED.addMessage())
        assertEquals(R.string.dictionary_add_unchanged, DictionaryEditOutcome.UNCHANGED.addMessage())
        assertEquals(R.string.dictionary_add_limit, DictionaryEditOutcome.LIMIT_REACHED.addMessage())
        assertEquals(R.string.dictionary_add_invalid, DictionaryEditOutcome.INVALID.addMessage())
    }
}
