package app.funput.funput.keyboard.popover.model

import app.funput.funput.keyboard.model.ShiftState

data class KeyAlternate(
    val text: String,
    val shiftedText: String = text.uppercase(),
    val accessibilityLabel: String = text,
) {
    init {
        require(text.isNotEmpty()) { "Alternate text must not be empty" }
        require(shiftedText.isNotEmpty()) { "Shifted alternate text must not be empty" }
        require(accessibilityLabel.isNotBlank()) { "Alternate accessibility label must not be blank" }
    }

    fun textFor(shiftState: ShiftState): String =
        if (shiftState.isActive) shiftedText else text
}
