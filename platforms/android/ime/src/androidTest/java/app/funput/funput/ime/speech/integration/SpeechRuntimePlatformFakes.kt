package app.funput.funput.ime.speech.integration

import android.content.Context
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.speech.model.*
import app.funput.funput.ime.speech.preparation.*

/** No recognizer, model download, or actual permission mutation in these integration tests. */
internal class SpeechTestService(context: Context, var connection: InputConnection) : InputMethodService() {
    var shown = true
    var microphoneGranted = true
    init { attachBaseContext(context) }
    override fun getCurrentInputConnection() = connection
    override fun isInputViewShown() = shown
    override fun checkSelfPermission(permission: String) =
        if (microphoneGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
}

internal class SpeechTestBackend : SpeechBackend {
    val recordings = mutableListOf<SpeechTestRecording>()
    override fun open(locale: SpeechLocale, listener: (SpeechEvent) -> Unit): SpeechRecording =
        SpeechTestRecording(listener).also { recordings += it }
}

internal class SpeechTestRecording(private val listener: (SpeechEvent) -> Unit) : SpeechRecording {
    var starts = 0
    var stops = 0
    var cancels = 0
    var closes = 0
    fun emit(event: SpeechEvent) = listener(event)
    override fun start() { starts++; emit(SpeechEvent.Ready) }
    override fun stop() { stops++ }
    override fun cancel() { cancels++ }
    override fun close() { closes++ }
}

internal class SpeechTestPreparation : SpeechPreparationService {
    var checks = 0
    var invalidations = 0
    override fun availability() = SpeechAvailability.AVAILABLE
    override fun invalidate(locale: SpeechLocale?) { invalidations++ }
    override fun check(locale: SpeechLocale, listener: (SpeechCapability) -> Unit): SpeechPreparationOperation {
        checks++
        return object : SpeechPreparationOperation {
            override fun start() = listener(SpeechCapability.READY)
            override fun close() = Unit
        }
    }
    override fun download(locale: SpeechLocale, listener: (SpeechDownloadEvent) -> Unit): SpeechPreparationOperation =
        error("IME must not download")
}
