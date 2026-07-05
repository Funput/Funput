package app.funput.funput.keyboard.layout

/** Density-independent keyboard geometry preset shared by renderer and future Settings. */
data class KeyboardSizingProfile(
    val id: String,
    val horizontalPaddingDp: Float = 6f,
    val verticalPaddingDp: Float = 6f,
    val horizontalGapRatio: Float = 0.11f,
    val verticalGapRatio: Float = 0.16f,
    val keyAspectRatio: Float = 0.75f,
    val heightScale: Float = 1f,
    val labelScale: Float = 0.96f,
) {
    init {
        require(horizontalPaddingDp >= 0f) { "Horizontal padding must not be negative" }
        require(verticalPaddingDp >= 0f) { "Vertical padding must not be negative" }
        require(horizontalGapRatio >= 0f) { "Horizontal gap ratio must not be negative" }
        require(verticalGapRatio >= 0f) { "Vertical gap ratio must not be negative" }
        require(keyAspectRatio > 0f) { "Key aspect ratio must be positive" }
        require(heightScale > 0f) { "Height scale must be positive" }
        require(labelScale > 0f) { "Label scale must be positive" }
    }

    companion object {
        val Compact = KeyboardSizingProfile(
            id = "compact",
            heightScale = 0.92f,
            labelScale = 0.92f,
        )

        val Normal = KeyboardSizingProfile(id = "normal")

        val Large = KeyboardSizingProfile(
            id = "large",
            heightScale = 1.08f,
            labelScale = 1.04f,
        )

        val Default: KeyboardSizingProfile = Normal

        val Presets: List<KeyboardSizingProfile> = listOf(Compact, Normal, Large)

        fun fromId(id: String?): KeyboardSizingProfile =
            Presets.firstOrNull { it.id == id } ?: Default
    }
}
