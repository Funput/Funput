package app.funput.funput.ime.speech.preparation.legacy

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.PreparationFixture
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import org.junit.Assert.*
import org.junit.Test

class LegacySpeechDownloadTest {
    @Test fun clientLivesUntilQueuedDownloadIsAcknowledged() {
        val f = PreparationFixture(33)
        f.factory.configure = { it.holdLegacy = true }
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.VI) { events += it }.start()
        assertTrue(events.isEmpty())
        assertEquals(0, f.client.closes)
        f.client.legacyListener(SpeechDownloadEvent.Requested)
        assertEquals(listOf(SpeechDownloadEvent.Requested), events)
        assertEquals(1, f.client.closes)
    }
    @Test fun staleTrackedEventsCannotTerminateLegacyFallback() {
        val f = PreparationFixture(34)
        f.factory.configure = { it.holdLegacy = true }
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.VI) { events += it }.start()
        val oldError = SpeechDownloadEvent.Failure(15)
        f.client.downloadListener(oldError)
        f.client.downloadListener(oldError)
        f.client.downloadListener(SpeechDownloadEvent.Progress(10))
        assertTrue(events.isEmpty())
        assertEquals(0, f.client.closes)
        f.client.legacyListener(SpeechDownloadEvent.Requested)
        assertEquals(listOf(SpeechDownloadEvent.Requested), events)
        assertEquals(1, f.client.closes)
    }
    @Test fun closingBeforeAcknowledgementDiscardsItAndReleasesClient() {
        val f = PreparationFixture(33)
        f.factory.configure = { it.holdLegacy = true }
        val op = f.service.download(SpeechLocale.VI) { error("closed") }
        op.start()
        op.close()
        f.client.legacyListener(SpeechDownloadEvent.Requested)
        assertEquals(1, f.client.closes)
    }
}
