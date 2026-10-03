package app.funput.funput.ime.speech.session

internal data class SpeechEditorAnchor(val generation: Long, val revision: Long, val caret: Int)

internal interface SpeechEditorGateway {
    fun prepareAnchor(listener: (SpeechEditorAnchor?) -> Unit): SpeechCancellation
    fun isValid(anchor: SpeechEditorAnchor): Boolean
    fun commit(anchor: SpeechEditorAnchor, text: String): Boolean
}

internal fun interface SpeechCancellation { fun cancel() }

internal fun interface SpeechClock { fun nowMillis(): Long }

internal fun interface SpeechScheduler {
    fun schedule(delayMillis: Long, task: () -> Unit): SpeechCancellation
}

internal enum class SpeechPhase { IDLE, PREPARING, LISTENING, FINALIZING, COMMITTING, ERROR }

internal enum class SpeechError {
    EDITOR_UNAVAILABLE, ANCHOR_TIMEOUT, BACKEND, READY_TIMEOUT, FINAL_TIMEOUT,
    EMPTY_FINAL, TEXT_TOO_LONG, COMMIT_REJECTED,
}

internal data class SpeechSessionState(
    val phase: SpeechPhase = SpeechPhase.IDLE,
    val preview: String = "",
    val error: SpeechError? = null,
    val backendErrorCode: Int? = null,
    val sessionId: Long = 0,
    val anchored: Boolean = false,
    val ready: Boolean = false,
    val stopRequested: Boolean = false,
    val stopIssued: Boolean = false,
)
