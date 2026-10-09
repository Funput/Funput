package app.funput.funput.ime.speech.mlkit

import android.speech.SpeechRecognizer as Platform
import app.funput.funput.ime.speech.preparation.platform.SpeechSupportResult
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import java.util.Locale

/**
 * ML Kit GenAI speech, Basic mode only: the traditional on-device model, independent of the
 * ROM's configured on-device recognition service. Errors keep platform SpeechRecognizer codes
 * so the session reducer and flow stay engine-agnostic.
 */
internal object MlKitSpeech {
    fun recognizer(localeTag: String): SpeechRecognizer = SpeechRecognition.getClient(speechRecognizerOptions {
        locale = Locale.forLanguageTag(localeTag)
        preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
    })

    fun errorCode(error: Throwable): Int = when (error) {
        is SecurityException -> Platform.ERROR_INSUFFICIENT_PERMISSIONS
        is GenAiException -> when (error.errorCode) {
            GenAiException.ErrorCode.NOT_AVAILABLE -> Platform.ERROR_LANGUAGE_UNAVAILABLE
            GenAiException.ErrorCode.BUSY -> Platform.ERROR_RECOGNIZER_BUSY
            GenAiException.ErrorCode.REQUEST_TOO_SMALL -> Platform.ERROR_NO_MATCH
            else -> Platform.ERROR_CLIENT
        }
        else -> Platform.ERROR_CLIENT
    }

    /** Maps one locale's feature status onto the installed/pending/supported lists. */
    fun support(status: Int, localeTag: String): SpeechSupportResult {
        val tag = listOf(localeTag)
        return when (status) {
            FeatureStatus.AVAILABLE -> SpeechSupportResult.Languages(tag, emptyList(), emptyList())
            FeatureStatus.DOWNLOADING -> SpeechSupportResult.Languages(emptyList(), tag, emptyList())
            FeatureStatus.DOWNLOADABLE -> SpeechSupportResult.Languages(emptyList(), emptyList(), tag)
            FeatureStatus.UNAVAILABLE -> SpeechSupportResult.Languages(emptyList(), emptyList(), emptyList())
            else -> SpeechSupportResult.Failure(Platform.ERROR_CANNOT_CHECK_SUPPORT)
        }
    }
}
