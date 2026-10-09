package app.funput.funput.ime.editing.typing

/** Tracks the first input during one visible keyboard session, across view recreation. */
internal class ImeTypingSession {
    private var active = false
    private var hasTyped = false
    private var onChanged: (Boolean) -> Unit = {}

    fun observe(observer: (Boolean) -> Unit) {
        onChanged = observer
        observer(hasTyped)
    }

    fun begin() {
        if (active) return
        active = true
        hasTyped = false
        onChanged(false)
    }

    fun end() { active = false }

    fun recordInput() {
        if (!active || hasTyped) return
        hasTyped = true
        onChanged(true)
    }
}
