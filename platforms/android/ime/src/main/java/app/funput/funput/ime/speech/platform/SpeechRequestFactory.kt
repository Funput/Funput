package app.funput.funput.ime.speech.platform

import android.content.Intent
import android.speech.RecognizerIntent
import app.funput.funput.ime.speech.model.SpeechLocale

internal data class SpeechRequest(
    val localeTag: String,
    val partialResults: Boolean = true,
    val preferOffline: Boolean = true,
    val maxResults: Int = 1,
)

internal object SpeechRequestFactory {
    fun create(locale: SpeechLocale) = SpeechRequest(locale.tag)

    fun toIntent(request: SpeechRequest) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, request.localeTag)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, request.partialResults)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, request.preferOffline)
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, request.maxResults)
}
