package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.*
import org.junit.Assert.*
import org.junit.Test

class SpeechLocaleOperationsTest {
    private val service = FakeSpeechPreparation()
    private val state = mutableMapOf<SpeechLocale, SpeechLocaleState>()
    private val scheduled = mutableListOf<Pair<() -> Unit, AutoCloseableSpy>>()
    private val consents = mutableListOf<SpeechLocale>()
    private val operations = SpeechLocaleOperations(service,
        scheduler = { _, task -> AutoCloseableSpy().also { scheduled += task to it } },
        consentNeeded = consents::add,
    ) { locale, value -> state[locale] = value }

    class AutoCloseableSpy : AutoCloseable {
        var closed = false
        override fun close() { closed = true }
    }

    /** Runs the latest re-check that is still scheduled. */
    private fun fireRecheck() = scheduled.last { !it.second.closed }.first()

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
    @Test fun scheduledAndLegacyRequestsNeverClaimReady() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        service.downloads.last().emit(SpeechDownloadEvent.Scheduled)
        assertEquals(SpeechCapability.PENDING, state.getValue(SpeechLocale.VI).capability)
        operations.download(SpeechLocale.EN)
        service.downloads.last().emit(SpeechDownloadEvent.Requested)
        assertEquals(SpeechCapability.UNKNOWN, state.getValue(SpeechLocale.EN).capability)
        assertEquals(2, service.checks.size)
    }
    @Test fun scheduledDownloadAsksForConsentOnceAndChecksAgainLater() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        service.downloads.single().emit(SpeechDownloadEvent.Scheduled)
        service.downloads.single().emit(SpeechDownloadEvent.Scheduled)
        assertEquals(listOf(SpeechLocale.VI), consents)
        fireRecheck()
        assertEquals(3, service.checks.size)
        assertEquals(SpeechLocale.VI, service.checks.last().locale)
    }
    @Test fun pendingLocaleKeepsCheckingUntilReady() {
        operations.resume()
        service.checks.first().emit(SpeechCapability.PENDING)
        fireRecheck()
        service.checks.last().emit(SpeechCapability.PENDING)
        fireRecheck()
        service.checks.last().emit(SpeechCapability.READY)
        assertEquals(SpeechCapability.READY, state.getValue(SpeechLocale.VI).capability)
        assertEquals(2, scheduled.size)
    }
    @Test fun recheckingStopsAfterItsBudget() {
        operations.resume()
        repeat(SpeechLocaleOperations.MAX_RECHECKS + 5) {
            service.checks.last { it.locale == SpeechLocale.VI }.emit(SpeechCapability.PENDING)
            scheduled.lastOrNull { !it.second.closed }?.let { pending ->
                pending.second.close()
                pending.first()
            }
        }
        assertEquals(SpeechLocaleOperations.MAX_RECHECKS, scheduled.size)
    }
    @Test fun confirmedPackReadsAsInstallingAcrossTheDialogRoundTrip() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        service.downloads.single().emit(SpeechDownloadEvent.Scheduled)
        operations.stop() // the consent dialog covers the screen
        operations.resume()
        service.checks.last { it.locale == SpeechLocale.VI }.emit(SpeechCapability.DOWNLOADABLE)
        val vi = state.getValue(SpeechLocale.VI)
        assertTrue(vi.installing)
        assertEquals(SpeechCapability.PENDING, vi.capability)
        fireRecheck()
        service.checks.last().emit(SpeechCapability.READY)
        assertEquals(SpeechLocaleState(capability = SpeechCapability.READY), state.getValue(SpeechLocale.VI))
    }
    @Test fun installingGivesUpAndOffersTheDownloadAgain() {
        operations.resume()
        operations.download(SpeechLocale.VI)
        service.downloads.single().emit(SpeechDownloadEvent.Scheduled)
        repeat(SpeechLocaleOperations.INSTALL_CHECKS + 1) {
            fireRecheck()
            service.checks.last().emit(SpeechCapability.DOWNLOADABLE)
        }
        assertEquals(SpeechLocaleState(capability = SpeechCapability.DOWNLOADABLE), state.getValue(SpeechLocale.VI))
    }
    @Test fun stoppingCancelsScheduledRechecksAndConsent() {
        operations.resume()
        service.checks.first().emit(SpeechCapability.PENDING)
        operations.download(SpeechLocale.EN)
        operations.stop()
        service.downloads.single().emit(SpeechDownloadEvent.Scheduled)
        assertTrue(scheduled.all { it.second.closed })
        assertTrue(consents.isEmpty())
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
