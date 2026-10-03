package app.funput.funput.ime.speech.preparation

/** Service presence is separate from locale/model readiness. */
enum class SpeechAvailability { AVAILABLE, UNAVAILABLE, UNSUPPORTED_OS, UNKNOWN }

enum class SpeechCapability { READY, PENDING, DOWNLOADABLE, UNSUPPORTED, UNKNOWN }

sealed interface SpeechDownloadEvent {
    /** API 33 request sent; the owner must check readiness again. */
    data object Requested : SpeechDownloadEvent
    data object Scheduled : SpeechDownloadEvent
    data class Progress(val percent: Int) : SpeechDownloadEvent
    data object Success : SpeechDownloadEvent
    data class Failure(val code: Int) : SpeechDownloadEvent
}
