package app.funput.funput.ime.speech.integration.presentation

import android.content.Context
import app.funput.funput.ime.R
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.speech.SpeechPanelState
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState

internal class AndroidSpeechUiPort(private val context: Context) : SpeechUiPort {
    private var view: FunputKeyboardView? = null
    var presenting = false
        private set

    fun attach(value: FunputKeyboardView) {
        dismiss()
        view?.microphone = KeyboardMicrophoneState()
        view = value
    }

    fun isCurrent(value: FunputKeyboardView) = view === value

    override fun present(state: SpeechPanelState) = presentation {
        view?.speechPanelState = state
        view?.showSpeechPanel()
    }

    override fun dismiss() = presentation {
        if (view?.activePanel == KeyboardPanel.SPEECH) view?.showLettersPanel()
    }

    fun detach() {
        dismiss()
        view?.microphone = KeyboardMicrophoneState()
        view = null
    }

    override fun microphone(visible: Boolean, active: Boolean, language: KeyboardLanguage) {
        val label = if (language == KeyboardLanguage.VIETNAMESE) R.string.speech_mic_vi else R.string.speech_mic_en
        view?.microphone = KeyboardMicrophoneState(visible, active, context.getString(label))
    }

    override fun message(notice: SpeechNotice): String = context.getString(when (notice) {
        SpeechNotice.PERMISSION -> R.string.speech_permission_required
        SpeechNotice.PENDING -> R.string.speech_spike_pending
        SpeechNotice.DOWNLOAD -> R.string.speech_spike_downloadable
        SpeechNotice.UNSUPPORTED -> R.string.speech_spike_unsupported_locale
        SpeechNotice.SETUP -> R.string.speech_setup_unavailable
        SpeechNotice.EDITOR -> R.string.speech_editor_changed
        SpeechNotice.TIMEOUT -> R.string.speech_timeout
        SpeechNotice.EMPTY -> R.string.speech_empty
        SpeechNotice.LENGTH -> R.string.speech_length
        SpeechNotice.BACKEND -> R.string.speech_backend_error
    })

    private inline fun presentation(action: () -> Unit) {
        val previous = presenting
        presenting = true
        try { action() } finally { presenting = previous }
    }
}
