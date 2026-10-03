package app.funput.funput.keyboard.utility

/** Presentation only; visibility is supplied by the host after its capability/editor checks. */
data class KeyboardMicrophoneState(
    val visible: Boolean = false,
    val active: Boolean = false,
    val accessibilityLabel: String = "Nhập bằng giọng nói, Tiếng Việt",
) {
    init { require(accessibilityLabel.isNotBlank()) }
}
