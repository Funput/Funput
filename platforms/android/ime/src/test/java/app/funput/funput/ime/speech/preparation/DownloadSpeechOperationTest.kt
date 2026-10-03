package app.funput.funput.ime.speech.preparation

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import org.junit.Assert.*
import org.junit.Test

class DownloadSpeechOperationTest {
    @Test fun api33SendsRequestWithoutInventingProgress() {
        val f = PreparationFixture(33)
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.EN) { events += it }.start()
        assertEquals(listOf(SpeechDownloadEvent.Requested), events)
        assertEquals(1, f.client.downloads)
        assertEquals(0, f.client.trackedDownloads)
        assertEquals(listOf(SpeechRequestFactory.create(SpeechLocale.EN)), f.client.requests)
        assertEquals(1, f.client.closes)
    }
    @Test fun api34ProgressThenSuccessClosesExactlyOnce() {
        val f = PreparationFixture()
        val events = mutableListOf<SpeechDownloadEvent>()
        val op = f.service.download(SpeechLocale.VI) { events += it }
        op.start()
        f.client.downloadListener(SpeechDownloadEvent.Progress(40))
        assertEquals(0, f.client.closes)
        f.client.downloadListener(SpeechDownloadEvent.Success)
        f.client.downloadListener(SpeechDownloadEvent.Success)
        op.close()
        assertEquals(listOf(SpeechDownloadEvent.Progress(40), SpeechDownloadEvent.Success), events)
        assertEquals(1, f.client.closes)
        assertEquals(0, f.client.downloads)
        assertEquals(listOf(SpeechRequestFactory.create(SpeechLocale.VI)), f.client.requests)
    }
    @Test fun scheduledIsTerminalAndNeverWaitsForSuccess() {
        val f = PreparationFixture()
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.VI) { events += it }.start()
        f.client.downloadListener(SpeechDownloadEvent.Scheduled)
        f.time.advance(60_000)
        f.client.downloadListener(SpeechDownloadEvent.Success)
        assertEquals(listOf(SpeechDownloadEvent.Scheduled), events)
        assertEquals(1, f.client.closes)
    }
    @Test fun missingEventSupportFallsBackToLegacyExactlyOnce() {
        val f = PreparationFixture()
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.VI) { events += it }.start()
        val error = SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS)
        f.client.downloadListener(error)
        f.client.downloadListener(error)
        assertEquals(listOf(SpeechDownloadEvent.Requested), events)
        assertEquals(1, f.client.downloads)
        assertEquals(1, f.client.closes)
    }
    @Test fun unrelatedErrorNeverRetriesDownload() {
        val f = PreparationFixture()
        val error = SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK)
        f.service.download(SpeechLocale.EN) { assertEquals(error, it) }.start()
        f.client.downloadListener(error)
        assertEquals(0, f.client.downloads)
        assertEquals(1, f.client.closes)
    }
    @Test fun deadlineIsAbsoluteEvenWithProgress() {
        val f = PreparationFixture()
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.EN) { events += it }.start()
        f.time.advance(59_999)
        f.client.downloadListener(SpeechDownloadEvent.Progress(99))
        f.time.advance(1)
        f.client.downloadListener(SpeechDownloadEvent.Success)
        assertEquals(listOf(SpeechDownloadEvent.Progress(99),
            SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)), events)
        assertEquals(1, f.client.closes)
    }
    @Test fun closingObservationHasNoSystemDownloadCancelCommand() {
        val f = PreparationFixture()
        val op = f.service.download(SpeechLocale.VI) { error("closed") }
        op.start()
        op.close()
        op.close()
        f.client.downloadListener(SpeechDownloadEvent.Progress(1))
        f.client.downloadListener(SpeechDownloadEvent.Success)
        assertEquals(1, f.client.trackedDownloads)
        assertEquals(1, f.client.closes)
    }
    @Test fun lowerApisAndUnavailableServiceNeverCreateClient() {
        for (api in listOf(26, 30, 31, 32)) {
            val f = PreparationFixture(api)
            f.service.download(SpeechLocale.VI) { assertTrue(it is SpeechDownloadEvent.Failure) }.start()
            assertTrue(f.factory.clients.isEmpty())
        }
        val f = PreparationFixture()
        f.factory.available = false
        f.service.download(SpeechLocale.VI) { assertTrue(it is SpeechDownloadEvent.Failure) }.start()
        assertTrue(f.factory.clients.isEmpty())
    }
    @Test fun commandExceptionClosesAndInvalidatesReadyCache() {
        val f = PreparationFixture()
        f.service.check(SpeechLocale.VI) {}.start()
        f.client.languages(installed = listOf("vi-VN"))
        f.factory.configure = { it.onDownload = { error("download") } }
        f.service.download(SpeechLocale.VI) { assertTrue(it is SpeechDownloadEvent.Failure) }.start()
        assertEquals(1, f.client.closes)
        f.service.check(SpeechLocale.VI) {}.start()
        assertEquals(3, f.factory.clients.size)
    }
    @Test fun progressIsClampedAndSynchronousSuccessIsSafe() {
        val f = PreparationFixture()
        f.factory.configure = { client -> client.onDownload = {
            client.downloadListener(SpeechDownloadEvent.Progress(-1))
            client.downloadListener(SpeechDownloadEvent.Progress(150))
            client.downloadListener(SpeechDownloadEvent.Success)
        } }
        val events = mutableListOf<SpeechDownloadEvent>()
        f.service.download(SpeechLocale.VI) { events += it }.start()
        assertEquals(listOf(SpeechDownloadEvent.Progress(0), SpeechDownloadEvent.Progress(100),
            SpeechDownloadEvent.Success), events)
        assertEquals(1, f.client.closes)
    }
}
