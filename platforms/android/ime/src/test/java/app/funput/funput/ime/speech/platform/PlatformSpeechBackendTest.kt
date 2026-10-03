package app.funput.funput.ime.speech.platform

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformSpeechBackendTest {
    @Test
    fun `open does not create probe or emit`() {
        val fixture = SpeechBackendFixture()
        assertTrue(fixture.factory.clients.isEmpty())
        assertEquals(0, fixture.factory.availabilityChecks)
        assertTrue(fixture.events.isEmpty())
        fixture.recording.close()
        fixture.recording.start()
        assertTrue(fixture.factory.clients.isEmpty())
    }

    @Test
    fun `start assigns client before synchronous final and creates only once`() {
        val fixture = SpeechBackendFixture()
        fixture.factory.startEvent = SpeechEvent.Final("xin chào")
        fixture.recording.start()
        fixture.recording.start()
        fixture.recording.close()
        assertEquals(listOf(SpeechEvent.Final("xin chào")), fixture.events)
        assertEquals(listOf(SpeechRequestFactory.create(SpeechLocale.VI)), fixture.client.requests)
        assertEquals(1, fixture.client.destroys)
        assertEquals(0, fixture.client.cancels)
    }

    @Test
    fun `stop once waits for final while partial snapshots remain visible`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        fixture.client.emit(SpeechEvent.Ready)
        fixture.client.emit(SpeechEvent.Partial("xin"))
        fixture.recording.stop()
        fixture.recording.stop()
        assertEquals(1, fixture.client.stops)
        assertEquals(0, fixture.client.destroys)
        fixture.client.emit(SpeechEvent.Partial("xin chào"))
        fixture.client.emit(SpeechEvent.Final("xin chào"))
        fixture.client.emit(SpeechEvent.Final("duplicate"))
        assertEquals(4, fixture.events.size)
        assertEquals(SpeechEvent.Final("xin chào"), fixture.events.last())
        assertEquals(1, fixture.client.destroys)
    }

    @Test
    fun `natural endpoint is nonterminal and still waits for final`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        fixture.client.emit(SpeechEvent.Ready)
        fixture.client.emit(SpeechEvent.Ended)
        assertEquals(listOf(SpeechEvent.Ready, SpeechEvent.Ended), fixture.events)
        assertEquals(0, fixture.client.stops)
        assertEquals(0, fixture.client.destroys)
        fixture.client.emit(SpeechEvent.Final("final after endpoint"))
        assertEquals(SpeechEvent.Final("final after endpoint"), fixture.events.last())
        assertEquals(1, fixture.client.destroys)
    }

    @Test
    fun `cancel and close are idempotent and discard late callbacks`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        fixture.client.cancelError = IllegalStateException()
        fixture.client.destroyError = IllegalStateException()
        fixture.recording.cancel()
        fixture.recording.close()
        fixture.recording.cancel()
        fixture.client.emit(SpeechEvent.Ready)
        fixture.client.emit(SpeechEvent.Final("late"))
        fixture.client.emit(SpeechEvent.Failure(42))
        assertEquals(1, fixture.client.cancels)
        assertEquals(1, fixture.client.destroys)
        assertTrue(fixture.events.isEmpty())
    }

    @Test
    fun `commands and callbacks share serialized queue`() {
        val fixture = SpeechBackendFixture(queued = true)
        fixture.recording.start()
        assertTrue(fixture.factory.clients.isEmpty())
        fixture.queue.drain()
        fixture.recording.cancel()
        fixture.client.emit(SpeechEvent.Final("queued after cancellation"))
        fixture.queue.drain()
        assertTrue(fixture.events.isEmpty())
        assertEquals(1, fixture.client.destroys)
    }

    @Test
    fun `old listener cannot emit into a new recording`() {
        val fixture = SpeechBackendFixture()
        fixture.recording.start()
        val oldClient = fixture.client
        fixture.recording.cancel()
        val newEvents = mutableListOf<SpeechEvent>()
        val next = fixture.backend.open(SpeechLocale.EN, newEvents::add)
        next.start()
        oldClient.emit(SpeechEvent.Final("old"))
        fixture.factory.clients.last().emit(SpeechEvent.Final("new"))
        assertTrue(fixture.events.isEmpty())
        assertEquals(listOf(SpeechEvent.Final("new")), newEvents)
    }
}
