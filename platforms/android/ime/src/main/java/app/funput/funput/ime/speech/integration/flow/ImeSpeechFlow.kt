package app.funput.funput.ime.speech.integration.flow

import app.funput.funput.ime.speech.integration.ImeSpeechUiBinder
import app.funput.funput.ime.speech.integration.preparation.ImeSpeechPreflight
import app.funput.funput.ime.speech.integration.presentation.SpeechNotice
import app.funput.funput.ime.speech.model.SpeechBackend
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.ime.speech.preparation.SpeechPreparationService
import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechClock
import app.funput.funput.ime.speech.session.SpeechEditorGateway
import app.funput.funput.ime.speech.session.SpeechPhase
import app.funput.funput.ime.speech.session.SpeechScheduler
import app.funput.funput.ime.speech.session.SpeechSessionController
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction

/** Serialized by the owner; framework commands and UI are injected at the boundary. */
internal class ImeSpeechFlow(
    backend: SpeechBackend,
    editor: SpeechEditorGateway,
    private val preparation: SpeechPreparationService,
    private val scheduler: SpeechScheduler,
    clock: SpeechClock,
    private val ui: ImeSpeechUiBinder,
    private val eligible: () -> Boolean,
    private val permitted: () -> Boolean,
    private val language: () -> KeyboardLanguage,
    private val invalidateEditor: (Boolean) -> Unit,
    private val setup: (SpeechLocale) -> Unit,
) {
    private val preflight = ImeSpeechPreflight(preparation, scheduler, clock)
    private val controller = SpeechSessionController(backend, editor, scheduler, clock) {
        if (it.backendErrorCode in listOf(12, 13)) preparation.invalidate()
        ui.render(it)
        if (it.phase in listOf(SpeechPhase.IDLE, SpeechPhase.ERROR)) stopSafety()
        refresh()
    }
    private var closed = false
    private var epoch = 0L
    private var safety: SpeechCancellation? = null
    val committing get() = controller.state.phase == SpeechPhase.COMMITTING

    fun refresh() {
        ui.microphone(!closed && eligible(), ui.engaged, language())
    }

    fun start() {
        if (closed || !eligible() || preflight.active || controller.state.phase !in
            listOf(SpeechPhase.IDLE, SpeechPhase.ERROR)) return
        cancel(false)
        val locale = if (language() == KeyboardLanguage.VIETNAMESE) SpeechLocale.VI else SpeechLocale.EN
        val token = epoch
        ui.open(language())
        refresh()
        if (closed || token != epoch || !ui.engaged || !eligible()) return
        if (!permitted()) {
            ui.error(SpeechNotice.PERMISSION, setup = true, retry = false)
            return
        }
        watch(token)
        preflight.begin(locale) { capability ->
            if (!closed && token == epoch && ui.engaged && eligible() && permitted()) {
                when (capability) {
                    SpeechCapability.READY, SpeechCapability.UNKNOWN -> controller.start(locale)
                    SpeechCapability.PENDING -> {
                        stopSafety()
                        ui.error(SpeechNotice.PENDING, setup = true)
                    }
                    SpeechCapability.DOWNLOADABLE -> {
                        stopSafety()
                        ui.error(SpeechNotice.DOWNLOAD, setup = true)
                    }
                    SpeechCapability.UNSUPPORTED -> {
                        stopSafety()
                        ui.error(SpeechNotice.UNSUPPORTED, retry = false)
                    }
                }
            } else if (token == epoch) cancel(false)
        }
    }

    fun action(action: SpeechPanelAction) {
        if (closed || !ui.engaged) return
        when (action) {
            SpeechPanelAction.CANCEL -> cancel(false)
            SpeechPanelAction.STOP -> if (preflight.active) cancel(false) else controller.stop()
            SpeechPanelAction.RETRY -> start()
            SpeechPanelAction.OPEN_SETUP -> {
                val locale = if (language() == KeyboardLanguage.VIETNAMESE) SpeechLocale.VI else SpeechLocale.EN
                cancel(false)
                preparation.invalidate(locale)
                setup(locale)
            }
        }
    }

    fun cancel(expectSelection: Boolean = false) {
        epoch++
        // Invalidate ownership before any presentation or client cleanup can re-enter.
        ui.invalidate()
        stopSafety()
        preflight.invalidate()
        invalidateEditor(expectSelection)
        controller.cancel()
        ui.dismiss()
        refresh()
    }

    fun back(): Boolean {
        if (!ui.engaged) return false
        cancel(false)
        return true
    }

    fun setupFailed() {
        if (closed || !eligible()) return
        ui.open(language())
        ui.error(SpeechNotice.SETUP, retry = false)
    }

    private fun watch(token: Long) {
        safety = scheduler.schedule(250) {
            if (closed || token != epoch || !ui.engaged) return@schedule
            safety = null
            if (!eligible() || !permitted()) cancel(false) else watch(token)
        }
    }

    private fun stopSafety() {
        safety?.cancel()
        safety = null
    }

    fun close() {
        if (closed) return
        closed = true
        cancel(false)
        controller.close()
    }
}
