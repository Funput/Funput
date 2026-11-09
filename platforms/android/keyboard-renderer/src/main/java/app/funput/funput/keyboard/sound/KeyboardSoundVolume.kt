package app.funput.funput.keyboard.sound

/**
 * How loud a key press sounds, as a fraction of the system volume. It applies to every style,
 * the system click included.
 */
enum class KeyboardSoundVolume(val storageId: String, val gain: Float) {
    SOFT("soft", 0.3f),
    MEDIUM("medium", 0.5f),
    LOUD("loud", 1f),
    ;

    companion object {
        /** Medium, the fixed level Funput clicked at before volumes existed. */
        val Default = MEDIUM

        /** The volume stored as [value], or [Default] for a missing or unknown one. */
        fun fromStorage(value: String?): KeyboardSoundVolume =
            entries.firstOrNull { volume -> volume.storageId == value } ?: Default
    }
}
