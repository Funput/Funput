package app.funput.funput.keyboard.model

enum class ShiftState {
    OFF,
    ON,
    CAPS_LOCK,
    ;

    val isActive: Boolean get() = this != OFF
}
