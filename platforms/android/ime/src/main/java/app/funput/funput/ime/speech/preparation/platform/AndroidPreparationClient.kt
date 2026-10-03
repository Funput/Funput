package app.funput.funput.ime.speech.preparation.platform

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.ModelDownloadListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.SpeechRecognizer
import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.platform.SpeechRequest
import app.funput.funput.ime.speech.platform.SpeechRequestFactory
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import java.util.concurrent.Executor

internal class AndroidPreparationClientFactory(
    context: Context,
    private val main: SpeechMainQueue,
) : PreparationClientFactory {
    private val appContext = context.applicationContext
    override val apiLevel: Int get() = Build.VERSION.SDK_INT
    override fun isOnDeviceAvailable(): Boolean = Build.VERSION.SDK_INT >= 31 &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

    override fun create(): PreparationClient {
        if (Build.VERSION.SDK_INT < 31) throw UnsupportedOperationException()
        return AndroidPreparationClient(SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext), main)
    }
}

private class AndroidPreparationClient(
    private val recognizer: SpeechRecognizer,
    main: SpeechMainQueue,
) : PreparationClient {
    private var closed = false
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executor { command -> main.dispatch(command::run) }

    override fun check(request: SpeechRequest, listener: (SpeechSupportResult) -> Unit) {
        if (Build.VERSION.SDK_INT < 33) throw UnsupportedOperationException()
        recognizer.checkRecognitionSupport(SpeechRequestFactory.toIntent(request), executor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    if (closed) return
                    val snapshot = runCatching {
                        SpeechSupportResult.Languages(support.installedOnDeviceLanguages.toList(),
                            support.pendingOnDeviceLanguages.toList(), support.supportedOnDeviceLanguages.toList())
                    }.getOrElse { SpeechSupportResult.Failure(SpeechRecognizer.ERROR_CLIENT) }
                    listener(snapshot)
                }
                override fun onError(error: Int) {
                    if (!closed) listener(SpeechSupportResult.Failure(error))
                }
            })
    }

    override fun requestDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit) {
        if (Build.VERSION.SDK_INT < 33) throw UnsupportedOperationException()
        // API 33 destroy() clears queued commands. Bind through a support check first.
        check(request) { result ->
            if (closed) return@check
            if (result is SpeechSupportResult.Failure && result.code != SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT) {
                listener(SpeechDownloadEvent.Failure(result.code))
                return@check
            }
            try {
                recognizer.triggerModelDownload(SpeechRequestFactory.toIntent(request))
                // The framework's command is queued first on this same main looper.
                handler.post { if (!closed) listener(SpeechDownloadEvent.Requested) }
            } catch (_: Exception) {
                listener(SpeechDownloadEvent.Failure(SpeechRecognizer.ERROR_CLIENT))
            }
        }
    }

    override fun trackDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit) {
        if (Build.VERSION.SDK_INT < 34) throw UnsupportedOperationException()
        recognizer.triggerModelDownload(SpeechRequestFactory.toIntent(request), executor,
            object : ModelDownloadListener {
                override fun onProgress(completedPercent: Int) =
                    listener(SpeechDownloadEvent.Progress(completedPercent.coerceIn(0, 100)))
                override fun onScheduled() = listener(SpeechDownloadEvent.Scheduled)
                override fun onSuccess() = listener(SpeechDownloadEvent.Success)
                override fun onError(error: Int) = listener(SpeechDownloadEvent.Failure(error))
            })
    }

    override fun close() {
        if (closed) return
        closed = true
        recognizer.destroy()
    }
}
