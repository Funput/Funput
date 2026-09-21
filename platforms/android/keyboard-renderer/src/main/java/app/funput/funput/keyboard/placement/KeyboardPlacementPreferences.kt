package app.funput.funput.keyboard.placement

/** Where the keyboard content sits inside the IME window. */
enum class KeyboardPlacementMode(val storageId: String) {
    STANDARD("standard"),
    ELEVATED("elevated"),
    ;

    companion object {
        fun fromStorageId(value: String?): KeyboardPlacementMode =
            entries.firstOrNull { it.storageId == value } ?: STANDARD
    }
}

/** Persisted placement choice. Mode-specific values survive switching modes. */
data class KeyboardPlacementPreferences(
    val activeMode: KeyboardPlacementMode = KeyboardPlacementMode.STANDARD,
    val elevatedOffsetDp: Float = DefaultElevatedOffsetDp,
) {
    init {
        require(elevatedOffsetDp.isFinite() && elevatedOffsetDp >= 0f) {
            "Elevated keyboard offset must be finite and non-negative"
        }
    }

    companion object {
        const val DefaultElevatedOffsetDp = 96f
        val Default = KeyboardPlacementPreferences()
    }
}
