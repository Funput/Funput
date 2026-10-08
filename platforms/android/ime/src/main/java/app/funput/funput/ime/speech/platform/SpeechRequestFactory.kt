package app.funput.funput.ime.speech.platform

import app.funput.funput.ime.speech.model.SpeechLocale

internal data class SpeechRequest(val localeTag: String)

internal object SpeechRequestFactory {
    fun create(locale: SpeechLocale) = SpeechRequest(locale.tag)
}
