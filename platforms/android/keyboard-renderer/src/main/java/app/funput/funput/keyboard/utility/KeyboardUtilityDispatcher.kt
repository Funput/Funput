package app.funput.funput.keyboard.utility

/** Renderer utilities share one path through touch, accessibility and alternates. */
enum class KeyboardUtilityAction {
    EMOJI, PLACEMENT, SETTINGS, CLIPBOARD_PANEL, CLIPBOARD_PASTE,
}

fun interface KeyboardUtilityDispatcher {
    fun dispatch(action: KeyboardUtilityAction)
}
