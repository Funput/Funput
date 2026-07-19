package app.funput.funput.ime.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalSuggestionRequestSlotTest {
    @Test
    fun `coalesces requests and keeps latest generation`() {
        val slot = PersonalSuggestionRequestSlot()

        assertTrue(slot.submit(request(1)))
        assertFalse(slot.submit(request(2)))
        assertFalse(slot.submit(request(3)))

        assertEquals(request(3), slot.takeLatest())
        assertFalse(slot.finishDrain())
    }

    @Test
    fun `request arriving at drain boundary schedules successor`() {
        val slot = PersonalSuggestionRequestSlot()
        slot.submit(request(1))
        slot.takeLatest()
        slot.submit(request(2))

        assertTrue(slot.finishDrain())
        assertEquals(request(2), slot.takeLatest())
    }

    @Test
    fun `stress remains bounded to one latest request`() {
        val slot = PersonalSuggestionRequestSlot()

        repeat(100_000) { slot.submit(request(it.toLong())) }

        assertEquals(request(99_999), slot.takeLatest())
        assertFalse(slot.finishDrain())
    }

    @Test
    fun `main thread request bookkeeping p95 stays below budget`() {
        val slot = PersonalSuggestionRequestSlot()
        val samples = LongArray(100_000)

        repeat(samples.size) { index ->
            val started = System.nanoTime()
            slot.submit(request(index.toLong()))
            samples[index] = System.nanoTime() - started
        }
        samples.sort()

        assertTrue("p95=${samples[95_000] / 1_000}µs", samples[95_000] < 100_000)
    }

    private fun request(generation: Long) = PersonalSuggestionRequest(
        prefix = "tu",
        generation = generation,
        session = 7,
        startedNanos = 0,
    )
}
