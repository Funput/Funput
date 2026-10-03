package app.funput.funput.ime.speech.platform

import android.content.Context
import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.model.SpeechBackend
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.model.SpeechRecording

internal class PlatformSpeechBackend(
    private val factory: SpeechClientFactory,
    private val main: SpeechMainQueue,
) : SpeechBackend {
    constructor(context: Context) : this(AndroidSpeechClientFactory(context), AndroidSpeechMainQueue())

    override fun open(locale: SpeechLocale, listener: (SpeechEvent) -> Unit): SpeechRecording =
        PlatformSpeechRecording(factory, main, SpeechRequestFactory.create(locale), listener)
}

private class PlatformSpeechRecording(
    private val factory: SpeechClientFactory,
    private val main: SpeechMainQueue,
    private val request: SpeechRequest,
    private val listener: (SpeechEvent) -> Unit,
) : SpeechRecording {
    private var client: SpeechClient? = null
    private var startRequested = false
    private var started = false
    private var stopped = false
    private var closed = false

    override fun start() = main.dispatch {
        if (closed || startRequested) return@dispatch
        startRequested = true
        try {
            if (factory.apiLevel < 31 || !factory.isOnDeviceAvailable()) {
                fail(SpeechRecognizer.ERROR_CLIENT)
                return@dispatch
            }
            client = factory.create { event -> main.dispatch { receive(event) } }
            started = true
            client?.start(request)
        } catch (error: Exception) {
            fail(if (error is SecurityException) SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                else SpeechRecognizer.ERROR_CLIENT)
        }
    }

    override fun stop() = main.dispatch {
        if (closed || !started || stopped) return@dispatch
        stopped = true
        try {
            client?.stop()
        } catch (error: Exception) {
            fail(if (error is SecurityException) SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                else SpeechRecognizer.ERROR_CLIENT)
        }
    }

    override fun cancel() = main.dispatch { release(cancel = true) }
    override fun close() = main.dispatch { release(cancel = true) }

    private fun receive(event: SpeechEvent) {
        if (closed || !started) return
        if (event is SpeechEvent.Final || event is SpeechEvent.Failure) release(cancel = false)
        listener(event)
    }

    private fun fail(code: Int) {
        if (closed) return
        release(cancel = true)
        listener(SpeechEvent.Failure(code))
    }

    private fun release(cancel: Boolean) {
        if (closed) return
        closed = true
        val owned = client
        client = null
        if (cancel) runCatching { owned?.cancel() }
        runCatching { owned?.destroy() }
    }
}
