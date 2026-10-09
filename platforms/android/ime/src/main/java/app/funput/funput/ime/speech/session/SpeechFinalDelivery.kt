package app.funput.funput.ime.speech.session

/** The controller has already consumed the final; never retry or insert a partial on failure. */
internal class SpeechFinalDelivery(private val editor: SpeechEditorGateway) {
    fun commit(anchor: SpeechEditorAnchor, text: String, isCurrent: () -> Boolean): SpeechSessionState {
        val committed = runCatching { editor.isValid(anchor) && isCurrent() && editor.commit(anchor, text) }.getOrDefault(false)
        return if (committed) SpeechSessionState() else SpeechSessionState(
            phase = SpeechPhase.ERROR, error = SpeechError.COMMIT_REJECTED,
        )
    }

    companion object { const val MAX_TEXT_UNITS = 4_096 }
}
