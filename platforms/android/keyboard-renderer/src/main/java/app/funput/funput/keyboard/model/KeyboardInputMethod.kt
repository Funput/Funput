package app.funput.funput.keyboard.model

enum class KeyboardInputMethod {
    TELEX,
    TELEX_ADVANCED,
    VNI,

    ;

    val isTelexFamily: Boolean
        get() = this != VNI
}
