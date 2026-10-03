package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.*

internal class FakeSpeechPreparation : SpeechPreparationService {
    class Operation : SpeechPreparationOperation {
        var starts = 0
        var closes = 0
        var onStart: () -> Unit = {}
        override fun start() { starts++; onStart() }
        override fun close() { closes++ }
    }
    data class Check(val locale: SpeechLocale, val op: Operation, val emit: (SpeechCapability) -> Unit)
    data class Download(val locale: SpeechLocale, val op: Operation, val emit: (SpeechDownloadEvent) -> Unit)
    var available = SpeechAvailability.AVAILABLE
    var probes = 0
    val checks = mutableListOf<Check>()
    val downloads = mutableListOf<Download>()
    val invalidated = mutableListOf<SpeechLocale?>()
    var synchronousCheck: SpeechCapability? = null
    override fun availability(): SpeechAvailability { probes++; return available }
    override fun invalidate(locale: SpeechLocale?) { invalidated += locale }
    override fun check(locale: SpeechLocale, listener: (SpeechCapability) -> Unit): SpeechPreparationOperation {
        val op = Operation().apply { onStart = { synchronousCheck?.let(listener) } }
        return op.also { checks += Check(locale, it, listener) }
    }
    override fun download(locale: SpeechLocale, listener: (SpeechDownloadEvent) -> Unit): SpeechPreparationOperation =
        Operation().also { downloads += Download(locale, it, listener) }
}
