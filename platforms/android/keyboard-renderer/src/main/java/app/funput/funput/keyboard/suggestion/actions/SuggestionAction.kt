package app.funput.funput.keyboard.suggestion.actions

/** What a long press on a learned suggestion offers to do with the word. */
enum class SuggestionAction(
    internal val title: String,
    internal val accessibilityLabel: String,
) {
    /** Never learn or offer the word again. */
    BLOCK("Không gợi ý", "Không gợi ý từ này"),

    /** Drop what was learned; typing the word again relearns it. */
    FORGET("Quên", "Quên từ này"),
    ;

    /** The TalkBack custom action that does the same from a suggestion's node. */
    internal val accessibilityActionId: Int get() = AccessibilityActionBase + ordinal

    internal companion object {
        // Clear of SmartGestureAccessibility's 0x01030000 range.
        private const val AccessibilityActionBase = 0x01030100

        fun forAccessibilityAction(actionId: Int): SuggestionAction? =
            entries.firstOrNull { it.accessibilityActionId == actionId }
    }
}
