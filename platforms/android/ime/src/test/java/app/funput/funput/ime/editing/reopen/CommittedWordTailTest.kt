package app.funput.funput.ime.editing.reopen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommittedWordTailTest {
    @Test
    fun `backspace over the space reopens the word`() {
        val tail = CommittedWordTail()
        tail.record("phủ ")

        assertEquals(TailStep(erased = " ", word = "phủ"), tail.backspace())
        tail.adoptWord()
        assertNull(tail.backspace())
    }

    @Test
    fun `a refused word stays shadowed until a later backspace makes it a syllable`() {
        val tail = CommittedWordTail()
        tail.record("chào ")
        tail.record("dungh ")

        assertEquals("dungh", tail.backspace()?.word)
        assertEquals("dung", tail.backspace()?.word)
        tail.adoptWord()
        assertEquals("chào", tail.backspace()?.word)
    }

    @Test
    fun `every separator has to be deleted before the word is offered`() {
        val tail = CommittedWordTail()
        tail.record("phủ, ")

        assertEquals(TailStep(erased = " ", word = null), tail.backspace())
        assertEquals("phủ", tail.backspace()?.word)
    }

    @Test
    fun `a supplementary character is erased whole`() {
        val tail = CommittedWordTail()
        tail.record("ok 👍")

        assertEquals(TailStep(erased = "👍", word = null), tail.backspace())
    }

    @Test
    fun `adopting after a separator keeps the tail`() {
        val tail = CommittedWordTail()
        tail.record("phủ, ")
        tail.backspace()
        tail.adoptWord()

        assertEquals("phủ", tail.backspace()?.word)
    }

    @Test
    fun `clear forgets the shadow`() {
        val tail = CommittedWordTail()
        tail.record("phủ ")
        tail.clear()
        assertNull(tail.backspace())
    }

    @Test
    fun `backspacing an empty shadow is a no-op`() {
        val tail = CommittedWordTail()
        assertNull(tail.backspace())
        assertNull(tail.backspace())
    }
}
