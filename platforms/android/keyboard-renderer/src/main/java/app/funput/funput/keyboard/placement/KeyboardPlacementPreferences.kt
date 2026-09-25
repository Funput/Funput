package app.funput.funput.keyboard.placement

/** Where the keyboard content sits inside the IME window. */
enum class KeyboardPlacementMode(val storageId: String) {
    STANDARD("standard"),
    ELEVATED("elevated"),
    ONE_HANDED("one_handed"),
    ;

    companion object {
        fun fromStorageId(value: String?): KeyboardPlacementMode =
            entries.firstOrNull { it.storageId == value } ?: STANDARD
    }
}

enum class OneHandedSide(val storageId: String) {
    LEFT("left"),
    RIGHT("right"),
    ;

    companion object {
        fun fromStorageId(value: String?): OneHandedSide =
            entries.firstOrNull { it.storageId == value } ?: RIGHT
    }
}

/** Persisted placement choice. Mode-specific values survive switching modes. */
data class KeyboardPlacementPreferences(
    val activeMode: KeyboardPlacementMode = KeyboardPlacementMode.STANDARD,
    val elevatedOffsetDp: Float = DefaultElevatedOffsetDp,
    val oneHandedWidthFraction: Float = DefaultOneHandedWidthFraction,
    val oneHandedSide: OneHandedSide = OneHandedSide.RIGHT,
) {
    init {
        require(elevatedOffsetDp.isFinite() && elevatedOffsetDp >= 0f) {
            "Elevated keyboard offset must be finite and non-negative"
        }
        require(oneHandedWidthFraction in MinOneHandedWidthFraction..MaxOneHandedWidthFraction) {
            "One-handed keyboard width must be within its supported range"
        }
    }

    companion object {
        const val DefaultElevatedOffsetDp = 96f
        const val DefaultOneHandedWidthFraction = 0.80f
        const val MinOneHandedWidthFraction = 0.68f
        const val MaxOneHandedWidthFraction = 0.90f
        val Default = KeyboardPlacementPreferences()
    }
}
