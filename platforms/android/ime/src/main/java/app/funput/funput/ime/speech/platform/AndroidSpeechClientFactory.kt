package app.funput.funput.ime.speech.platform

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechEvent

internal class AndroidSpeechClientFactory(context: Context) : SpeechClientFactory {
    private val appContext = context.applicationContext
    override val apiLevel get() = Build.VERSION.SDK_INT

    override fun isOnDeviceAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

    override fun create(listener: (SpeechEvent) -> Unit): SpeechClient {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw UnsupportedOperationException()
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        try {
            recognizer.setRecognitionListener(AndroidSpeechListener(listener))
        } catch (error: Exception) {
            runCatching { recognizer.destroy() }
            throw error
        }
        return AndroidSpeechClient(recognizer)
    }
}

private class AndroidSpeechClient(private val recognizer: SpeechRecognizer) : SpeechClient {
    override fun start(request: SpeechRequest) = recognizer.startListening(SpeechRequestFactory.toIntent(request))
    override fun stop() = recognizer.stopListening()
    override fun cancel() = recognizer.cancel()
    override fun destroy() = recognizer.destroy()
}

private class AndroidSpeechListener(private val emit: (SpeechEvent) -> Unit) : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) = emit(SpeechEvent.Ready)
    override fun onError(error: Int) = emit(SpeechEvent.Failure(error))
    override fun onPartialResults(partialResults: Bundle?) = readResults(partialResults, final = false)
    override fun onResults(results: Bundle?) = readResults(results, final = true)
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = emit(SpeechEvent.Ended)
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun readResults(results: Bundle?, final: Boolean) {
        val event = try {
            val hypotheses = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (final) SpeechResultSnapshot.final(hypotheses) else SpeechResultSnapshot.partial(hypotheses)
        } catch (_: Exception) {
            SpeechEvent.Failure(SpeechRecognizer.ERROR_CLIENT)
        }
        emit(event)
    }
}
