package app.funput.funput.ime.speech.mlkit

import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent
import com.google.mlkit.genai.common.DownloadStatus

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
        is DownloadStatus.DownloadFailed -> SpeechDownloadEvent.Failure(MlKitSpeech.errorCode(status.e))
    }
}
