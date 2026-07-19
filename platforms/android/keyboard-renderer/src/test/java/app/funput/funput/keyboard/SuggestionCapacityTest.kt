package app.funput.funput.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionCapacityTest {
    @Test
    fun `keeps only candidates whose segments remain at least 64dp`() {
        assertEquals(0, SuggestionCapacity.visibleCount(63f, 1f, 3))
        assertEquals(1, SuggestionCapacity.visibleCount(64f, 1f, 3))
        assertEquals(2, SuggestionCapacity.visibleCount(128f, 1f, 3))
        assertEquals(3, SuggestionCapacity.visibleCount(192f, 1f, 3))
    }

    @Test
    fun `accounts for density and caps output at three`() {
        assertEquals(2, SuggestionCapacity.visibleCount(256f, 2f, 5))
        assertEquals(3, SuggestionCapacity.visibleCount(1_000f, 1f, 8))
    }
}
