package app.funput.funput.keyboard.layout

data class KeyboardGeometrySpec(
    val horizontalPadding: Float,
    val verticalPadding: Float,
    val horizontalGap: Float,
    val verticalGap: Float,
    val suggestionBarHeight: Float,
    val suggestionBarGap: Float,
) {
    init {
        require(horizontalPadding >= 0f) { "Horizontal padding must not be negative" }
        require(verticalPadding >= 0f) { "Vertical padding must not be negative" }
        require(horizontalGap >= 0f) { "Horizontal gap must not be negative" }
        require(verticalGap >= 0f) { "Vertical gap must not be negative" }
        require(suggestionBarHeight > 0f) { "Suggestion bar height must be positive" }
        require(suggestionBarGap >= 0f) { "Suggestion bar gap must not be negative" }
    }

    companion object {
        fun fromDensity(density: Float): KeyboardGeometrySpec {
            require(density > 0f) { "Density must be positive" }
            return KeyboardGeometrySpec(
                horizontalPadding = 7f * density,
                verticalPadding = 8f * density,
                horizontalGap = 5f * density,
                verticalGap = 6f * density,
                suggestionBarHeight = 42f * density,
                suggestionBarGap = 6f * density,
            )
        }
    }
}
