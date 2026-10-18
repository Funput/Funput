package app.funput.funput.ui.settings.speech.model

import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.preparation.SpeechPreparationOperation
import app.funput.funput.ime.speech.preparation.SpeechPreparationService

/**
 * Owns check/download observers per locale, independently of app permission and recording.
 * A locale that is downloading is checked again on its own while the screen is visible, so the
 * row turns ready without a manual refresh.
 */
internal class SpeechLocaleOperations(
    private val service: SpeechPreparationService,
    private val scheduler: SpeechRecheckScheduler,
    private val consentNeeded: (SpeechLocale) -> Unit = {},
    private val update: (SpeechLocale, SpeechLocaleState) -> Unit,
) {
    private val handles = mutableMapOf<SpeechLocale, SpeechPreparationOperation>()
    private val versions = mutableMapOf<SpeechLocale, Long>()
    private val rechecks = mutableMapOf<SpeechLocale, AutoCloseable>()
    private val attempts = mutableMapOf<SpeechLocale, Int>()
    /** Checks left for a pack the user confirmed; survives pause, so the dialog round trip keeps it. */
    private val installing = mutableMapOf<SpeechLocale, Int>()
    private var sequence = 0L
    private var active = false

    fun resume() {
        active = true
        SpeechLocale.entries.forEach(::check)
    }

    fun stop() {
        active = false
        versions.clear()
        attempts.clear()
        val owned = handles.values.toList()
        handles.clear()
        owned.forEach { it.close() }
        rechecks.values.toList().forEach { it.close() }
        rechecks.clear()
    }

    /** A user-initiated check starts a fresh budget of automatic re-checks. */
    fun check(locale: SpeechLocale) {
        attempts.remove(locale)
        runCheck(locale)
    }

    private fun runCheck(locale: SpeechLocale) {
        if (!active) return
        val version = replace(locale)
        service.invalidate(locale)
        update(locale, SpeechLocaleState(checking = true))
        val operation = service.check(locale) { capability ->
            if (active && versions[locale] == version) {
                handles.remove(locale)?.close()
                if (stillInstalling(locale, capability)) {
                    update(locale, SpeechLocaleState(capability = SpeechCapability.PENDING, installing = true))
                    scheduleRecheck(locale, force = true)
                } else {
                    update(locale, SpeechLocaleState(capability = capability))
                    if (capability == SpeechCapability.PENDING) scheduleRecheck(locale)
                }
            }
        }
        handles[locale] = operation
        operation.start()
    }

    fun download(locale: SpeechLocale) {
        if (!active) return
        attempts.remove(locale)
        val version = replace(locale)
        update(locale, SpeechLocaleState(downloading = true))
        val operation = service.download(locale) { event ->
            if (active && versions[locale] == version) {
                val progress = event is SpeechDownloadEvent.Progress
                if (!progress) {
                    // A terminal event ends this observer: nothing after it (a repeated Scheduled
                    // included) may act again, so retire its version as well as its handle.
                    handles.remove(locale)?.close()
                    versions.remove(locale)
                }
                val scheduled = event == SpeechDownloadEvent.Scheduled
                update(locale, SpeechLocaleState(
                    capability = if (scheduled) SpeechCapability.PENDING else SpeechCapability.UNKNOWN,
                    downloading = progress,
                    download = event,
                ))
                if (scheduled || event == SpeechDownloadEvent.Success) installing[locale] = INSTALL_CHECKS
                when {
                    event == SpeechDownloadEvent.Success -> runCheck(locale)
                    // Only reached while the screen stayed resumed: had Speech Services shown its
                    // own dialog, the screen would have paused and closed this observer.
                    scheduled -> {
                        consentNeeded(locale)
                        scheduleRecheck(locale)
                    }
                }
            }
        }
        handles[locale] = operation
        operation.start()
    }

    /**
     * Speech Services finishes a download minutes before it reports the pack installed, and says
     * DOWNLOADABLE meanwhile. Until its budget runs out, that answer means "still installing" for
     * a pack the user just asked for, instead of offering the download again.
     */
    private fun stillInstalling(locale: SpeechLocale, capability: SpeechCapability): Boolean {
        val left = installing[locale] ?: return false
        val waiting = capability == SpeechCapability.DOWNLOADABLE || capability == SpeechCapability.PENDING
        if (!waiting || left <= 0) {
            installing.remove(locale)
            return false
        }
        installing[locale] = left - 1
        return true
    }

    private fun scheduleRecheck(locale: SpeechLocale, force: Boolean = false) {
        val attempt = (attempts[locale] ?: 0) + 1
        if (attempt > MAX_RECHECKS && !force) return
        attempts[locale] = attempt
        rechecks.remove(locale)?.close()
        rechecks[locale] = scheduler.schedule(RECHECK_MILLIS) {
            rechecks.remove(locale)
            if (active) runCheck(locale)
        }
    }

    private fun replace(locale: SpeechLocale): Long {
        val version = ++sequence
        versions[locale] = version
        handles.remove(locale)?.close()
        rechecks.remove(locale)?.close()
        return version
    }

    internal companion object {
        const val RECHECK_MILLIS = 3_000L
        /** About three minutes of polling; a pack that is still waiting keeps its pending row. */
        const val MAX_RECHECKS = 60
        /** About five minutes: on a Xiaomi Pad an English pack took 3 min 14 s to be reported. */
        const val INSTALL_CHECKS = 100
    }
}
