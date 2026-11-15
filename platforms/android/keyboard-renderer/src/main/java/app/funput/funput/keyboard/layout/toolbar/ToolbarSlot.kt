package app.funput.funput.keyboard.layout.toolbar

/** A toolbar action the user can place on the leading or trailing side of the suggestion band. */
enum class ToolbarSlot {
    MICROPHONE,
    PLACEMENT,
    CLIPBOARD,
    EMOJI,
    ;

    /** Required slots keep their place when the row is tight; optional ones yield. */
    val yieldsWhenNarrow: Boolean
        get() = this == PLACEMENT || this == CLIPBOARD
}
