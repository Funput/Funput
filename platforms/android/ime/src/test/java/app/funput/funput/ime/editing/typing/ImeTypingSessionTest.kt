package app.funput.funput.ime.editing.typing

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeTypingSessionTest {
    @Test
    fun `first input hides placement once until keyboard reopens`() {
        val state = ImeTypingSession()
        val changes = mutableListOf<Boolean>()
        state.begin()
        state.observe(changes::add)
        state.recordInput()
        state.recordInput()
        state.begin() // Repeated starts while visible must not reset the session.
        assertEquals(listOf(false, true), changes)

        state.end()
        state.begin()
        state.recordInput()
        assertEquals(listOf(false, true, false, true), changes)
    }

    @Test
    fun `replacement view receives current state without restoring old toolbar`() {
        val state = ImeTypingSession()
        val oldView = mutableListOf<Boolean>()
        val newView = mutableListOf<Boolean>()
        state.begin()
        state.observe(oldView::add)
        state.recordInput()
        state.observe(newView::add)
        state.end()
        state.begin()
        assertEquals(listOf(false, true), oldView)
        assertEquals(listOf(true, false), newView)
    }

    @Test
    fun `typing while keyboard is hidden does not consume next introduction`() {
        val state = ImeTypingSession()
        val changes = mutableListOf<Boolean>()
        state.observe(changes::add)
        state.recordInput()
        state.begin()
        assertEquals(listOf(false, false), changes)
    }
}
