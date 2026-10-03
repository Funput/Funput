package app.funput.funput.ime.speech.preparation

import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.platform.SpeechSupportResult
import java.util.Locale

internal object SpeechCapabilityClassifier {
    fun classify(locale: SpeechLocale, result: SpeechSupportResult): SpeechCapability = when (result) {
        is SpeechSupportResult.Failure -> when (result.code) {
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> SpeechCapability.UNSUPPORTED
            else -> SpeechCapability.UNKNOWN
        }
        is SpeechSupportResult.Languages -> when {
            result.installed.matches(locale) -> SpeechCapability.READY
            result.pending.matches(locale) -> SpeechCapability.PENDING
            result.supported.matches(locale) -> SpeechCapability.DOWNLOADABLE
            else -> SpeechCapability.UNSUPPORTED
        }
    }

    private fun List<String>.matches(locale: SpeechLocale): Boolean = any {
        Locale.forLanguageTag(it).toLanguageTag().equals(locale.tag, ignoreCase = true)
    }
}
