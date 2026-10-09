package app.funput.funput.ime.speech.preparation

import androidx.annotation.MainThread
import app.funput.funput.ime.speech.model.SpeechLocale

/** Public app facade for preparation. It cannot start a recording. */
interface SpeechPreparationService {
    fun invalidate(locale: SpeechLocale? = null)
    @MainThread
    fun availability(): SpeechAvailability
    fun check(locale: SpeechLocale, listener: (SpeechCapability) -> Unit): SpeechPreparationOperation
    fun download(locale: SpeechLocale, listener: (SpeechDownloadEvent) -> Unit): SpeechPreparationOperation
}

/** Store the handle before start; close suppresses delivery and releases its temporary client. */
interface SpeechPreparationOperation : AutoCloseable {
    fun start()
    // Closing observation of a download does not cancel a system-managed download.
    override fun close()
}
