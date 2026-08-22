package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommittedWordTailTest {
    @Test
    fun `backspace over the space reopens the word`() {
        val tail = CommittedWordTail()
        tail.record("phủ ")

        assertEquals("phủ", tail.backspace())
        tail.resolve(true)
        assertNull(tail.backspace())
    }

    @Test
    fun `a refused word stays shadowed until a later backspace makes it a syllable`() {
        val tail = CommittedWordTail()
        tail.record("chào ")
        tail.record("dungh ")

        assertEquals("dungh", tail.backspace())
        tail.resolve(false)
        assertEquals("dung", tail.backspace())
        tail.resolve(true)
        assertEquals("chào", tail.backspace())
    }

    @Test
    fun `every separator has to be deleted before the word is offered`() {
        val tail = CommittedWordTail()
        tail.record("phủ, ")

        assertNull(tail.backspace())
        assertEquals("phủ", tail.backspace())
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
