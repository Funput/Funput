package app.funput.funput.theme

/** Visual treatment applied to the painted surface of every keyboard key. */
enum class KeyboardKeySurfaceStyle {
    /** Fill and border only. */
    FLAT,

    /** Flat plus a directional rim and a halo under a held key. */
    GLASS,

    /** Glass plus the lit body and lensed edge of Apple's Liquid Glass material. */
    LIQUID_GLASS,
    ;

    companion object {
        val Default: KeyboardKeySurfaceStyle = FLAT

        fun parseOrDefault(value: String?): KeyboardKeySurfaceStyle =
            entries.firstOrNull { style -> style.name == value } ?: Default
    }
}
