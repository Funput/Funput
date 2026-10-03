package app.funput.funput.ime.speech.integration.preparation

import android.content.Context
import app.funput.funput.ime.R
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.OnDeviceSpeechPreparationService
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechPreparationOperation

/** One bounded preflight per tap; showing the view probes service presence without a client. */
internal class ImeSpeechPreparationBinding(context: Context, private val message: (Int) -> Unit) {
    private val service = OnDeviceSpeechPreparationService(context)
    private var operation: SpeechPreparationOperation? = null
    private var version = 0L
    private var preparing = false

    fun show() {
        if (service.availability() != SpeechAvailability.AVAILABLE) message(R.string.speech_spike_unavailable)
    }

    fun begin(locale: SpeechLocale, ready: () -> Unit) {
        if (preparing) return
        preparing = true
        val token = ++version
        message(R.string.speech_spike_checking)
        operation = service.check(locale) { capability ->
            if (version == token) {
                operation?.close()
                operation = null
                preparing = false
                when (capability) {
                    SpeechCapability.READY, SpeechCapability.UNKNOWN -> ready()
                    SpeechCapability.PENDING -> message(R.string.speech_spike_pending)
                    SpeechCapability.DOWNLOADABLE -> message(R.string.speech_spike_downloadable)
                    SpeechCapability.UNSUPPORTED -> message(R.string.speech_spike_unsupported_locale)
                }
            }
        }
        operation?.start()
    }

    fun stop(): Boolean {
        if (!preparing) return false
        invalidate()
        return true
    }

    fun recordingError(code: Int?) {
        if (code == 12 || code == 13) service.invalidate()
    }

    fun invalidate() {
        ++version
        preparing = false
        val owned = operation
        operation = null
        owned?.close()
    }
}
