package app.funput.funput.ime.speech.model

internal sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data object Ended : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Failure(val code: Int) : SpeechEvent
}
