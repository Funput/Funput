package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.model.SpeechRecording
import app.funput.funput.ime.speech.session.watchdog.SpeechWatchdog
import app.funput.funput.ime.speech.session.watchdog.SpeechDeadline

internal class SpeechOwnedSession(val id: Long, val locale: SpeechLocale, scheduler: SpeechScheduler, clock: SpeechClock) {
    val watchdog = SpeechWatchdog(scheduler, clock)
    var anchor: SpeechEditorAnchor? = null
    var recording: SpeechRecording? = null
        private set
    private var preparation: SpeechCancellation? = null
    private var closed = false

    fun updateDeadlines(effects: List<SpeechEffect>, onFinalTimeout: () -> Unit) {
        if (SpeechEffect.Ready in effects) watchdog.disarm(SpeechDeadline.READY)
        if (SpeechEffect.AwaitFinal in effects) {
            watchdog.disarm(SpeechDeadline.READY)
            watchdog.disarm(SpeechDeadline.SESSION)
            watchdog.arm(SpeechDeadline.FINAL, onFinalTimeout)
        }
    }

    fun expiredError(): SpeechError? = when {
        watchdog.expired(SpeechDeadline.ANCHOR) -> SpeechError.ANCHOR_TIMEOUT
        watchdog.expired(SpeechDeadline.FINAL) -> SpeechError.FINAL_TIMEOUT
        watchdog.expired(SpeechDeadline.READY) -> SpeechError.READY_TIMEOUT
        else -> null
    }

    fun ownPreparation(handle: SpeechCancellation) {
        if (closed || anchor != null) handle.cancel() else preparation = handle
    }

    fun anchored(value: SpeechEditorAnchor) {
        anchor = value
        val owned = preparation
        preparation = null
        owned?.cancel()
    }

    fun ownRecording(handle: SpeechRecording) {
        if (closed) {
            runCatching { handle.cancel() }
            runCatching { handle.close() }
        } else recording = handle
    }

    fun release(cancel: Boolean) {
        if (closed) return
        closed = true
        watchdog.close()
        val pending = preparation
        preparation = null
        runCatching { pending?.cancel() }
        val owned = recording
        recording = null
        if (cancel) runCatching { owned?.cancel() }
        runCatching { owned?.close() }
    }
}
