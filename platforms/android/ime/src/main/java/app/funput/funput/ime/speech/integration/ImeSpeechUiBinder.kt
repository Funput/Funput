package app.funput.funput.ime.speech.integration

import app.funput.funput.ime.speech.integration.presentation.SpeechNotice
import app.funput.funput.ime.speech.integration.presentation.SpeechUiPort
import app.funput.funput.ime.speech.session.SpeechError
import app.funput.funput.ime.speech.session.SpeechPhase
import app.funput.funput.ime.speech.session.SpeechSessionState
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState

/** Late results may update no UI after the owner dismisses the panel. */
internal class ImeSpeechUiBinder(private val port: SpeechUiPort) {
    var engaged = false
        private set
    private var language = KeyboardLanguage.VIETNAMESE

    fun open(locale: KeyboardLanguage) {
        language = locale
        engaged = true
        port.present(SpeechPanelState(language = language))
    }

    fun invalidate() { engaged = false }

    fun dismiss() {
        engaged = false
        port.dismiss()
    }

    fun microphone(visible: Boolean, active: Boolean, locale: KeyboardLanguage) =
        port.microphone(visible, active, locale)

    fun error(notice: SpeechNotice, setup: Boolean = false, retry: Boolean = true) {
        if (!engaged) return
        port.present(SpeechPanelState(SpeechPanelStage.ERROR, language,
            message = port.message(notice), canRetry = retry, canOpenSetup = setup))
    }

    fun render(state: SpeechSessionState) {
        if (!engaged) return
        when (state.phase) {
            SpeechPhase.IDLE -> dismiss()
            SpeechPhase.ERROR -> {
                val setup = state.backendErrorCode in listOf(9, 12, 13)
                error(if (state.backendErrorCode == 9) SpeechNotice.PERMISSION else notice(state.error), setup)
            }
            else -> port.present(SpeechPanelState(stage = when (state.phase) {
                SpeechPhase.PREPARING -> SpeechPanelStage.PREPARING
                SpeechPhase.LISTENING -> SpeechPanelStage.LISTENING
                else -> SpeechPanelStage.FINALIZING
            }, language = language, preview = state.preview))
        }
    }

    private fun notice(error: SpeechError?): SpeechNotice = when (error) {
        SpeechError.EDITOR_UNAVAILABLE, SpeechError.COMMIT_REJECTED -> SpeechNotice.EDITOR
        SpeechError.ANCHOR_TIMEOUT, SpeechError.READY_TIMEOUT, SpeechError.FINAL_TIMEOUT -> SpeechNotice.TIMEOUT
        SpeechError.EMPTY_FINAL -> SpeechNotice.EMPTY
        SpeechError.TEXT_TOO_LONG -> SpeechNotice.LENGTH
        else -> SpeechNotice.BACKEND
    }
}
