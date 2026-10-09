package app.funput.funput.ime.speech.mlkit

import android.os.Build
import app.funput.funput.ime.speech.platform.SpeechRequest
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import app.funput.funput.ime.speech.preparation.platform.PreparationClient
import app.funput.funput.ime.speech.preparation.platform.PreparationClientFactory
import app.funput.funput.ime.speech.preparation.platform.SpeechSupportResult
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class MlKitPreparationClientFactory : PreparationClientFactory {
    override val apiLevel: Int get() = Build.VERSION.SDK_INT
    override fun isOnDeviceAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    override fun create(): PreparationClient = MlKitPreparationClient()
}

/** No microphone commands. Closing stops observation; ML Kit owns the download itself. */
private class MlKitPreparationClient : PreparationClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val owned = mutableListOf<SpeechRecognizer>()

    override fun check(request: SpeechRequest, listener: (SpeechSupportResult) -> Unit) {
        val recognizer = open(request)
        scope.launch {
            val result = try {
                MlKitSpeech.support(recognizer.checkStatus(), request.localeTag)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                SpeechSupportResult.Failure(MlKitSpeech.errorCode(error))
            }
            listener(result)
        }
    }

    override fun trackDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit) {
        val recognizer = open(request)
        val mapper = MlKitDownloadMapper()
        scope.launch {
            recognizer.download()
                .catch { error ->
                    if (error is CancellationException) throw error
                    listener(SpeechDownloadEvent.Failure(MlKitSpeech.errorCode(error)))
                }
                .collect { status -> mapper.map(status)?.let(listener) }
        }
    }

    override fun close() {
        scope.cancel()
        owned.forEach { runCatching { it.close() } }
        owned.clear()
    }

    private fun open(request: SpeechRequest) = MlKitSpeech.recognizer(request.localeTag).also { owned += it }
}
