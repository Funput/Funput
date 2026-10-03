package app.funput.funput.ime.speech.integration.preparation

import app.funput.funput.ime.speech.integration.flow.FlowPreparation
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.session.SpikeScheduler
import org.junit.Assert.*
import org.junit.Test

class ImeSpeechPreflightTest {
    @Test fun synchronousResultClosesExactlyOnceBeforeContinuing() {
        val service = FlowPreparation()
        val scheduler = SpikeScheduler()
        service.onStart = { it.deliver(SpeechCapability.READY) }
        val preflight = ImeSpeechPreflight(service, scheduler, scheduler)
        var received = 0
        preflight.begin(SpeechLocale.VI) {
            assertEquals(1, service.operations.single().closes)
            assertFalse(preflight.active)
            received++
        }
        preflight.invalidate()
        scheduler.advance(3_000)
        assertEquals(1, received)
        assertEquals(1, service.operations.single().closes)
    }

    @Test fun timeoutClosesAndTriesUnknownWithoutWaitingForever() {
        val service = FlowPreparation()
        val scheduler = SpikeScheduler()
        val preflight = ImeSpeechPreflight(service, scheduler, scheduler)
        val results = mutableListOf<SpeechCapability>()
        preflight.begin(SpeechLocale.VI, results::add)
        scheduler.advance(2_999)
        assertTrue(results.isEmpty())
        scheduler.advance(1)
        service.operations.single().deliver(SpeechCapability.READY)
        assertEquals(listOf(SpeechCapability.UNKNOWN), results)
        assertEquals(1, service.operations.single().closes)
    }

    @Test fun oldCallbackAndTimerCannotCompleteANewTap() {
        val service = FlowPreparation()
        val scheduler = SpikeScheduler()
        val preflight = ImeSpeechPreflight(service, scheduler, scheduler)
        val results = mutableListOf<SpeechCapability>()
        preflight.begin(SpeechLocale.VI, results::add)
        val oldTimer = scheduler.tasks.single()
        val oldOperation = service.operations.single()
        preflight.begin(SpeechLocale.EN, results::add)
        oldOperation.deliver(SpeechCapability.READY)
        oldTimer.action()
        assertTrue(results.isEmpty())
        assertTrue(preflight.active)
        service.operations.last().deliver(SpeechCapability.DOWNLOADABLE)
        assertEquals(listOf(SpeechCapability.DOWNLOADABLE), results)
        assertEquals(listOf(1, 1), service.operations.map { it.closes })
    }

    @Test fun operationFactoryFailureStillHasATerminalOutcome() {
        val service = FlowPreparation().also { it.failure = IllegalStateException() }
        val scheduler = SpikeScheduler()
        val preflight = ImeSpeechPreflight(service, scheduler, scheduler)
        val results = mutableListOf<SpeechCapability>()
        preflight.begin(SpeechLocale.VI, results::add)
        assertEquals(listOf(SpeechCapability.UNKNOWN), results)
        assertFalse(preflight.active)
    }
    @Test fun expiredCallbackUsesTheAbsoluteDeadlineEvenIfTimerWasDelayed() {
        val service = FlowPreparation()
        val scheduler = SpikeScheduler()
        val preflight = ImeSpeechPreflight(service, scheduler, scheduler)
        val results = mutableListOf<SpeechCapability>()
        preflight.begin(SpeechLocale.VI, results::add)
        scheduler.now = 3_000
        service.operations.single().deliver(SpeechCapability.READY)
        assertEquals(listOf(SpeechCapability.UNKNOWN), results)
        assertEquals(1, service.operations.single().closes)
    }
}
