package app.funput.funput.ime.speech.integration.actions

import org.junit.Assert.*
import org.junit.Test

class SpeechHardwareBoundaryTest {
    @Test fun backCancelsOnceAndConsumesDownRepeatAndUp() {
        var engaged = true
        var cancellations = 0
        var invalidations = 0
        val boundary = SpeechHardwareBoundary({
            if (engaged) { cancellations++; engaged = false; true } else false
        }, { invalidations++ })
        assertTrue(boundary.down(true))
        assertTrue(boundary.down(true))
        assertTrue(boundary.up(true))
        assertEquals(1, cancellations)
        assertEquals(0, invalidations)
        assertFalse(boundary.down(true))
        assertFalse(boundary.up(true))
        assertEquals(2, invalidations)
    }

    @Test fun navigationAndModifiersInvalidateBeforeForwarding() {
        val order = mutableListOf<String>()
        val boundary = SpeechHardwareBoundary({ false }, { order += "cancel" })
        repeat(4) {
            if (!boundary.down(false)) order += "down"
            if (!boundary.up(false)) order += "up"
        }
        assertEquals(List(4) { listOf("cancel", "down", "cancel", "up") }.flatten(), order)
    }
}
