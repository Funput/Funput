package app.funput.funput.theme

/** Stable identifiers for built-in keyboard theme presets. */
enum class KeyboardThemeId(val id: String) {
    DARK("dark"),
    LIGHT("light"),
    ;

    companion object {
        val Default: KeyboardThemeId = DARK

        val Presets: List<KeyboardThemeId> = entries

        fun fromId(id: String?): KeyboardThemeId =
            entries.firstOrNull { it.id == id } ?: Default
    }
}

/** Resolves built-in keyboard themes for renderer consumers. */
object KeyboardThemeCatalog {
    fun default(): KeyboardTheme = resolve(KeyboardThemeId.Default)

    fun dark(): KeyboardTheme = KeyboardThemes.Dark

    fun light(): KeyboardTheme = KeyboardThemes.Light

    fun resolve(id: KeyboardThemeId): KeyboardTheme = when (id) {
        KeyboardThemeId.DARK -> KeyboardThemes.Dark
        KeyboardThemeId.LIGHT -> KeyboardThemes.Light
    }
}
