package app.funput.funput.keyboard

/**
 * How strongly a key press vibrates, chosen by the user. The on/off switch is separate, so turning
 * vibration off and on again keeps the strength.
 */
enum class KeyboardHapticStrength(val storageId: String) {
    LIGHT("light"),
    MEDIUM("medium"),
    STRONG("strong"),
    ;

    companion object {
        /**
         * Medium, the closest to the system key-press feedback Funput used before strengths existed,
         * so nobody's keyboard changes feel on update.
         */
        val Default = MEDIUM

        /** The strength stored as [value], or [Default] for a missing or unknown one. */
        fun fromStorage(value: String?): KeyboardHapticStrength =
            entries.firstOrNull { strength -> strength.storageId == value } ?: Default
    }
}
