package app.funput.funput.ime.speech.platform

import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechEvent

internal interface SpeechClient {
    fun start(request: SpeechRequest)
    fun stop()
    fun cancel()
    fun destroy()
}

internal interface SpeechClientFactory {
    val apiLevel: Int
    fun isOnDeviceAvailable(): Boolean
    fun create(listener: (SpeechEvent) -> Unit): SpeechClient
}

internal fun interface SpeechMainQueue {
    fun dispatch(task: () -> Unit)
}

internal class AndroidSpeechMainQueue : SpeechMainQueue {
    private val handler = Handler(Looper.getMainLooper())

    override fun dispatch(task: () -> Unit) {
        if (Looper.myLooper() == handler.looper) task() else handler.post { task() }
    }
}

internal object SpeechResultSnapshot {
    fun partial(hypotheses: List<String>?) = SpeechEvent.Partial(firstText(hypotheses))

    fun final(hypotheses: List<String>?): SpeechEvent {
        val text = firstText(hypotheses)
        return if (text.isBlank()) SpeechEvent.Failure(SpeechRecognizer.ERROR_NO_MATCH) else SpeechEvent.Final(text)
    }

    private fun firstText(hypotheses: List<String>?) =
        hypotheses?.firstOrNull { it.isNotBlank() }.orEmpty()
}
