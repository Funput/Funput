package app.funput.funput.ime.speech.mlkit

import android.os.Build
import androidx.annotation.RequiresApi
import android.speech.SpeechRecognizer as Platform
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/** Basic mode is documented for API 31+; the model itself is checked by preparation. */
internal class MlKitSpeechClientFactory : SpeechClientFactory {
    override val apiLevel get() = Build.VERSION.SDK_INT
    override fun isOnDeviceAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    override fun create(listener: (SpeechEvent) -> Unit): SpeechClient {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw UnsupportedOperationException()
        return MlKitSpeechClient(listener)
    }
}

/** Commands arrive on the main thread; responses are collected there too. */
@RequiresApi(Build.VERSION_CODES.S)
private class MlKitSpeechClient(private val emit: (SpeechEvent) -> Unit) : SpeechClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recognizer: SpeechRecognizer? = null
    private var held = ""
    private var holdJob: Job? = null

    override fun start(request: SpeechRequest) {
        val owned = MlKitSpeech.recognizer(request.localeTag)
        recognizer = owned
        val mapper = MlKitResponseMapper()
        scope.launch {
            owned.startRecognition(speechRecognizerRequest { audioSource = AudioSource.fromMic() })
                .onStart { emit(SpeechEvent.Ready) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    cancelHold()
                    emit(SpeechEvent.Failure(MlKitSpeech.errorCode(error)))
                }
                .collect { response -> mapper.map(response)?.let(::receive) }
            // Stream completed (e.g. after stop): deliver any held text immediately.
            flushHeld()
        }
    }

    /**
     * Holds a final segment for [SILENCE_GRACE_MILLIS] so the user can pause mid-sentence and
     * keep talking; new speech within the window extends the same utterance.
     */
    private fun receive(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.Partial -> {
                cancelHold()
                emit(SpeechEvent.Partial(join(held, event.text)))
            }
            is SpeechEvent.Final -> {
                cancelHold()
                held = join(held, event.text)
                holdJob = scope.launch {
                    delay(SILENCE_GRACE_MILLIS)
                    flushHeld()
                }
            }
            is SpeechEvent.Failure -> {
                cancelHold()
                if (held.isNotBlank() && event.code == Platform.ERROR_NO_MATCH) flushHeld() else emit(event)
            }
            else -> emit(event)
        }
    }

    private fun flushHeld() {
        cancelHold()
        val text = held
        held = ""
        if (text.isNotBlank()) emit(SpeechEvent.Final(text))
    }

    private fun cancelHold() {
        val job = holdJob
        holdJob = null
        // Safe even from inside the job: emit() is non-suspending, so it still runs to completion.
        job?.cancel()
    }

    private fun join(prefix: String, text: String) =
        if (prefix.isBlank()) text.trim() else if (text.isBlank()) prefix else "$prefix ${text.trim()}"

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

    private companion object {
        /** Extra silence tolerated after a final segment before the text is sent. */
        const val SILENCE_GRACE_MILLIS = 2_500L
    }

    override fun destroy() {
        scope.cancel()
        val owned = recognizer
        recognizer = null
        owned?.close()
    }
}
