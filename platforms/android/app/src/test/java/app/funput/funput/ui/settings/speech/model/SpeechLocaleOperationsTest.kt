package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.*
import org.junit.Assert.*
import org.junit.Test

class SpeechLocaleOperationsTest {
    private val service = FakeSpeechPreparation()
    private val state = mutableMapOf<SpeechLocale, SpeechLocaleState>()
    private val operations = SpeechLocaleOperations(service) { locale, value -> state[locale] = value }

    @Test fun openingOnlyChecksLocalesAndClosingRejectsAllLateEvents() {
        operations.resume()
        assertEquals(SpeechLocale.entries, service.checks.map { it.locale })
        assertTrue(service.downloads.isEmpty())
        operations.stop()
        service.checks.forEach { it.emit(SpeechCapability.READY); assertEquals(1, it.op.closes) }
        assertTrue(state.values.all { it.checking })
    }
    @Test fun refreshCancelsOldCheckAndDoesNotAcceptItsFinalResult() {
        operations.resume()
        val old = service.checks.first()
        operations.check(SpeechLocale.VI)
        old.emit(SpeechCapability.READY)
        assertTrue(state.getValue(SpeechLocale.VI).checking)
        service.checks.last().emit(SpeechCapability.DOWNLOADABLE)
        assertEquals(SpeechCapability.DOWNLOADABLE, state.getValue(SpeechLocale.VI).capability)
        assertEquals(1, old.op.closes)
    }
    @Test fun downloadRequiresActionAndSuccessRechecksBeforeReady() {
        operations.resume()
        service.checks.first().emit(SpeechCapability.DOWNLOADABLE)
        operations.download(SpeechLocale.VI)
        val download = service.downloads.single()
        download.emit(SpeechDownloadEvent.Progress(25))
        assertTrue(state.getValue(SpeechLocale.VI).downloading)
        download.emit(SpeechDownloadEvent.Success)
        assertTrue(state.getValue(SpeechLocale.VI).checking)
        assertEquals(3, service.checks.size)
        service.checks.last().emit(SpeechCapability.READY)
        assertEquals(SpeechCapability.READY, state.getValue(SpeechLocale.VI).capability)
        assertEquals(1, download.op.closes)
    }
    @Test fun scheduledAndLegacyRequestsRemainManualAndNeverClaimReady() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        service.downloads.last().emit(SpeechDownloadEvent.Scheduled)
        assertEquals(SpeechCapability.PENDING, state.getValue(SpeechLocale.VI).capability)
        operations.download(SpeechLocale.EN)
        service.downloads.last().emit(SpeechDownloadEvent.Requested)
        assertEquals(SpeechCapability.UNKNOWN, state.getValue(SpeechLocale.EN).capability)
        assertEquals(2, service.checks.size)
    }
    @Test fun closingDownloadSuppressesSuccessAndDoesNotStartAnotherCheck() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        operations.stop()
        service.downloads.single().emit(SpeechDownloadEvent.Success)
        assertEquals(2, service.checks.size)
        assertEquals(1, service.downloads.single().op.closes)
    }
    @Test fun synchronousResponseFindsStoredHandleAndReleasesIt() {
        service.synchronousCheck = SpeechCapability.READY
        operations.resume()
        assertTrue(state.values.all { it.capability == SpeechCapability.READY })
        operations.stop()
        assertTrue(service.checks.all { it.op.closes == 1 })
    }
}
