package app.funput.funput.ime.speech.preparation.operations

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapabilityCache
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
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
    private val request = SpeechRequestFactory.create(locale)

    override fun startOnMain() {
        if (availability() != SpeechAvailability.AVAILABLE) {
            return deliver(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT))
        }
        cache.invalidate(locale)
        acquire(DEADLINE_MILLIS, { SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_NETWORK_TIMEOUT) }) { client ->
            client.trackDownload(request) { event -> receive {
                if (event == SpeechDownloadEvent.Success) cache.invalidate(locale)
                val safeEvent = if (event is SpeechDownloadEvent.Progress) {
                    event.copy(percent = event.percent.coerceIn(0, 100))
                } else event
                deliver(safeEvent, terminal = event !is SpeechDownloadEvent.Progress)
            } }
        }
    }

    private companion object {
        // Model packs are downloaded by the app process; allow slow networks before giving up.
        const val DEADLINE_MILLIS = 600_000L
    }
}
