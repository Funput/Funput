package app.funput.funput.ime.speech.mlkit

import android.speech.SpeechRecognizer as Platform
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException

/** Tracks byte totals so progress can be reported as a percentage. */
internal class MlKitDownloadMapper {
    private var total = 0L

    fun map(status: DownloadStatus): SpeechDownloadEvent? = when (status) {
        is DownloadStatus.DownloadStarted -> {
            total = status.bytesToDownload
            SpeechDownloadEvent.Progress(0)
        }
        is DownloadStatus.DownloadProgress -> if (total <= 0) null
            else SpeechDownloadEvent.Progress((status.totalBytesDownloaded * 100 / total).toInt().coerceIn(0, 100))
        is DownloadStatus.DownloadCompleted -> SpeechDownloadEvent.Success
        is DownloadStatus.DownloadFailed -> failure(status.e)
    }

    /**
     * Every download failure arrives as NOT_AVAILABLE; the reason only survives in the message.
     * These texts come from ML Kit genai-speech-recognition 1.0.0-alpha1: re-check them (and
     * MlKitMappingTest) whenever the dependency is upgraded.
     */
    private fun failure(error: GenAiException): SpeechDownloadEvent {
        val message = error.message.orEmpty()
        return when {
            // Speech Services queued the pack, usually because its consent dialog could not open.
            SCHEDULED in message -> SpeechDownloadEvent.Scheduled
            NO_INTERNET in message -> SpeechDownloadEvent.Failure(Platform.ERROR_NETWORK)
            MODEL_UNAVAILABLE in message -> SpeechDownloadEvent.Failure(Platform.ERROR_LANGUAGE_UNAVAILABLE)
            // NOT_AVAILABLE here is not "language unsupported" (that is MODEL_UNAVAILABLE above):
            // a generic download error stays retryable.
            else -> SpeechDownloadEvent.Failure(Platform.ERROR_CLIENT)
        }
    }

    private companion object {
        const val SCHEDULED = "retried automatically"
        const val NO_INTERNET = "END_REASON_NO_INTERNET_CONNECTION"
        const val MODEL_UNAVAILABLE = "END_REASON_MODEL_UNAVAILABLE"
    }
}
