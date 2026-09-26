package app.funput.funput.keyboard.popover.model

import app.funput.funput.keyboard.model.ShiftState

sealed interface KeyAlternate {
    val accessibilityLabel: String

    data class Text(
        val text: String,
        val shiftedText: String = text.uppercase(),
        override val accessibilityLabel: String = text,
    ) : KeyAlternate {
        init {
            require(text.isNotEmpty()) { "Alternate text must not be empty" }
            require(shiftedText.isNotEmpty()) { "Shifted alternate text must not be empty" }
            require(accessibilityLabel.isNotBlank()) { "Alternate accessibility label must not be blank" }
        }

        fun textFor(shiftState: ShiftState): String =
            if (shiftState.isActive) shiftedText else text
    }

    enum class Action(override val accessibilityLabel: String) : KeyAlternate {
        PLACEMENT("Tùy chỉnh vị trí bàn phím"),
        SETTINGS("Cài đặt, mở ứng dụng Funput"),
    }
}
