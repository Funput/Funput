package app.funput.funput.keyboard.ui.speech

import app.funput.funput.keyboard.model.KeyboardLanguage

/** Immutable presentation; this module has no dependency on speech/session implementations. */
data class SpeechPanelState(
    val stage: SpeechPanelStage = SpeechPanelStage.PREPARING,
    val language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE,
    val preview: String = "",
    val message: String? = null,
    val canRetry: Boolean = false,
    val canOpenSetup: Boolean = false,
)

enum class SpeechPanelStage { PREPARING, LISTENING, FINALIZING, ERROR }

enum class SpeechPanelAction { STOP, CANCEL, RETRY, OPEN_SETUP }
