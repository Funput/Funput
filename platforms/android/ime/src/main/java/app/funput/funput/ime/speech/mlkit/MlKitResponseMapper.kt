package app.funput.funput.ime.speech.mlkit

import android.speech.SpeechRecognizer as Platform
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.platform.SpeechResultSnapshot
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse

/** A completed stream without any final hypothesis is a no-match, not a silent hang. */
internal class MlKitResponseMapper {
    private var finalSeen = false

    fun map(response: SpeechRecognizerResponse): SpeechEvent? = when (response) {
        is SpeechRecognizerResponse.PartialTextResponse -> SpeechResultSnapshot.partial(listOf(response.text))
        is SpeechRecognizerResponse.FinalTextResponse -> {
            finalSeen = true
            SpeechResultSnapshot.final(listOf(response.text))
        }
        is SpeechRecognizerResponse.CompletedResponse ->
            if (finalSeen) null else SpeechEvent.Failure(Platform.ERROR_NO_MATCH)
        is SpeechRecognizerResponse.ErrorResponse -> SpeechEvent.Failure(MlKitSpeech.errorCode(response.e))
    }
}
