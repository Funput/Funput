package app.funput.funput.ime.speech.integration.preparation

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechPreparationOperation
import app.funput.funput.ime.speech.preparation.SpeechPreparationService
import app.funput.funput.ime.speech.session.SpeechClock
import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechScheduler

/** A tap owns a bounded capability operation. Cancellation never means cancelling a system download. */
internal class ImeSpeechPreflight(
    private val service: SpeechPreparationService,
    private val scheduler: SpeechScheduler,
    private val clock: SpeechClock,
) {
    private var version = 0L
    private var operation: SpeechPreparationOperation? = null
    private var timeout: SpeechCancellation? = null
    var active = false
        private set

    fun begin(locale: SpeechLocale, result: (SpeechCapability) -> Unit) {
        invalidate()
        val token = version
        active = true
        val deadline = clock.nowMillis() + 3_000
        fun deliver(value: SpeechCapability) {
            if (!active || token != version) return
            invalidate()
            result(if (clock.nowMillis() >= deadline) SpeechCapability.UNKNOWN else value)
        }
        try {
            val handle = service.check(locale, ::deliver)
            if (!active || token != version) {
                handle.close()
                return
            }
            operation = handle
            timeout = scheduler.schedule(3_000) { deliver(SpeechCapability.UNKNOWN) }
            if (active && token == version) handle.start()
        } catch (_: RuntimeException) {
            deliver(SpeechCapability.UNKNOWN)
        }
    }

    fun invalidate() {
        version++
        active = false
        val handle = operation
        operation = null
        val timer = timeout
        timeout = null
        timer?.cancel()
        handle?.close()
    }
}
