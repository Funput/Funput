package app.funput.funput.ime.speech.platform

import app.funput.funput.ime.speech.model.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPlatformFailureTest {
    @Test
    fun `old API refuses before checking availability or creating a client`() {
        val fixture = SpeechBackendFixture()
        fixture.factory.apiLevel = 30
        fixture.recording.start()
        assertEquals(listOf(SpeechEvent.Failure(5)), fixture.events)
        assertEquals(0, fixture.factory.availabilityChecks)
        assertTrue(fixture.factory.clients.isEmpty())
    }

    @Test
    fun `missing on-device service has no fallback`() {
        val fixture = SpeechBackendFixture()
        fixture.factory.available = false
        fixture.recording.start()
        assertEquals(listOf(SpeechEvent.Failure(5)), fixture.events)
        assertTrue(fixture.factory.clients.isEmpty())
    }

    @Test
    fun `factory failure is terminal without creating another client`() {
        val fixture = SpeechBackendFixture()
        fixture.factory.creationError = UnsupportedOperationException()
        fixture.recording.start()
        fixture.recording.start()
        assertEquals(listOf(SpeechEvent.Failure(5)), fixture.events)
        assertTrue(fixture.factory.clients.isEmpty())
    }

    @Test
    fun `permission exception destroys client and reports once`() {
        val fixture = SpeechBackendFixture()
        fixture.factory.startError = SecurityException()
        fixture.recording.start()
        fixture.recording.close()
        fixture.client.emit(SpeechEvent.Failure(5))
        assertEquals(listOf(SpeechEvent.Failure(9)), fixture.events)
        assertEquals(1, fixture.client.destroys)
    }

    @Test
    fun `stop exception destroys client and reports client failure`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        fixture.client.stopError = IllegalStateException()
        fixture.recording.stop()
        assertEquals(listOf(SpeechEvent.Failure(5)), fixture.events)
        assertEquals(1, fixture.client.destroys)
    }

    @Test
    fun `service integer error is preserved and terminal`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        fixture.client.emit(SpeechEvent.Failure(1234))
        fixture.client.emit(SpeechEvent.Partial("late"))
        assertEquals(listOf(SpeechEvent.Failure(1234)), fixture.events)
        assertEquals(1, fixture.client.destroys)
    }
}
