package app.funput.funput.keyboard.layout

data class KeyboardGeometrySpec(
    val horizontalPadding: Float,
    val verticalPadding: Float,
    val horizontalGap: Float,
    val verticalGap: Float,
    val horizontalGapRatio: Float,
    val verticalGapRatio: Float,
    val keyAspectRatio: Float,
    val suggestionBarHeight: Float,
    val suggestionBarGap: Float,
    val heightScale: Float = 1f,
) {
    init {
        require(horizontalPadding >= 0f) { "Horizontal padding must not be negative" }
        require(verticalPadding >= 0f) { "Vertical padding must not be negative" }
        require(horizontalGap >= 0f) { "Horizontal gap must not be negative" }
        require(verticalGap >= 0f) { "Vertical gap must not be negative" }
        require(horizontalGapRatio >= 0f) { "Horizontal gap ratio must not be negative" }
        require(verticalGapRatio >= 0f) { "Vertical gap ratio must not be negative" }
        require(keyAspectRatio > 0f) { "Key aspect ratio must be positive" }
        require(suggestionBarHeight > 0f) { "Suggestion bar height must be positive" }
        require(suggestionBarGap >= 0f) { "Suggestion bar gap must not be negative" }
        require(heightScale > 0f) { "Height scale must be positive" }
    }

    companion object {
        fun fromProfile(density: Float, profile: KeyboardSizingProfile): KeyboardGeometrySpec {
            require(density > 0f) { "Density must be positive" }
            // Vertical dimensions scale with heightScale so the rows fill the taller/shorter panel
            // that KeyboardDimensions produces for the profile; horizontal metrics stay width-driven.
            val heightScale = profile.heightScale
            return KeyboardGeometrySpec(
                horizontalPadding = profile.horizontalPaddingDp * density,
                verticalPadding = profile.verticalPaddingDp * density * heightScale,
                horizontalGap = 0f,
                verticalGap = 0f,
                horizontalGapRatio = profile.horizontalGapRatio,
                verticalGapRatio = profile.verticalGapRatio,
                keyAspectRatio = profile.keyAspectRatio,
                suggestionBarHeight = ToolbarMetrics.SuggestionBarHeightDp * density * heightScale,
                suggestionBarGap = ToolbarMetrics.SuggestionBarGapDp * density * heightScale,
                heightScale = heightScale,
            )
        }

        fun fromDensity(density: Float): KeyboardGeometrySpec =
            fromProfile(density, KeyboardSizingProfile.Default)
    }
}
