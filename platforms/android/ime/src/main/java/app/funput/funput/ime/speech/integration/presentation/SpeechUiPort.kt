package app.funput.funput.ime.speech.integration.presentation

import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState

internal enum class SpeechNotice { PERMISSION, PENDING, DOWNLOAD, UNSUPPORTED, SETUP, EDITOR, TIMEOUT, EMPTY, LENGTH, BACKEND }

internal interface SpeechUiPort {
    fun present(state: SpeechPanelState)
    fun dismiss()
    fun microphone(visible: Boolean, active: Boolean, language: KeyboardLanguage)
    fun message(notice: SpeechNotice): String
}
