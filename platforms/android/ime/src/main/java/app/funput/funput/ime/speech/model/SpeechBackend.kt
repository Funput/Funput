package app.funput.funput.ime.speech.model

internal interface SpeechBackend {
    fun open(locale: SpeechLocale, listener: (SpeechEvent) -> Unit): SpeechRecording
}

internal interface SpeechRecording {
    fun start()
    fun stop()
    fun cancel()
    fun close()
}
