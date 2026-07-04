package app.funput.funput.keyboard.model

/** Describes the focused editor so the renderer and IME can apply matching behavior. */
enum class KeyboardEditorMode(val supportsVietnameseComposition: Boolean) {
    TEXT(supportsVietnameseComposition = true),
    EMAIL(supportsVietnameseComposition = false),
    URL(supportsVietnameseComposition = false),
}
