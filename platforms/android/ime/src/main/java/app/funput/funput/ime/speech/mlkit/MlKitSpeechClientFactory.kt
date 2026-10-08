package app.funput.funput.ime.speech.mlkit

import android.os.Build
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.platform.SpeechClient
import app.funput.funput.ime.speech.platform.SpeechClientFactory
import app.funput.funput.ime.speech.platform.SpeechRequest
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/** Basic mode is documented for API 31+; the model itself is checked by preparation. */
internal class MlKitSpeechClientFactory : SpeechClientFactory {
    override val apiLevel get() = Build.VERSION.SDK_INT
    override fun isOnDeviceAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    override fun create(listener: (SpeechEvent) -> Unit): SpeechClient = MlKitSpeechClient(listener)
}

/** Commands arrive on the main thread; responses are collected there too. */
private class MlKitSpeechClient(private val emit: (SpeechEvent) -> Unit) : SpeechClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recognizer: SpeechRecognizer? = null

    override fun start(request: SpeechRequest) {
        val owned = MlKitSpeech.recognizer(request.localeTag)
        recognizer = owned
        val mapper = MlKitResponseMapper()
        scope.launch {
            owned.startRecognition(speechRecognizerRequest { audioSource = AudioSource.fromMic() })
                .onStart { emit(SpeechEvent.Ready) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(SpeechEvent.Failure(MlKitSpeech.errorCode(error)))
                }
                .collect { response -> mapper.map(response)?.let(emit) }
        }
    }

    override fun stop() {
        val owned = recognizer ?: return
        scope.launch {
            try {
                owned.stopRecognition()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit(SpeechEvent.Failure(MlKitSpeech.errorCode(error)))
            }
        }
    }

    override fun cancel() = scope.coroutineContext.cancelChildren()

    override fun destroy() {
        scope.cancel()
        val owned = recognizer
        recognizer = null
        owned?.close()
    }
}
