package app.funput.funput.ime.suggestions

import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A long-press edit must reach the spelling the store holds, not the one the bar drew. */
class PersonalSuggestionCandidatesTest {
    private val drawn = mutableListOf<List<String>>()
    private val candidates = PersonalSuggestionCandidates(show = { drawn += it }, shift = { ShiftState.OFF })

    @Test
    fun `the stored spelling stands behind the cased one`() {
        candidates.receive(listOf("việt", "viết"), prefix = "Vi")

        assertEquals(listOf("Việt", "Viết"), candidates.shown)
        assertTrue(candidates.isShown(0, "Việt"))
        assertEquals("việt", candidates.stored(0, "Việt"))
    }

    @Test
    fun `a slot that no longer reads the same word has nothing stored`() {
        candidates.receive(listOf("việt"), prefix = "vi")

        assertNull(candidates.stored(0, "viết"))
        assertNull(candidates.stored(1, "việt"))
        assertFalse(candidates.isShown(0, "Việt"))
    }

    @Test
    fun `clearing drops both lists`() {
        candidates.receive(listOf("việt"), prefix = "vi")
        candidates.clear()

        assertNull(candidates.stored(0, "việt"))
        assertEquals(emptyList<String>(), drawn.last())
    }
}
