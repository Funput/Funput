package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.preparation.SpeechPreparationOperation
import app.funput.funput.ime.speech.preparation.SpeechPreparationService

/** Owns check/download observers per locale, independently of app permission and recording. */
internal class SpeechLocaleOperations(
    private val service: SpeechPreparationService,
    private val update: (SpeechLocale, SpeechLocaleState) -> Unit,
) {
    private val handles = mutableMapOf<SpeechLocale, SpeechPreparationOperation>()
    private val versions = mutableMapOf<SpeechLocale, Long>()
    private var sequence = 0L
    private var active = false

    fun resume() {
        active = true
        SpeechLocale.entries.forEach(::check)
    }

    fun stop() {
        active = false
        versions.clear()
        val owned = handles.values.toList()
        handles.clear()
        owned.forEach { it.close() }
    }

    fun check(locale: SpeechLocale) {
        if (!active) return
        val version = replace(locale)
        service.invalidate(locale)
        update(locale, SpeechLocaleState(checking = true))
        val operation = service.check(locale) { capability ->
            if (active && versions[locale] == version) {
                handles.remove(locale)?.close()
                update(locale, SpeechLocaleState(capability = capability))
            }
        }
        handles[locale] = operation
        operation.start()
    }

    fun download(locale: SpeechLocale) {
        if (!active) return
        val version = replace(locale)
        update(locale, SpeechLocaleState(downloading = true))
        val operation = service.download(locale) { event ->
            if (active && versions[locale] == version) {
                val progress = event is SpeechDownloadEvent.Progress
                if (!progress) handles.remove(locale)?.close()
                update(locale, SpeechLocaleState(
                    capability = if (event == SpeechDownloadEvent.Scheduled) SpeechCapability.PENDING
                        else SpeechCapability.UNKNOWN,
                    downloading = progress,
                    download = event,
                ))
                if (event == SpeechDownloadEvent.Success) check(locale)
            }
        }
        handles[locale] = operation
        operation.start()
    }

    private fun replace(locale: SpeechLocale): Long {
        val version = ++sequence
        versions[locale] = version
        handles.remove(locale)?.close()
        return version
    }
}
