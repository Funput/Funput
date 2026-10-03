package app.funput.funput.ime.speech.preparation.operations

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapabilityCache
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.preparation.platform.PreparationClient
import app.funput.funput.ime.speech.preparation.platform.PreparationClientFactory
import app.funput.funput.ime.speech.preparation.platform.PreparationTime

internal class DownloadSpeechOperation(
    factory: PreparationClientFactory,
    main: SpeechMainQueue,
    time: PreparationTime,
    private val locale: SpeechLocale,
    private val cache: SpeechCapabilityCache,
    private val availability: () -> SpeechAvailability,
    listener: (SpeechDownloadEvent) -> Unit,
) : PreparationLease<SpeechDownloadEvent>(factory, main, time, listener) {
    private var legacyRequested = false
    private val request = SpeechRequestFactory.create(locale)

    override fun startOnMain() {
        if (factory.apiLevel < 33 || availability() != SpeechAvailability.AVAILABLE) {
            return deliver(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT))
        }
        cache.invalidate(locale)
        acquire(60_000, { SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK_TIMEOUT) }) { client ->
            if (factory.apiLevel < 34) requestLegacy(client)
            else client.trackDownload(request) { event -> receive {
                if (legacyRequested) return@receive
                if (event is SpeechDownloadEvent.Failure &&
                    event.code == SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS && !legacyRequested) {
                    runCatching { requestLegacy(client) }.onFailure {
                        deliver(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT))
                    }
                } else {
                    if (event == SpeechDownloadEvent.Success) cache.invalidate(locale)
                    val safeEvent = if (event is SpeechDownloadEvent.Progress) {
                        event.copy(percent = event.percent.coerceIn(0, 100))
                    } else event
                    deliver(safeEvent, terminal = event !is SpeechDownloadEvent.Progress)
                }
            } }
        }
    }

    private fun requestLegacy(client: PreparationClient) {
        legacyRequested = true
        client.requestDownload(request) { event -> receive { deliver(event) } }
    }
}
