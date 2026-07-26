package app.funput.funput.theme

/** Visual treatment applied to the painted surface of every keyboard key. */
enum class KeyboardKeySurfaceStyle {
    FLAT,
    GLASS,
    ;

    companion object {
        val Default: KeyboardKeySurfaceStyle = FLAT

        fun parseOrDefault(value: String?): KeyboardKeySurfaceStyle =
            entries.firstOrNull { style -> style.name == value } ?: Default
    }
}
