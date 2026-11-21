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

    @Test
    fun `held words stay on the bar but take no tap`() {
        candidates.receive(listOf("bạn", "bán"), prefix = "ba")
        candidates.hold()

        assertEquals(listOf("bạn", "bán"), candidates.shown)
        assertEquals("nothing was redrawn", 1, drawn.size)
        assertFalse(candidates.isShown(0, "bạn"))
        assertNull(candidates.stored(0, "bạn"))
    }

    @Test
    fun `the next answer replaces what was held, and the same answer is tappable again`() {
        candidates.receive(listOf("bạn", "bán"), prefix = "ba")
        candidates.hold()
        candidates.receive(listOf("bạn"), prefix = "bạ")

        assertEquals(listOf("bạn"), drawn.last())
        assertTrue(candidates.isShown(0, "bạn"))

        candidates.hold()
        candidates.receive(listOf("bạn"), prefix = "bạn")

        assertEquals("an unchanged list is not drawn twice", 2, drawn.size)
        assertTrue(candidates.isShown(0, "bạn"))
    }

    @Test
    fun `an empty answer clears what was held`() {
        candidates.receive(listOf("bạn"), prefix = "ba")
        candidates.hold()
        candidates.receive(emptyList(), prefix = "bax")

        assertEquals(emptyList<String>(), drawn.last())
        assertFalse(candidates.held)
    }

    @Test
    fun `a held bar is not recased, and an empty one holds nothing`() {
        candidates.hold()
        assertFalse(candidates.held)

        candidates.receive(listOf("bạn"), prefix = "ba")
        candidates.hold()
        candidates.draw(prefix = "Ba")

        assertEquals(listOf("bạn"), candidates.shown)
        candidates.clear()
        assertFalse(candidates.held)
    }
}
