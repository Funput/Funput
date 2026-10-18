package app.funput.funput.ui.settings.speech.model

/** Runs [task] after a delay; the returned handle cancels it. */
internal fun interface SpeechRecheckScheduler {
    fun schedule(delayMillis: Long, task: () -> Unit): AutoCloseable
}
