package app.funput.funput.ime.speech.preparation.platform

import app.funput.funput.ime.speech.platform.SpeechRequest
import app.funput.funput.ime.speech.preparation.SpeechDownloadEvent

internal sealed interface SpeechSupportResult {
    data class Languages(
        val installed: List<String>,
        val pending: List<String>,
        val supported: List<String>,
    ) : SpeechSupportResult
    data class Failure(val code: Int) : SpeechSupportResult
}

/** This client has no microphone commands. */
internal interface PreparationClient {
    fun check(request: SpeechRequest, listener: (SpeechSupportResult) -> Unit)
    fun requestDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit)
    fun trackDownload(request: SpeechRequest, listener: (SpeechDownloadEvent) -> Unit)
    fun close()
}

internal interface PreparationClientFactory {
    val apiLevel: Int
    fun isOnDeviceAvailable(): Boolean
    /** Creation cannot deliver operation callbacks. */
    fun create(): PreparationClient
}

internal fun interface PreparationCancellation { fun cancel() }
internal interface PreparationTime {
    fun nowMillis(): Long
    fun schedule(delayMillis: Long, task: () -> Unit): PreparationCancellation
}
